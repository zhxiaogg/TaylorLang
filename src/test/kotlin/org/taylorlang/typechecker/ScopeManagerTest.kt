package org.taylorlang.typechecker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import org.taylorlang.ast.Type
import org.taylorlang.ast.SourceLocation

class ScopeManagerTest : StringSpec({
    
    "should declare and lookup variables correctly" {
        val scopeManager = ScopeManager()
        val intType = Type.PrimitiveType("Int")
        
        // Declare a variable
        val result = scopeManager.declareVariable("x", intType, true)
        result.isSuccess shouldBe true
        
        // Look it up
        val binding = scopeManager.lookupVariable("x")
        binding?.name shouldBe "x"
        binding?.type shouldBe intType
        binding?.isMutable shouldBe true
        
        // Check if defined
        scopeManager.isVariableDefined("x") shouldBe true
        scopeManager.isVariableDefined("y") shouldBe false
    }
    
    "should handle mutable vs immutable variables" {
        val scopeManager = ScopeManager()
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        // Declare mutable and immutable variables
        scopeManager.declareVariable("mutableVar", intType, true)
        scopeManager.declareVariable("immutableVar", stringType, false)
        
        // Check mutability
        scopeManager.isVariableMutable("mutableVar").getOrThrow() shouldBe true
        scopeManager.isVariableMutable("immutableVar").getOrThrow() shouldBe false
        
        // Check types
        scopeManager.getVariableType("mutableVar").getOrThrow() shouldBe intType
        scopeManager.getVariableType("immutableVar").getOrThrow() shouldBe stringType
    }
    
    "should handle scope nesting correctly" {
        val scopeManager = ScopeManager()
        val intType = Type.PrimitiveType("Int")
        val stringType = Type.PrimitiveType("String")
        
        // Declare in outer scope
        scopeManager.declareVariable("x", intType, true)
        
        // Push new scope
        scopeManager.pushScope()
        
        // Should still see outer variable
        scopeManager.isVariableDefined("x") shouldBe true
        scopeManager.getVariableType("x").getOrThrow() shouldBe intType
        
        // Declare shadowing variable
        scopeManager.declareVariable("x", stringType, false)
        
        // Should see inner variable (shadowed)
        scopeManager.getVariableType("x").getOrThrow() shouldBe stringType
        scopeManager.isVariableMutable("x").getOrThrow() shouldBe false
        
        // Pop scope
        scopeManager.popScope()
        
        // Should see outer variable again
        scopeManager.getVariableType("x").getOrThrow() shouldBe intType
        scopeManager.isVariableMutable("x").getOrThrow() shouldBe true
    }
    
    "should prevent duplicate declarations in same scope" {
        val scopeManager = ScopeManager()
        val intType = Type.PrimitiveType("Int")
        
        // First declaration should succeed
        scopeManager.declareVariable("x", intType, true).isSuccess shouldBe true
        
        // Second declaration in same scope should fail
        val duplicateResult = scopeManager.declareVariable("x", intType, false)
        duplicateResult.isFailure shouldBe true
        duplicateResult.exceptionOrNull() should beInstanceOf<TypeError.DuplicateDefinition>()
    }
    
    "should handle undefined variable errors" {
        val scopeManager = ScopeManager()
        
        // Lookup undefined variable
        scopeManager.lookupVariable("undefined") shouldBe null
        
        // Get type of undefined variable should fail
        val typeResult = scopeManager.getVariableType("undefined")
        typeResult.isFailure shouldBe true
        typeResult.exceptionOrNull() should beInstanceOf<TypeError.UnresolvedSymbol>()
        
        // Check mutability of undefined variable should fail
        val mutabilityResult = scopeManager.isVariableMutable("undefined")
        mutabilityResult.isFailure shouldBe true
        mutabilityResult.exceptionOrNull() should beInstanceOf<TypeError.UnresolvedSymbol>()
    }
    
    "should track scope depth correctly" {
        val scopeManager = ScopeManager()
        
        // Start with global scope (depth 0)
        scopeManager.getScopeDepth() shouldBe 0
        
        scopeManager.pushScope()
        scopeManager.getScopeDepth() shouldBe 1
        
        scopeManager.pushScope()
        scopeManager.getScopeDepth() shouldBe 2
        
        scopeManager.popScope()
        scopeManager.getScopeDepth() shouldBe 1
        
        scopeManager.popScope()
        scopeManager.getScopeDepth() shouldBe 0
    }
    
    "should provide debug information" {
        val scopeManager = ScopeManager()
        val intType = Type.PrimitiveType("Int")
        
        scopeManager.declareVariable("global", intType, true)
        scopeManager.pushScope()
        scopeManager.declareVariable("local", intType, false)
        
        val debugInfo = scopeManager.debugScopeStack()
        debugInfo.contains("global") shouldBe true
        debugInfo.contains("local") shouldBe true
    }
})