// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class Bar {
    ONE,
    TWO
}

fun isOne(i: Bar) = i == Bar.ONE

fun box(): String {
    return when (isOne(Bar.ONE) && !isOne(Bar.TWO) && Bar.ONE != Bar.TWO) {
        true -> "OK"
        else -> "Failure"
    }
}
