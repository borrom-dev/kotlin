object Foo {
    object Bar {
        operator fun invoke() {}
    }
}
fun test() {
    Foo.B<caret>ar()
}
