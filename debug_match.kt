fun main() {
    val x = match 42 {
        42 -> println("matched")
        _ -> println("no match")
    }
}