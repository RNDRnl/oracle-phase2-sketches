package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.events.listen
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.*
import java.io.Serializable

class FilterSet(val faculties: List<String>, val topics: List<String>, val topGraduate: String?, val dates: Pair<Int, Int>): Serializable {
    companion object {
        val EMPTY = FilterSet(facultyNames, topicNames, null, 1900 to 2023)
    }
}

class FilterMenu(val data: DataModel, val frame: Rectangle, val mouse: MouseEvents): Animatable() {

    val filtersChanged = Event<FilterSet>()

    val discover = DiscoverFilter()
    val topGraduates = TopGraduatesFilter()
    val date = DateFilter()

    val filters = listOf(discover, topGraduates, date)

    var bounds = Rectangle(filters[0].bounds.corner, filters[0].boundsWidth, filters.sumOf { it.bounds.height })
    var opened = false
        set(value) {
            if(value) open() else close()
            field = value
        }

    var t = 0.0

    fun open() {
        ::t.cancel()
        ::t.animate(1.0, 1000, Easing.CubicInOut)
    }

    fun close() {
        ::t.cancel()
        ::t.animate(0.0, 1000, Easing.CubicInOut)
    }

    init {
        listOf(discover, topGraduates, date).map { it.changed }.listen {
            filtersChanged.trigger(FilterSet(
                discover.activeFaculties.values.toList(),
                discover.activeTopics.values.toList(),
                null,
                0 to 2023))
        }
    }

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
        opened = filters.any { it.visible }
    }

    fun dragged(e: MouseEvent) {
        val scrollable = filters.take(2).firstOrNull { e.position in it.bounds }

        scrollable?.dragged(e)
    }

    fun draw(drawer: Drawer) {
        updateAnimation()
        bounds = Rectangle(filters[0].bounds.corner, filters[0].boundsWidth, filters.sumOf { it.bounds.height })

        if(opened) {
            drawer.shadeStyle = linearGradient(ColorRGBa.BLACK, ColorRGBa.TRANSPARENT, rotation = -90.0)
            drawer.rectangle(0.0, 0.0, bounds.width * t, drawer.height * 1.0)
            drawer.shadeStyle = null
        }

        filters.map {
            drawer.defaults()
            it.draw(drawer)
        }
    }


    init {
        discover.articles = data.articles
        discover.reset()
    }
}