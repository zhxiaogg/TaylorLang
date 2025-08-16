package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*
import org.taylorlang.ast.visitor.BaseASTVisitor

/**
 * Typed statement wrappers for the result of statement type checking.
 * These classes maintain the original AST structure while adding type information.
 */
sealed class TypedStatement {
    data class FunctionDeclaration(
        val declaration: FunctionDecl,
        val body: TypedFunctionBody
    ) : TypedStatement()
    
    data class TypeDeclaration(
        val declaration: TypeDecl
    ) : TypedStatement()
    
    data class VariableDeclaration(
        val declaration: ValDecl,
        val initializer: TypedExpression,
        val inferredType: Type
    ) : TypedStatement()
    
    data class MutableVariableDeclaration(
        val declaration: VarDecl,
        val initializer: TypedExpression,
        val inferredType: Type
    ) : TypedStatement()
    
    data class Assignment(
        val assignment: org.taylorlang.ast.Assignment,
        val value: TypedExpression
    ) : TypedStatement()
    
    data class ExpressionStatement(
        val expression: TypedExpression
    ) : TypedStatement()
    
    data class ReturnStatement(
        val returnStatement: org.taylorlang.ast.ReturnStatement,
        val expression: TypedExpression?
    ) : TypedStatement()
}

/**
 * Typed function body variants.
 */
sealed class TypedFunctionBody {
    data class Expression(
        val expression: TypedExpression
    ) : TypedFunctionBody()
    
    data class Block(
        val statements: List<TypedStatement>
    ) : TypedFunctionBody()
}

/**
 * Visitor for type checking statements using the visitor pattern.
 * 
 * This visitor handles statement type checking logic, including:
 * - Function declaration validation
 * - Variable declaration type inference
 * - Type declaration processing
 * - Expression statement wrapping
 * - Statement context management
 * - Proper variable scoping with mutable/immutable distinction
 */
