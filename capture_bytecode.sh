#!/bin/bash

# Create debug output directory
mkdir -p debug_capture

# Find the TaylorLang jar in libs
JAR_FILE="build/libs/TaylorLang-0.1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Building jar first..."
    ./gradlew jar
fi

# Create a simple debug program 
cat > debug_capture/debug_program.taylor << 'EOF'
fun getOkValue() : Result<Int, Throwable> {
    TaylorResult.ok(42)
}

fun main(args) : Result<Int, Throwable> {
    TaylorResult.ok(try getOkValue())
}
EOF

# Try to compile it with the taylor compiler
echo "Attempting to compile with taylor compiler..."
java -jar "$JAR_FILE" debug_capture/debug_program.taylor -o debug_capture/

# Check if OkExecutionTest.class was generated somewhere
find debug_capture -name "*.class" -exec echo "Found class file: {}" \;
find debug_capture -name "*.class" -exec javap -v {} \;

echo "Debug capture completed"