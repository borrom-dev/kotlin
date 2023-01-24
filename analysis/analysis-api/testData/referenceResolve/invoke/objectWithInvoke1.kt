object Foo {

    object Bar {
        operator fun invoke() {}
    }
}
fun test() {
    Fo<caret>o.Bar()
}