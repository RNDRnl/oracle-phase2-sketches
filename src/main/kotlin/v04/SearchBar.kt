package v04

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KeyEvents
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.events.listen
import org.openrndr.shape.Rectangle
import kotlin.math.sin

class SearchBar(val keyboard: KeyEvents, val frame: Rectangle) {

    val queryChanged = Event<String>()
    var currentText: String = ""
        set(value) {
            field = value
            currentTextWidth = fm.textWidth(currentText)
            queryChanged.trigger(value)
        }

    var isBackspace = false
    var currentTextWidth = 0.0
        set(value) {
            field = value
            if(value > frame.width + frame.x) {
                val amt = fm.glyphMetrics[currentText.last()]?.advanceWidth ?: 0.0
                if (!isBackspace) {
                    offset -= amt
                } else {
                    offset += amt
                }
            }
        }
    var offset = 0.0

    fun setupListeners() {
        keyboard.character.listen {
            currentText += it.character
        }

        listOf(keyboard.keyDown, keyboard.keyRepeat).listen {
            if (it.key == KEY_BACKSPACE && currentText.isNotEmpty()) {
                isBackspace = true
                currentText = currentText.dropLast(1)
            } else {
                isBackspace = false
            }
        }
    }


    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 42.0)
    private val t = System.currentTimeMillis()

    fun draw(drawer: Drawer) = drawer.run {

        drawer.fontMap = fm
        drawer.stroke = null
        drawer.fill = ColorRGBa.WHITE


        text(currentText, frame.x + offset, frame.y + fm.height)

        // cursor
        drawer.fill = ColorRGBa.WHITE.opacify(sin((System.currentTimeMillis() - t) / 1000 * 20.0) *0.5 + 0.5)
        drawer.rectangle(frame.x + currentTextWidth, frame.y, 2.0, fm.height)

    }

    init {
        setupListeners()
    }

}
