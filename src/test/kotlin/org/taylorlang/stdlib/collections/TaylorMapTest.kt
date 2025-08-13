package org.taylorlang.stdlib.collections

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

/**
 * Comprehensive test suite for TaylorMap implementation.
 * 
 * Tests all core functionality including:
 * - Construction and factory methods
 * - Access operations (get, containsKey, containsValue)
 * - Modification operations (put, remove)
 * - Collection operations (keys, values, entries)
 * - Functional operations (map, filter, fold, etc.)
 * - Conversion operations (toKotlinMap, toString)
 * - Equality and hash code
 */
class TaylorMapTest {

    // =============================================================================
    // Construction and Basic Properties
    // =============================================================================

    @Nested
    inner class ConstructionTests {

        @Test
        fun `test empty map creation`() {
            val emptyMap = TaylorMap.empty<String, Int>()
            
            assertTrue(emptyMap.isEmpty)
            assertEquals(0, emptyMap.size)
            assertNull(emptyMap.get("any"))
            assertFalse(emptyMap.containsKey("any"))
            assertFalse(emptyMap.containsValue(42))
        }

        @Test
        fun `test single entry map creation`() {
            val singleMap = TaylorMap.of("key1", 42)
            
            assertFalse(singleMap.isEmpty)
            assertEquals(1, singleMap.size)
            assertEquals(42, singleMap.get("key1"))
            assertTrue(singleMap.containsKey("key1"))
            assertTrue(singleMap.containsValue(42))
            assertFalse(singleMap.containsKey("other"))
            assertFalse(singleMap.containsValue(99))
        }

        @Test
        fun `test multiple entries map creation`() {
            val map = TaylorMap.of(
                "a" to 1,
                "b" to 2, 
                "c" to 3,
                "d" to 4
            )
            
            assertFalse(map.isEmpty)
            assertEquals(4, map.size)
            assertEquals(1, map.get("a"))
            assertEquals(2, map.get("b"))
            assertEquals(3, map.get("c"))
            assertEquals(4, map.get("d"))
            assertNull(map.get("e"))
        }

        @Test
        fun `test map from collection of pairs`() {
            val pairs = listOf("x" to 10, "y" to 20, "z" to 30)
            val map = TaylorMap.from(pairs)
            
            assertEquals(3, map.size)
            assertEquals(10, map.get("x"))
            assertEquals(20, map.get("y"))
            assertEquals(30, map.get("z"))
        }

        @Test
        fun `test map from Kotlin Map`() {
            val kotlinMap = mapOf("foo" to "bar", "hello" to "world")
            val taylorMap = TaylorMap.fromJava(kotlinMap)
            
            assertEquals(2, taylorMap.size)
            assertEquals("bar", taylorMap.get("foo"))
            assertEquals("world", taylorMap.get("hello"))
            assertEquals(kotlinMap, taylorMap.toKotlinMap())
        }

        @Test
        fun `test empty collections from empty map`() {
            val empty = TaylorMap.empty<String, Int>()
            
            assertTrue(empty.keys().isEmpty)
            assertTrue(empty.values().isEmpty)
            assertTrue(empty.entries().isEmpty)
        }
    }

    // =============================================================================
    // Access Operations
    // =============================================================================

    @Nested
    inner class AccessTests {

        @Test
        fun `test get operation`() {
            val map = TaylorMap.of(
                "apple" to 1,
                "banana" to 2,
                "cherry" to 3,
                "date" to 4
            )
            
            assertEquals(1, map["apple"])
            assertEquals(2, map["banana"])
            assertEquals(3, map["cherry"])
            assertEquals(4, map["date"])
            assertNull(map["elderberry"])
        }

        @Test
        fun `test get on empty map`() {
            val empty = TaylorMap.empty<String, Int>()
            
            assertNull(empty["anything"])
            assertNull(empty.get("anything"))
        }

        @Test
        fun `test containsKey operation`() {
            val map = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            
            assertTrue(map.containsKey("a"))
            assertTrue(map.containsKey("b"))
            assertTrue(map.containsKey("c"))
            assertFalse(map.containsKey("d"))
            assertFalse(map.containsKey(""))
        }

        @Test
        fun `test containsValue operation`() {
            val map = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            
            assertTrue(map.containsValue(1))
            assertTrue(map.containsValue(2))
            assertTrue(map.containsValue(3))
            assertFalse(map.containsValue(4))
            assertFalse(map.containsValue(0))
        }

        @Test
        fun `test containsValue with duplicate values`() {
            val map = TaylorMap.of("a" to 1, "b" to 1, "c" to 2)
            
            assertTrue(map.containsValue(1))
            assertTrue(map.containsValue(2))
            assertFalse(map.containsValue(3))
        }
    }

