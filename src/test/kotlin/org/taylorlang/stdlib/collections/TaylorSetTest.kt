package org.taylorlang.stdlib.collections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

/**
 * Comprehensive test suite for TaylorSet implementation.
 * 
 * Tests all core functionality including:
 * - Construction and factory methods
 * - Core set operations (add, remove, contains)
 * - Set algebra operations (union, intersection, difference)
 * - Subset/superset/disjoint operations
 * - Functional operations (map, filter, fold, etc.)
 * - Conversion operations (toKotlinSet, toString)
 * - Equality and hash code
 */
class TaylorSetTest {

    // =============================================================================
    // Construction and Basic Properties
    // =============================================================================

    @Nested
    inner class ConstructionTests {

        @Test
        fun `test empty set creation`() {
            val emptySet = TaylorSet.empty<String>()
            
            assertTrue(emptySet.isEmpty)
            assertEquals(0, emptySet.size)
            assertFalse(emptySet.contains("any"))
        }

        @Test
        fun `test single element set creation`() {
            val singleSet = TaylorSet.of("element1")
            
            assertFalse(singleSet.isEmpty)
            assertEquals(1, singleSet.size)
            assertTrue(singleSet.contains("element1"))
            assertFalse(singleSet.contains("other"))
        }

        @Test
        fun `test multiple elements set creation with duplicates`() {
            val multiSet = TaylorSet.of(1, 3, 2, 3, 1, 4)
            
            assertFalse(multiSet.isEmpty)
            assertEquals(4, multiSet.size) // Duplicates removed
            assertTrue(multiSet.contains(1))
            assertTrue(multiSet.contains(2))
            assertTrue(multiSet.contains(3))
            assertTrue(multiSet.contains(4))
            assertFalse(multiSet.contains(5))
        }

        @Test
        fun `test set creation from collection`() {
            val list = listOf(5, 1, 3, 1, 2, 3)
            val set = TaylorSet.from(list)
            
            assertEquals(4, set.size) // [1, 2, 3, 5]
            assertTrue(set.contains(1))
            assertTrue(set.contains(2))
            assertTrue(set.contains(3))
            assertTrue(set.contains(5))
        }

        @Test
        fun `test set creation from kotlin set`() {
            val kotlinSet = setOf("a", "c", "b")
            val taylorSet = TaylorSet.fromJava(kotlinSet)
            
            assertEquals(3, taylorSet.size)
            assertTrue(taylorSet.contains("a"))
            assertTrue(taylorSet.contains("b"))
            assertTrue(taylorSet.contains("c"))
        }
    }

    // =============================================================================
    // Core Set Operations
    // =============================================================================

