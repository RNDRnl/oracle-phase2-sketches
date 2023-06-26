package v05.filters

import org.openrndr.draw.Drawer
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import java.io.File

class TopGraduatesFilter: Filter() {

    init {
        icon = loadSVG(File("data/icons/topGraduatesIcon.svg"))
        title = "TOP GRADUATES"
        subtitle = "170+ Top graduates dissertations"
    }

    override fun draw(drawer: Drawer) {

        updateAnimation()
        val expandedY = drawer.height * 0.6 * expandT
        bounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight * 2 - 10.0) - expandedY,
            boundsWidth,
            boundsHeight + expandedY)

        drawBasics(drawer)


    }

}