package v04

import org.openrndr.draw.FontImageMap

fun FontImageMap.textWidth(string: String): Double {
    return string.fold(0.0) { a, b -> (glyphMetrics[b]?.advanceWidth ?: 0.0) + a }
}