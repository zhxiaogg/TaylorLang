# Wildcard Pattern Implementation in Bytecode

## Context
Wildcard patterns (`_`) in functional languages are designed to match any value without binding. However, they require careful implementation in bytecode to maintain stack consistency while providing efficient matching.

## Key Insight: Wildcard Patterns Are Not No-Ops
**Critical Understanding**: Wildcard patterns must still handle the target value on the stack, even though they don't perform comparisons or bind variables.

## Solution/Approach
**Essential Pattern**: Wildcard patterns must consume the target value from the stack and immediately jump to success.

```kotlin
// INCORRECT - ignoring stack state
is Pattern.WildcardPattern -> {
    // Wrong: This leaves the target value on the stack
    methodVisitor.visitJumpInsn(GOTO, successLabel)
}

// CORRECT - proper stack management
is Pattern.WildcardPattern -> {
    // Wildcard patterns always match - consume the target value
    if (targetType == BuiltinTypes.DOUBLE || 
        (targetType is Type.PrimitiveType && targetType.name.lowercase() in listOf("double", "float"))) {
        methodVisitor.visitInsn(POP2)  // Doubles occupy 2 stack slots
    } else {
        methodVisitor.visitInsn(POP)   // Most values occupy 1 stack slot
    }
    methodVisitor.visitJumpInsn(GOTO, successLabel)
}
```

## Benefits
- **Maintains Stack Consistency**: Ensures predictable stack state across all pattern types
- **Prevents VerifyError**: Stack map frames remain consistent at merge points
- **Enables Optimization**: Compiler can optimize wildcard patterns as unconditional matches
- **Simplifies Debugging**: Clear stack operations make bytecode easier to analyze

## Pattern Recognition
This principle applies to all "always-match" patterns:

1. **Wildcard Patterns** (`_`): Match any value without binding
2. **Identifier Patterns** (`x`): Match any value and bind to variable
3. **Guard-less Patterns**: Any pattern without additional conditions

All these patterns must handle the target value appropriately, even if they don't use it for comparison.

## Implementation Strategy
```kotlin
fun generateAlwaysMatchPattern(pattern: Pattern, targetType: Type, successLabel: Label) {
    // Step 1: Consume target value from stack
    consumeTargetValue(targetType)
    
    // Step 2: Perform pattern-specific actions
    when (pattern) {
        is Pattern.WildcardPattern -> {
            // No additional action needed
        }
        is Pattern.IdentifierPattern -> {
            // Bind the value to the identifier
            bindVariable(pattern.name, targetType)
        }
    }
    
    // Step 3: Jump to success
    methodVisitor.visitJumpInsn(GOTO, successLabel)
}

private fun consumeTargetValue(targetType: Type) {
    val usesTwoSlots = when (targetType) {
        BuiltinTypes.DOUBLE, BuiltinTypes.LONG -> true
        is Type.PrimitiveType -> targetType.name.lowercase() in listOf("double", "float", "long")
        else -> false
    }
    
    if (usesTwoSlots) {
        methodVisitor.visitInsn(POP2)
    } else {
        methodVisitor.visitInsn(POP)
    }
}
```

## Performance Considerations
Wildcard patterns can be highly optimized:

```kotlin
// Optimization: Skip target value generation for pure wildcards
fun optimizeWildcardMatch(pattern: Pattern, targetExpr: Expression): Boolean {
    if (pattern is Pattern.WildcardPattern && hasNoSideEffects(targetExpr)) {
        // Skip generating target expression entirely
        return true
    }
    return false
}
```

## Related Concepts
- [Pattern Matching Stack Management](./pattern-matching-stack-management.md)
- [Control Flow Optimization](./pattern-matching-control-flow.md)
- [Variable Binding Patterns](./pattern-variable-binding.md)

---
*Discovered during: Wildcard pattern bytecode generation implementation*
*Date: 2025-08-15*