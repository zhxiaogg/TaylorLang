package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.taylorlang.ast.*

/**
 * Specialized bytecode generator for literal expressions.
 * 
 * This component handles the generation of JVM bytecode for all literal value types
 * in TaylorLang, including:
 * - Integer literals (LDC with int values)
 * - Float literals (LDC with double values)  
 * - Boolean literals (LDC with 0/1 int values)
 * - String literals (LDC with string constants)
 * 
 * By isolating literal generation, we achieve:
 * - Clear separation from complex expression generation logic
 * - Simple, focused testing for literal value handling
 * - Easy extension for new literal types
 * - Consistent literal generation patterns across the compiler
 */
class LiteralBytecodeGenerator(
    private val methodVisitor: MethodVisitor
) {
    
    /**
     * Generate bytecode for any literal expression
     */
    fun generateLiteral(literal: Literal) {
        when (literal) {
            is Literal.IntLiteral -> generateIntLiteral(literal.value)
            is Literal.FloatLiteral -> generateFloatLiteral(literal.value)
            is Literal.BooleanLiteral -> generateBooleanLiteral(literal.value)
            is Literal.StringLiteral -> generateStringLiteral(literal.value)
            is Literal.TupleLiteral -> generateTupleLiteral(literal.elements)
            is Literal.NullLiteral -> generateNullLiteral()
        }
    }
    
    /**
     * Generate bytecode for integer literals.
     * Uses LDC instruction to load constant integer values onto the stack.
     */
    fun generateIntLiteral(value: Int) {
        methodVisitor.visitLdcInsn(value)
    }
    
    /**
     * Generate bytecode for float literals.
     * Uses LDC instruction to load constant double values onto the stack.
     * TaylorLang treats all floating point numbers as doubles for simplicity.
     */
    fun generateFloatLiteral(value: Double) {
        methodVisitor.visitLdcInsn(value)
    }
    
    /**
     * Generate bytecode for boolean literals.
     * Converts boolean values to JVM integers (0 for false, 1 for true)
     * since the JVM doesn't have a native boolean type on the stack.
     */
    fun generateBooleanLiteral(value: Boolean) {
        methodVisitor.visitLdcInsn(if (value) 1 else 0)
    }
    
    /**
     * Generate bytecode for string literals.
     * Uses LDC instruction to load constant string values onto the stack.
     * The JVM automatically interns string literals for memory efficiency.
     */
    fun generateStringLiteral(value: String) {
        methodVisitor.visitLdcInsn(value)
    }
    
    /**
     * Generate bytecode for tuple literals.
     * Currently not fully implemented - throws TODO for unsupported case.
     * This maintains exhaustive pattern matching while documenting unimplemented features.
     */
    fun generateTupleLiteral(elements: kotlinx.collections.immutable.PersistentList<org.taylorlang.ast.Expression>) {
        TODO("TupleLiteral bytecode generation not yet implemented")
    }
    
    /**
     * Generate bytecode for null literals.
     * Currently not fully implemented - throws TODO for unsupported case.
     * This maintains exhaustive pattern matching while documenting unimplemented features.
     */
    fun generateNullLiteral() {
        TODO("NullLiteral bytecode generation not yet implemented")
    }
}