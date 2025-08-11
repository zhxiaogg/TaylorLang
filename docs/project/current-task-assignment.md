# Task Assignment: Complete User-Defined Functions Implementation

**Date**: 2025-08-11  
**Assignee**: kotlin-java-engineer  
**Priority**: HIGH - Core language feature  
**Timeline**: Days 2-5 (3-4 days remaining)  
**Current Status**: IN PROGRESS - Partial implementation with compilation errors

## Current State Analysis

You have successfully started the user-defined functions implementation:
- ✅ Grammar updated with `fun` keyword and return statement support
- ✅ AST nodes added (ReturnStatement)
- ✅ Parser integration complete
- 🔴 BytecodeGenerator has compilation errors that need immediate fixing

## Immediate Actions Required (Day 2 - TODAY)

### 1. Fix Compilation Errors (30 minutes)

**File**: `src/main/kotlin/org/taylorlang/codegen/BytecodeGenerator.kt`

**Error 1 - Line 219**: Add the missing `generateReturnStatement()` method:
```kotlin
private fun generateReturnStatement(statement: TypedStatement.ReturnStatement) {
    if (statement.expression != null) {
        // Generate the return value
        generateExpression(statement.expression)
        // Use appropriate return instruction based on type
        val returnInstruction = when (statement.expression.type) {
            BuiltinTypes.INT -> IRETURN
            BuiltinTypes.BOOLEAN -> IRETURN
            BuiltinTypes.DOUBLE -> DRETURN
            BuiltinTypes.UNIT -> RETURN
            else -> ARETURN // For objects/strings
        }
        methodVisitor!!.visitInsn(returnInstruction)
    } else {
        // Void return
        methodVisitor!!.visitInsn(RETURN)
    }
}
```

**Error 2 - Line 238**: Add the missing case in the when expression for TypedExpression:
```kotlin
is TypedExpression.ReturnExpression -> {
    // Return expressions shouldn't appear here, they should be statements
    throw IllegalStateException("Return should be a statement, not an expression")
}
```

### 2. Complete Function Declaration Generation (Day 2-3)

**WHY**: Functions need to be compiled as separate JVM methods to enable modular code and recursion.

**WHAT TO IMPLEMENT**:

1. **Update `generateStatement()` to handle function declarations**:
```kotlin
is TypedStatement.FunctionDeclaration -> {
    generateFunctionDeclaration(statement)
}
```

2. **Create `generateFunctionDeclaration()` method**:
```kotlin
private fun generateFunctionDeclaration(function: TypedStatement.FunctionDeclaration) {
    // Save current method visitor
    val previousMethodVisitor = methodVisitor
    val previousSlotManager = variableSlotManager
    
    // Create new method
    val methodDescriptor = createMethodDescriptor(function.parameters, function.returnType)
    methodVisitor = classWriter.visitMethod(
        ACC_PUBLIC + ACC_STATIC,  // All functions are static for now
        function.name,
        methodDescriptor,
        null,  // No generic signature for now
        null   // No exceptions declared
    )
    methodVisitor!!.visitCode()
    
    // Create new variable slot manager for this function
    variableSlotManager = VariableSlotManager()
    
    // Allocate slots for parameters
    for (param in function.parameters) {
        val slot = variableSlotManager.allocateSlot(param.name, param.type)
        // Parameters are already on the stack, store them in local variables
        val storeInstruction = variableSlotManager.getStoreInstruction(param.type)
        methodVisitor!!.visitVarInsn(storeInstruction, slot)
    }
    
    // Generate function body
    when (function.body) {
        is TypedFunctionBody.ExpressionBody -> {
            generateExpression(function.body.expression)
            // Add return instruction
            val returnInstruction = getReturnInstruction(function.returnType)
            methodVisitor!!.visitInsn(returnInstruction)
        }
        is TypedFunctionBody.BlockBody -> {
            for (statement in function.body.statements) {
                generateStatement(statement)
            }
            // Add default return if needed
            if (function.returnType == BuiltinTypes.UNIT) {
                methodVisitor!!.visitInsn(RETURN)
            }
        }
    }
    
    // Complete method
    methodVisitor!!.visitMaxs(0, 0)  // ASM will compute
    methodVisitor!!.visitEnd()
    
    // Restore previous context
    methodVisitor = previousMethodVisitor
    variableSlotManager = previousSlotManager
}
```

3. **Helper method for method descriptors**:
```kotlin
private fun createMethodDescriptor(parameters: List<TypedParameter>, returnType: Type): String {
    val paramDescriptors = parameters.joinToString("") { param ->
        getTypeDescriptor(param.type)
    }
    val returnDescriptor = getTypeDescriptor(returnType)
    return "($paramDescriptors)$returnDescriptor"
}

private fun getTypeDescriptor(type: Type): String {
    return when (type) {
        BuiltinTypes.INT -> "I"
        BuiltinTypes.BOOLEAN -> "Z"
        BuiltinTypes.DOUBLE -> "D"
        BuiltinTypes.STRING -> "Ljava/lang/String;"
        BuiltinTypes.UNIT -> "V"
        else -> "Ljava/lang/Object;"  // Default for unknown types
    }
}

private fun getReturnInstruction(type: Type): Int {
    return when (type) {
        BuiltinTypes.INT, BuiltinTypes.BOOLEAN -> IRETURN
        BuiltinTypes.DOUBLE -> DRETURN
        BuiltinTypes.UNIT -> RETURN
        else -> ARETURN
    }
}
```

