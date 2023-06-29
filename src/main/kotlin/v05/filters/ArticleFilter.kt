package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.Article
import v05.facultyColor
import kotlin.math.sin

class ArticleFilter(map: List<String>, articles: List<Article>): Filter(map, articles) {

    override var title = "PUBLICATIONS"
    var currentArticle: Article? = null
        set(value) {
            field = value
            if(value != null) {
                changed.trigger(Unit)
            }
        }

    var yOffset = 0.0
    override fun dragged(e: MouseEvent) {
        yOffset += e.dragDisplacement.y
    }

    override var lastPos = Vector2.ZERO
        set(value) {
            field = value
            if(isVisible) {
                val selected = entriesInView.minByOrNull { it.key.distanceTo(field) }

                selected?.value.let { index ->
                    val i = index!!
                    currentArticle = articles?.get(i)
                }
                changed.trigger(Unit)
            }
        }

    val publicationFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 27.0)

    override fun draw(drawer: Drawer, bounds: Rectangle) {
        headerBox =  Rectangle(80.0 + (bounds.width - 80.0) * 0.6, 90.0, (bounds.width - 80.0) * 0.4, 32.0)

        val c = if(isCurrent && boundingBox != headerBox) ColorRGBa.WHITE else null
        drawer.stroke = ColorRGBa.WHITE
        drawer.fill = c
        drawer.rectangle(headerBox)

        val r = bounds.movedBy(Vector2(bounds.width, 0.0))
        beforeDraw(drawer, r)

        if(isVisible && list.isNotEmpty() && !articles.isNullOrEmpty()) {
            drawer.drawStyle.clip = boundingBox

            entriesInView = list.withIndex().associate { (i, item) ->
                var itemBox = Rectangle(origin, 0.0, 0.0)

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = publicationFm
                    val tw = textWidth(item)

                    itemBox = Rectangle(
                        origin.x + bounds.x,
                        origin.y + i * 30.0 + (25.0 * i) + yOffset,
                        tw + 40.0,
                        29.0
                    )

                    if(itemBox.y < bounds.height && itemBox.y > 80.0) {
                        val seconds = (System.currentTimeMillis() - initialT) / 1000.0
                        val xOffset = (tw - bounds.width + bounds.x).coerceAtLeast(0.0) / 2.0
                        val t = (sin(seconds * 0.5) * 0.5 + 0.5) * xOffset

                        val article = articles!![i]
                        val color = article.faculty.facultyColor()

                        cursor.x = (if(tw < bounds.width) itemBox.corner.x else itemBox.corner.x - t) + 20.0
                        cursor.y = itemBox.center.y + publicationFm.height / 2.0


                        val div = item.split("|")
                        div.take(2).forEachIndexed { i, item ->
                            val cc = if(i == 0) color else color.mix(ColorRGBa.GRAY, 0.65)
                            item.forEach { c ->
                                drawer.fill = if(article == currentArticle) ColorRGBa.BLACK else cc
                                text(c.toString())
                            }
                        }
                    }

                }


                (itemBox.corner + bounds.corner) to i
            }


            drawer.drawStyle.clip = null
        }


        drawer.fill = null
        drawer.stroke = ColorRGBa.BLUE
        drawer.rectangle(boundingBox)

    }
}