// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class E {
    A;

    fun values(b: Boolean) {}
    fun E.values(): Array<E> = arrayOf(A)
}

fun f(e: E) = when (e) {
    E.A -> "OK"
}

fun box(): String {
    return f(E.A)
}
