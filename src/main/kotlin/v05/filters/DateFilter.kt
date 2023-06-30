package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.map
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle


class DateFilter(val drawer: Drawer, val model: DateFilterModel): Filter() {

    inner class Selector(var state: DateFilterState) {
        var pos: Double
            get() {
                return state.year.map(1900.0, 2020.0, 0.0, 1.0)
            }
            set(value) {
                state.year = value.map(0.0, 1.0, 1900.0, 2020.0)
            }
    }

    val selectors = model.states.map { Selector(it) }
    var closestSelector: Selector? = null

    init {
        actionBounds = Rectangle(80.0, 90.0 + 32.0 + 600.0, 460.0, 150.0)

        buttonDown.listen {e ->
            e.cancelPropagation()
            closestSelector = selectors.minBy { actionBounds.position(it.pos, 0.5).distanceTo(e.position)}
        }

        dragged.listen {
            val mappedPosition = map(actionBounds.x + 10.0, actionBounds.x + actionBounds.width, 0.0, 1.0, it.position.x)
            closestSelector?.pos = mappedPosition.coerceIn(0.0, 1.0)
        }

        buttonUp.listen {
            closestSelector = null
        }
    }

    override fun draw() {
        if(visible) {
            val rail = LineSegment(actionBounds.x + 15.0, actionBounds.center.y, actionBounds.x + actionBounds.width - 30.0, actionBounds.center.y)

            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rail)

            selectors.forEach {
                val center = rail.position(it.pos)
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE
                drawer.text(it.state.year.toString(), center.x - 15.0, center.y - 25.0)
                drawer.circle(center, 15.0)
            }
        }

    }
}

