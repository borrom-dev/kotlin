// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: FAILS_IN_JS_IR
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JVM, JVM_IR


fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}


inline fun inlineF(a:Int, vararg b: String, c: Int = 0) = 0
inline fun <reified T> inlineReifiedF(a: T, vararg b: String, c: Int = 0) = 0
fun normalF(a:Int, vararg b: String, c: Int = 0) = 0

fun noConversion(ref: (Int, Array<String>, Int) -> Int) = ref
fun suspendConversion(ref: suspend (Int, Array<String>, Int) -> Int) = ref
fun unitConversion(ref: (Int, Array<String>, Int) -> Unit) = ref
fun defaultConversion(ref: (Int, Array<String>) -> Int) = ref
fun varargConversion(ref: (Int, String, Int) -> Int) = ref
fun suspendAndUnitConversion(ref: suspend (Int, Array<String>, Int) -> Unit) = ref
fun suspendAndDefaultConversion(ref: suspend (Int, Array<String>) -> Int) = ref
fun suspendAndVarargConversion(ref: suspend (Int, String, Int) -> Int) = ref
fun unitAndDefaultConversion(ref: (Int, Array<String>) -> Unit) = ref
fun unitAndVarargConversion(ref: (Int, String, Int) -> Unit) = ref
fun defaultAndVarargConversion(ref: (Int, String) -> Int) = ref
fun allExeceptSuspendConversion(ref: (Int, String) -> Unit) = ref
fun allExeceptUnitConversion(ref: suspend (Int, String) -> Int) = ref
fun allExeceptVarargConversion(ref: suspend (Int, Array<String>) -> Unit) = ref
fun allExeceptDefaultConversion(ref: suspend (Int, String, Int) -> Unit) = ref
fun allConversions(ref: suspend (Int, String) -> Unit) = ref

fun testNormal() {
    checkEqual(noConversion(::normalF), noConversion(::normalF))
    checkEqual(suspendConversion(::normalF), suspendConversion(::normalF))
    checkEqual(unitConversion(::normalF), unitConversion(::normalF))
    checkEqual(defaultConversion(::normalF), defaultConversion(::normalF))
    checkEqual(varargConversion(::normalF), varargConversion(::normalF))
    checkEqual(suspendAndUnitConversion(::normalF), suspendAndUnitConversion(::normalF))
    checkEqual(suspendAndDefaultConversion(::normalF), suspendAndDefaultConversion(::normalF))
    checkEqual(suspendAndVarargConversion(::normalF), suspendAndVarargConversion(::normalF))
    checkEqual(unitAndDefaultConversion(::normalF), unitAndDefaultConversion(::normalF))
    checkEqual(unitAndVarargConversion(::normalF), unitAndVarargConversion(::normalF))
    checkEqual(defaultAndVarargConversion(::normalF), defaultAndVarargConversion(::normalF))
    checkEqual(allExeceptSuspendConversion(::normalF), allExeceptSuspendConversion(::normalF))
    checkEqual(allExeceptUnitConversion(::normalF), allExeceptUnitConversion(::normalF))
    checkEqual(allExeceptVarargConversion(::normalF), allExeceptVarargConversion(::normalF))
    checkEqual(allExeceptDefaultConversion(::normalF), allExeceptDefaultConversion(::normalF))
    checkEqual(allConversions(::normalF), allConversions(::normalF))
}

fun testInline() {
    checkEqual(noConversion(::inlineF), noConversion(::inlineF))
    checkEqual(suspendConversion(::inlineF), suspendConversion(::inlineF))
    checkEqual(unitConversion(::inlineF), unitConversion(::inlineF))
    checkEqual(defaultConversion(::inlineF), defaultConversion(::inlineF))
    checkEqual(varargConversion(::inlineF), varargConversion(::inlineF))
    checkEqual(suspendAndUnitConversion(::inlineF), suspendAndUnitConversion(::inlineF))
    checkEqual(suspendAndDefaultConversion(::inlineF), suspendAndDefaultConversion(::inlineF))
    checkEqual(suspendAndVarargConversion(::inlineF), suspendAndVarargConversion(::inlineF))
    checkEqual(unitAndDefaultConversion(::inlineF), unitAndDefaultConversion(::inlineF))
    checkEqual(unitAndVarargConversion(::inlineF), unitAndVarargConversion(::inlineF))
    checkEqual(defaultAndVarargConversion(::inlineF), defaultAndVarargConversion(::inlineF))
    checkEqual(allExeceptSuspendConversion(::inlineF), allExeceptSuspendConversion(::inlineF))
    checkEqual(allExeceptUnitConversion(::inlineF), allExeceptUnitConversion(::inlineF))
    checkEqual(allExeceptVarargConversion(::inlineF), allExeceptVarargConversion(::inlineF))
    checkEqual(allExeceptDefaultConversion(::inlineF), allExeceptDefaultConversion(::inlineF))
    checkEqual(allConversions(::inlineF), allConversions(::inlineF))
}

