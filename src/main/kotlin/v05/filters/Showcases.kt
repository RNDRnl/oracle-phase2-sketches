package v05.filters

import org.openrndr.draw.Drawer
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.Article
import v05.facultyNames
import java.io.File

class Showcases(articles: List<Article>): FilterMenu(articles) {

    init {
        icon = loadSVG(File("data/icons/topGraduatesIcon.svg"))
        title = "TOP GRADUATES"
        subtitle = "170+ Top graduates dissertations"
    }

    val facultyFilter = FacultyFilter(facultyNames)

    override fun draw(drawer: Drawer) {

        updateAnimation()
        val expandedY = drawer.height * 0.6 * expandT
        bounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight * 1 - 10.0) - expandedY,
            boundsWidth,
            boundsHeight + expandedY)

        drawBasics(drawer)


    }

}