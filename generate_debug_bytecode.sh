#!/bin/bash

# Create a debug directory
mkdir -p debug_bytecode

# Run the specific test and save output, then copy the class file if it exists
./gradlew test --tests "*TryExpressionBytecodeTest*test try expression execution with Ok result*" --info 2>&1 | tee debug_bytecode/test_output.log

# Try to find any generated class files in temp directories
find /tmp -name "OkExecutionTest.class" 2>/dev/null | head -1 | xargs -I {} cp {} debug_bytecode/

# Also check build temp directories
find build -name "OkExecutionTest.class" 2>/dev/null | head -1 | xargs -I {} cp {} debug_bytecode/

echo "Debug output saved to debug_bytecode/"
echo "Class file copied if found"