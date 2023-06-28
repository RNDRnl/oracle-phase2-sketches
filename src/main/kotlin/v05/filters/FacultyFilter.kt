package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.facultyAbbreviation
import v05.facultyColors
import kotlin.math.sin

class FacultyFilter(list: List<String>): Filter(list) {

    override var isActive = true
    override var title = "FACULTIES"

    val facultyAbbrFm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)
    val facultyFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw(drawer: Drawer, bounds: Rectangle) {
        headerBox = Rectangle(80.0, 90.0, (bounds.width - 80.0) * 0.3, 32.0)
        beforeDraw(drawer, bounds)

        if(isActive) {
            entriesInView = list.withIndex().associate { (i, item) ->
                var bbox = Rectangle(origin, 0.0, 0.0)

                drawer.writer {
                    gaplessNewLine()
                    bbox = Rectangle(
                        origin.x,
                        i * 30.0 + (25.0 * i) + origin.y,
                        80.0,
                        30.0
                    )

                    if(bbox.y < bounds.height && bbox.y > 80.0) {


                        drawer.fontMap = facultyAbbrFm
                        drawer.fill = if (activeList.contains(i)) facultyColors[i] else ColorRGBa.TRANSPARENT
                        drawer.stroke = facultyColors[i]
                        drawer.roundedRectangle(bbox.toRounded(999.0))

                        val t0 = item.facultyAbbreviation()
                        drawer.fill =
                            if (activeList.contains(i)) ColorRGBa.BLACK else facultyColors[i]
                        cursor.x = bbox.center.x - textWidth(t0) / 2.0
                        cursor.y = bbox.center.y + facultyAbbrFm.height / 2.0
                        text(t0)

                        drawer.fontMap = facultyFm
                        drawer.fill = ColorRGBa.WHITE.opacify(0.8)
                        cursor.x = bbox.corner.x + bbox.width + 5.0
                        text(item.uppercase())
                    }

                }

                (bbox.center + bounds.corner) to i
            }
        }

    }
}