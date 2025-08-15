# VerifyError Debugging Strategies

## Context
JVM VerifyErrors occur when bytecode violates verification rules, often due to stack inconsistencies or type mismatches.

## Key Insight
Most VerifyErrors stem from inconsistent stack states at control flow merge points. The JVM requires all paths to a given instruction have identical stack shapes.

## Solution/Approach
1. **Enable Detailed Verification**: Run with `-XX:+UnlockDiagnosticVMOptions -XX:+VerboseVerification`
2. **Analyze Stack Traces**: Focus on the exact bytecode offset mentioned in error
3. **Use javap -v**: Disassemble the problematic class to inspect bytecode
4. **Check Merge Points**: Labels where multiple paths converge need special attention
5. **Verify Stack Balance**: Ensure each path pushes/pops same number of items

## Benefits
- Quickly identify root cause of verification failures
- Systematic approach reduces debugging time
- Prevents similar errors in future

## Example
```
VerifyError: Expecting to find object/array on stack
  at offset 47 in method process()

Solution: Check what's on stack at offset 47:
- Path 1 pushed Integer
- Path 2 pushed String
Fix: Ensure both paths push same type
```

## Related Concepts
- Stack map frames
- Type verification
- Control flow analysis

---
*Discovered during: Pattern matching VerifyError resolution*
*Date: 2024*