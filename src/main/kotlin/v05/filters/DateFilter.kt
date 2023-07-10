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
                return state.year.map(1900.0, 2023.0, 0.0, 1.0)
            }
            set(value) {
                state.year = value.map(0.0, 1.0, 1900.0, 2023.0)
            }
    }

    val selectors = model.states.map { Selector(it) }
    var closestSelector: Selector? = null

    init {
        visible = false
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
          //  closestSelector = null
        }
    }

    override fun draw() {
        if(visible) {
            val rail = LineSegment(actionBounds.x + 15.0, actionBounds.center.y, actionBounds.x + actionBounds.width - 30.0, actionBounds.center.y)

            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rail)

            val min = rail.position(selectors.minBy { it.pos }.pos)
            val max = rail.position(selectors.maxBy { it.pos }.pos)

            val minYear = selectors.minBy { it.state.year }.state.year.toInt().toString()
            val maxYear = selectors.maxBy { it.state.year }.state.year.toInt().toString()

            drawer.fill = ColorRGBa.WHITE
            drawer.rectangle(min.x, min.y - 15.0, max.x - min.x, 30.0)
            drawer.stroke = ColorRGBa.WHITE

            val lineLength = (max.x - 12.0) - (min.x + 10.0)
            val pushOut =  -(lineLength - 6.0).coerceAtMost(0.0)/2.0
            drawer.lineSegment(min.x + 10.0 - pushOut, min.y - 30.0, max.x - 12.0 + pushOut, max.y - 30.0)

            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null
            drawer.text(minYear, min.x - 15.0 - pushOut, min.y - 25.0)
            drawer.text(maxYear, max.x - 7.0 + pushOut, max.y - 25.0)

            selectors.forEach {
                val center = rail.position(it.pos)
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE
                //drawer.text(it.state.year.toInt().toString(), center.x - 15.0, center.y - 25.0)
                drawer.circle(center, 15.0)
            }
        }

    }
}

