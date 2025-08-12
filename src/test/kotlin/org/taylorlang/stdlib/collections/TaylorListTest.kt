package org.taylorlang.stdlib.collections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

/**
 * Comprehensive test suite for TaylorList implementation.
 * 
 * Tests all core functionality including:
 * - Construction and factory methods
 * - Access operations (head, tail, get)
 * - Modification operations (prepend, append, concat)  
 * - Functional operations (map, filter, fold, reduce)
 * - Utility operations (take, drop, reverse, contains)
 * - Conversion operations (toKotlinList, toString)
 * - Equality and hash code
 */
class TaylorListTest {

    // =============================================================================
    // Construction and Basic Properties
    // =============================================================================

    @Nested
    inner class ConstructionTests {

        @Test
        fun `test empty list creation`() {
            val emptyList = TaylorList.empty<Int>()
            
            assertTrue(emptyList.isEmpty)
            assertEquals(0, emptyList.size)
            assertNull(emptyList.head())
            assertEquals(TaylorList.empty<Int>(), emptyList.tail())
        }

        @Test
        fun `test single element list creation`() {
            val singleList = TaylorList.of(42)
            
            assertFalse(singleList.isEmpty)
            assertEquals(1, singleList.size)
            assertEquals(42, singleList.head())
            assertTrue(singleList.tail().isEmpty)
        }

        @Test
        fun `test multiple elements list creation`() {
            val list = TaylorList.of(1, 2, 3, 4, 5)
            
            assertFalse(list.isEmpty)
            assertEquals(5, list.size)
            assertEquals(1, list.head())
            assertEquals(TaylorList.of(2, 3, 4, 5), list.tail())
        }

        @Test
        fun `test list from collection`() {
            val kotlinList = listOf("a", "b", "c")
            val taylorList = TaylorList.from(kotlinList)
            
            assertEquals(3, taylorList.size)
            assertEquals("a", taylorList.head())
            assertEquals(kotlinList, taylorList.toKotlinList())
        }

        @Test
        fun `test repeat factory method`() {
            val repeated = TaylorList.repeat("x", 3)
            
            assertEquals(3, repeated.size)
            assertEquals(TaylorList.of("x", "x", "x"), repeated)
        }

        @Test
        fun `test range factory method`() {
            val range = TaylorList.range(1, 5)
            
            assertEquals(5, range.size)
            assertEquals(TaylorList.of(1, 2, 3, 4, 5), range)
        }

        @Test
        fun `test empty range`() {
            val emptyRange = TaylorList.range(5, 1)
            
            assertTrue(emptyRange.isEmpty)
        }
    }

    // =============================================================================
    // Access Operations
    // =============================================================================

    @Nested
    inner class AccessTests {

        @Test
        fun `test get operation`() {
            val list = TaylorList.of("a", "b", "c", "d")
            
            assertEquals("a", list.get(0))
            assertEquals("b", list.get(1))
            assertEquals("c", list.get(2))
            assertEquals("d", list.get(3))
            assertNull(list.get(4))
            assertNull(list.get(-1))
        }

        @Test
        fun `test get on empty list`() {
            val empty = TaylorList.empty<String>()
            
            assertNull(empty.get(0))
            assertNull(empty.get(1))
        }

        @Test
        fun `test indexed access operator`() {
            val list = TaylorList.of(10, 20, 30)
            
            assertEquals(10, list[0])
            assertEquals(20, list[1])
            assertEquals(30, list[2])
            assertNull(list[3])
        }
    }

    // =============================================================================
    // Modification Operations  
    // =============================================================================

    @Nested
    inner class ModificationTests {

        @Test
        fun `test prepend operation`() {
            val original = TaylorList.of(2, 3, 4)
            val prepended = original.prepend(1)
            
            assertEquals(TaylorList.of(1, 2, 3, 4), prepended)
            assertEquals(4, prepended.size)
            // Original should be unchanged
            assertEquals(TaylorList.of(2, 3, 4), original)
        }

        @Test
        fun `test append operation`() {
            val original = TaylorList.of(1, 2, 3)
            val appended = original.append(4)
            
            assertEquals(TaylorList.of(1, 2, 3, 4), appended)
            assertEquals(4, appended.size)
            // Original should be unchanged
            assertEquals(TaylorList.of(1, 2, 3), original)
        }

        @Test
        fun `test append to empty list`() {
            val empty = TaylorList.empty<String>()
            val appended = empty.append("first")
            
            assertEquals(TaylorList.of("first"), appended)
            assertEquals(1, appended.size)
        }

        @Test
        fun `test concat operation`() {
            val list1 = TaylorList.of(1, 2, 3)
            val list2 = TaylorList.of(4, 5, 6)
            val concatenated = list1.concat(list2)
            
            assertEquals(TaylorList.of(1, 2, 3, 4, 5, 6), concatenated)
            assertEquals(6, concatenated.size)
            // Originals should be unchanged
            assertEquals(TaylorList.of(1, 2, 3), list1)
            assertEquals(TaylorList.of(4, 5, 6), list2)
        }

        @Test
        fun `test concat with empty list`() {
            val list = TaylorList.of(1, 2, 3)
            val empty = TaylorList.empty<Int>()
            
            assertEquals(list, list.concat(empty))
            assertEquals(list, empty.concat(list))
        }
    }

