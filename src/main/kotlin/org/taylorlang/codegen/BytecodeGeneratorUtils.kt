package org.taylorlang.codegen

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Utility class for low-level JVM bytecode generation operations.
 * 
 * This class provides:
 * - Stack management utilities
 * - Label and jump generation
 * - Method call generation
 * - Common bytecode patterns
 */
class BytecodeGeneratorUtils(
    private val methodVisitor: MethodVisitor,
    private val typeConverter: TypeConverter
) {
    
    /**
     * Generate consistent stack state for match expression branches.
     * This prevents VerifyError by ensuring all branches leave the same stack state.
     * 
     * @param resultType The expected result type of the match expression
     * @param caseExprType The actual type produced by the case expression
     * @param leavesValueOnStack Whether the case expression leaves a value on the stack
     */
    fun ensureConsistentStackState(
        resultType: Type,
        caseExprType: Type,
        leavesValueOnStack: Boolean
    ) {
        val resultJvmType = typeConverter.getJvmType(resultType)
        
        if (resultJvmType == TypeConverter.VOID_TYPE) {
            // Match expression expects void - pop any values left on stack
            if (leavesValueOnStack) {
                val caseJvmType = typeConverter.getJvmType(caseExprType)
                if (caseJvmType == TypeConverter.DOUBLE_PRIMITIVE_TYPE) {
                    methodVisitor.visitInsn(POP2) // Pop double value (2 slots)
                } else {
                    methodVisitor.visitInsn(POP) // Pop single value
                }
            }
            // Stack is now empty for all branches - consistent!
        } else {
            // Match expression expects a value - ensure we have one
            if (!leavesValueOnStack) {
                // Push default value to match expected result type
                typeConverter.generateDefaultValue(resultType)
            } else {
                // CRITICAL FIX FOR VERIFYERROR: Ensure stack value type matches expected result type
                val caseJvmType = typeConverter.getJvmType(caseExprType)
                if (caseJvmType != resultJvmType) {
                    typeConverter.convertType(caseExprType, resultType)
                }
            }
        }
    }
    
    /**
     * Check if an expression leaves a value on the stack.
     */
    fun expressionLeavesValueOnStack(expression: Expression, expressionType: Type): Boolean {
        return when (expression) {
            is FunctionCall -> {
                if ((expression.target as? Identifier)?.name == "println") {
                    false // println returns void
                } else {
                    typeConverter.getJvmType(expressionType) != TypeConverter.VOID_TYPE
                }
            }
            else -> typeConverter.getJvmType(expressionType) != TypeConverter.VOID_TYPE
        }
    }
    
    /**
     * Generate a safe default value for any type when pattern matching fails.
     */
    fun generateMatchFailureDefault(resultType: Type) {
        when (typeConverter.getJvmType(resultType)) {
            TypeConverter.VOID_TYPE -> { /* No value needed for void */ }
            TypeConverter.INT_TYPE, TypeConverter.BOOLEAN_PRIMITIVE_TYPE -> {
                methodVisitor.visitLdcInsn(0)
            }
            TypeConverter.DOUBLE_PRIMITIVE_TYPE -> {
                methodVisitor.visitLdcInsn(0.0)
            }
            TypeConverter.STRING_TYPE -> {
                methodVisitor.visitLdcInsn("")
            }
            else -> {
                methodVisitor.visitInsn(ACONST_NULL)
            }
        }
    }
    
    /**
     * Generate labels for match expression cases.
     */
    fun generateCaseLabels(caseCount: Int): Pair<List<Label>, Label> {
        val caseBodyLabels = (0 until caseCount).map { Label() }
        val endLabel = Label()
        return Pair(caseBodyLabels, endLabel)
    }
    
    /**
     * Generate safe method invocation with proper error handling.
     */
    fun generateMethodCall(
        owner: String,
        methodName: String,
        descriptor: String,
        isInterface: Boolean = false,
        isStatic: Boolean = false
    ) {
        val opcode = when {
            isStatic -> INVOKESTATIC
            isInterface -> INVOKEINTERFACE
            methodName == "<init>" -> INVOKESPECIAL
            else -> INVOKEVIRTUAL
        }
        
        methodVisitor.visitMethodInsn(opcode, owner, methodName, descriptor, isInterface)
    }
    
    /**
     * Generate field access with proper type handling.
     */
    fun generateFieldAccess(
        owner: String,
        fieldName: String,
        fieldType: Type,
        isStatic: Boolean = false
    ) {
        val opcode = if (isStatic) GETSTATIC else GETFIELD
        val descriptor = typeConverter.getJvmTypeDescriptor(fieldType)
        methodVisitor.visitFieldInsn(opcode, owner, fieldName, descriptor)
    }
    
    /**
     * Generate conditional jump with proper stack management.
     */
    fun generateConditionalJump(
        condition: ComparisonType,
        targetLabel: Label
    ) {
        val opcode = when (condition) {
            ComparisonType.EQUAL -> IFEQ
            ComparisonType.NOT_EQUAL -> IFNE
            ComparisonType.GREATER_THAN -> IFGT
            ComparisonType.GREATER_EQUAL -> IFGE
            ComparisonType.LESS_THAN -> IFLT
            ComparisonType.LESS_EQUAL -> IFLE
            ComparisonType.NULL_CHECK -> IFNULL
            ComparisonType.NOT_NULL_CHECK -> IFNONNULL
        }
        
        methodVisitor.visitJumpInsn(opcode, targetLabel)
    }
    
    /**
     * Generate instanceof check with proper type handling.
     */
    fun generateInstanceofCheck(targetType: Type): String {
        val className = when (targetType) {
            is Type.NamedType -> targetType.name.replace('.', '/')
            is Type.PrimitiveType -> {
                when (targetType.name.lowercase()) {
                    "string" -> "java/lang/String"
                    "int" -> "java/lang/Integer"
                    "double" -> "java/lang/Double"
                    "boolean" -> "java/lang/Boolean"
                    else -> "java/lang/Object"
                }
            }
            else -> "java/lang/Object"
        }
        
        methodVisitor.visitTypeInsn(INSTANCEOF, className)
        return className
    }
    
    /**
     * Generate type cast with safety checks.
     */
    fun generateTypeCast(targetType: Type) {
        val className = when (targetType) {
            is Type.NamedType -> targetType.name.replace('.', '/')
            is Type.PrimitiveType -> {
                when (targetType.name.lowercase()) {
                    "string" -> "java/lang/String"
                    "int" -> "java/lang/Integer"
                    "double" -> "java/lang/Double"
                    "boolean" -> "java/lang/Boolean"
                    else -> "java/lang/Object"
                }
            }
            else -> "java/lang/Object"
        }
        
        methodVisitor.visitTypeInsn(CHECKCAST, className)
    }
    
    /**
     * Generate exception handling setup.
     */
    fun generateTryCatchBlock(
        startLabel: Label,
        endLabel: Label,
        handlerLabel: Label,
        exceptionType: String?
    ) {
        methodVisitor.visitTryCatchBlock(startLabel, endLabel, handlerLabel, exceptionType)
    }
    
    /**
     * Generate line number information for debugging.
     */
    fun generateLineNumber(lineNumber: Int, label: Label) {
        methodVisitor.visitLineNumber(lineNumber, label)
    }
    
    enum class ComparisonType {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        GREATER_EQUAL,
        LESS_THAN,
        LESS_EQUAL,
        NULL_CHECK,
        NOT_NULL_CHECK
    }
}