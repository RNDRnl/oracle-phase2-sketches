package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.Article
import v05.facultyColor
import kotlin.math.sin

class ArticleFilter(val drawer: Drawer, var articles: List<Article>): Filter(){

    val articleSelected = Event<Unit>()

    override var title = "PUBLICATIONS"
    var currentArticle: Article? = null
        set(value) {
            field = value
            if(value != null) {
                articleSelected.trigger(Unit)
            }
        }

    var yOffset = 0.0
    var entriesInView = mapOf<Vector2, Int>()

    var lastPos = Vector2.ZERO
        set(value) {
            field = value
            if(visible) {
                val selected = entriesInView.minByOrNull { it.key.distanceTo(field) }

                if(selected != null) {
                    currentArticle = articles[selected.value]
                    articleSelected.trigger(Unit)
                }
            }
        }

    init {
        actionBounds = Rectangle(10.0, 0.0, 460.0, 600.0)
        buttonDown.listen {
            it.cancelPropagation()
        }

        dragged.listen {
            yOffset += it.dragDisplacement.y
        }
    }

    val publicationFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 27.0)
    var initialT = System.currentTimeMillis()
    override fun draw() {

        if(visible && articles.isNotEmpty()) {

            entriesInView = articles.withIndex().associate { (i, article) ->
                var itemBox = Rectangle(actionBounds.corner, 0.0, 0.0)

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = publicationFm
                    val tw = textWidth(article.title)

                    itemBox = Rectangle(
                        actionBounds.corner.x + actionBounds.corner.x,
                        actionBounds.corner.y + i * 30.0 + (25.0 * i) + yOffset,
                        tw + 40.0,
                        29.0
                    )

                    if(itemBox.y < actionBounds.height && itemBox.y > 80.0) {
                        val seconds = (System.currentTimeMillis() - initialT) / 1000.0
                        val xOffset = (tw - actionBounds.width + actionBounds.x).coerceAtLeast(0.0) / 2.0
                        val t = (sin(seconds * 0.5) * 0.5 + 0.5) * xOffset

                        val color = article.faculty.facultyColor()

                        cursor.x = (if(tw < actionBounds.width) itemBox.corner.x else itemBox.corner.x - t) + 20.0
                        cursor.y = itemBox.center.y + publicationFm.height / 2.0


                        val div = article.title.split("|")
                        div.take(2).forEachIndexed { i, item ->
                            val cc = if(i == 0) color else color.mix(ColorRGBa.GRAY, 0.65)
                            item.forEach { c ->
                                drawer.fill = if(article == currentArticle) ColorRGBa.BLACK else cc
                                text(c.toString())
                            }
                        }
                    }

                }


                (itemBox.corner + actionBounds.corner) to i
            }


        }
    }
}