    // =============================================================================
    // Modification Operations  
    // =============================================================================

    @Nested
    inner class ModificationTests {

        @Test
        fun `test put operation on empty map`() {
            val empty = TaylorMap.empty<String, Int>()
            val updated = empty.put("key", 42)
            
            assertEquals(1, updated.size)
            assertEquals(42, updated.get("key"))
            // Original should remain empty
            assertTrue(empty.isEmpty)
        }

        @Test
        fun `test put operation adding new key`() {
            val original = TaylorMap.of("a" to 1, "b" to 2)
            val updated = original.put("c", 3)
            
            assertEquals(3, updated.size)
            assertEquals(1, updated.get("a"))
            assertEquals(2, updated.get("b"))
            assertEquals(3, updated.get("c"))
            // Original should be unchanged
            assertEquals(2, original.size)
            assertNull(original.get("c"))
        }

        @Test
        fun `test put operation replacing existing key`() {
            val original = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val updated = original.put("b", 99)
            
            assertEquals(3, updated.size)
            assertEquals(1, updated.get("a"))
            assertEquals(99, updated.get("b"))
            assertEquals(3, updated.get("c"))
            // Original should be unchanged
            assertEquals(2, original.get("b"))
        }

        @Test
        fun `test remove operation on existing key`() {
            val original = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val updated = original.remove("b")
            
            assertEquals(2, updated.size)
            assertEquals(1, updated.get("a"))
            assertNull(updated.get("b"))
            assertEquals(3, updated.get("c"))
            assertFalse(updated.containsKey("b"))
            // Original should be unchanged
            assertEquals(3, original.size)
            assertTrue(original.containsKey("b"))
        }

        @Test
        fun `test remove operation on non-existing key`() {
            val original = TaylorMap.of("a" to 1, "b" to 2)
            val updated = original.remove("c")
            
            assertEquals(2, updated.size)
            assertEquals(original.toKotlinMap(), updated.toKotlinMap())
        }

        @Test
        fun `test remove all keys results in empty map`() {
            val original = TaylorMap.of("a" to 1, "b" to 2)
            val empty = original.remove("a").remove("b")
            
            assertTrue(empty.isEmpty)
            assertEquals(0, empty.size)
        }

        @Test
        fun `test multiple puts maintain BST structure`() {
            var map = TaylorMap.empty<Int, String>()
            map = map.put(5, "five")
            map = map.put(3, "three")
            map = map.put(7, "seven")
            map = map.put(1, "one")
            map = map.put(9, "nine")
            
            assertEquals(5, map.size)
            assertEquals("one", map.get(1))
            assertEquals("three", map.get(3))
            assertEquals("five", map.get(5))
            assertEquals("seven", map.get(7))
            assertEquals("nine", map.get(9))
            
            // Keys should be in sorted order
            val keys = map.keys().toKotlinList()
            assertEquals(listOf(1, 3, 5, 7, 9), keys)
        }
    }

    // =============================================================================
    // Collection Operations
    // =============================================================================

    @Nested
    inner class CollectionOperationsTests {

        @Test
        fun `test keys operation`() {
            val map = TaylorMap.of("c" to 3, "a" to 1, "b" to 2)
            val keys = map.keys()
            
            assertEquals(3, keys.size)
            assertEquals(TaylorList.of("a", "b", "c"), keys)
        }

        @Test
        fun `test values operation`() {
            val map = TaylorMap.of("c" to 30, "a" to 10, "b" to 20)
            val values = map.values()
            
            assertEquals(3, values.size)
            assertEquals(TaylorList.of(10, 20, 30), values)
        }

        @Test
        fun `test entries operation`() {
            val map = TaylorMap.of("b" to 2, "a" to 1, "c" to 3)
            val entries = map.entries()
            
            assertEquals(3, entries.size)
            assertEquals(TaylorList.of("a" to 1, "b" to 2, "c" to 3), entries)
        }

        @Test
        fun `test empty map collections`() {
            val empty = TaylorMap.empty<String, Int>()
            
            assertTrue(empty.keys().isEmpty)
            assertTrue(empty.values().isEmpty)
            assertTrue(empty.entries().isEmpty)
        }
    }

    // =============================================================================
    // Functional Operations
    // =============================================================================

