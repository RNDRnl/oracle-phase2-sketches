package v05.filters

import org.openrndr.draw.Drawer
import org.openrndr.svg.loadSVG
import java.io.File

class DiscoverFilter: Filter() {

    init {
        icon = loadSVG(File("data/icons/discoverIcon.svg"))
        title = "DISCOVER & FILTER"
        subtitle = "FACULTIES, TOPICS, TITLES & AUTHORS"
    }

    override fun draw(drawer: Drawer) {

        updateAnimation()

        drawer.translate(
            10.0,
            drawer.height - bounds.height * 3 - 10.0)
        drawBasics(drawer)


    }

}