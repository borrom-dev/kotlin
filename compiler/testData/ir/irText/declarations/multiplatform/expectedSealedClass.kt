// IGNORE_BACKEND_K2: JVM_IR
// !LANGUAGE: +MultiPlatformProjects

expect sealed class Ops()
expect class Add() : Ops

actual sealed class Ops actual constructor()
actual class Add actual constructor() : Ops()