### 3. Implement Function Calls (Day 3-4)

**Update `generateFunctionCall()`**:
```kotlin
private fun generateFunctionCall(call: TypedExpression.FunctionCall) {
    // Evaluate arguments and push onto stack
    for (arg in call.arguments) {
        generateExpression(arg)
    }
    
    // Look up function signature
    val function = context.getFunction(call.name)
    if (function != null) {
        val descriptor = createMethodDescriptor(function.parameters, function.returnType)
        // Generate static method call
        methodVisitor!!.visitMethodInsn(
            INVOKESTATIC,
            className,  // Same class for now
            call.name,
            descriptor,
            false
        )
    } else {
        // Handle builtin functions (like println)
        generateBuiltinCall(call)
    }
}
```

### 4. Testing Requirements (Day 4-5)

Create comprehensive tests in `BytecodeGeneratorTest.kt`:

```kotlin
@Test
fun testSimpleFunction() {
    val program = """
        fun add(x: Int, y: Int): Int {
            return x + y
        }
        
        fun main() {
            println(add(5, 3))
        }
    """
    // Test should output "8"
}

@Test
fun testRecursiveFunction() {
    val program = """
        fun factorial(n: Int): Int {
            if (n <= 1) {
                return 1
            } else {
                return n * factorial(n - 1)
            }
        }
        
        fun main() {
            println(factorial(5))
        }
    """
    // Test should output "120"
}

@Test
fun testFunctionWithLocalVariables() {
    val program = """
        fun calculate(a: Int, b: Int): Int {
            val sum = a + b
            val product = a * b
            return sum + product
        }
        
        fun main() {
            println(calculate(3, 4))
        }
    """
    // Test should output "19" (3+4=7, 3*4=12, 7+12=19)
}
```

## Success Criteria

### Must Have (Core Requirements)
- ✅ Project compiles without errors
- ✅ Functions can be declared with parameters and return types
- ✅ Functions can be called with arguments
- ✅ Return statements work correctly
- ✅ Local variables work inside functions
- ✅ Recursive functions work
- ✅ At least 15 comprehensive tests passing

### Should Have (Quality Requirements)
- ✅ Clean separation between function scope and global scope
- ✅ Proper error messages for undefined functions
- ✅ Type checking for function arguments
- ✅ Stack properly balanced after function calls

### Nice to Have (Stretch Goals)
- Function overloading support
- Default parameter values
- Lambda expressions
- Higher-order functions

## Technical Guidance

### JVM Method Structure
```
.method public static functionName(params)returnType
    .limit stack N
    .limit locals M
    ; Method body
    return-instruction
.end method
```

### Key ASM Methods
- `classWriter.visitMethod()` - Creates a new method
- `methodVisitor.visitCode()` - Starts method body
- `methodVisitor.visitVarInsn()` - Load/store local variables
- `methodVisitor.visitMethodInsn()` - Call methods
- `methodVisitor.visitInsn()` - Simple instructions like RETURN
- `methodVisitor.visitMaxs()` - Set stack/locals size
- `methodVisitor.visitEnd()` - Complete method

### Important Considerations

1. **Scope Management**: Each function needs its own VariableSlotManager
2. **Parameter Handling**: Parameters are passed on the stack and need to be stored in local variables
3. **Return Type Validation**: Ensure return statements match function signature
4. **Recursion Support**: Functions must be able to call themselves
5. **Main Function**: Special handling for `fun main()` as JVM entry point

## Resources

- **JVM Method Descriptors**: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.3.3
- **ASM Method Generation**: https://asm.ow2.io/asm4-guide.pdf (Chapter 3)
- **JVM Instruction Set**: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-6.html
- **Calling Conventions**: https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-2.html#jvms-2.6

## Progress Tracking

Use the todo list to track your progress:
1. ✅ Grammar and AST changes (COMPLETE)
2. 🚧 Fix compilation errors (IN PROGRESS)
3. ⏳ Function declaration generation
4. ⏳ Function call generation
5. ⏳ Return statement completion
6. ⏳ Comprehensive testing
7. ⏳ Recursive function support

## Questions to Consider

1. How will you handle forward references (calling a function defined later)?
2. Should functions have access to global variables?
3. How will you handle function name conflicts?
4. What happens if a function doesn't return a value when it should?

## Final Notes

You're on the right track! The grammar and AST changes look good. Focus on:
1. Getting the project to compile first
2. Implementing the simplest case (a function with no parameters that returns a constant)
3. Gradually adding complexity (parameters, local variables, recursion)
4. Writing tests for each feature as you go

Remember: The variable storage system you built provides a solid foundation. Use the same patterns for managing function-local variables.

Good luck! You've got this! 💪