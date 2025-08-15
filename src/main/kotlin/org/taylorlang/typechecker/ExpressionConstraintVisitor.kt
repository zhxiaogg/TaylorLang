package org.taylorlang.typechecker

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.*

/**
 * Specialized visitor for collecting constraints from expressions.
 * 
 * This visitor handles all expression-related constraint collection including:
 * - Literal expressions (int, float, string, boolean, null, tuple)
 * - Binary and unary operations with type promotion
 * - Function calls with signature resolution
 * - Property and index access
 * - Variable identifier resolution
 * 
 * Designed to work as part of a modular constraint collection system where
 * the main ConstraintCollector coordinates between specialized visitors.
 */
class ExpressionConstraintVisitor(
    private val collector: ConstraintCollector
) {
    
    // =============================================================================
    // Literal Expression Handlers
    // =============================================================================
    
    fun handleIntLiteral(
        literal: Literal.IntLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.INT
        
        return if (expectedType != null) {
            // Checking mode: generate equality constraint
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            // Synthesis mode: just return the literal type
            ConstraintResult.withType(literalType)
        }
    }
    
    fun handleFloatLiteral(
        literal: Literal.FloatLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.DOUBLE
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            ConstraintResult.withType(literalType)
        }
    }
    
    fun handleStringLiteral(
        literal: Literal.StringLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.STRING
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            ConstraintResult.withType(literalType)
        }
    }
    
    fun handleBooleanLiteral(
        literal: Literal.BooleanLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        val literalType = BuiltinTypes.BOOLEAN
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(literalType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(literalType, constraint)
        } else {
            ConstraintResult.withType(literalType)
        }
    }
    
    fun handleNullLiteral(
        literal: Literal.NullLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        return if (expectedType != null) {
            // Null can inhabit any nullable type, so we need to ensure expected type is nullable
            val nullableConstraintType = when (expectedType) {
                is Type.NullableType -> expectedType.baseType
                else -> {
                    // Create a fresh type variable for the base type
                    val freshVar = TypeVar.fresh()
                    val baseType = Type.NamedType(freshVar.id)  // Convert TypeVar to Type representation
                    
                    // Generate constraint that expected type equals nullable version of base type
                    val nullableExpected = Type.NullableType(baseType)
                    val constraint = Constraint.Equality(expectedType, nullableExpected, literal.sourceLocation)
                    
                    return ConstraintResult.withConstraint(nullableExpected, constraint)
                }
            }
            val nullableType = Type.NullableType(nullableConstraintType)
            val constraint = Constraint.Equality(nullableType, expectedType, literal.sourceLocation)
            ConstraintResult.withConstraint(nullableType, constraint)
        } else {
            // In synthesis mode, create a nullable type with fresh type variable
            val freshVar = TypeVar.fresh()
            val baseType = Type.NamedType(freshVar.id)
            val nullableType = Type.NullableType(baseType)
            ConstraintResult.withType(nullableType)
        }
    }
    
    fun handleTupleLiteral(
        literal: Literal.TupleLiteral,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from all tuple elements
        val elementResults = literal.elements.map { element ->
            collector.collectConstraints(element, context)
        }
        
        // Merge all constraints from elements
        val allConstraints = elementResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        
        // Extract element types
        val elementTypes = elementResults.map { it.type }.toPersistentList()
        val tupleType = Type.TupleType(elementTypes)
        
        return if (expectedType != null) {
            // Generate equality constraint with expected type
            val constraint = Constraint.Equality(tupleType, expectedType, literal.sourceLocation)
            ConstraintResult(tupleType, allConstraints.add(constraint))
        } else {
            ConstraintResult(tupleType, allConstraints)
        }
    }
    
    
    // =============================================================================
    // Variable and Identifier Handlers
    // =============================================================================
    
    fun handleIdentifier(
        identifier: Identifier,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Look up the variable in the context
        val scheme = context.lookupVariable(identifier.name)
        
        if (scheme != null) {
            // Variable found - instantiate its type scheme
            val (instanceType, instanceConstraints) = collector.instantiateScheme(scheme)
            
            return if (expectedType != null) {
                // Checking mode: add equality constraint
                val constraint = Constraint.Equality(instanceType, expectedType, identifier.sourceLocation)
                ConstraintResult(instanceType, instanceConstraints.add(constraint))
            } else {
                // Synthesis mode: return instantiated type
                ConstraintResult(instanceType, instanceConstraints)
            }
        } else {
            // Variable not found - check if it's a zero-argument constructor
            val functionSig = context.lookupFunctionSignature(identifier.name)
            
            if (functionSig != null && functionSig.parameterTypes.isEmpty()) {
                // Treat as constructor call with no arguments
                val constructorCall = ConstructorCall(
                    constructor = identifier.name,
                    arguments = persistentListOf(),
                    sourceLocation = identifier.sourceLocation
                )
                return collector.handleConstructorCall(constructorCall, expectedType, context)
            } else {
                // Unresolved symbol - create a fresh type variable and add error constraint
                val freshVar = TypeVar.fresh()
                val unknownType = Type.NamedType(freshVar.id)
                
                // TODO: In a full implementation, we'd collect type errors separately
                // For now, we'll continue with type inference using the fresh variable
                
                return if (expectedType != null) {
                    val constraint = Constraint.Equality(unknownType, expectedType, identifier.sourceLocation)
                    ConstraintResult.withConstraint(unknownType, constraint)
                } else {
                    ConstraintResult.withType(unknownType)
                }
            }
        }
    }
    
    // =============================================================================
    // Binary and Unary Operation Handlers
    // =============================================================================
    
    fun handleBinaryOp(
        binaryOp: BinaryOp,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from left and right operands
        val leftResult = collector.collectConstraints(binaryOp.left, context)
        val rightResult = collector.collectConstraints(binaryOp.right, context)
        
        // Merge operand constraints
        val baseConstraints = leftResult.constraints.merge(rightResult.constraints)
        
        // Generate additional constraints based on operator type
        val (resultType, operatorConstraints) = generateBinaryOpConstraints(
            binaryOp.operator,
            leftResult.type,
            rightResult.type,
            binaryOp.sourceLocation
        )
        
        val allConstraints = baseConstraints.merge(operatorConstraints)
        
        return if (expectedType != null) {
            // Add equality constraint with expected type
            val constraint = Constraint.Equality(resultType, expectedType, binaryOp.sourceLocation)
            ConstraintResult(resultType, allConstraints.add(constraint))
        } else {
            ConstraintResult(resultType, allConstraints)
        }
    }
    
    fun handleUnaryOp(
        unaryOp: UnaryOp,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from operand
        val operandResult = collector.collectConstraints(unaryOp.operand, context)
        
        // Generate additional constraints based on operator type
        val (resultType, operatorConstraints) = generateUnaryOpConstraints(
            unaryOp.operator,
            operandResult.type,
            unaryOp.sourceLocation
        )
        
        val allConstraints = operandResult.constraints.merge(operatorConstraints)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, unaryOp.sourceLocation)
            ConstraintResult(resultType, allConstraints.add(constraint))
        } else {
            ConstraintResult(resultType, allConstraints)
        }
    }
    
    // =============================================================================
    // Function Call Handlers
    // =============================================================================
    
    fun handleFunctionCall(
        call: FunctionCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Extract function name from target (assuming it's an Identifier for now)
        val functionName = when (call.target) {
            is Identifier -> call.target.name
            else -> {
                // For complex expressions, we'd need to handle them differently
                // For now, create a fresh type and continue
                val freshVar = TypeVar.fresh()
                return ConstraintResult.withType(Type.NamedType(freshVar.id))
            }
        }
        
        // Look up function signature
        val functionSignature = context.lookupFunctionSignature(functionName)
        
        if (functionSignature != null) {
            return handleTypedFunctionCall(call, functionSignature, expectedType, context)
        } else {
            // Unknown function - create fresh type variables and generate constraints
            return handleUnknownFunctionCall(call, expectedType, context)
        }
    }
    
    private fun handleTypedFunctionCall(
        call: FunctionCall,
        signature: FunctionSignature,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from arguments
        val argumentResults = call.arguments.map { arg ->
            collector.collectConstraints(arg, context)
        }
        
        // Merge argument constraints
        val baseConstraints = argumentResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        
        // Generate type constraints for function application
        val (resultType, callConstraints) = generateFunctionCallConstraints(
            signature,
            argumentResults.map { it.type },
            call.sourceLocation
        )
        
        val allConstraints = baseConstraints.merge(callConstraints)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, call.sourceLocation)
            ConstraintResult(resultType, allConstraints.add(constraint))
        } else {
            ConstraintResult(resultType, allConstraints)
        }
    }
    
    private fun handleUnknownFunctionCall(
        call: FunctionCall,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from arguments
        val argumentResults = call.arguments.map { arg ->
            collector.collectConstraints(arg, context)
        }
        
        // Create fresh type variables for function signature
        val paramTypes = argumentResults.map { 
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        val returnType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        // Generate equality constraints between argument types and parameter types
        val argumentConstraints = argumentResults.zip(paramTypes).map { (argResult, paramType) ->
            Constraint.Equality(argResult.type, paramType, call.sourceLocation)
        }
        
        // Merge all constraints
        val baseConstraints = argumentResults.fold(ConstraintSet.empty()) { acc, result ->
            acc.merge(result.constraints)
        }
        val allConstraints = argumentConstraints.fold(baseConstraints) { acc, constraint ->
            acc.add(constraint)
        }
        
        return ConstraintResult(returnType, allConstraints)
    }
    
    // =============================================================================
    // Property and Index Access Handlers
    // =============================================================================
    
    fun handlePropertyAccess(
        access: PropertyAccess,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from target expression
        val targetResult = collector.collectConstraints(access.target, context)
        
        // For now, create fresh type variable for property access result
        // In a full implementation, this would look up field types from type definitions
        val freshVar = TypeVar.fresh()
        val resultType = Type.NamedType(freshVar.id)
        
        return if (expectedType != null) {
            val constraint = Constraint.Equality(resultType, expectedType, access.sourceLocation)
            ConstraintResult(resultType, targetResult.constraints.add(constraint))
        } else {
            ConstraintResult(resultType, targetResult.constraints)
        }
    }
    
    fun handleIndexAccess(
        access: IndexAccess,
        expectedType: Type?,
        context: InferenceContext
    ): ConstraintResult {
        // Collect constraints from target and index expressions
        val targetResult = collector.collectConstraints(access.target, context)
        val indexResult = collector.collectConstraints(access.index, context)
        
        val baseConstraints = targetResult.constraints.merge(indexResult.constraints)
        
        // Create fresh type variables for indexing operation
        val elementType = expectedType ?: run {
            val freshVar = TypeVar.fresh()
            Type.NamedType(freshVar.id)
        }
        
        // Generate constraint that target is indexable with the index type
        // For now, we'll assume integer indexing into collections
        val indexConstraint = Constraint.Equality(indexResult.type, BuiltinTypes.INT, access.sourceLocation)
        
        val allConstraints = baseConstraints.add(indexConstraint)
        
        return ConstraintResult(elementType, allConstraints)
    }
    
    // =============================================================================
    // Constraint Generation Helper Methods
    // =============================================================================
    
    /**
     * Generate constraints for binary operations based on operator type.
     */
    private fun generateBinaryOpConstraints(
        operator: BinaryOperator,
        leftType: Type,
        rightType: Type,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        return when (operator) {
            BinaryOperator.PLUS, BinaryOperator.MINUS, 
            BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE, BinaryOperator.MODULO -> {
                // Arithmetic operations: proper numeric type inference
                val constraints = mutableListOf<Constraint>()
                
                // Handle string concatenation for PLUS operator
                if (operator == BinaryOperator.PLUS) {
                    // If either operand is String, result is String
                    if (TypeOperations.areEqual(leftType, BuiltinTypes.STRING) || 
                        TypeOperations.areEqual(rightType, BuiltinTypes.STRING)) {
                        val resultType = BuiltinTypes.STRING
                        return Pair(resultType, ConstraintSet.empty())
                    }
                }
                
                // For numeric operations, determine the result type based on operands
                val resultType = when {
                    // If both types are the same primitive, return that type
                    TypeOperations.areEqual(leftType, rightType) && 
                    BuiltinTypes.isNumeric(leftType) -> leftType
                    
                    // Use type promotion rules for mixed types
                    BuiltinTypes.isNumeric(leftType) && BuiltinTypes.isNumeric(rightType) -> {
                        BuiltinTypes.getWiderNumericType(leftType, rightType) ?: BuiltinTypes.DOUBLE
                    }
                    
                    // For type variables or unresolved types, use constraints
                    else -> {
                        // Generate subtype constraints to ensure operands are numeric
                        constraints.add(Constraint.Subtype(leftType, BuiltinTypes.DOUBLE, location))
                        constraints.add(Constraint.Subtype(rightType, BuiltinTypes.DOUBLE, location))
                        BuiltinTypes.DOUBLE // Default fallback
                    }
                }
                
                Pair(resultType, ConstraintSet.fromCollection(constraints))
            }
            
            BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL -> {
                // Comparison operations: require numeric types, result is Boolean
                val constraints = mutableListOf<Constraint>()
                
                // Always generate subtype constraints for consistent behavior
                constraints.add(Constraint.Subtype(leftType, BuiltinTypes.DOUBLE, location))
                constraints.add(Constraint.Subtype(rightType, BuiltinTypes.DOUBLE, location))
                
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.fromCollection(constraints))
            }
            
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> {
                // Equality operations: operands should be same type, result is Boolean
                val equalityConstraint = Constraint.Equality(leftType, rightType, location)
                
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.of(equalityConstraint))
            }
            
            BinaryOperator.AND, BinaryOperator.OR -> {
                // Logical operations: operands should be Boolean, result is Boolean
                val boolConstraint1 = Constraint.Equality(leftType, BuiltinTypes.BOOLEAN, location)
                val boolConstraint2 = Constraint.Equality(rightType, BuiltinTypes.BOOLEAN, location)
                
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.of(boolConstraint1, boolConstraint2))
            }
            
            BinaryOperator.NULL_COALESCING -> {
                // Null coalescing: left should be nullable, result is right type
                val freshVar = TypeVar.fresh()
                val baseType = Type.NamedType(freshVar.id)
                val nullableType = Type.NullableType(baseType)
                val nullableConstraint = Constraint.Equality(leftType, nullableType, location)
                
                Pair(rightType, ConstraintSet.of(nullableConstraint))
            }
        }
    }
    
    /**
     * Generate constraints for unary operations based on operator type.
     */
    private fun generateUnaryOpConstraints(
        operator: UnaryOperator,
        operandType: Type,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        return when (operator) {
            UnaryOperator.MINUS -> {
                // Numeric negation: operand should be numeric, result is same type
                val numericConstraint = Constraint.Subtype(operandType, BuiltinTypes.DOUBLE, location)
                Pair(operandType, ConstraintSet.of(numericConstraint))
            }
            
            UnaryOperator.NOT -> {
                // Logical negation: operand should be Boolean, result is Boolean
                val boolConstraint = Constraint.Equality(operandType, BuiltinTypes.BOOLEAN, location)
                Pair(BuiltinTypes.BOOLEAN, ConstraintSet.of(boolConstraint))
            }
        }
    }
    
    /**
     * Generate constraints for function calls with known signatures.
     */
    private fun generateFunctionCallConstraints(
        signature: FunctionSignature,
        argumentTypes: List<Type>,
        location: SourceLocation?
    ): Pair<Type, ConstraintSet> {
        // Generate constraints for each argument
        val constraints = signature.parameterTypes.zip(argumentTypes).map { (paramType, argType) ->
            Constraint.Subtype(argType, paramType, location)
        }
        
        // Handle generic functions by instantiating type parameters
        val resultType = if (signature.typeParameters.isNotEmpty()) {
            // Create fresh type variables for type parameters
            val typeVarSubstitution = signature.typeParameters.associateWith {
                val freshVar = TypeVar.fresh()
                Type.NamedType(freshVar.id)
            }
            
            // Substitute type parameters in return type
            collector.substituteTypeVariables(signature.returnType, typeVarSubstitution)
        } else {
            signature.returnType
        }
        
        return Pair(resultType, ConstraintSet.fromCollection(constraints))
    }
}