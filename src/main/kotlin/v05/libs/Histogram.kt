package v05.libs

import org.openrndr.extra.noise.uniform
import kotlin.math.abs

fun <T> List<T>.histogramOf(m: (T) -> Int?): Map<Int, Int> {
    if (this.isEmpty()) {
        return emptyMap()
    }
    val result = mutableMapOf<Int, Int>()

    for (i in this) {
        val index = m(i)
        if (index != null) {
            result[index] = result.getOrPut(index) { 0 } + 1
        }
    }
    return result
}

fun List<Int>.cumsum() : List<Int> {
    var sum = 0
    val result = mutableListOf<Int>()
    for (i in this) {
        sum += i
        result.add(sum)
    }
    return result
}


data class Bracket(val start:Int, val endInclusive: Int, val count: Int)

fun Map<Int, Int>.brackets() : List<Bracket> {

    if (this.isEmpty()) {
        return emptyList()
    }

    val minkey = this.keys.min()
    val maxkey = this.keys.max()

    val expanded = (minkey .. maxkey).map {
        val m = this.getOrElse(it) { 0 }
        m
    }
    val cs = expanded.mapIndexed { index, it -> Pair(index, it) }

    var splits = (cs.balancedSplit())
    splits = splits.map { it.balancedSplit() }.flatten()
    splits = splits.map { it.balancedSplit() }.flatten()
    splits = splits.map { it.balancedSplit() }.flatten()
//    splits = splits.map { it.balancedSplit() }.flatten()

    return splits.map {
        Bracket(it.first().first + minkey, it.last().first + minkey,
            ((it.first().first + minkey) .. (it.last().first + minkey)).sumOf {
                this@brackets.getOrElse(it) { 0 }
            }
        ) }
}

fun List<Pair<Int, Int>>.balancedSplit() : List<List<Pair<Int,Int>>> {
    var split = 1
    var minDiff = Int.MAX_VALUE

    if (this.size <= 1) {
        return listOf(this)
    }

    for (i in 1 until size-1) {

        var leftSum = 0
        for (s in 0 until i) {
            leftSum += this[s].second
        }
        var rightSum = 0
        for (s in i until size) {
            rightSum += this[s].second
        }

        val diff = abs(leftSum - rightSum)
        if (diff < minDiff) {
            minDiff = diff
            split = i
        }
    }
    return listOf(this.take(split), this.drop(split))
}


fun main() {
    val bla = List(4000) { Int.uniform(1900, 2023+1)} + List(4000) { Int.uniform(2000, 2023+1)}
    val h = bla.histogramOf { it }
    println(h)
    println(h.brackets())
}