    @Nested
    inner class CoreOperationsTests {

        @Test
        fun `test add operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val newSet = emptySet.add(42)
            
            assertTrue(emptySet.isEmpty)
            assertEquals(1, newSet.size)
            assertTrue(newSet.contains(42))
        }

        @Test
        fun `test add operation preserves existing elements`() {
            val originalSet = TaylorSet.of(1, 2, 3)
            val newSet = originalSet.add(4)
            
            assertEquals(3, originalSet.size)
            assertEquals(4, newSet.size)
            assertTrue(newSet.contains(1))
            assertTrue(newSet.contains(2))
            assertTrue(newSet.contains(3))
            assertTrue(newSet.contains(4))
        }

        @Test
        fun `test add operation with existing element returns same set`() {
            val originalSet = TaylorSet.of(1, 2, 3)
            val newSet = originalSet.add(2)
            
            assertEquals(3, originalSet.size)
            assertEquals(3, newSet.size)
            assertTrue(newSet.contains(1))
            assertTrue(newSet.contains(2))
            assertTrue(newSet.contains(3))
        }

        @Test
        fun `test remove operation on empty set`() {
            val emptySet = TaylorSet.empty<String>()
            val result = emptySet.remove("any")
            
            assertSame(emptySet, result)
            assertTrue(result.isEmpty)
        }

        @Test
        fun `test remove operation on single element set`() {
            val singleSet = TaylorSet.of("element")
            val result = singleSet.remove("element")
            
            assertTrue(result.isEmpty)
            assertEquals(0, result.size)
        }

        @Test
        fun `test remove operation preserves other elements`() {
            val originalSet = TaylorSet.of(1, 2, 3, 4, 5)
            val newSet = originalSet.remove(3)
            
            assertEquals(5, originalSet.size)
            assertEquals(4, newSet.size)
            assertTrue(newSet.contains(1))
            assertTrue(newSet.contains(2))
            assertFalse(newSet.contains(3))
            assertTrue(newSet.contains(4))
            assertTrue(newSet.contains(5))
        }

        @Test
        fun `test remove non-existent element`() {
            val originalSet = TaylorSet.of(1, 2, 3)
            val newSet = originalSet.remove(4)
            
            assertEquals(3, originalSet.size)
            assertEquals(3, newSet.size)
            assertTrue(newSet.contains(1))
            assertTrue(newSet.contains(2))
            assertTrue(newSet.contains(3))
        }

        @Test
        fun `test contains operation`() {
            val set = TaylorSet.of("apple", "banana", "cherry")
            
            assertTrue(set.contains("apple"))
            assertTrue(set.contains("banana"))
            assertTrue(set.contains("cherry"))
            assertFalse(set.contains("date"))
            assertFalse(set.contains("elderberry"))
        }
    }

    // =============================================================================
    // Set Algebra Operations
    // =============================================================================

    @Nested
    inner class SetAlgebraTests {

        @Test
        fun `test union with empty set`() {
            val set = TaylorSet.of(1, 2, 3)
            val emptySet = TaylorSet.empty<Int>()
            
            val result1 = set.union(emptySet)
            val result2 = emptySet.union(set)
            
            assertEquals(3, result1.size)
            assertEquals(3, result2.size)
            assertTrue(result1.contains(1))
            assertTrue(result1.contains(2))
            assertTrue(result1.contains(3))
            assertEquals(result1.toKotlinSet(), result2.toKotlinSet())
        }

        @Test
        fun `test union with disjoint sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(4, 5, 6)
            
            val result = set1.union(set2)
            
            assertEquals(6, result.size)
            assertTrue(result.contains(1))
            assertTrue(result.contains(2))
            assertTrue(result.contains(3))
            assertTrue(result.contains(4))
            assertTrue(result.contains(5))
            assertTrue(result.contains(6))
        }

        @Test
        fun `test union with overlapping sets`() {
            val set1 = TaylorSet.of(1, 2, 3, 4)
            val set2 = TaylorSet.of(3, 4, 5, 6)
            
            val result = set1.union(set2)
            
            assertEquals(6, result.size)
            assertTrue(result.contains(1))
            assertTrue(result.contains(2))
            assertTrue(result.contains(3))
            assertTrue(result.contains(4))
            assertTrue(result.contains(5))
            assertTrue(result.contains(6))
        }

        @Test
        fun `test intersection with empty set`() {
            val set = TaylorSet.of(1, 2, 3)
            val emptySet = TaylorSet.empty<Int>()
            
            val result1 = set.intersection(emptySet)
            val result2 = emptySet.intersection(set)
            
            assertTrue(result1.isEmpty)
            assertTrue(result2.isEmpty)
        }

