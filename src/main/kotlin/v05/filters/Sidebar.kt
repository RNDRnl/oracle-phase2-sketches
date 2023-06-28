package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.shape.Rectangle
import v05.*
import v05.filters.Filter
import java.io.Serializable

class FilterSet(val faculties: List<String>, val topics: List<String>, val dates: Pair<Int, Int>): Serializable {
    companion object {
        val EMPTY = FilterSet(facultyNames, topicNames, 1900 to 2023)
    }
}

class Sidebar(val data: DataModel, val state: State, val frame: Rectangle, val mouse: MouseEvents): Animatable() {

    val filtersChanged = Event<FilterSet>()

    val discover = Discover(data.articles)
    val showcases = Showcases(data.articles)

    val filterMenus = listOf(discover, showcases)

    var bounds = Rectangle(discover.bounds.corner, discover.boundsWidth, filterMenus.sumOf { it.bounds.height })
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

    fun setupListeners() {
        listOf(discover, showcases).forEach { filterMenu ->
            filterMenu.filters.map { filter ->
                filter.changed.listen {
                    if(filterMenu == discover) {
                        val dateFilterList = discover.dateFilter.activeList.values.toList()

                        filtersChanged.trigger(FilterSet(
                            discover.facultyFilter.activeList.values.toList(),
                            discover.topicFilter.activeList.values.toList(),
                            discover.dateFilter.selectors.minBy { it.year }.year.toInt() to discover.dateFilter.selectors.maxBy { it.year }.year.toInt()
                        ))

                        discover.articleFilter.articles = state.filtered.values.toList()
                    } else {
                        filtersChanged.trigger(FilterSet(
                            showcases.facultyFilter.list,
                            topicNames,
                            1900 to 2023
                        ))
                    }
                }
            }
        }
    }

    fun buttonDown(e: MouseEvent) {
        val target = filterMenus.lastOrNull {
            e.position in it.bounds
        }

        target?.buttonDown(e)
    }

    fun buttonUp(e: MouseEvent){
        val target = filterMenus.lastOrNull {
            e.position in it.bounds
        }

        if(target != null) {
            if(e.position in target.bounds.copy(height = target.boundsHeight)) {
                when(target) {
                    discover -> {
                        discover.apply {
                            visible = !visible
                            expanded = if(showcases.expanded) true else !expanded
                        }
                        showcases.apply {
                            visible = false
                            expanded = false
                        }
                    }
                    showcases -> {
                        showcases.apply {
                            visible = !visible
                            expanded = !expanded
                        }
                        discover.apply {
                            visible = false
                            expanded = showcases.expanded
                        }
                    }
                }
            } else {
                if(e.type == MouseEventType.BUTTON_UP) {
                    target.buttonUp(e)
                }
            }
        }

        opened = filterMenus.any { it.visible }
    }


    fun dragged(e: MouseEvent) {
        val scrollable = filterMenus.take(2).firstOrNull { e.position in it.bounds }
        scrollable?.dragged(e)
    }

    fun draw(drawer: Drawer) {
        updateAnimation()
        bounds = Rectangle(filterMenus[0].bounds.corner, filterMenus[0].boundsWidth, filterMenus.sumOf { it.bounds.height })

        if(opened) {
            drawer.shadeStyle = linearGradient(ColorRGBa.BLACK, ColorRGBa.TRANSPARENT, rotation = -90.0)
            drawer.rectangle(0.0, 0.0, bounds.width * t, drawer.height * 1.0)
            drawer.shadeStyle = null
        }

        filterMenus.map {
            drawer.defaults()
            it.draw(drawer)
        }
    }


    init {
        setupListeners()
    }
}