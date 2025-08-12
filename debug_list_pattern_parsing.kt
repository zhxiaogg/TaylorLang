fun testListPatterns() {
    val patterns = listOf(
        "match x { case [] => 0 }",
        "match x { case [a] => a }",
        "match x { case [a, b] => a + b }",
        "match x { case [first, ...rest] => first }"
    )
    
    for (pattern in patterns) {
        println("Testing: $pattern")
    }
}