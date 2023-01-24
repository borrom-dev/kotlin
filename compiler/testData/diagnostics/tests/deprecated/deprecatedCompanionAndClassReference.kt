// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// ISSUE: KT-54209

class A {
    @Deprecated("Deprecated companion")
    companion object
}


fun test() {
    A::class // KClass<A>
    A.<!DEPRECATION!>Companion<!>::class // KClass<A.Companion>
    (<!DEPRECATION!>A<!>)::class // // KClass<A.Companion>
}
