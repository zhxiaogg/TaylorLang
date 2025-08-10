package org.taylorlang.codegen

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.io.File
import java.io.FileOutputStream

fun generateWhileLoopDebug() {
    val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
    
    classWriter.visit(
        V17,
        ACC_PUBLIC + ACC_SUPER,
        "WhileLoopDebug",
        null,
        "java/lang/Object",
        null
    )
    
    // Generate constructor
    val constructor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
    constructor.visitCode()
    constructor.visitVarInsn(ALOAD, 0)
    constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
    constructor.visitInsn(RETURN)
    constructor.visitMaxs(1, 1)
    constructor.visitEnd()
    
    // Generate main method
    val mv = classWriter.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)
    mv.visitCode()
    
    // My implementation:
    val conditionLabel = org.objectweb.asm.Label()
    val loopEnd = org.objectweb.asm.Label()
    
    // Jump to condition check first
    mv.visitJumpInsn(GOTO, conditionLabel)
    
    // Loop body start label
    val loopBodyStart = org.objectweb.asm.Label()
    mv.visitLabel(loopBodyStart)
    
    // Body: println("loop")
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
    mv.visitLdcInsn("loop")
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
    
    // Condition check label
    mv.visitLabel(conditionLabel)
    
    // Generate condition: false (0)
    mv.visitLdcInsn(0)
    
    // Jump back to loop body if condition is true (non-zero)
    mv.visitJumpInsn(IFNE, loopBodyStart)
    
    // After loop: println("done")
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
    mv.visitLdcInsn("done")
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
    
    mv.visitInsn(RETURN)
    mv.visitMaxs(2, 1)
    mv.visitEnd()
    
    classWriter.visitEnd()
    
    // Write class file
    File("WhileLoopDebug.class").writeBytes(classWriter.toByteArray())
}

fun main() {
    generateWhileLoopDebug()
    println("Generated WhileLoopDebug.class")
}