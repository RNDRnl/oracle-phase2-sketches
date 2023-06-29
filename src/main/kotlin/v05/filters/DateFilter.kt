package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.map
import org.openrndr.math.max
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle


class DateFilterModel: FilterModel() {

    override val list = listOf(1900, 2000)
    override val states = list.map { DateFilterState(it) }
    val filteredList: List<Int>
        get() = filter()

    fun filter(): List<Int> {
        val low = minOf(states[0].year, states[1].year).toInt()
        val high = maxOf(states[0].year, states[1].year).toInt()
        return listOf(low, high)
    }

    init {
        states.forEachIndexed { i, it ->
            it.stateChanged.listen {
                filterChanged.trigger(Unit)
            }
        }
    }
}

class DateFilterNew(val drawer: Drawer, val model: DateFilterModel): FilterNew() {

    override var isVisible = true

    inner class Selector(var state: DateFilterState) {
        var pos: Double
            get() {
                return state.year.map(1900.0, 2020.0, 0.0, 1.0)
            }
            set(value) {
                state.year = value.map(0.0, 1.0, 1880.0, 2020.0)
            }
    }

    val selectors = model.states.map { Selector(it) }
    var closestSelector: Selector? = null

    override val bounds = Rectangle(80.0, 90.0 + 32.0, 460.0, 32.0)
    override var headerBox = Rectangle(bounds.x, (bounds.height - 125.0), bounds.width, 120.0).offsetEdges(-20.0)

    fun buttonUp(e: MouseEvent) {
        closestSelector = null
    }

    override fun draw() {

        val rail = LineSegment(bounds.x, bounds.center.y, bounds.width, bounds.center.y)

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

