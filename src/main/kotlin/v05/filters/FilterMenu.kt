package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.*

class FilterMenu(val data: DataModel, val frame: Rectangle, val mouse: MouseEvents): Animatable() {

    val filterChanged = Event<List<String?>>()

    val discover = DiscoverFilter()
    val topGraduates = TopGraduatesFilter()
    val date = DateFilter()

    val filters = listOf(discover, topGraduates, date)

    fun buttonUpDown(e: MouseEvent) {
        val target = filters.lastOrNull {
            e.position in it.bounds
        }

        if(target != null) {
            if(e.position in target.bounds.copy(height = target.boundsHeight)) {
                when(target) {
                    filters[0] -> {
                        filters[0].apply {
                            visible = !visible
                            expanded = if(filters[1].expanded) true else !expanded
                        }
                        filters[1].apply {
                            visible = false
                            expanded = false
                        }
                    }
                    filters[1] -> {
                        filters[1].apply {
                            visible = !visible
                            expanded = !expanded
                        }
                        filters[0].apply {
                            visible = false
                            expanded = filters[1].expanded
                        }
                    }
                }
            } else {
                if(e.type == MouseEventType.BUTTON_UP) {
                    target.buttonUp(e)
                }
            }
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
            drawer.stroke = ColorRGBa.BLUE
            drawer.fill = null
            drawer.rectangle(it.bounds)

            drawer.stroke = ColorRGBa.GREEN
            drawer.fill = null
            drawer.rectangle(it.bounds.copy(height = 80.0))


            it.draw(drawer)
        }
    }


    init {
        discover.articles = data.articles
        discover.titlesToAuthors = data.articles.map { it.title + " | " + it.author }
        discover.reset()
    }
}