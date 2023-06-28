package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.map
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

class DateFilter (list: List<String>): Filter(list) {

    override var isActive = true

    inner class Selector(var year: String) {
        var pos: Double
            get() {
                return year.toDouble().map(list.first().toDouble(), list.last().toDouble(), 0.0, 1.0)
            }
            set(value) {
                year = value.map(0.0, 1.0, 1880.0, 2020.0).toInt().toString()
                changed.trigger(Unit)
            }
    }

    val selectors = listOf(Selector(list.first()), Selector(list.last()))
    var closestSelector: Selector? = null

    override fun buttonUp(e: MouseEvent) {
        closestSelector = null
    }

    override fun draw(drawer: Drawer, bounds: Rectangle) {
        val r = Rectangle(bounds.x, (bounds.height - 125.0), bounds.width, 120.0).offsetEdges(-20.0)
        headerBox = r
        beforeDraw(drawer, r)

        val rail = LineSegment(boundingBox.x, boundingBox.center.y, boundingBox.width, boundingBox.center.y)

        drawer.stroke = ColorRGBa.WHITE
        drawer.lineSegment(rail)

        selectors.forEach {
            val center = rail.position(it.pos)
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.text(it.year, center.x - 15.0, center.y - 25.0)
            drawer.circle(center, 15.0)
        }
    }

}