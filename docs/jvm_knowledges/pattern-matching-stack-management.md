# JVM Stack Management for Pattern Matching

## Context
Pattern matching in functional languages requires sophisticated bytecode generation with careful stack management. During TaylorLang implementation, critical stack management patterns emerged that prevent VerifyError and ensure correct execution flow.

## Key Insight: Double Value Stack Management
The JVM treats `double` and `long` values as occupying two stack slots, requiring special handling in pattern matching operations.

## Solution/Approach
**Critical Pattern**: Always use `POP2` instead of `POP` for double values in wildcard and identifier patterns.

```kotlin
// INCORRECT - causes stack corruption
if (targetType == BuiltinTypes.DOUBLE) {
    methodVisitor.visitInsn(POP)  // Wrong! Only removes one slot
}

// CORRECT - maintains stack integrity
if (targetType == BuiltinTypes.DOUBLE) {
    methodVisitor.visitInsn(POP2)  // Removes both slots
}
```

## Benefits
- **Prevents VerifyError**: Maintains correct stack frame analysis
- **Ensures Correctness**: Pattern matching works reliably across all numeric types
- **Improves Debugging**: Clear stack state reduces mysterious runtime issues

## Pattern Recognition
This principle extends to all JVM operations involving doubles:
- Storage operations: Use `DSTORE` instead of `ISTORE`
- Loading operations: Use `DLOAD` instead of `ILOAD`
- Arithmetic: Use `DADD`, `DSUB`, etc. for double operations
- Stack manipulation: Always consider slot count for doubles and longs

## Example Implementation
```kotlin
fun handlePatternValue(targetType: Type) {
    val requiresTwoSlots = targetType == BuiltinTypes.DOUBLE || 
                          targetType == BuiltinTypes.LONG ||
                          (targetType is Type.PrimitiveType && 
                           targetType.name.lowercase() in listOf("double", "float"))
    
    if (requiresTwoSlots) {
        methodVisitor.visitInsn(POP2)
    } else {
        methodVisitor.visitInsn(POP)
    }
}
```

## Related Concepts
- [Control Flow Label Management](./pattern-matching-control-flow.md)
- [Type System Integration](./pattern-matching-type-inference.md)
- [VerifyError Prevention](./verifyerror-debugging.md)

---
*Discovered during: Pattern matching bytecode generation fixes*
*Date: 2025-08-15*