package org.taylorlang.codegen

import org.taylorlang.ast.Type

/**
 * Manages JVM local variable slots for bytecode generation.
 * 
 * The JVM uses local variable slots to store method parameters and local variables.
 * Different types require different numbers of slots:
 * - Most types (int, float, object references): 1 slot
 * - double and long: 2 slots 
 * 
 * Slot 0 is reserved for 'this' in instance methods, or the first parameter in static methods.
 * 
 * This class tracks variable names to slot mappings and handles slot allocation.
 */
class VariableSlotManager {
    private val variableSlots = mutableMapOf<String, Int>()
    private var nextAvailableSlot = 1 // Start at 1, slot 0 reserved for 'this'
    
    /**
     * Allocate a slot for a new variable.
     * @param name Variable name
     * @param type Variable type (affects slot count for doubles/longs)
     * @return The slot number allocated for this variable
     */
    fun allocateSlot(name: String, type: Type): Int {
        if (variableSlots.containsKey(name)) {
            throw IllegalArgumentException("Variable '$name' already has an allocated slot")
        }
        
        val slot = nextAvailableSlot
        variableSlots[name] = slot
        
        // Increment by the number of slots this type requires
        nextAvailableSlot += getSlotCount(type)
        
        return slot
    }
    
    /**
     * Get the slot number for a variable.
     * @param name Variable name
     * @return The slot number, or null if not allocated
     */
    fun getSlot(name: String): Int? {
        return variableSlots[name]
    }
    
    /**
     * Get the slot number for a variable, throwing if not found.
     * @param name Variable name
     * @return The slot number
     * @throws IllegalArgumentException if variable not found
     */
    fun getSlotOrThrow(name: String): Int {
        return getSlot(name) ?: throw IllegalArgumentException("Variable '$name' has no allocated slot")
    }
    
    /**
     * Check if a variable has an allocated slot.
     */
    fun hasSlot(name: String): Boolean {
        return variableSlots.containsKey(name)
    }
    
    /**
     * Get the total number of local variable slots used.
     * This is useful for setting the maxLocals value in JVM bytecode.
     */
    fun getMaxSlots(): Int {
        return nextAvailableSlot
    }
    
    /**
     * Get all variable names that have allocated slots.
     */
    fun getAllVariableNames(): Set<String> {
        return variableSlots.keys
    }
    
    /**
     * Clear all variable slot allocations.
     */
    fun clear() {
        variableSlots.clear()
        nextAvailableSlot = 1
    }
    
    /**
     * Get the number of JVM slots required for a given type.
     * Most types use 1 slot, but double and long use 2 slots.
     */
    private fun getSlotCount(type: Type): Int {
        return when (type) {
            is Type.PrimitiveType -> when (type.name) {
                "Double", "Long" -> 2
                else -> 1
            }
            else -> 1 // Objects, arrays, etc. use 1 slot
        }
    }
    
    /**
     * Get the appropriate load instruction opcode for a type.
     */
    fun getLoadInstruction(type: Type): Int {
        return when (type) {
            is Type.PrimitiveType -> when (type.name) {
                "Int", "Boolean" -> org.objectweb.asm.Opcodes.ILOAD
                "Double" -> org.objectweb.asm.Opcodes.DLOAD
                "Float" -> org.objectweb.asm.Opcodes.FLOAD
                "Long" -> org.objectweb.asm.Opcodes.LLOAD
                else -> org.objectweb.asm.Opcodes.ALOAD // String and other objects
            }
            else -> org.objectweb.asm.Opcodes.ALOAD // Objects, arrays, etc.
        }
    }
    
    /**
     * Get the appropriate store instruction opcode for a type.
     */
    fun getStoreInstruction(type: Type): Int {
        return when (type) {
            is Type.PrimitiveType -> when (type.name) {
                "Int", "Boolean" -> org.objectweb.asm.Opcodes.ISTORE
                "Double" -> org.objectweb.asm.Opcodes.DSTORE
                "Float" -> org.objectweb.asm.Opcodes.FSTORE
                "Long" -> org.objectweb.asm.Opcodes.LSTORE
                else -> org.objectweb.asm.Opcodes.ASTORE // String and other objects
            }
            else -> org.objectweb.asm.Opcodes.ASTORE // Objects, arrays, etc.
        }
    }
    
    /**
     * For debugging: get a string representation of all allocated slots.
     */
    fun debugSlotAllocations(): String {
        return buildString {
            appendLine("Variable Slot Allocations:")
            for ((name, slot) in variableSlots) {
                appendLine("  $name -> slot $slot")
            }
            appendLine("Next available slot: $nextAvailableSlot")
        }
    }
}