import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import org.openrndr.draw.*
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.color.presets.PURPLE
import org.openrndr.extra.olive.OliveProgram
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle
import java.lang.StringBuilder
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 720
        height = 1280
    }

    class Motion : Animatable() {
        var backgroundFade = 0.0
    }

    program {
        val fontRef = loadFont("data/fonts/default.otf", 64.0)
        val motion = Motion()

        val rt = renderTarget(720, 720) {
            colorBuffer()
        }

        drawer.isolatedWithTarget(rt) {
            drawer.clear(ColorRGBa.BLUE)
        }

        var value: String = ""
        var valueShadow: String = ""

        fun addStringAtIndex(originalString: String, index: Int, stringToAdd: String): String {
            if (index < 0 || index > originalString.length) {
                throw IndexOutOfBoundsException("Invalid index")
            }

            val stringBuilder = StringBuilder(originalString)
            stringBuilder.insert(index, stringToAdd)
            return stringBuilder.toString()
        }

        fun removeCharAtIndex(input: String, index: Int): String {
            if (index < 0 || index >= input.length) {
                return input // Return the original string if the index is out of range
            }

            val stringBuilder = StringBuilder(input)
            stringBuilder.deleteCharAt(index)
            return stringBuilder.toString()
        }

        keyboard.character.listen {
            value = addStringAtIndex(value, valueShadow.length, it.character.toString())
            valueShadow += it.character
        }

        keyboard.keyDown.listen {
            if(it.key == KEY_ENTER && it.modifiers.contains(KeyModifier.SHIFT)) {
                // trigger
                motion.apply {
                    ::backgroundFade.animate(1.0, 0, Easing.CubicInOut)
                    ::backgroundFade.animate(0.0, 250, Easing.CubicInOut)
                }
            }
        }

        fun keyFunction(trigger: KeyEvent) {
            if (trigger.key == KEY_BACKSPACE) {
                if(value.isNotEmpty()) {
                    value = removeCharAtIndex(value, valueShadow.length-1)
                    valueShadow = valueShadow.substring(0, valueShadow.length - 1)
                }
            }

            if(trigger.key == KEY_DELETE) {
                value = removeCharAtIndex(value, valueShadow.length)
            }

            // shadow
            if(trigger.key == KEY_ARROW_LEFT) {
                if(valueShadow.isNotEmpty()) {
                    valueShadow = valueShadow.substring(0, valueShadow.length - 1)
                }
            }
            if(trigger.key == KEY_ARROW_RIGHT) {
                if(valueShadow.length < value.length) {
                    valueShadow += value[valueShadow.length]
                }
            }
        }

        keyboard.keyDown.listen {
            keyFunction(it)
        }

        keyboard.keyRepeat.listen {
            keyFunction(it)
        }

        extend {
            motion.updateAnimation()

            drawer.clear(ColorRGBa.BLACK)
            drawer.image(rt.colorBuffer(0))

            writer {
                drawer.fontMap = fontRef
                drawer.fill = ColorRGBa.WHITE
                box = Rectangle(0.0, 720.0 + 48, width*1.0, height*1.0)
                text(value, true)
                drawer.fill = mix(ColorRGBa.DARK_GRAY.shade(0.2), ColorRGBa.WHITE, motion.backgroundFade)
                drawer.rectangle(0.0, 720.0, width*1.0, cursor.y-720.0+20)
            }

            writer {
                drawer.fontMap = fontRef
                drawer.fill = ColorRGBa.TRANSPARENT
                box = Rectangle(0.0, 720.0 + 48, width*1.0, height*1.0)
                text(valueShadow, true)
                drawer.fill = ColorRGBa.WHITE.opacify(sin(seconds*20.0)*0.5 + 0.5)
                drawer.stroke = null
                drawer.rectangle(cursor.x, cursor.y-40.0, 2.0, 50.0)
            }

            writer {
                drawer.fontMap = fontRef
                drawer.fill = ColorRGBa.WHITE
                box = Rectangle(0.0, 720.0 + 48, width*1.0, height*1.0)
                text(value, true)
            }
        }
    }
}
