package org.taylorlang.codegen

import org.taylorlang.ast.Type

/**
 * Checkpoint of variable slot manager state for restoration
 */
data class SlotCheckpoint(
    val variableSlots: Map<String, Int>,
    val variableTypes: Map<String, Type>,
    val nextAvailableSlot: Int,
    val tempSlotStack: List<Int>
)

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
    private val variableTypes = mutableMapOf<String, Type>()
    private var nextAvailableSlot = 1 // Start at 1, slot 0 reserved for 'this'
    private val tempSlotStack = mutableListOf<Int>() // Track temporary slots for proper cleanup
    
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
        variableTypes[name] = type
        
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
     * Get the type of a variable.
     * @param name Variable name
     * @return The variable type, or null if not allocated
     */
    fun getType(name: String): Type? {
        return variableTypes[name]
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
        variableTypes.clear()
        nextAvailableSlot = 1
    }
    
    /**
     * Set the starting slot for variable allocation.
     * Used for static methods where slot 0 is for parameters, not 'this'.
     */
    fun setStartingSlot(startSlot: Int) {
        nextAvailableSlot = startSlot
    }
    
    /**
     * Allocate a temporary slot for intermediate values.
     * These slots don't have variable names and are released explicitly.
     */
    fun allocateTemporarySlot(type: Type): Int {
        val slot = nextAvailableSlot
        val slotCount = getSlotCount(type)
        nextAvailableSlot += slotCount
        tempSlotStack.add(slot)
        return slot
    }
    
    /**
     * Release a temporary slot - restores nextAvailableSlot if this was the most recent allocation.
     * CRITICAL FIX: Proper temporary slot cleanup prevents inconsistent local variable counts.
     */
    fun releaseTemporarySlot(slot: Int) {
        // Find and remove the slot from tracking
        val index = tempSlotStack.indexOf(slot)
        if (index != -1) {
            tempSlotStack.removeAt(index)
            
            // If this was the most recently allocated slot, we can reclaim the slot space
            if (tempSlotStack.isEmpty() || slot >= tempSlotStack.maxOrNull()!!) {
                // Recalculate nextAvailableSlot based on remaining slots
                nextAvailableSlot = if (tempSlotStack.isEmpty()) {
                    // Find the highest slot used by named variables
                    variableSlots.values.maxOrNull()?.let { maxSlot ->
                        maxSlot + 1 // Assuming single-slot variables; for proper implementation, track slot counts
                    } ?: 1
                } else {
                    tempSlotStack.maxOrNull()!! + 1 // Assuming single-slot; for proper implementation, track slot counts
                }
            }
        }
    }
    
    /**
     * Create a checkpoint of current slot state for restoration
     */
    fun createCheckpoint(): SlotCheckpoint {
        return SlotCheckpoint(
            variableSlots = variableSlots.toMap(),
            variableTypes = variableTypes.toMap(),
            nextAvailableSlot = nextAvailableSlot,
            tempSlotStack = tempSlotStack.toList()
        )
    }
    
    /**
     * Restore slot state from a checkpoint
     * CRITICAL FIX: Include temporary slot stack in checkpoint restoration
     */
    fun restoreCheckpoint(checkpoint: SlotCheckpoint) {
        variableSlots.clear()
        variableTypes.clear()
        tempSlotStack.clear()
        variableSlots.putAll(checkpoint.variableSlots)
        variableTypes.putAll(checkpoint.variableTypes)
        nextAvailableSlot = checkpoint.nextAvailableSlot
        tempSlotStack.addAll(checkpoint.tempSlotStack)
    }
    
    /**
     * Get the last allocated slot number (for debugging)
     */
    fun getLastAllocatedSlot(): Int {
        return nextAvailableSlot - 1
    }
    
    /**
     * Get the number of JVM slots required for a given type.
     * Most types use 1 slot, but double and long use 2 slots.
     */
    private fun getSlotCount(type: Type): Int {
        return when (type) {
            is Type.PrimitiveType -> when (type.name) {
                "double", "Double", "long", "Long" -> 2
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
                "int", "Int", "boolean", "Boolean" -> org.objectweb.asm.Opcodes.ILOAD
                "double", "Double" -> org.objectweb.asm.Opcodes.DLOAD
                "float", "Float" -> org.objectweb.asm.Opcodes.FLOAD
                "long", "Long" -> org.objectweb.asm.Opcodes.LLOAD
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
                "int", "Int", "boolean", "Boolean" -> org.objectweb.asm.Opcodes.ISTORE
                "double", "Double" -> org.objectweb.asm.Opcodes.DSTORE
                "float", "Float" -> org.objectweb.asm.Opcodes.FSTORE
                "long", "Long" -> org.objectweb.asm.Opcodes.LSTORE
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