        @Test
        fun `test intersection with disjoint sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(4, 5, 6)
            
            val result = set1.intersection(set2)
            
            assertTrue(result.isEmpty)
        }

        @Test
        fun `test intersection with overlapping sets`() {
            val set1 = TaylorSet.of(1, 2, 3, 4)
            val set2 = TaylorSet.of(3, 4, 5, 6)
            
            val result = set1.intersection(set2)
            
            assertEquals(2, result.size)
            assertTrue(result.contains(3))
            assertTrue(result.contains(4))
            assertFalse(result.contains(1))
            assertFalse(result.contains(2))
            assertFalse(result.contains(5))
            assertFalse(result.contains(6))
        }

        @Test
        fun `test difference with empty set`() {
            val set = TaylorSet.of(1, 2, 3)
            val emptySet = TaylorSet.empty<Int>()
            
            val result1 = set.difference(emptySet)
            val result2 = emptySet.difference(set)
            
            assertEquals(3, result1.size)
            assertTrue(result2.isEmpty)
            assertTrue(result1.contains(1))
            assertTrue(result1.contains(2))
            assertTrue(result1.contains(3))
        }

        @Test
        fun `test difference with disjoint sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(4, 5, 6)
            
            val result = set1.difference(set2)
            
            assertEquals(3, result.size)
            assertTrue(result.contains(1))
            assertTrue(result.contains(2))
            assertTrue(result.contains(3))
        }

        @Test
        fun `test difference with overlapping sets`() {
            val set1 = TaylorSet.of(1, 2, 3, 4)
            val set2 = TaylorSet.of(3, 4, 5, 6)
            
            val result = set1.difference(set2)
            
            assertEquals(2, result.size)
            assertTrue(result.contains(1))
            assertTrue(result.contains(2))
            assertFalse(result.contains(3))
            assertFalse(result.contains(4))
        }
    }

    // =============================================================================
    // Subset/Superset Operations
    // =============================================================================

    @Nested
    inner class SubsetSupersetTests {

        @Test
        fun `test isSubsetOf with empty sets`() {
            val emptySet1 = TaylorSet.empty<Int>()
            val emptySet2 = TaylorSet.empty<Int>()
            val nonEmptySet = TaylorSet.of(1, 2, 3)
            
            assertTrue(emptySet1.isSubsetOf(emptySet2))
            assertTrue(emptySet1.isSubsetOf(nonEmptySet))
            assertFalse(nonEmptySet.isSubsetOf(emptySet1))
        }

        @Test
        fun `test isSubsetOf with identical sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(1, 2, 3)
            
            assertTrue(set1.isSubsetOf(set2))
            assertTrue(set2.isSubsetOf(set1))
        }

        @Test
        fun `test isSubsetOf with proper subset`() {
            val subset = TaylorSet.of(1, 2)
            val superset = TaylorSet.of(1, 2, 3, 4)
            
            assertTrue(subset.isSubsetOf(superset))
            assertFalse(superset.isSubsetOf(subset))
        }

        @Test
        fun `test isSubsetOf with disjoint sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(4, 5, 6)
            
            assertFalse(set1.isSubsetOf(set2))
            assertFalse(set2.isSubsetOf(set1))
        }

        @Test
        fun `test isSupersetOf operations`() {
            val subset = TaylorSet.of(1, 2)
            val superset = TaylorSet.of(1, 2, 3, 4)
            
            assertTrue(superset.isSupersetOf(subset))
            assertFalse(subset.isSupersetOf(superset))
        }

        @Test
        fun `test disjointWith operations`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(4, 5, 6)
            val set3 = TaylorSet.of(3, 4, 5)
            
            assertTrue(set1.disjointWith(set2))
            assertTrue(set2.disjointWith(set1))
            assertFalse(set1.disjointWith(set3))
            assertFalse(set3.disjointWith(set1))
        }
    }

    // =============================================================================
    // Functional Operations
    // =============================================================================

    @Nested
    inner class FunctionalOperationsTests {

        @Test
        fun `test map operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.map { it * 2 }
            
            assertTrue(result.isEmpty)
        }

        @Test
        fun `test map operation transforming elements`() {
            val set = TaylorSet.of(1, 2, 3)
            val result = set.map { it * 2 }
            
            assertEquals(3, result.size)
            assertTrue(result.contains(2))
            assertTrue(result.contains(4))
            assertTrue(result.contains(6))
        }

        @Test
        fun `test map operation with duplicate results`() {
            val set = TaylorSet.of(1, 2, 3, 4)
            val result = set.map { it / 2 } // 1->0, 2->1, 3->1, 4->2
            
            assertEquals(3, result.size) // Duplicates removed
            assertTrue(result.contains(0))
            assertTrue(result.contains(1))
            assertTrue(result.contains(2))
        }

        @Test
        fun `test filter operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.filter { it > 0 }
            
            assertTrue(result.isEmpty)
        }

        @Test
        fun `test filter operation keeping some elements`() {
            val set = TaylorSet.of(1, 2, 3, 4, 5, 6)
            val result = set.filter { it % 2 == 0 }
            
            assertEquals(3, result.size)
            assertTrue(result.contains(2))
            assertTrue(result.contains(4))
            assertTrue(result.contains(6))
            assertFalse(result.contains(1))
            assertFalse(result.contains(3))
            assertFalse(result.contains(5))
        }

        @Test
        fun `test filter operation keeping all elements`() {
            val set = TaylorSet.of(1, 2, 3)
            val result = set.filter { true }
            
            assertEquals(3, result.size)
            assertTrue(result.contains(1))
            assertTrue(result.contains(2))
            assertTrue(result.contains(3))
        }

        @Test
        fun `test filter operation keeping no elements`() {
            val set = TaylorSet.of(1, 2, 3)
            val result = set.filter { false }
            
            assertTrue(result.isEmpty)
        }

        @Test
        fun `test fold operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.fold(0) { acc, elem -> acc + elem }
            
            assertEquals(0, result)
        }

        @Test
        fun `test fold operation summing elements`() {
            val set = TaylorSet.of(1, 2, 3, 4, 5)
            val result = set.fold(0) { acc, elem -> acc + elem }
            
            assertEquals(15, result)
        }

        @Test
        fun `test fold operation with string concatenation`() {
            val set = TaylorSet.of("a", "b", "c")
            val result = set.fold("") { acc, elem -> acc + elem }
            
            assertEquals("abc", result) // In sorted order
        }

        @Test
        fun `test reduce operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.reduce { a, b -> a + b }
            
            assertNull(result)
        }

        @Test
        fun `test reduce operation on single element set`() {
            val singleSet = TaylorSet.of(42)
            val result = singleSet.reduce { a, b -> a + b }
            
            assertEquals(42, result)
        }

        @Test
        fun `test reduce operation summing elements`() {
            val set = TaylorSet.of(1, 2, 3, 4, 5)
            val result = set.reduce { a, b -> a + b }
            
            assertEquals(15, result)
        }

        @Test
        fun `test find operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.find { it > 2 }
            
            assertNull(result)
        }

        @Test
        fun `test find operation finding element`() {
            val set = TaylorSet.of(1, 2, 3, 4, 5)
            val result = set.find { it > 3 }
            
            assertTrue(result == 4 || result == 5) // First found (in-order traversal)
        }

        @Test
        fun `test find operation not finding element`() {
            val set = TaylorSet.of(1, 2, 3)
            val result = set.find { it > 5 }
            
            assertNull(result)
        }

        @Test
        fun `test all operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.all { it > 0 }
            
            assertTrue(result)
        }

        @Test
        fun `test all operation with all matching`() {
            val set = TaylorSet.of(1, 2, 3, 4, 5)
            val result = set.all { it > 0 }
            
            assertTrue(result)
        }

        @Test
        fun `test all operation with some not matching`() {
            val set = TaylorSet.of(-1, 1, 2, 3)
            val result = set.all { it > 0 }
            
            assertFalse(result)
        }

        @Test
        fun `test any operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.any { it > 0 }
            
            assertFalse(result)
        }

        @Test
        fun `test any operation with some matching`() {
            val set = TaylorSet.of(-2, -1, 0, 1, 2)
            val result = set.any { it > 0 }
            
            assertTrue(result)
        }

        @Test
        fun `test any operation with none matching`() {
            val set = TaylorSet.of(-3, -2, -1)
            val result = set.any { it > 0 }
            
            assertFalse(result)
        }

        @Test
        fun `test count operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val result = emptySet.count { it > 0 }
            
            assertEquals(0, result)
        }

        @Test
        fun `test count operation counting matching elements`() {
            val set = TaylorSet.of(1, 2, 3, 4, 5, 6)
            val result = set.count { it % 2 == 0 }
            
            assertEquals(3, result)
        }

        @Test
        fun `test forEach operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            val results = mutableListOf<Int>()
            
            emptySet.forEach { results.add(it) }
            
            assertTrue(results.isEmpty())
        }

        @Test
        fun `test forEach operation collecting elements`() {
            val set = TaylorSet.of(1, 2, 3)
            val results = mutableListOf<Int>()
            
            set.forEach { results.add(it) }
            
            assertEquals(3, results.size)
            assertEquals(listOf(1, 2, 3), results.sorted())
        }
    }

    // =============================================================================
    // Conversion and Utility Operations
    // =============================================================================

    @Nested
    inner class ConversionTests {

        @Test
        fun `test toList operation on empty set`() {
            val emptySet = TaylorSet.empty<String>()
            val result = emptySet.toList()
            
            assertTrue(result.isEmpty)
        }

        @Test
        fun `test toList operation returning sorted elements`() {
            val set = TaylorSet.of(3, 1, 4, 1, 5) // 1 appears twice
            val result = set.toList()
            
            assertEquals(4, result.size) // Duplicates removed
            assertEquals(listOf(1, 3, 4, 5), result.toKotlinList())
        }

        @Test
        fun `test toKotlinSet operation`() {
            val taylorSet = TaylorSet.of("c", "a", "b")
            val kotlinSet = taylorSet.toKotlinSet()
            
            assertEquals(3, kotlinSet.size)
            assertTrue(kotlinSet.contains("a"))
            assertTrue(kotlinSet.contains("b"))
            assertTrue(kotlinSet.contains("c"))
        }

        @Test
        fun `test toString operation on empty set`() {
            val emptySet = TaylorSet.empty<Int>()
            
            assertEquals("{}", emptySet.toString())
        }

        @Test
        fun `test toString operation on single element set`() {
            val singleSet = TaylorSet.of(42)
            
            assertEquals("{42}", singleSet.toString())
        }

        @Test
        fun `test toString operation on multiple element set`() {
            val set = TaylorSet.of(3, 1, 2)
            val result = set.toString()
            
            assertEquals("{1, 2, 3}", result) // Sorted order
        }
    }

    // =============================================================================
    // Equality and Hash Code
    // =============================================================================

    @Nested
    inner class EqualityTests {

        @Test
        fun `test equality with same instance`() {
            val set = TaylorSet.of(1, 2, 3)
            
            assertEquals(set, set)
            assertEquals(set.hashCode(), set.hashCode())
        }

        @Test
        fun `test equality with equivalent sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(3, 1, 2) // Different order
            
            assertEquals(set1, set2)
            assertEquals(set1.hashCode(), set2.hashCode())
        }

        @Test
        fun `test equality with different sets`() {
            val set1 = TaylorSet.of(1, 2, 3)
            val set2 = TaylorSet.of(1, 2, 4)
            
            assertNotEquals(set1, set2)
        }

        @Test
        fun `test equality with empty sets`() {
            val empty1 = TaylorSet.empty<Int>()
            val empty2 = TaylorSet.empty<String>()
            
            assertEquals(empty1, empty2)
            assertEquals(empty1.hashCode(), empty2.hashCode())
        }

        @Test
        fun `test equality with different sizes`() {
            val set1 = TaylorSet.of(1, 2)
            val set2 = TaylorSet.of(1, 2, 3)
            
            assertNotEquals(set1, set2)
        }

        @Test
        fun `test equality with non-TaylorSet object`() {
            val set = TaylorSet.of(1, 2, 3)
            val other = setOf(1, 2, 3)
            
            assertNotEquals(set, other)
        }
    }
}