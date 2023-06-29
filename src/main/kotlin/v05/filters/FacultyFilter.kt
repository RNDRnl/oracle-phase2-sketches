package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.shape.Rectangle
import v05.*

class FacultyFilterModel: FilterModel() {

    override val list = facultyNames
    override val states = list.map { ToggleFilterState() }
    val filteredList: List<String>
        get() = filter()

    fun filter(): List<String> {
        return list.filterIndexed { i, _ -> states[i].visible  }
    }

    init {
        states.forEach {
            it.stateChanged.listen {
                if (states.none { it.visible }) {
                    states.forEach {
                        it.visible = true
                    }
                }
                filterChanged.trigger(Unit)
            }
        }
    }

    fun reset() {
        states.forEach { it.visible = true }
    }

}

class FacultyFilterNew(val drawer: Drawer, val model: FacultyFilterModel): FilterNew() {

    var isMinimized = false
    override var title = "FACULTIES"

    override var headerBox = Rectangle(80.0, 90.0, 460.0 * 0.3, 32.0)
    override val bounds = Rectangle(80.0, 90.0 + 32.0, 460.0, 600.0)

    val facultyAbbrFm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)
    val facultyFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw() {

        val offsetX = if(isMinimized) 30.0 else 0.0

        drawer.stroke = ColorRGBa.WHITE
        drawer.fill = if(isCurrent) ColorRGBa.WHITE else null
        drawer.rectangle(headerBox)

        if(isVisible) {
            model.states.forEachIndexed { i, state ->

                val item = model.list[i]
                val itemBox = Rectangle(
                    bounds.x - (offsetX * 2.1),
                    i * 30.0 + (25.0 * i) + bounds.y,
                    80.0 - offsetX,
                    30.0
                )

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
