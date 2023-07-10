package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.writer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.State
import java.io.File

class Discover(state: State): FilterMenu(state) {

    val boundsWidth = 510.0
    val boundsHeight = 80.0

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
                active = !active
            }
        }
    }

    override fun draw(drawer: Drawer) {
        val expandedY = drawer.height * 0.75 * animations.expandT
        var abHeight = if(!active) 80.0 else boundsHeight + expandedY
        actionBounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight * 2) - drawer.height * 0.75 * animations.expandT,
            boundsWidth,
            abHeight)

        drawBasics(drawer)

        drawer.fontMap = subtitleFm
        drawer.fill = ColorRGBa.WHITE
        drawer.writer {
            cursor.x = actionBounds.x + 78.0
            cursor.y = actionBounds.y + 45.0 + titleFm.height
            val pubs = if (state.filtered.size == 1) "1 PUBLICATION IN" else "${state.filtered.size} PUBLICATIONS IN "
            val facs = if(state.facultyFilter.filteredList.size == 1) "1 FACULTY AMONG" else "${state.facultyFilter.filteredList.size} FACULTIES AMONG "
            val topics = if(state.topicFilter.filteredList.size == 1) "1 TOPIC" else "${state.topicFilter.filteredList.size} TOPICS"

            text(pubs + facs + topics)
        }


    }
}