    @Nested
    inner class FunctionalOperationsTests {

        @Test
        fun `test mapValues operation`() {
            val original = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val doubled = original.mapValues { it * 2 }
            
            assertEquals(3, doubled.size)
            assertEquals(2, doubled.get("a"))
            assertEquals(4, doubled.get("b"))
            assertEquals(6, doubled.get("c"))
            // Original should be unchanged
            assertEquals(1, original.get("a"))
        }

        @Test
        fun `test mapKeys operation`() {
            val original = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val uppercased = original.mapKeys { it.uppercase() }
            
            assertEquals(3, uppercased.size)
            assertEquals(1, uppercased.get("A"))
            assertEquals(2, uppercased.get("B"))
            assertEquals(3, uppercased.get("C"))
            assertNull(uppercased.get("a"))
        }

        @Test
        fun `test map operation transforming both keys and values`() {
            val original = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val transformed = original.map { (k, v) -> k.uppercase() to v * 10 }
            
            assertEquals(3, transformed.size)
            assertEquals(10, transformed.get("A"))
            assertEquals(20, transformed.get("B"))
            assertEquals(30, transformed.get("C"))
        }

        @Test
        fun `test filter operation`() {
            val original = TaylorMap.of("a" to 1, "b" to 2, "c" to 3, "d" to 4)
            val evens = original.filter { (_, v) -> v % 2 == 0 }
            
            assertEquals(2, evens.size)
            assertEquals(2, evens.get("b"))
            assertEquals(4, evens.get("d"))
            assertNull(evens.get("a"))
            assertNull(evens.get("c"))
        }

        @Test
        fun `test filter with key predicate`() {
            val original = TaylorMap.of("apple" to 1, "banana" to 2, "cherry" to 3)
            val startsWithA = original.filter { (k, _) -> k.startsWith("a") }
            
            assertEquals(1, startsWithA.size)
            assertEquals(1, startsWithA.get("apple"))
        }

        @Test
        fun `test fold operation`() {
            val map = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val sum = map.fold(0) { acc, (_, v) -> acc + v }
            
            assertEquals(6, sum)
        }

        @Test
        fun `test fold with key and value`() {
            val map = TaylorMap.of("a" to 1, "b" to 2)
            val concatenated = map.fold("") { acc, (k, v) -> acc + k + v }
            
            assertEquals("a1b2", concatenated)
        }

        @Test
        fun `test find operation`() {
            val map = TaylorMap.of("a" to 1, "b" to 2, "c" to 3, "d" to 4)
            val found = map.find { (_, v) -> v > 2 }
            
            assertNotNull(found)
            assertTrue(found!!.second > 2)
        }

        @Test
        fun `test find operation not found`() {
            val map = TaylorMap.of("a" to 1, "b" to 2)
            val notFound = map.find { (_, v) -> v > 10 }
            
            assertNull(notFound)
        }

        @Test
        fun `test all operation`() {
            val map = TaylorMap.of("a" to 2, "b" to 4, "c" to 6)
            
            assertTrue(map.all { (_, v) -> v % 2 == 0 })
            assertFalse(map.all { (_, v) -> v > 3 })
        }

        @Test
        fun `test any operation`() {
            val map = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            
            assertTrue(map.any { (_, v) -> v % 2 == 0 })
            assertFalse(map.any { (_, v) -> v > 10 })
        }

        @Test
        fun `test count operation`() {
            val map = TaylorMap.of("a" to 1, "b" to 2, "c" to 3, "d" to 4)
            val evenCount = map.count { (_, v) -> v % 2 == 0 }
            
            assertEquals(2, evenCount)
        }

        @Test
        fun `test operations on empty map`() {
            val empty = TaylorMap.empty<String, Int>()
            
            assertTrue(empty.mapValues { it * 2 }.isEmpty)
            assertTrue(empty.filter { (_, v) -> v > 0 }.isEmpty)
            assertEquals(0, empty.fold(0) { acc, (_, v) -> acc + v })
            assertNull(empty.find { (_, v) -> v > 0 })
            assertTrue(empty.all { (_, v) -> v > 0 })
            assertFalse(empty.any { (_, v) -> v > 0 })
            assertEquals(0, empty.count { (_, v) -> v > 0 })
        }
    }

    // =============================================================================
    // Conversion and Utility Operations
    // =============================================================================

