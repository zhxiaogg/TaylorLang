package org.taylorlang.typechecker

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Typed program wrapper containing typed statements.
 */
data class TypedProgram(val statements: List<TypedStatement>)

/**
 * Refactored TypeChecker using the visitor pattern and strategy pattern.
 * 
 * This is a complete rewrite of the TypeChecker that leverages:
 * - Visitor pattern for AST traversal
 * - Strategy pattern for switching between type checking modes
 * - Modular design with separate concerns
 * - Clean separation of data models and logic
 * 
 * Key improvements:
 * - Reduced complexity through visitor decomposition
 * - Better testability with focused components
 * - Extensibility for future type checking features
 * - Clean error handling and reporting
 */
class RefactoredTypeChecker(
    private val mode: TypeCheckingMode = TypeCheckingMode.ALGORITHMIC
) {
    
    private val strategy: TypeCheckingStrategy = mode.createStrategy()
    
    /**
     * Type check a complete program.
     * 
     * This method orchestrates the type checking process:
     * 1. Creates initial context with built-in types
     * 2. First pass: collects type definitions and function signatures
     * 3. Second pass: type checks all statements with context accumulation
     * 4. Returns either a typed program or collected errors
     */
    fun typeCheck(program: Program): Result<TypedProgram> {
        val errors = mutableListOf<TypeError>()
        val context = TypeContext.withBuiltins()
        
        // First pass: collect type definitions and function signatures
        val contextWithDeclarations = collectDeclarations(program, context, errors)
        
        if (errors.isNotEmpty()) {
            return Result.failure(createMultipleErrorsOrSingle(errors))
        }
        
        // Second pass: type check all statements with context accumulation
        return typeCheckStatements(program.statements, contextWithDeclarations)
    }
    
    /**
     * Type check an expression with the configured strategy.
     */
    fun typeCheckExpression(
        expression: Expression,
        context: TypeContext
    ): Result<TypedExpression> {
        return strategy.typeCheckExpression(expression, context)
    }
    
    /**
     * Type check an expression with an expected type.
     */
    fun typeCheckExpressionWithExpected(
        expression: Expression,
        expectedType: Type,
        context: TypeContext
    ): Result<TypedExpression> {
        return strategy.typeCheckExpressionWithExpected(expression, expectedType, context)
    }
    
    /**
     * Collect constraints from an expression without solving them.
     * Only available for constraint-based mode.
     */
    fun collectConstraintsOnly(
        expression: Expression,
        context: TypeContext
    ): Result<ConstraintResult> {
        return when (strategy) {
            is ConstraintBasedTypeCheckingStrategy -> {
                try {
                    val inferenceContext = InferenceContext.fromTypeContext(context)
                    val constraintCollector = ConstraintCollector()
                    val constraintResult = constraintCollector.collectConstraints(expression, inferenceContext)
                    Result.success(constraintResult)
                } catch (e: Exception) {
                    Result.failure(TypeError.InvalidOperation(
                        "Constraint collection failed: ${e.message}",
                        emptyList(),
                        expression.sourceLocation
                    ))
                }
            }
            else -> Result.failure(TypeError.InvalidOperation(
                "Constraint collection is only available in constraint-based mode",
                emptyList(),
                expression.sourceLocation
            ))
        }
    }
    
    // =============================================================================
    // Private Implementation
    // =============================================================================
    
    /**
     * First pass: collect type definitions and function signatures.
     */
    private fun collectDeclarations(
        program: Program, 
        initialContext: TypeContext,
        errors: MutableList<TypeError>
    ): TypeContext {
        return program.statements.fold(initialContext) { ctx, statement ->
            when (statement) {
                is TypeDecl -> {
                    try {
                        val statementChecker = StatementTypeChecker(ctx)
                        val typeDef = statementChecker.createTypeDefinition(statement)
                        val newCtx = ctx.withType(statement.name, typeDef)
                        
                        // Also add each variant constructor as a function signature
                        addVariantConstructors(newCtx, statement, typeDef)
                    } catch (e: TypeError) {
                        errors.add(e)
                        ctx // Return unchanged context on error
                    }
                }
                is FunctionDecl -> {
                    val statementChecker = StatementTypeChecker(ctx)
                    val signatureResult = statementChecker.createFunctionSignature(statement)
                    signatureResult.fold(
                        onSuccess = { signature ->
                            ctx.withFunction(statement.name, signature)
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
                            ctx
                        }
                    )
                }
                else -> ctx
            }
        }
    }
    
    /**
     * Add variant constructors as function signatures.
     */
    private fun addVariantConstructors(
        context: TypeContext,
        typeDecl: TypeDecl,
        typeDef: TypeDefinition.UnionTypeDef
    ): TypeContext {
        return typeDef.variants.fold(context) { acc, variant ->
            val signature = FunctionSignature(
                typeParameters = typeDecl.typeParams.toList(),
                parameterTypes = variant.fields,
                returnType = Type.UnionType(
                    name = typeDecl.name,
                    typeArguments = typeDecl.typeParams.map { Type.NamedType(it) }.toPersistentList()
                )
            )
            acc.withFunction(variant.name, signature)
        }
    }
    
    /**
     * Second pass: type check all statements with context accumulation.
     */
    private fun typeCheckStatements(
        statements: List<Statement>,
        initialContext: TypeContext
    ): Result<TypedProgram> {
        val errors = mutableListOf<TypeError>()
        var currentContext = initialContext
        val typedStatements = mutableListOf<TypedStatement>()
        
        for (statement in statements) {
            val statementChecker = StatementTypeChecker(currentContext)
            val result = statement.accept(statementChecker)
            
            result.fold(
                onSuccess = { typedStatement ->
                    typedStatements.add(typedStatement)
                    
                    // Update context with new variable bindings from val declarations
                    if (typedStatement is TypedStatement.VariableDeclaration) {
                        currentContext = currentContext.withVariable(
                            typedStatement.declaration.name,
                            typedStatement.inferredType
                        )
                    }
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
                }
            )
        }
        
        return if (errors.isEmpty()) {
            Result.success(TypedProgram(typedStatements))
        } else {
            Result.failure(createMultipleErrorsOrSingle(errors))
        }
    }
    
    /**
     * Create a single error or multiple errors wrapper.
     */
    private fun createMultipleErrorsOrSingle(errors: List<TypeError>): TypeError {
        return if (errors.size == 1) {
            errors.first()
        } else {
            TypeError.MultipleErrors(errors)
        }
    }
    
    // =============================================================================
    // Companion Object - Factory Methods
    // =============================================================================
    
    companion object {
        /**
         * Create a TypeChecker instance configured for constraint-based type checking.
         */
        fun withConstraints(): RefactoredTypeChecker {
            return RefactoredTypeChecker(TypeCheckingMode.CONSTRAINT_BASED)
        }
        
        /**
         * Create a TypeChecker instance configured for algorithmic type checking.
         */
        fun algorithmic(): RefactoredTypeChecker {
            return RefactoredTypeChecker(TypeCheckingMode.ALGORITHMIC)
        }
        
        /**
         * Create a TypeChecker instance with the specified mode.
         */
        fun withMode(mode: TypeCheckingMode): RefactoredTypeChecker {
            return RefactoredTypeChecker(mode)
        }
    }
}