package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.shape.Rectangle
import v05.*

class FilterMenu(val data: DataModel, val frame: Rectangle, val mouse: MouseEvents): Animatable() {

    val filterChanged = Event<List<String?>>()
    val filters = listOf(DiscoverFilter(), TopGraduates(), DateFilter())

    fun buttonUpDown(e: MouseEvent) {
        val target = filters.firstOrNull { e.position in it.bounds }
        if(target != null) {
            target.expanded = !target.expanded
        }
    }

    fun dragged(e: MouseEvent) {
        if(e.position in filters.last().bounds) {
            //TODO
        }
    }

    fun draw(drawer: Drawer) {
        filters.map {
            drawer.defaults()
            it.draw(drawer)
        }
    }
}