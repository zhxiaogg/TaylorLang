import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Label
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader

/**
 * Manual bytecode generation test to verify while(false) logic
 */
fun main() {
    val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
    
    // Create class
    classWriter.visit(
        V17,
        ACC_PUBLIC + ACC_SUPER,
        "ManualWhileTest",
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
    
    // Generate main method with while(false) loop
    val main = classWriter.visitMethod(
        ACC_PUBLIC + ACC_STATIC,
        "main",
        "([Ljava/lang/String;)V",
        null,
        null
    )
    main.visitCode()
    
    // Create labels
    val loopStartLabel = Label()
    val conditionCheckLabel = Label()
    val loopEndLabel = Label()
    
    // Generate while(false) loop:
    // 1. Jump to condition check first
    main.visitJumpInsn(GOTO, conditionCheckLabel)
    
    // 2. Loop body (println("SHOULD NOT PRINT"))
    main.visitLabel(loopStartLabel)
    main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
    main.visitLdcInsn("SHOULD NOT PRINT")
    main.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
    
    // 3. Condition check
    main.visitLabel(conditionCheckLabel)
    main.visitInsn(ICONST_0) // Push false (0) onto stack
    main.visitJumpInsn(IFNE, loopStartLabel) // Jump if NOT zero (i.e., if true)
    
    // 4. End of loop
    main.visitLabel(loopEndLabel)
    
    // Add a final println to show execution continues
    main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
    main.visitLdcInsn("After loop")
    main.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
    
    // Return
    main.visitInsn(RETURN)
    main.visitMaxs(2, 1)
    main.visitEnd()
    
    classWriter.visitEnd()
    
    // Write class file
    val tempDir = File(System.getProperty("java.io.tmpdir"))
    val classFile = File(tempDir, "ManualWhileTest.class")
    FileOutputStream(classFile).use { fos ->
        fos.write(classWriter.toByteArray())
    }
    
    // Load and execute
    val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
    val clazz = classLoader.loadClass("ManualWhileTest")
    val mainMethod = clazz.getDeclaredMethod("main", Array<String>::class.java)
    
    println("=== Manual bytecode test ===")
    println("Expected: 'After loop' only")
    println("Actual output:")
    mainMethod.invoke(null, arrayOf<String>())
}