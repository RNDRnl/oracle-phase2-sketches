
package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.*
import java.io.File


class Discover: FilterMenu() {

    init {
        icon = loadSVG(File("data/icons/discoverIcon.svg"))
        title = "DISCOVER & FILTER"
        subtitle = "FACULTIES, TOPICS, TITLES & AUTHORS"

        actionBounds = Rectangle(10.0, 0.0, boundsWidth, boundsHeight)

        buttonDown.listen {
            it.cancelPropagation()
        }

        buttonUp.listen {
            if (it.position in actionBounds.copy(height =  80.0)) {
                expanded = !expanded
            }
        }
    }

    /*override fun dragged(e: MouseEvent) {
        super.dragged(e)
        if(articleFilter.isVisible) {
            articleFilter.dragged(e)
        }
    }

    var articlesOpened = false

    override fun buttonUp(e: MouseEvent) {

        val target = filters.firstOrNull { e.position in it.headerBox.movedBy(bounds.corner) }
        target?.let {
            target.isCurrent = true
            filters.minus(setOf(target, dateFilter)).onEach { it.isCurrent = false }
        }

        when(target) {
            facultyFilter -> {
                facultyFilter.isVisible = true
                facultyFilter.isMinimized = false
                filters.minus(setOf(target, dateFilter)).onEach { it.isVisible = false }
                articlesOpened = false
            }
            topicFilter -> {
                topicFilter.isVisible = true
                facultyFilter.isMinimized = true
                articleFilter.isVisible = false
                articlesOpened = false
            }
            articleFilter -> {
                topicFilter.isVisible = true
                articleFilter.isVisible = true
                facultyFilter.isMinimized = true
                articlesOpened = true
            }
            null -> {
                val newTarget = filters.minus(dateFilter).firstOrNull { it.isVisible && e.position in it.boundingBox }
                newTarget?.let {
                    it.lastPos = e.position
                }
            }
        }
    }*/

    override fun draw(drawer: Drawer) {

        val expandedY = drawer.height * 0.75 * animations.expandT
        actionBounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight * 2) - drawer.height * 0.75 * animations.expandT,
            boundsWidth,
            boundsHeight + expandedY)

        drawBasics(drawer)

    }

}

