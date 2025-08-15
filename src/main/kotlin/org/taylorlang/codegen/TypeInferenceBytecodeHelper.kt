package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Helper class for type inference and type-related operations in bytecode generation.
 * 
 * This component centralizes all type-related logic that was previously scattered
 * throughout ExpressionBytecodeGenerator, providing:
 * - Expression type inference
 * - JVM type mapping and conversion
 * - Type checking predicates
 * - Primitive boxing operations
 * 
 * By extracting type operations, we achieve better separation of concerns and
 * make type-related bytecode logic reusable across different generators.
 */
class TypeInferenceBytecodeHelper(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager
) {
    
    /**
     * Infer the type of an expression based on its structure
     */
    fun inferExpressionType(expr: Expression): Type {
        return when (expr) {
            is Literal.IntLiteral -> BuiltinTypes.INT
            is Literal.FloatLiteral -> BuiltinTypes.DOUBLE
            is Literal.BooleanLiteral -> BuiltinTypes.BOOLEAN
            is Literal.StringLiteral -> BuiltinTypes.STRING
            is BinaryOp -> {
                // For binary operations, determine the result type
                when (expr.operator) {
                    BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
                    BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL,
                    BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL,
                    BinaryOperator.AND, BinaryOperator.OR -> BuiltinTypes.BOOLEAN
                    else -> {
                        // Arithmetic operations - handle string concatenation first
                        val leftType = inferExpressionType(expr.left)
                        val rightType = inferExpressionType(expr.right)
                        when {
                            leftType == BuiltinTypes.STRING || rightType == BuiltinTypes.STRING -> BuiltinTypes.STRING
                            isIntegerExpression(expr) -> BuiltinTypes.INT
                            else -> BuiltinTypes.DOUBLE
                        }
                    }
                }
            }
            is UnaryOp -> {
                when (expr.operator) {
                    UnaryOperator.NOT -> BuiltinTypes.BOOLEAN
                    UnaryOperator.MINUS -> inferExpressionType(expr.operand)
                }
            }
            is IfExpression -> {
                // For if expressions, determine the result type based on branches
                val thenType = inferExpressionType(expr.thenExpression)
                if (expr.elseExpression != null) {
                    val elseType = inferExpressionType(expr.elseExpression)
                    // Promote to the wider type if different
                    when {
                        thenType == BuiltinTypes.DOUBLE || elseType == BuiltinTypes.DOUBLE -> BuiltinTypes.DOUBLE
                        thenType == BuiltinTypes.STRING || elseType == BuiltinTypes.STRING -> BuiltinTypes.STRING
                        thenType == BuiltinTypes.BOOLEAN || elseType == BuiltinTypes.BOOLEAN -> BuiltinTypes.BOOLEAN
                        else -> thenType
                    }
                } else {
                    // No else branch - type is nullable or Unit
                    thenType
                }
            }
            is WhileExpression -> {
                // While loops return Unit
                BuiltinTypes.UNIT
            }
            is MatchExpression -> {
                // Match expressions return the common type of all case expressions
                if (expr.cases.isEmpty()) {
                    BuiltinTypes.UNIT
                } else {
                    // Find the common type of all case expressions
                    val caseTypes = expr.cases.map { inferExpressionType(it.expression) }
                    // For now, return the type of the first case
                    // In a full implementation, this would find the least upper bound
                    caseTypes.firstOrNull() ?: BuiltinTypes.UNIT
                }
            }
            is FunctionCall -> {
                // Infer function call return type
                when ((expr.target as? Identifier)?.name) {
                    "println" -> BuiltinTypes.UNIT // println returns void/unit
                    "emptyList" -> Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(BuiltinTypes.INT)) // Default to List<Int>
                    "singletonList" -> {
                        // Get the element type from the argument
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT // Default
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf" -> {
                        // Single element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf2" -> {
                        // Two element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf3" -> {
                        // Three element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    "listOf4" -> {
                        // Four element list
                        val elementType = if (expr.arguments.isNotEmpty()) {
                            inferExpressionType(expr.arguments[0])
                        } else {
                            BuiltinTypes.INT
                        }
                        Type.GenericType("List", kotlinx.collections.immutable.persistentListOf(elementType))
                    }
                    else -> BuiltinTypes.INT // Default for unknown functions
                }
            }
            is Identifier -> {
                // CRITICAL FIX: Look up the actual variable type from the slot manager
                // This prevents type mismatches in function calls and other expressions
                variableSlotManager.getType(expr.name) ?: BuiltinTypes.INT // Default to INT if not found
            }
            else -> BuiltinTypes.INT // Default fallback
        }
    }
    
    /**
     * Check if an expression evaluates to an integer
     */
    fun isIntegerExpression(expr: Expression): Boolean {
        return when (expr) {
            is Literal.IntLiteral -> true
            is BinaryOp -> isIntegerExpression(expr.left) && isIntegerExpression(expr.right)
            is Identifier -> {
                // CRITICAL FIX: Check if the identifier is bound to an integer type
                // This prevents arithmetic expressions with integer variables from defaulting to double
                val identifierType = variableSlotManager.getType(expr.name)
                identifierType == BuiltinTypes.INT
            }
            else -> false
        }
    }
    
    /**
     * Map TaylorLang type to JVM type descriptor
     */
    fun getJvmType(type: Type): String {
        return when (type) {
            is Type.PrimitiveType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    else -> "Ljava/lang/Object;"
                }
            }
            is Type.NamedType -> {
                when (type.name.lowercase()) {
                    "int" -> "I"
                    "double", "float" -> "D"
                    "boolean" -> "Z"
                    "string" -> "Ljava/lang/String;"
                    "unit", "void" -> "V"
                    else -> "Ljava/lang/Object;"
                }
            }
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Check if type is an integer type
     */
    fun isIntegerType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "int"
            is Type.NamedType -> type.name.lowercase() == "int"
            else -> false
        }
    }
    
    /**
     * Check if type is a double/float type
     */
    fun isDoubleType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() in listOf("double", "float")
            is Type.NamedType -> type.name.lowercase() in listOf("double", "float")
            else -> false
        }
    }
    
    /**
     * Check if type is a boolean type
     */
    fun isBooleanType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "boolean"
            is Type.NamedType -> type.name.lowercase() == "boolean"
            else -> false
        }
    }
    
    /**
     * Check if type is a string type
     */
    fun isStringType(type: Type): Boolean {
        return when (type) {
            is Type.PrimitiveType -> type.name.lowercase() == "string"
            is Type.NamedType -> type.name.lowercase() == "string"
            else -> false
        }
    }
    
    /**
     * CRITICAL FIX: Box primitive types to their Object wrapper classes.
     * This prevents VerifyError when passing primitives to methods expecting Objects.
     * Stack: [primitive_value] -> [Object_value]
     */
    fun boxPrimitiveToObject(type: Type) {
        when {
            isIntegerType(type) -> {
                // int -> Integer
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
                )
            }
            isDoubleType(type) -> {
                // double -> Double
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
            }
            isBooleanType(type) -> {
                // boolean (int 0/1) -> Boolean
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                )
            }
            // String and other Object types don't need boxing
            else -> {
                // Already an Object type - no boxing needed
            }
        }
    }
}