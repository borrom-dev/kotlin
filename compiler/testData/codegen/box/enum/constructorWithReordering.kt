// IGNORE_BACKEND: JS
// KT-55828
// IGNORE_BACKEND_K2: NATIVE
fun f(): String = "O"
fun g(): String = "K"

enum class E(val x: String, val y: String) {
    A(y = g(), x = f())
}

fun box(): String = E.A.x + E.A.y
