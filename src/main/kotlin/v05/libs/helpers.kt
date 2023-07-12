package v05

import org.openrndr.draw.FontImageMap
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4

fun FontImageMap.textWidth(string: String): Double {
    return string.fold(0.0) { a, b -> (glyphMetrics[b]?.advanceWidth ?: 0.0) + a }
}

fun Vector2.transform(m : Matrix44) : Vector2 {
    return (m * this.xy01).xy
}

fun Matrix44.scale(): Double = (this * Vector4(1.0, 1.0, 0.0, 0.0).normalized).xy.length

val lipsum = "Lorem ipsum dolor sit amet. " +
        "Quo quia delectus sed iste eaque nam deleniti " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! "