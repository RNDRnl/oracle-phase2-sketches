package v04

import kotlin.math.min


class StringDistance(var left: String, var right: String) {
    val table: Array<IntArray> = Array(left.length + 1) { IntArray(right.length + 1) }
    val steps: MutableList<String> = ArrayList()
    private fun findPath(): List<IntArray?> {
        var i = table.size - 1
        var j = table[0].size - 1
        val path: MutableList<IntArray?> = ArrayList()
        path.add(intArrayOf(i, j))
        while (!(i == 0 && j == 0)) {
            val candidates: MutableList<IntArray> = ArrayList()
            val current = table[i][j]
            if (i > 0) {
                if (table[i - 1][j] < current) {
                    candidates.add(intArrayOf(i - 1, j))
                }
            }
            if (j > 0) {
                if (table[i][j - 1] < current) {
                    candidates.add(intArrayOf(i, j - 1))
                }
            }
            if (i > 0 && j > 0) {
                if (table[i - 1][j - 1] <= current) {
                    candidates.add(intArrayOf(i - 1, j - 1))
                }
            }
            path.add(candidates[0])
            i = candidates[0][0]
            j = candidates[0][1]
        }

        return path.reversed()
    }

    fun step(t: Double): String {
        var idx = (steps.size * t).toInt()
        if (idx < 0) idx = 0
        if (idx >= steps.size) {
            idx = steps.size - 1
        }
        return steps[idx]
    }

    fun findSteps() {
        val path = findPath()
        var step = left
        steps.add(step)
        var cursor = 0
        var lastPos: IntArray? = intArrayOf(0, 0)
        for (pos in path.subList(1, path.size)) {
            val j = pos!![1]
            val di = pos[0] - lastPos!![0]
            val dj = pos[1] - lastPos[1]
            if (di == 1 && dj == 0) {
                step = step.substring(0, cursor) + if (cursor + 1 <= step.length) step.substring(cursor + 1) else ""
            } else if (dj == 1 && di == 1) {
                step = step.substring(
                    0,
                    cursor
                ) + right[j - 1] + if (cursor + 1 <= step.length) step.substring(cursor + 1) else ""
                cursor++
            } else if ((di == 0) and (dj == 1)) {
                step =
                    step.substring(0, cursor) + right[j - 1] + if (cursor < step.length) step.substring(cursor) else ""
                cursor++
            }

//            System.out.println(step);
            lastPos = pos
            steps.add(step)
        }
    }

    init {
        for (i in 0 until left.length + 1) {
            table[i][0] = i
        }
        for (j in 0 until right.length + 1) {
            table[0][j] = j
        }
        for (j in 1 until right.length + 1) {
            for (i in 1 until left.length + 1) {
                if (left[i - 1] == right[j - 1]) {
                    table[i][j] = table[i - 1][j - 1]
                } else {
                    table[i][j] = min(min(table[i - 1][j] + 1, table[i][j - 1] + 1), table[i - 1][j - 1] + 1)
                }
            }
        }
        findSteps()
    }
}

fun textSteps(left:String, right:String): List<String> {
    val sd = StringDistance(left, right)
    return sd.steps
}

fun String.mix(other: String, factor: Double) : String {
    val steps = textSteps(this, other)
    return steps[(factor.coerceIn(0.0, 1.0) * steps.size).toInt().coerceAtMost(steps.size-1)]
}