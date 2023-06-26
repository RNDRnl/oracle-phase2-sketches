package v05.filters

import org.openrndr.draw.Drawer
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import java.io.File

class DateFilter: Filter() {

    init {
        icon = loadSVG(File("data/icons/dateIcon.svg"))
    }

    override fun draw(drawer: Drawer) {


        bounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight - 10.0),
            boundsWidth,
            boundsHeight)


        drawBasics(drawer)

    }


}