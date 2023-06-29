package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.shape.Rectangle
import v05.*

class FacultyFilter(val drawer: Drawer, val model: FacultyFilterModel): Filter() {

    override var visible = true
    override var isCurrent = true
    var isMinimized = false
    override var title = "FACULTIES"

    init {
        actionBounds = Rectangle(10.0, 0.0, 460.0, 600.0)

        buttonDown.listen {
            it.cancelPropagation()
            for (i in model.states.indices) {
                if (it.position in itemBox(i)) {
                    model.states[i].visible = !model.states[i].visible
                }
            }
        }
    }

    fun itemBox(i: Int): Rectangle {
        val offsetX = if(isMinimized) 30.0 else 0.0

        return Rectangle(
            actionBounds.x - (offsetX * 2.1),
            i * 30.0 + (25.0 * i) + actionBounds.y,
            80.0 - offsetX,
            30.0
        )
    }

    val facultyAbbrFm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)
    val facultyFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw() {

        if(visible) {
            model.states.forEachIndexed { i, state ->

                val item = model.list[i]
                val itemBox = itemBox(i)

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = facultyAbbrFm
                    drawer.fill = if (state.visible) facultyColors[i] else ColorRGBa.TRANSPARENT
                    drawer.stroke = facultyColors[i]
                    drawer.roundedRectangle(itemBox.toRounded(999.0))

                    val t0 = item.facultyAbbreviation()
                    drawer.fill =  if (state.visible) ColorRGBa.BLACK else facultyColors[i]
                    cursor.x = itemBox.center.x - textWidth(t0) / 2.0
                    cursor.y = itemBox.center.y + facultyAbbrFm.height / 2.0
                    text(t0)

                    if(!isMinimized) {
                        drawer.fontMap = facultyFm
                        drawer.fill = ColorRGBa.WHITE.opacify(0.8)
                        cursor.x = itemBox.corner.x + itemBox.width + 5.0
                        text(item.uppercase())
                    }

                }

            }
        }

    }
}
