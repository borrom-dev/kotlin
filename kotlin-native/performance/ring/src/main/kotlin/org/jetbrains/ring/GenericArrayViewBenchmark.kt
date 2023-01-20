/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

// Benchmark is inspired by multik library.

interface MemoryView<T> where T : Number {
    public abstract fun get(index: Int): T
}

class MemoryViewIntArray(val data: IntArray) : MemoryView<Int> {
    override fun get(index: Int): Int = data[index]
}

class MemoryViewLongArray(val data: LongArray) : MemoryView<Long> {
    override fun get(index: Int): Long = data[index]
}

class MemoryViewDoubleArray(val data: DoubleArray) : MemoryView<Double> {
    override fun get(index: Int): Double = data[index]
}

class Array2D<T>(val data: MemoryView<T>, val width: Int, val height: Int) where T : Number{
    public fun getGeneric(ind1: Int, ind2: Int): Int {
        return data.get(width * ind1 + ind2).toInt()
    }

    public inline fun getGenericInlined(ind1: Int, ind2: Int): Int {
        return data.get(width * ind1 + ind2).toInt()
    }

    public inline fun getSpecializedInlined(ind1: Int, ind2: Int): Int {
        return (data as MemoryViewIntArray).get(width * ind1 + ind2)
    }
}

open class GenericArrayViewBenchmark {
    private val N = 2000
    private val gold = {
        val maxNum = (N * N - 1).toLong()
        val sum = maxNum * (maxNum + 1) / 2
        sum.toInt()
    }()

    private val intArr = Array2D<Int>(MemoryViewIntArray(IntArray(N * N, {i -> i})), N, N)
    // To confuse devirtualizer:
    private val longArr = Array2D<Long>(MemoryViewLongArray(LongArray(N * N, {i -> i.toLong()})), N, N)
    private val doubleArr = Array2D<Double>(MemoryViewDoubleArray(DoubleArray(N * N, {i -> i.toDouble()})), N, N)

    private inline fun <T> bench(arr: Array2D<T>, getter: (Array2D<T>, Int, Int) -> Int) where T : Number {
        var sum = 0

        for (i in 0 until N) {
            for (j in 0 until N) {
                sum += getter(arr, i, j)
            }
        }

        chechResult(sum)
    }

    private fun chechResult(sum: Int) {
        if (sum != gold) {
            throw AssertionError("Incorrect result $sum ($gold expected)")
        }
    }

    // Bench cases:

    fun origin() { bench(intArr, {a, i, j -> a.getGeneric(i, j)}) }
    fun inlined() { bench(intArr, {a, i, j -> a.getGenericInlined(i, j)}) }
    fun specialized() { bench(intArr, {a, i, j -> a.getSpecializedInlined(i, j)}) }
    fun manual() { bench(intArr, {a, i, j -> a.width * i + j}) }
}
