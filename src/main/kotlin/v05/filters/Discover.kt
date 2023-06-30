
package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.*
import java.io.File


class Discover: FilterMenu() {

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
                expanded = !expanded
            }
        }
    }

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

