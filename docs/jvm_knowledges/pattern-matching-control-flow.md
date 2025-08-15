# Pattern Matching Control Flow in JVM Bytecode

## Context
Pattern matching requires complex control flow with multiple success/failure paths. The JVM verifier requires consistent stack states at label merge points, making control flow generation critical for pattern matching success.

## Key Insight: Failure Label Placement Timing
**Critical Bug Pattern**: Generating failure labels before pattern tests complete can cause premature failure jumps and incorrect control flow.

## Solution/Approach
**Essential Pattern**: Always generate failure labels AFTER all pattern tests are complete, not during pattern test generation.

```kotlin
// INCORRECT - premature failure label generation
fun generatePatternTests(cases: List<MatchCase>) {
    for (i in cases.indices) {
        val case = cases[i]
        val failureLabel = Label()
        
        // Generate pattern test
        generatePatternTest(case.pattern, successLabel, failureLabel)
        
        // WRONG: Visiting failure label here causes premature jumps
        methodVisitor.visitLabel(failureLabel)  // TOO EARLY!
    }
}

// CORRECT - delayed failure label placement
fun generatePatternTests(cases: List<MatchCase>) {
    val nextCaseLabels = mutableListOf<Label>()
    
    for (i in cases.indices) {
        val nextCaseLabel = if (i < cases.size - 1) Label() else Label()
        nextCaseLabels.add(nextCaseLabel)
        
        generatePatternTest(case.pattern, successLabel, nextCaseLabel)
        
        // Only visit next case label when ready for next pattern
        if (i < cases.size - 1) {
            methodVisitor.visitLabel(nextCaseLabel)
        }
    }
    
    // Visit final failure label after ALL pattern tests
    methodVisitor.visitLabel(nextCaseLabels.last())
    // Generate default/failure behavior here
}
```

## Benefits
- **Prevents Control Flow Corruption**: Ensures patterns are tested in correct order
- **Eliminates False Negatives**: Patterns that should match don't fail prematurely
- **Improves Deterministic Behavior**: Pattern matching behaves predictably
- **Reduces VerifyError Risk**: Consistent control flow satisfies JVM verifier

## Pattern Recognition
This principle applies to all multi-step conditional logic in bytecode:

1. **Generate all conditions first**
2. **Place intermediate labels strategically** 
3. **Visit failure/default labels last**
4. **Ensure consistent stack states at merge points**

## Example Implementation
```kotlin
fun generateMatchExpression(cases: List<MatchCase>) {
    // Phase 1: Generate all pattern tests with forward jumps
    val caseLabels = cases.map { Label() }
    val endLabel = Label()
    
    for (i in cases.indices) {
        val nextLabel = if (i < cases.size - 1) Label() else Label()
        
        // Load target value for comparison
        loadTargetValue()
        
        // Test pattern with proper label management
        generatePatternTest(cases[i].pattern, caseLabels[i], nextLabel)
        
        // Visit next case label only when ready for next test
        if (i < cases.size - 1) {
            methodVisitor.visitLabel(nextLabel)
        } else {
            // Final failure case
            methodVisitor.visitLabel(nextLabel)
            generateDefaultValue()
            methodVisitor.visitJumpInsn(GOTO, endLabel)
        }
    }
    
    // Phase 2: Generate case bodies
    for (i in cases.indices) {
        methodVisitor.visitLabel(caseLabels[i])
        generateCaseBody(cases[i])
        methodVisitor.visitJumpInsn(GOTO, endLabel)
    }
    
    methodVisitor.visitLabel(endLabel)
}
```

## Related Concepts
- [Stack Management for Pattern Matching](./pattern-matching-stack-management.md)
- [Consistent Stack States](./verifyerror-debugging.md)
- [Match Expression Type Safety](./pattern-matching-type-inference.md)

---
*Discovered during: Pattern matching control flow bug fixes*
*Date: 2025-08-15*