    @Nested
    inner class ConversionTests {

        @Test
        fun `test toKotlinMap conversion`() {
            val taylorMap = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val kotlinMap = taylorMap.toKotlinMap()
            
            assertEquals(3, kotlinMap.size)
            assertEquals(1, kotlinMap["a"])
            assertEquals(2, kotlinMap["b"])
            assertEquals(3, kotlinMap["c"])
        }

        @Test
        fun `test toString operation`() {
            val empty = TaylorMap.empty<String, Int>()
            assertEquals("{}", empty.toString())
            
            val single = TaylorMap.of("key", "value")
            assertEquals("{key=value}", single.toString())
            
            val multiple = TaylorMap.of("a" to 1, "b" to 2)
            val str = multiple.toString()
            assertTrue(str.contains("a=1"))
            assertTrue(str.contains("b=2"))
            assertTrue(str.startsWith("{"))
            assertTrue(str.endsWith("}"))
        }

        @Test
        fun `test round-trip conversion`() {
            val original = mapOf("x" to 10, "y" to 20, "z" to 30)
            val taylorMap = TaylorMap.fromJava(original)
            val converted = taylorMap.toKotlinMap()
            
            assertEquals(original, converted)
        }
    }

    // =============================================================================
    // Equality and Hash Code
    // =============================================================================

    @Nested
    inner class EqualityTests {

        @Test
        fun `test equality with same contents`() {
            val map1 = TaylorMap.of("a" to 1, "b" to 2, "c" to 3)
            val map2 = TaylorMap.of("c" to 3, "a" to 1, "b" to 2) // Different order
            
            assertEquals(map1, map2)
            assertEquals(map1.hashCode(), map2.hashCode())
        }

        @Test
        fun `test equality with different contents`() {
            val map1 = TaylorMap.of("a" to 1, "b" to 2)
            val map2 = TaylorMap.of("a" to 1, "b" to 3)
            val map3 = TaylorMap.of("a" to 1, "c" to 2)
            
            assertNotEquals(map1, map2)
            assertNotEquals(map1, map3)
            assertNotEquals(map2, map3)
        }

        @Test
        fun `test equality with empty maps`() {
            val empty1 = TaylorMap.empty<String, Int>()
            val empty2 = TaylorMap.empty<String, Int>()
            
            assertEquals(empty1, empty2)
            assertEquals(empty1.hashCode(), empty2.hashCode())
        }

        @Test
        fun `test equality with different types`() {
            val map = TaylorMap.of("a" to 1)
            
            assertNotEquals(map, null)
            assertNotEquals(map, "not a map")
            assertNotEquals(map, listOf("a" to 1))
        }

        @Test
        fun `test self equality`() {
            val map = TaylorMap.of("a" to 1, "b" to 2)
            
            assertEquals(map, map)
            assertTrue(map == map)
        }
    }

    // =============================================================================
    // Edge Cases and Complex Scenarios
    // =============================================================================

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `test large map operations`() {
            var map = TaylorMap.empty<Int, String>()
            
            // Add 100 entries
            for (i in 1..100) {
                map = map.put(i, "value$i")
            }
            
            assertEquals(100, map.size)
            assertEquals("value50", map.get(50))
            assertTrue(map.containsKey(1))
            assertTrue(map.containsKey(100))
            assertFalse(map.containsKey(101))
            
            // Remove half the entries
            for (i in 1..50) {
                map = map.remove(i)
            }
            
            assertEquals(50, map.size)
            assertNull(map.get(25))
            assertEquals("value75", map.get(75))
        }

        @Test
        fun `test chained operations`() {
            val result = TaylorMap.of("a" to 1, "b" to 2, "c" to 3, "d" to 4)
                .put("e", 5)
                .remove("b")
                .mapValues { it * 10 }
                .filter { (_, v) -> v > 20 }
            
            assertEquals(3, result.size)
            assertEquals(30, result.get("c"))
            assertEquals(40, result.get("d"))
            assertEquals(50, result.get("e"))
            assertNull(result.get("a"))
            assertNull(result.get("b"))
        }

        @Test
        fun `test with null values`() {
            val map = TaylorMap.of("a" to null, "b" to "value")
            
            assertEquals(2, map.size)
            assertNull(map.get("a"))
            assertEquals("value", map.get("b"))
            assertTrue(map.containsKey("a"))
            assertTrue(map.containsValue(null))
            assertTrue(map.containsValue("value"))
        }

        @Test
        fun `test BST balancing with sequential inserts`() {
            var map = TaylorMap.empty<Int, Int>()
            
            // Insert in ascending order (worst case for unbalanced BST)
            for (i in 1..10) {
                map = map.put(i, i * i)
            }
            
            assertEquals(10, map.size)
            for (i in 1..10) {
                assertEquals(i * i, map.get(i))
            }
            
            // Keys should still be in sorted order
            val keys = map.keys().toKotlinList()
            assertEquals((1..10).toList(), keys)
        }
    }
}