class StatementTypeChecker(
    private var context: TypeContext,
    private val scopeManager: ScopeManager = ScopeManager(),
    private val expressionStrategy: TypeCheckingStrategy? = null
) : BaseASTVisitor<Result<TypedStatement>>() {
    
    override fun defaultResult(): Result<TypedStatement> {
        return Result.failure(TypeError.InvalidOperation(
            "Unsupported statement type",
            emptyList(),
            null
        ))
    }
    
    override fun combine(first: Result<TypedStatement>, second: Result<TypedStatement>): Result<TypedStatement> {
        // For statement type checking, we typically don't combine results
        // Each statement visit should return its own result
        return second
    }
    
    // =============================================================================
    // Statement Type Checking
    // =============================================================================
    
    override fun visitStatement(node: Statement): Result<TypedStatement> {
        return when (node) {
            is FunctionDecl -> visitFunctionDecl(node)
            is TypeDecl -> visitTypeDecl(node)
            is ValDecl -> visitValDecl(node)
            is VarDecl -> visitVarDecl(node)
            is org.taylorlang.ast.Assignment -> visitAssignment(node)
            is org.taylorlang.ast.ReturnStatement -> visitReturnStatement(node)
            is Expression -> visitExpressionStatement(node)
        }
    }
    
    override fun visitExpression(node: Expression): Result<TypedStatement> {
        // Expressions as statements should be wrapped in ExpressionStatement
        return visitExpressionStatement(node)
    }
    
    // Override all expression visit methods to delegate to visitExpressionStatement
    // This ensures that any expression used as a statement gets properly wrapped
    override fun visitFunctionCall(node: FunctionCall): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitBinaryOp(node: BinaryOp): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitUnaryOp(node: UnaryOp): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitIdentifier(node: Identifier): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitLiteral(node: Literal): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitConstructorCall(node: ConstructorCall): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitIfExpression(node: IfExpression): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitMatchExpression(node: MatchExpression): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitBlockExpression(node: BlockExpression): Result<TypedStatement> {
        return visitExpressionStatement(node)
    }
    
    override fun visitWhileExpression(node: WhileExpression): Result<TypedStatement> {
        // CRITICAL FIX: Properly handle while expressions to preserve loop structure
        // Instead of using the default BaseASTVisitor behavior which processes condition and body separately,
        // we need to treat the WhileExpression as a unified expression that should be type-checked as a whole
        return visitExpressionStatement(node)
    }
    
    /**
     * Visit an expression as a statement.
     * This creates an expression statement wrapper.
     */
    private fun visitExpressionStatement(node: Expression): Result<TypedStatement> {
        val expressionChecker = ExpressionTypeChecker(context)
        return node.accept(expressionChecker).map { typedExpr ->
            TypedStatement.ExpressionStatement(typedExpr)
        }
    }
    
    override fun visitFunctionDecl(node: FunctionDecl): Result<TypedStatement> {
        val errors = mutableListOf<TypeError>()
        
        // Resolve parameter types and create context with function parameters
        val paramContext = node.parameters.fold(context) { ctx, param ->
            val paramType = param.type ?: run {
                errors.add(TypeError.UndefinedType(
                    "Missing type annotation for parameter '${param.name}'",
                    param.sourceLocation
                ))
                BuiltinTypes.UNIT
            }
            ctx.withVariable(param.name, paramType)
        }
        
        // Get the declared return type
        val declaredReturnType = node.returnType ?: BuiltinTypes.UNIT
        
        // Type check function body
        val typedBody = when (node.body) {
            is FunctionBody.ExpressionBody -> {
                // CRITICAL FIX: Use the strategy-based approach instead of ExpressionTypeChecker
                // This ensures TryExpression unwrapping works correctly in constraint-based mode
                val expressionResult = if (expressionStrategy != null) {
                    // Use strategy with expected return type to enable try expression validation
                    expressionStrategy.typeCheckExpressionWithExpected(
                        node.body.expression, 
                        declaredReturnType, 
                        paramContext
                    )
                } else {
                    // Fallback to ExpressionTypeChecker for algorithmic mode
                    val expressionChecker = ExpressionTypeChecker(paramContext)
                    node.body.expression.accept(expressionChecker)
                }
                
                expressionResult.fold(
                    onSuccess = { typedExpr ->
                        // Validate return type matches function body type
                        if (!TypeOperations.areEqual(typedExpr.type, declaredReturnType)) {
                            errors.add(TypeError.TypeMismatch(
                                expected = declaredReturnType,
                                actual = typedExpr.type,
                                location = node.body.expression.sourceLocation
                            ))
                        }
                        TypedFunctionBody.Expression(typedExpr)
                    },
                    onFailure = { error ->
                        errors.add(when (error) {
                            is TypeError -> error
                            else -> TypeError.InvalidOperation(
                                error.message ?: "Unknown error", 
                                emptyList(), 
                                null
                            )
                        })
                        null
                    }
                )
            }
            is FunctionBody.BlockBody -> {
                // For block bodies, validate the return type is Unit unless explicitly declared
                if (declaredReturnType != BuiltinTypes.UNIT) {
                    errors.add(TypeError.TypeMismatch(
                        expected = declaredReturnType,
                        actual = BuiltinTypes.UNIT,
                        location = node.sourceLocation
                    ))
                }
                TypedFunctionBody.Block(emptyList())
            }
        }
        
        return if (errors.isEmpty() && typedBody != null) {
            Result.success(TypedStatement.FunctionDeclaration(
                node.copy(),
                typedBody
            ))
        } else {
            val error = if (errors.size == 1) {
                errors.first()
            } else {
                TypeError.MultipleErrors(errors)
            }
            Result.failure(error)
        }
    }
    
    override fun visitTypeDecl(node: TypeDecl): Result<TypedStatement> {
        // Type declarations are processed during context building phase
        // Here we just validate and wrap them
        try {
            validateTypeDeclaration(node)
            return Result.success(TypedStatement.TypeDeclaration(node))
        } catch (error: TypeError) {
            return Result.failure(error)
        }
    }
    
    override fun visitValDecl(node: ValDecl): Result<TypedStatement> {
        // Use the provided expression strategy if available, otherwise fall back to ExpressionTypeChecker
        val typedInitializerResult = if (expressionStrategy != null) {
            expressionStrategy.typeCheckExpression(node.initializer, context)
        } else {
            val expressionChecker = ExpressionTypeChecker(context)
            node.initializer.accept(expressionChecker)
        }
        
        return typedInitializerResult.mapCatching { typedInitializer ->
            val inferredType = typedInitializer.type
            
            // Check declared type matches inferred type if provided
            val finalType = node.type?.let { declaredType ->
                if (!TypeOperations.areEqual(declaredType, inferredType)) {
                    throw TypeError.TypeMismatch(
                        expected = declaredType,
                        actual = inferredType,
                        location = node.sourceLocation
                    )
                }
                declaredType
            } ?: inferredType
            
            // Register immutable variable in scope manager
            scopeManager.declareVariable(
                name = node.name,
                type = finalType,
                isMutable = false,
                location = node.sourceLocation
            ).getOrThrow()
            
            // Also add to TypeContext for identifier resolution
            context = context.withVariable(node.name, finalType)
            
            TypedStatement.VariableDeclaration(
                node.copy(),
                typedInitializer,
                finalType
            )
        }
    }
    
    override fun visitVarDecl(node: VarDecl): Result<TypedStatement> {
        // Use the provided expression strategy if available, otherwise fall back to ExpressionTypeChecker
        val typedInitializerResult = if (expressionStrategy != null) {
            expressionStrategy.typeCheckExpression(node.initializer, context)
        } else {
            val expressionChecker = ExpressionTypeChecker(context)
            node.initializer.accept(expressionChecker)
        }
        
        return typedInitializerResult.mapCatching { typedInitializer ->
            val inferredType = typedInitializer.type
            
            // Check declared type matches inferred type if provided
            val finalType = node.type?.let { declaredType ->
                if (!TypeOperations.areEqual(declaredType, inferredType)) {
                    throw TypeError.TypeMismatch(
                        expected = declaredType,
                        actual = inferredType,
                        location = node.sourceLocation
                    )
                }
                declaredType
            } ?: inferredType
            
            // Register mutable variable in scope manager
            scopeManager.declareVariable(
                name = node.name,
                type = finalType,
                isMutable = true,
                location = node.sourceLocation
            ).getOrThrow()
            
            // Also add to TypeContext for identifier resolution
            context = context.withVariable(node.name, finalType)
            
            TypedStatement.MutableVariableDeclaration(
                node.copy(),
                typedInitializer,
                finalType
            )
        }
    }
    
    override fun visitAssignment(node: org.taylorlang.ast.Assignment): Result<TypedStatement> {
        // Check that the variable exists and is mutable
        val variableType = scopeManager.getVariableType(node.variable, node.sourceLocation).getOrElse { error ->
            return Result.failure(error)
        }
        
        val isMutable = scopeManager.isVariableMutable(node.variable, node.sourceLocation).getOrElse { error ->
            return Result.failure(error)
        }
        
        if (!isMutable) {
            return Result.failure(TypeError.InvalidOperation(
                "Cannot assign to immutable variable '${node.variable}'",
                emptyList(),
                node.sourceLocation
            ))
        }
        
        // Type check the assigned value
        val expressionChecker = ExpressionTypeChecker(context)
        return node.value.accept(expressionChecker).mapCatching { typedValue ->
            // Check type compatibility
            if (!TypeOperations.areEqual(variableType, typedValue.type)) {
                throw TypeError.TypeMismatch(
                    expected = variableType,
                    actual = typedValue.type,
                    location = node.sourceLocation
                )
            }
            
            TypedStatement.Assignment(
                node.copy(),
                typedValue
            )
        }
    }
    
    override fun visitReturnStatement(node: org.taylorlang.ast.ReturnStatement): Result<TypedStatement> {
        // Type check the return expression if present
        val typedExpression = node.expression?.let { expr ->
            val expressionChecker = ExpressionTypeChecker(context)
            expr.accept(expressionChecker).getOrNull()
        }
        
        // For now, we don't validate the return type against the function's declared return type
        // That validation will be done during comprehensive function type checking
        return Result.success(TypedStatement.ReturnStatement(
            node.copy(),
            typedExpression
        ))
    }
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    /**
     * Validate a type declaration for correctness.
     * This includes checking for duplicate variant names and other validation rules.
     */
    private fun validateTypeDeclaration(typeDecl: TypeDecl) {
        // Validate that variant names are unique within the union
        val variantNames = mutableSetOf<String>()
        val duplicateVariants = mutableListOf<String>()
        
        typeDecl.unionType.variants.forEach { variant ->
            if (!variantNames.add(variant.name)) {
                duplicateVariants.add(variant.name)
            }
        }
        
        if (duplicateVariants.isNotEmpty()) {
            throw TypeError.DuplicateDefinition(
                "Duplicate variant names in union type '${typeDecl.name}': ${duplicateVariants.joinToString(", ")}",
                typeDecl.sourceLocation
            )
        }
    }
    
    /**
     * Create a type definition from a type declaration.
     * This converts the AST representation to the internal type definition format.
     */
    fun createTypeDefinition(typeDecl: TypeDecl): TypeDefinition.UnionTypeDef {
        validateTypeDeclaration(typeDecl)
        
        val variants = typeDecl.unionType.variants.map { variant ->
            val types = when (variant) {
                is ProductType.Positioned -> variant.types
                is ProductType.Named -> variant.fields.map { it.type }
            }
            VariantDef(variant.name, types)
        }
        return TypeDefinition.UnionTypeDef(typeDecl.typeParams.toList(), variants)
    }
    
    /**
     * Create a function signature from a function declaration.
     */
    fun createFunctionSignature(function: FunctionDecl): Result<FunctionSignature> {
        val paramTypes = function.parameters.map { param ->
            param.type ?: BuiltinTypes.UNIT // Default to Unit if no type specified
        }
        
        val returnType = function.returnType ?: BuiltinTypes.UNIT
        
        return Result.success(FunctionSignature(
            typeParameters = function.typeParams.toList(),
            parameterTypes = paramTypes,
            returnType = returnType
        ))
    }
    
}