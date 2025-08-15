package org.taylorlang.codegen

import org.objectweb.asm.MethodVisitor
import org.taylorlang.ast.*
import org.taylorlang.typechecker.*

/**
 * Specialized bytecode generator for variable access operations.
 * 
 * This component handles the generation of JVM bytecode for:
 * - Variable loading from local slots
 * - Variable storing to local slots
 * - Type-safe variable access with slot manager integration
 * - Placeholder generation for unknown identifiers
 * 
 * Key features:
 * - Integration with VariableSlotManager for slot allocation
 * - Type-aware load/store instruction selection
 * - Proper handling of unknown variables (function names, etc.)
 * - Stack-safe variable operations
 */
class VariableAccessBytecodeGenerator(
    private val methodVisitor: MethodVisitor,
    private val variableSlotManager: VariableSlotManager,
    private val typeHelper: TypeInferenceBytecodeHelper
) {
    
    /**
     * Generate bytecode for variable loading (identifier access)
     */
    fun generateVariableLoad(identifier: Identifier) {
        // Load variable from local slot
        if (variableSlotManager.hasSlot(identifier.name)) {
            val slot = variableSlotManager.getSlot(identifier.name)!!
            // CRITICAL FIX: Use the actual variable type stored in the slot, not the inferred type
            // This prevents JVM verification errors when types don't match
            val variableType = variableSlotManager.getType(identifier.name)!!
            val loadInstruction = variableSlotManager.getLoadInstruction(variableType)
            methodVisitor.visitVarInsn(loadInstruction, slot)
        } else {
            // For now, load 0 as placeholder for unknown identifiers (e.g., functions)
            methodVisitor.visitLdcInsn(0)
        }
    }
    
    /**
     * Generate bytecode for variable storing
     */
    fun generateVariableStore(variableName: String, variableType: Type) {
        // Ensure slot is allocated for the variable
        if (!variableSlotManager.hasSlot(variableName)) {
            variableSlotManager.allocateSlot(variableName, variableType)
        }
        
        val slot = variableSlotManager.getSlot(variableName)!!
        val storeInstruction = variableSlotManager.getStoreInstruction(variableType)
        methodVisitor.visitVarInsn(storeInstruction, slot)
    }
    
    /**
     * Generate bytecode for variable loading with explicit type
     */
    fun generateTypedVariableLoad(variableName: String, expectedType: Type) {
        if (variableSlotManager.hasSlot(variableName)) {
            val slot = variableSlotManager.getSlot(variableName)!!
            val actualType = variableSlotManager.getType(variableName)!!
            val loadInstruction = variableSlotManager.getLoadInstruction(actualType)
            methodVisitor.visitVarInsn(loadInstruction, slot)
            
            // Add type conversion if needed
            if (actualType != expectedType) {
                generateTypeConversion(actualType, expectedType)
            }
        } else {
            // Generate default value for expected type
            generateDefaultValue(expectedType)
        }
    }
    
    /**
     * Generate default value for a given type
     */
    private fun generateDefaultValue(type: Type) {
        when (typeHelper.getJvmType(type)) {
            "I", "Z" -> methodVisitor.visitLdcInsn(0)
            "D" -> methodVisitor.visitLdcInsn(0.0)
            else -> methodVisitor.visitLdcInsn("")
        }
    }
    
    /**
     * Generate type conversion between compatible types
     */
    private fun generateTypeConversion(fromType: Type, toType: Type) {
        // For now, only handle basic conversions
        when {
            typeHelper.isIntegerType(fromType) && typeHelper.isDoubleType(toType) -> {
                // int to double conversion
                methodVisitor.visitInsn(org.objectweb.asm.Opcodes.I2D)
            }
            typeHelper.isDoubleType(fromType) && typeHelper.isIntegerType(toType) -> {
                // double to int conversion
                methodVisitor.visitInsn(org.objectweb.asm.Opcodes.D2I)
            }
            // Add more conversions as needed
        }
    }
    
    /**
     * Check if a variable exists in the slot manager
     */
    fun hasVariable(variableName: String): Boolean {
        return variableSlotManager.hasSlot(variableName)
    }
    
    /**
     * Get the type of a variable if it exists
     */
    fun getVariableType(variableName: String): Type? {
        return variableSlotManager.getType(variableName)
    }
}