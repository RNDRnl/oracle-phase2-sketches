package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.*
import java.io.File

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
    }

}