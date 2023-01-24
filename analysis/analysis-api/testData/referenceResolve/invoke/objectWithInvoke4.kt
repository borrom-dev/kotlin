class Foo {
    companion object Bar {
        operator fun invoke() {}
    }
}
fun test() {
    Fo<caret>o.Bar()
}