fun testInlineReifined() {
    checkEqual(noConversion(::inlineReifiedF), noConversion(::inlineReifiedF))
    checkEqual(suspendConversion(::inlineReifiedF), suspendConversion(::inlineReifiedF))
    checkEqual(unitConversion(::inlineReifiedF), unitConversion(::inlineReifiedF))
    checkEqual(defaultConversion(::inlineReifiedF), defaultConversion(::inlineReifiedF))
    checkEqual(varargConversion(::inlineReifiedF), varargConversion(::inlineReifiedF))
    checkEqual(suspendAndUnitConversion(::inlineReifiedF), suspendAndUnitConversion(::inlineReifiedF))
    checkEqual(suspendAndDefaultConversion(::inlineReifiedF), suspendAndDefaultConversion(::inlineReifiedF))
    checkEqual(suspendAndVarargConversion(::inlineReifiedF), suspendAndVarargConversion(::inlineReifiedF))
    checkEqual(unitAndDefaultConversion(::inlineReifiedF), unitAndDefaultConversion(::inlineReifiedF))
    checkEqual(unitAndVarargConversion(::inlineReifiedF), unitAndVarargConversion(::inlineReifiedF))
    checkEqual(defaultAndVarargConversion(::inlineReifiedF), defaultAndVarargConversion(::inlineReifiedF))
    checkEqual(allExeceptSuspendConversion(::inlineReifiedF), allExeceptSuspendConversion(::inlineReifiedF))
    checkEqual(allExeceptUnitConversion(::inlineReifiedF), allExeceptUnitConversion(::inlineReifiedF))
    checkEqual(allExeceptVarargConversion(::inlineReifiedF), allExeceptVarargConversion(::inlineReifiedF))
    checkEqual(allExeceptDefaultConversion(::inlineReifiedF), allExeceptDefaultConversion(::inlineReifiedF))
    checkEqual(allConversions(::inlineReifiedF), allConversions(::inlineReifiedF))
}

fun box() : String {
    testNormal()
    testInline()
    testInlineReifined()

    val allRefs = mapOf(
        "noConversionNormal" to noConversion(::normalF),
        "suspendConversionNormal" to suspendConversion(::normalF),
        "unitConversionNormal" to unitConversion(::normalF),
        "defaultConversionNormal" to defaultConversion(::normalF),
        "varargConversionNormal" to varargConversion(::normalF),
        "suspendAndUnitConversionNormal" to suspendAndUnitConversion(::normalF),
        "suspendAndDefaultConversionNormal" to suspendAndDefaultConversion(::normalF),
        "suspendAndVarargConversionNormal" to suspendAndVarargConversion(::normalF),
        "unitAndDefaultConversionNormal" to unitAndDefaultConversion(::normalF),
        "unitAndVarargConversionNormal" to unitAndVarargConversion(::normalF),
        "defaultAndVarargConversionNormal" to defaultAndVarargConversion(::normalF),
        "allExeceptSuspendConversionNormal" to allExeceptSuspendConversion(::normalF),
        "allExeceptUnitConversionNormal" to allExeceptUnitConversion(::normalF),
        "allExeceptVarargConversionNormal" to allExeceptVarargConversion(::normalF),
        "allExeceptDefaultConversionNormal" to allExeceptDefaultConversion(::normalF),
        "allConversionsNormal" to allConversions(::normalF),

        "noConversionInline" to noConversion(::inlineF),
        "suspendConversionInline" to suspendConversion(::inlineF),
        "unitConversionInline" to unitConversion(::inlineF),
        "defaultConversionInline" to defaultConversion(::inlineF),
        "varargConversionInline" to varargConversion(::inlineF),
        "suspendAndUnitConversionInline" to suspendAndUnitConversion(::inlineF),
        "suspendAndDefaultConversionInline" to suspendAndDefaultConversion(::inlineF),
        "suspendAndVarargConversionInline" to suspendAndVarargConversion(::inlineF),
        "unitAndDefaultConversionInline" to unitAndDefaultConversion(::inlineF),
        "unitAndVarargConversionInline" to unitAndVarargConversion(::inlineF),
        "defaultAndVarargConversionInline" to defaultAndVarargConversion(::inlineF),
        "allExeceptSuspendConversionInline" to allExeceptSuspendConversion(::inlineF),
        "allExeceptUnitConversionInline" to allExeceptUnitConversion(::inlineF),
        "allExeceptVarargConversionInline" to allExeceptVarargConversion(::inlineF),
        "allExeceptDefaultConversionInline" to allExeceptDefaultConversion(::inlineF),
        "allConversionsInline" to allConversions(::inlineF),

        "noConversionInlineReified" to noConversion(::inlineReifiedF),
        "suspendConversionInlineReified" to suspendConversion(::inlineReifiedF),
        "unitConversionInlineReified" to unitConversion(::inlineReifiedF),
        "defaultConversionInlineReified" to defaultConversion(::inlineReifiedF),
        "varargConversionInlineReified" to varargConversion(::inlineReifiedF),
        "suspendAndUnitConversionInlineReified" to suspendAndUnitConversion(::inlineReifiedF),
        "suspendAndDefaultConversionInlineReified" to suspendAndDefaultConversion(::inlineReifiedF),
        "suspendAndVarargConversionInlineReified" to suspendAndVarargConversion(::inlineReifiedF),
        "unitAndDefaultConversionInlineReified" to unitAndDefaultConversion(::inlineReifiedF),
        "unitAndVarargConversionInlineReified" to unitAndVarargConversion(::inlineReifiedF),
        "defaultAndVarargConversionInlineReified" to defaultAndVarargConversion(::inlineReifiedF),
        "allExeceptSuspendConversionInlineReified" to allExeceptSuspendConversion(::inlineReifiedF),
        "allExeceptUnitConversionInlineReified" to allExeceptUnitConversion(::inlineReifiedF),
        "allExeceptVarargConversionInlineReified" to allExeceptVarargConversion(::inlineReifiedF),
        "allExeceptDefaultConversionInlineReified" to allExeceptDefaultConversion(::inlineReifiedF),
        "allConversionsInlineReified" to allConversions(::inlineReifiedF),
    )

    for ((name1, ref1) in allRefs) {
        for ((name2, ref2) in allRefs) {
            if (name1 != name2) {
                if (ref1 == ref2) {
                    return "$name1 and $name2 wrappers should not be equal"
                }
            }
        }
    }

    return "OK"
}