    // =============================================================================
    // Functional Operations
    // =============================================================================

    @Nested
    inner class FunctionalOperationsTests {

        @Test
        fun `test map operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            val doubled = numbers.map { it * 2 }
            
            assertEquals(TaylorList.of(2, 4, 6, 8, 10), doubled)
            assertEquals(5, doubled.size)
            // Original should be unchanged
            assertEquals(TaylorList.of(1, 2, 3, 4, 5), numbers)
        }

        @Test
        fun `test map on empty list`() {
            val empty = TaylorList.empty<Int>()
            val mapped = empty.map { it * 2 }
            
            assertTrue(mapped.isEmpty)
        }

        @Test
        fun `test map type transformation`() {
            val numbers = TaylorList.of(1, 2, 3)
            val strings = numbers.map { it.toString() }
            
            assertEquals(TaylorList.of("1", "2", "3"), strings)
        }

        @Test
        fun `test filter operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5, 6)
            val evens = numbers.filter { it % 2 == 0 }
            
            assertEquals(TaylorList.of(2, 4, 6), evens)
            assertEquals(3, evens.size)
        }

        @Test
        fun `test filter on empty list`() {
            val empty = TaylorList.empty<Int>()
            val filtered = empty.filter { it > 0 }
            
            assertTrue(filtered.isEmpty)
        }

        @Test
        fun `test filter all elements match`() {
            val numbers = TaylorList.of(2, 4, 6)
            val evens = numbers.filter { it % 2 == 0 }
            
            assertEquals(numbers, evens)
        }

        @Test
        fun `test filter no elements match`() {
            val numbers = TaylorList.of(1, 3, 5)
            val evens = numbers.filter { it % 2 == 0 }
            
            assertTrue(evens.isEmpty)
        }

        @Test
        fun `test fold operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            val sum = numbers.fold(0) { acc, x -> acc + x }
            
            assertEquals(15, sum)
        }

        @Test
        fun `test fold on empty list`() {
            val empty = TaylorList.empty<Int>()
            val result = empty.fold(42) { acc, x -> acc + x }
            
            assertEquals(42, result)
        }

        @Test
        fun `test fold with different types`() {
            val numbers = TaylorList.of(1, 2, 3)
            val concatenated = numbers.fold("") { acc, x -> acc + x.toString() }
            
            assertEquals("123", concatenated)
        }

        @Test
        fun `test reduce operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            val sum = numbers.reduce { a, b -> a + b }
            
            assertEquals(15, sum)
        }

        @Test
        fun `test reduce on empty list`() {
            val empty = TaylorList.empty<Int>()
            val result = empty.reduce { a, b -> a + b }
            
            assertNull(result)
        }

        @Test
        fun `test reduce on single element`() {
            val single = TaylorList.of(42)
            val result = single.reduce { a, b -> a + b }
            
            assertEquals(42, result)
        }
    }

    // =============================================================================
    // Search and Check Operations
    // =============================================================================

    @Nested
    inner class SearchTests {

        @Test
        fun `test find operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            
            assertEquals(4, numbers.find { it > 3 })
            assertNull(numbers.find { it > 10 })
        }

        @Test
        fun `test find on empty list`() {
            val empty = TaylorList.empty<Int>()
            
            assertNull(empty.find { it > 0 })
        }

        @Test
        fun `test all operation`() {
            val positives = TaylorList.of(1, 2, 3, 4, 5)
            val mixed = TaylorList.of(-1, 2, 3, 4, 5)
            
            assertTrue(positives.all { it > 0 })
            assertFalse(mixed.all { it > 0 })
        }

        @Test
        fun `test all on empty list`() {
            val empty = TaylorList.empty<Int>()
            
            assertTrue(empty.all { it > 0 })
        }

        @Test
        fun `test any operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            val negatives = TaylorList.of(-1, -2, -3)
            
            assertTrue(numbers.any { it > 3 })
            assertFalse(negatives.any { it > 0 })
        }

        @Test
        fun `test any on empty list`() {
            val empty = TaylorList.empty<Int>()
            
            assertFalse(empty.any { it > 0 })
        }

        @Test
        fun `test contains operation`() {
            val fruits = TaylorList.of("apple", "banana", "cherry")
            
            assertTrue(fruits.contains("banana"))
            assertFalse(fruits.contains("grape"))
        }

        @Test
        fun `test contains on empty list`() {
            val empty = TaylorList.empty<String>()
            
            assertFalse(empty.contains("anything"))
        }
    }

    // =============================================================================
    // Utility Operations
    // =============================================================================

    @Nested
    inner class UtilityTests {

        @Test
        fun `test take operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            
            assertEquals(TaylorList.of(1, 2, 3), numbers.take(3))
            assertEquals(numbers, numbers.take(10))
            assertTrue(numbers.take(0).isEmpty)
            assertTrue(numbers.take(-1).isEmpty)
        }

        @Test
        fun `test take on empty list`() {
            val empty = TaylorList.empty<Int>()
            
            assertTrue(empty.take(3).isEmpty)
        }

        @Test
        fun `test drop operation`() {
            val numbers = TaylorList.of(1, 2, 3, 4, 5)
            
            assertEquals(TaylorList.of(4, 5), numbers.drop(3))
            assertEquals(numbers, numbers.drop(0))
            assertEquals(numbers, numbers.drop(-1))
            assertTrue(numbers.drop(10).isEmpty)
        }

        @Test
        fun `test drop on empty list`() {
            val empty = TaylorList.empty<Int>()
            
            assertTrue(empty.drop(3).isEmpty)
        }

        @Test
        fun `test reverse operation`() {
            val original = TaylorList.of(1, 2, 3, 4, 5)
            val reversed = original.reverse()
            
            assertEquals(TaylorList.of(5, 4, 3, 2, 1), reversed)
            assertEquals(5, reversed.size)
            // Original should be unchanged
            assertEquals(TaylorList.of(1, 2, 3, 4, 5), original)
        }

        @Test
        fun `test reverse on empty list`() {
            val empty = TaylorList.empty<String>()
            val reversed = empty.reverse()
            
            assertTrue(reversed.isEmpty)
        }

        @Test
        fun `test reverse on single element`() {
            val single = TaylorList.of(42)
            val reversed = single.reverse()
            
            assertEquals(single, reversed)
        }
    }

    // =============================================================================
    // Conversion and String Operations
    // =============================================================================

    @Nested
    inner class ConversionTests {

        @Test
        fun `test toKotlinList conversion`() {
            val taylorList = TaylorList.of("a", "b", "c")
            val kotlinList = taylorList.toKotlinList()
            
            assertEquals(listOf("a", "b", "c"), kotlinList)
            assertEquals(3, kotlinList.size)
        }

        @Test
        fun `test empty list toKotlinList`() {
            val empty = TaylorList.empty<Int>()
            val kotlinList = empty.toKotlinList()
            
            assertTrue(kotlinList.isEmpty())
        }

        @Test
        fun `test toString representation`() {
            val numbers = TaylorList.of(1, 2, 3)
            val empty = TaylorList.empty<Int>()
            
            assertEquals("[1, 2, 3]", numbers.toString())
            assertEquals("[]", empty.toString())
        }
    }

    // =============================================================================
    // Equality and Hash Code
    // =============================================================================

    @Nested
    inner class EqualityTests {

        @Test
        fun `test equality of identical lists`() {
            val list1 = TaylorList.of(1, 2, 3)
            val list2 = TaylorList.of(1, 2, 3)
            
            assertEquals(list1, list2)
            assertEquals(list1.hashCode(), list2.hashCode())
        }

        @Test
        fun `test inequality of different lists`() {
            val list1 = TaylorList.of(1, 2, 3)
            val list2 = TaylorList.of(1, 2, 4)
            val list3 = TaylorList.of(1, 2)
            
            assertNotEquals(list1, list2)
            assertNotEquals(list1, list3)
        }

        @Test
        fun `test empty lists equality`() {
            val empty1 = TaylorList.empty<Int>()
            val empty2 = TaylorList.empty<String>()
            
            assertEquals(empty1, empty2)
            assertEquals(empty1.hashCode(), empty2.hashCode())
        }

        @Test
        fun `test equality with non-list objects`() {
            val list = TaylorList.of(1, 2, 3)
            val kotlinList = listOf(1, 2, 3)
            
            assertNotEquals(list, kotlinList)
            assertNotEquals(list, "not a list")
            assertNotEquals(list, null)
        }

        @Test
        fun `test self equality and identity`() {
            val list = TaylorList.of(1, 2, 3)
            
            assertEquals(list, list)
            assertTrue(list === list)
        }
    }

    // =============================================================================
    // Performance and Edge Cases
    // =============================================================================

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `test operations on large lists`() {
            val largeList = TaylorList.range(1, 1000)
            
            assertEquals(1000, largeList.size)
            assertEquals(1, largeList.head())
            assertEquals(1000, largeList.get(999))
            assertNull(largeList.get(1000))
        }

        @Test
        fun `test chained functional operations`() {
            val result = TaylorList.range(1, 10)
                .filter { it % 2 == 0 }
                .map { it * 2 }
                .take(3)
            
            assertEquals(TaylorList.of(4, 8, 12), result)
        }

        @Test
        fun `test null element handling`() {
            val listWithNull = TaylorList.of("a", null, "c")
            
            assertEquals(3, listWithNull.size)
            assertEquals("a", listWithNull.head())
            assertNull(listWithNull.get(1))
            assertEquals("c", listWithNull.get(2))
        }

        @Test
        fun `test structural sharing after prepend`() {
            val original = TaylorList.of(2, 3, 4, 5)
            val prepended = original.prepend(1)
            
            // The tail of prepended should be the same instance as original
            assertEquals(original, prepended.tail())
            // This tests structural sharing - the tail should be shared
            assertTrue(prepended is TaylorList.Cons && prepended.tail === original)
        }
    }
}