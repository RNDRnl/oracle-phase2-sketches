package v05.filters

import org.openrndr.draw.Drawer
import org.openrndr.svg.loadSVG
import java.io.File

class TopGraduates: Filter() {

    init {
        icon = loadSVG(File("data/icons/topGraduatesIcon.svg"))
        title = "TOP GRADUATES"
        subtitle = "170+ Top graduates dissertations"
    }

    override fun draw(drawer: Drawer) {

        updateAnimation()

        drawer.translate(
            10.0,
            drawer.height - bounds.height * 2 - 10.0)
        drawBasics(drawer)


    }

}