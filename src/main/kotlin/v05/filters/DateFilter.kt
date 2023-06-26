package v05.filters

import org.openrndr.draw.Drawer
import org.openrndr.svg.loadSVG
import java.io.File

class DateFilter: Filter() {

    init {
        icon = loadSVG(File("data/icons/dateIcon.svg"))
    }

    override fun draw(drawer: Drawer) {

        drawer.translate(
            10.0,
            drawer.height - bounds.height * 1 - 10.0)
        drawBasics(drawer)

    }


}