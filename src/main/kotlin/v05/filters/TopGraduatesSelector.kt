package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.libs.UIElementImpl


class TopGraduatesSelector : UIElementImpl() {

    var current = 0

    var boxes = listOf(
        Rectangle(0.0, 0.0, 185.0, 32.0),
        Rectangle(0.0, 0.0, 195.0, 32.0)
    )

    val texts = listOf("FACULTIES", "TOPICS")

    init {
        buttonDown.listen { e ->
            e.cancelPropagation()

            current = boxes.indexOf(boxes.minBy { it.center.distanceTo(e.position) })
        }
    }

    val offset = Vector2(50.0, 90.0)
    val fM = loadFont("data/fonts/default.otf", 18.0)
    fun draw(drawer: Drawer, bounds: Rectangle) {

        if (visible) {

            val r0 = Rectangle(bounds.corner + offset, 185.0, 32.0)
            val r1 = Rectangle(bounds.corner + Vector2(r0.width, 0.0) + offset, 195.0, 32.0)

            boxes = listOf(r0, r1)

            boxes.forEachIndexed { i, it ->
                drawer.fill = if (i <= current) ColorRGBa.WHITE else ColorRGBa.TRANSPARENT
                drawer.stroke = ColorRGBa.WHITE
                drawer.rectangle(it)

                drawer.fill = if (i <= current) ColorRGBa.BLACK else ColorRGBa.WHITE
                drawer.writer {
                    drawer.fontMap = fM
                    box = it
                    cursor.x = it.center.x - textWidth(texts[i]) / 2.0
                    cursor.y = it.center.y + it.height / 4.0 - 2.0
                    text(texts[i])
                }
            }

            actionBounds = Rectangle(boxes[0].corner, boxes[1].x + boxes[1].width - boxes[0].x, boxes[1].height)
        }
    }
}