plugins {
    kotlin("jvm") version "2.1.10"
    antlr
    application
}

group = "org.taylorlang"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
    
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6")
    
    // ANTLR for parsing
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    
    // ASM for bytecode generation
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    
    // Functional programming libraries
    implementation("io.arrow-kt:arrow-core:1.2.1")
    implementation("org.pcollections:pcollections:4.0.1")
    
    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}

// Configure ANTLR source generation
tasks.generateGrammarSource {
    outputDirectory = file("src/main/generated/antlr")
    arguments = arguments + listOf("-visitor", "-long-messages")
}

// Ensure generated sources are included in compilation
sourceSets {
    main {
        java {
            srcDirs("src/main/generated/antlr")
        }
    }
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.taylorlang.compiler.MainKt")
}