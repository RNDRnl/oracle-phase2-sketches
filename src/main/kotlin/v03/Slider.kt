package v03

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle


class Slider(val pos: Vector2, val width: Double, val label: String = "SLIDER") {

    var current = 0.5
        set(value) {
            field = value.coerceIn(0.0, 1.0)
        }
    val radius = 10.0
    val bounds = Rectangle(pos - Vector2(0.0, radius), width, radius * 2.0)

    fun draw(drawer: Drawer) {
        drawer.stroke = ColorRGBa.WHITE

        val rail = LineSegment(pos, pos + Vector2(width, 0.0))
        drawer.lineSegment(rail)

        drawer.fill = ColorRGBa.WHITE
        drawer.text(label, pos - Vector2(0.0, 17.0))
        drawer.circle(rail.position(current), radius)
    }

}