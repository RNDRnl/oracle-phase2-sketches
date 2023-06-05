import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle
import v04.VirtualKeyboard
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1920
        height = 1080
        position = IntVector2(0, - 1280)
    }

    program {
        val key = VirtualKeyboard(Rectangle(0.0, height / 2.0, width * 1.0, height / 2.0))
        extend {
            key.draw(drawer)
        }
    }
}
