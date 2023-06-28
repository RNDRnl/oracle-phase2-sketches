package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.*
import java.io.File
import kotlin.math.sin

class Discover(articles: List<Article>): FilterMenu(articles) {

    init {
        icon = loadSVG(File("data/icons/discoverIcon.svg"))
        title = "DISCOVER & FILTER"
        subtitle = "FACULTIES, TOPICS, TITLES & AUTHORS"
    }

    val topicFilter = TopicFilter(topicNames)
    val articleFilter = ArticleFilter(articles.map { it.title + " | " + it.author }, articles)

    override val filters = listOf(facultyFilter, topicFilter, dateFilter, articleFilter)

    override fun dragged(e: MouseEvent) {
        super.dragged(e)
        if(articleFilter.isActive) {
            articleFilter.dragged(e)
        }
    }


    override fun draw(drawer: Drawer) {

        updateAnimation()
        drawer.drawStyle.clip = bounds

        val expandedY = drawer.height * 0.75 * expandT
        bounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight * 2 - 10.0) - expandedY,
            boundsWidth,
            boundsHeight + expandedY)

        drawBasics(drawer)

        if(visible) {
            filters.forEach {
                it.draw(drawer, bounds)
            }
        }

        drawer.drawStyle.clip = null
    }

}