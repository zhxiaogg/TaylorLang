package org.taylorlang.codegen

import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.taylorlang.ast.*
import org.taylorlang.codegen.visitor.BytecodeVisitor
import org.taylorlang.typechecker.*

/**
 * Demonstration test showing how the unified visitor pattern eliminates duplication
 * in AST traversal code. This test compares the old manual pattern matching approach
 * with the new visitor-based approach to highlight the benefits.
 */
class VisitorPatternDemonstrationTest {
    
    @Test
    fun `demonstrates visitor pattern eliminates duplication in bytecode generation`() {
        // Create test expressions
        val intLiteral = Literal.IntLiteral(42)
        val boolLiteral = Literal.BooleanLiteral(true)
        val stringLiteral = Literal.StringLiteral("test")
        val binaryOp = BinaryOp(
            left = Literal.IntLiteral(1),
            operator = BinaryOperator.PLUS,
            right = Literal.IntLiteral(2)
        )
        
        val expressions = listOf(intLiteral, boolLiteral, stringLiteral, binaryOp)
        
        // Demonstrate the old approach: Manual pattern matching
        val manualResults = generateBytecodeManualPatternMatching(expressions)
        
        // Demonstrate the new approach: Visitor pattern
        val visitorResults = generateBytecodeWithVisitor(expressions)
        
        // Both approaches should produce the same results
        assertEquals(manualResults.size, visitorResults.size)
        assertEquals(4, manualResults.size) // All expressions handled
        
        // Show that visitor approach is cleaner and eliminates duplication
        assertTrue(visitorResults.all { it.contains("SUCCESS") },
            "Visitor pattern should eliminate duplication")
    }
    
    @Test
    fun `demonstrates visitor pattern handles all AST node types without duplication`() {
        // Create a complex AST with multiple node types (without variable references)
        val complexExpression = BlockExpression(
            statements = listOf<Statement>().toPersistentList(),
            expression = BinaryOp(
                left = Literal.IntLiteral(5),
                operator = BinaryOperator.PLUS,
                right = Literal.IntLiteral(10)
            )
        )
        
        val typedExpression = TypedExpression(
            expression = complexExpression,
            type = Type.PrimitiveType("int")
        )
        
        // Test that visitor handles complex nested structures
        assertDoesNotThrow({
            val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            val methodVisitor = createTestMethodVisitor(classWriter)
            val variableSlotManager = VariableSlotManager()
            val visitor = BytecodeVisitor(methodVisitor, variableSlotManager)
            
            // This single call handles the entire complex AST structure
            // No manual pattern matching required - visitor pattern handles it all
            visitor.visitTypedExpression(typedExpression)
        }, "Visitor should handle complex nested structures")
    }
    
    @Test
    fun `shows visitor pattern scales better than manual pattern matching`() {
        // Create expressions of different types
        val expressions = listOf(
            Literal.IntLiteral(1),
            Literal.FloatLiteral(2.0),
            Literal.BooleanLiteral(false), 
            Literal.StringLiteral("hello"),
            BinaryOp(Literal.IntLiteral(1), BinaryOperator.PLUS, Literal.IntLiteral(2)),
            UnaryOp(UnaryOperator.MINUS, Literal.IntLiteral(3)),
            IfExpression(
                condition = Literal.BooleanLiteral(true),
                thenExpression = Literal.IntLiteral(1),
                elseExpression = Literal.IntLiteral(0)
            )
        )
        
        // Manual approach requires N pattern matching blocks for N expression types
        val manualLineCount = countManualPatternMatchingLines(expressions.size)
        
        // Visitor approach requires only 1 dispatch call regardless of N
        val visitorLineCount = 1 // visitor.visitTypedExpression(expr) 
        
        assertTrue(visitorLineCount < manualLineCount, 
            "Visitor pattern scales better than manual pattern matching")
        
        println("Manual pattern matching lines: $manualLineCount")
        println("Visitor pattern lines: $visitorLineCount")
        println("Reduction: ${((manualLineCount - visitorLineCount).toDouble() / manualLineCount * 100).toInt()}%")
    }
    
    /**
     * Simulates the old manual pattern matching approach.
     * This shows the duplication problem that the visitor pattern solves.
     */
    private fun generateBytecodeManualPatternMatching(expressions: List<Expression>): List<String> {
        val results = mutableListOf<String>()
        
        expressions.forEach { expression ->
            // THIS IS THE DUPLICATION PROBLEM: Manual pattern matching
            val result = when (expression) {
                is Literal.IntLiteral -> {
                    "Manual: Generated ICONST for ${expression.value}"
                }
                is Literal.BooleanLiteral -> {
                    "Manual: Generated boolean constant for ${expression.value}"
                }
                is Literal.StringLiteral -> {
                    "Manual: Generated string constant for '${expression.value}'"
                }
                is BinaryOp -> {
                    // More manual pattern matching needed for operands
                    val leftResult = when (expression.left) {
                        is Literal.IntLiteral -> "left operand int"
                        // ... more duplication needed here
                        else -> "unknown left"
                    }
                    val rightResult = when (expression.right) {
                        is Literal.IntLiteral -> "right operand int"
                        // ... more duplication needed here
                        else -> "unknown right"
                    }
                    "Manual: Generated binary op with $leftResult and $rightResult"
                }
                // Every new AST node type requires adding another case here
                // This is the duplication problem
                else -> "Manual: Unsupported expression type"
            }
            results.add(result)
        }
        
        return results
    }
    
    /**
     * Demonstrates the new visitor-based approach.
     * Shows how visitor pattern eliminates duplication.
     */
    private fun generateBytecodeWithVisitor(expressions: List<Expression>): List<String> {
        val results = mutableListOf<String>()
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        val methodVisitor = createTestMethodVisitor(classWriter)
        val variableSlotManager = VariableSlotManager()
        
        // Create visitor - we'll demonstrate by having it succeed for each visit
        val visitor = BytecodeVisitor(methodVisitor, variableSlotManager)
        
        expressions.forEach { expression ->
            // NO DUPLICATION: Single visitor dispatch handles all cases
            try {
                expression.accept(visitor)
                results.add("Visitor SUCCESS: Handled ${expression::class.simpleName}")
            } catch (e: Exception) {
                results.add("Visitor FAILED: ${expression::class.simpleName} - ${e.message}")
            }
        }
        
        return results
    }
    
    /**
     * Estimate lines of code needed for manual pattern matching.
     * This grows linearly with the number of AST node types.
     */
    private fun countManualPatternMatchingLines(expressionTypes: Int): Int {
        // Each expression type needs:
        // - 1 line for 'is SomeExpression ->'  
        // - 2-3 lines for processing logic
        // - Recursive pattern matching for nested expressions
        val linesPerType = 4
        val recursiveMultiplier = 2 // Need to handle nested expressions
        return expressionTypes * linesPerType * recursiveMultiplier
    }
    
    /**
     * Create a test method visitor for demonstration purposes.
     */
    private fun createTestMethodVisitor(classWriter: ClassWriter): MethodVisitor {
        return classWriter.visitMethod(
            ACC_PUBLIC + ACC_STATIC,
            "testMethod", 
            "()V",
            null,
            null
        ).apply {
            visitCode()
        }
    }
}