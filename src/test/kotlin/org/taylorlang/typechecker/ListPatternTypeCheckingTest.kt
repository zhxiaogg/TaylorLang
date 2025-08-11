package org.taylorlang.typechecker

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.taylorlang.ast.*
import kotlinx.collections.immutable.persistentListOf

class ListPatternTypeCheckingTest : DescribeSpec({
    
    val typeContext = TypeContext()
    val expressionChecker = ExpressionTypeChecker(typeContext)
    val patternChecker = PatternTypeChecker(typeContext, expressionChecker)
    
    describe("List Pattern Type Checking") {
        
        it("should type check empty list pattern against List<Int>") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(),
                restVariable = null
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("Int"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeSuccess { patternInfo ->
                patternInfo.bindings shouldBe emptyMap()
                patternInfo.coveredVariants shouldBe emptySet()
            }
        }
        
        it("should type check single element list pattern with identifier") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.IdentifierPattern("x")
                ),
                restVariable = null
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("Int"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeSuccess { patternInfo ->
                patternInfo.bindings.size shouldBe 1
                patternInfo.bindings["x"] shouldBe Type.PrimitiveType("Int")
            }
        }
        
        it("should type check fixed-length list pattern") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.IdentifierPattern("first"),
                    Pattern.IdentifierPattern("second")
                ),
                restVariable = null
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("String"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeSuccess { patternInfo ->
                patternInfo.bindings.size shouldBe 2
                patternInfo.bindings["first"] shouldBe Type.PrimitiveType("String")
                patternInfo.bindings["second"] shouldBe Type.PrimitiveType("String")
            }
        }
        
        it("should type check head/tail list pattern") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.IdentifierPattern("head")
                ),
                restVariable = "tail"
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("Int"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeSuccess { patternInfo ->
                patternInfo.bindings.size shouldBe 2
                patternInfo.bindings["head"] shouldBe Type.PrimitiveType("Int")
                patternInfo.bindings["tail"] shouldBe Type.GenericType(
                    name = "List",
                    arguments = persistentListOf(Type.PrimitiveType("Int"))
                )
            }
        }
        
        it("should type check complex head/tail pattern with multiple elements") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.IdentifierPattern("first"),
                    Pattern.IdentifierPattern("second")
                ),
                restVariable = "rest"
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("Boolean"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeSuccess { patternInfo ->
                patternInfo.bindings.size shouldBe 3
                patternInfo.bindings["first"] shouldBe Type.PrimitiveType("Boolean")
                patternInfo.bindings["second"] shouldBe Type.PrimitiveType("Boolean")
                patternInfo.bindings["rest"] shouldBe Type.GenericType(
                    name = "List",
                    arguments = persistentListOf(Type.PrimitiveType("Boolean"))
                )
            }
        }
        
        it("should type check nested patterns in list elements") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.LiteralPattern(Literal.IntLiteral(42)),
                    Pattern.IdentifierPattern("y")
                ),
                restVariable = null
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("Int"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeSuccess { patternInfo ->
                patternInfo.bindings.size shouldBe 1
                patternInfo.bindings["y"] shouldBe Type.PrimitiveType("Int")
            }
        }
        
        it("should reject list pattern against non-list type") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.IdentifierPattern("x")
                ),
                restVariable = null
            )
            val targetType = Type.PrimitiveType("Int")
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeFailure { error ->
                error.shouldBeInstanceOf<TypeError.TypeMismatch>()
                error.actual shouldBe Type.PrimitiveType("Int")
            }
        }
        
        it("should handle type mismatch in list element patterns") {
            val pattern = Pattern.ListPattern(
                elements = persistentListOf(
                    Pattern.LiteralPattern(Literal.StringLiteral("hello"))
                ),
                restVariable = null
            )
            val targetType = Type.GenericType(
                name = "List",
                arguments = persistentListOf(Type.PrimitiveType("Int"))
            )
            
            val result = patternChecker.checkPattern(pattern, targetType)
            
            result shouldBeFailure { error ->
                error.shouldBeInstanceOf<TypeError.TypeMismatch>()
            }
        }
    }
})