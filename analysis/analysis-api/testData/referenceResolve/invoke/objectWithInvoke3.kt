class Foo {
    companion object Bar {
        operator fun invoke() {}
    }
}
fun test() {
    Foo.B<caret>ar()
}
