package v05.filters

import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.events.Event
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import v05.Article
import v05.facultyColor
import kotlin.math.sin

class ArticleFilter(val drawer: Drawer, var articles: List<Article>) : Filter() {

    val articleSelected = Event<Unit>()

    override var title = "PUBLICATIONS"
    var currentArticle: Article? = null
        set(value) {
            field = value
            if (value != null) {
                articleSelected.trigger(Unit)
            }
        }

    var yOffset = 0.0
        set(value) {
            field = value.coerceAtMost(0.0)
        }
    var entriesInView = mapOf<Vector2, Int>()

    var lastPos = Vector2.ZERO
        set(value) {
            field = value
            if (visible) {
                val selected = entriesInView.minByOrNull { it.key.distanceTo(field) }

                if (selected != null) {
                    currentArticle = articles[selected.value]
                    articleSelected.trigger(Unit)
                }
            }
        }

    var lastEventType: MouseEventType? = null

    init {
        actionBounds = Rectangle(10.0, 0.0, 460.0, 600.0)
        buttonDown.listen {
            it.cancelPropagation()
            lastEventType = it.type
        }

        dragged.listen {
            lastEventType = it.type
            yOffset += it.dragDisplacement.y


        }

        buttonUp.listen {
            if (lastEventType == MouseEventType.BUTTON_DOWN) lastPos = it.position
        }

    }

    val publicationFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 28.0)
    val publicationFs = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 18.0)
    var initialT = System.currentTimeMillis()
    override fun draw() {

        val itemHeight = 55.0

        val totalHeight = articles.size * itemHeight
        val viewHeight = actionBounds.height

        val vtRatio = viewHeight / totalHeight
        val scrollWidgetHeight = (vtRatio * viewHeight).coerceAtLeast(20.0)
        val stRatio = yOffset/totalHeight
        val scrollWidgetY = (-yOffset / totalHeight).map(0.0, 1.0, 0.0, viewHeight-scrollWidgetHeight,clamp = true)

        if (visible && articles.isNotEmpty()) {

            if (vtRatio < 1.0)
                drawer.rectangle(actionBounds.position(1.0, 1.0).x, actionBounds.position(1.0,0.0).y + scrollWidgetY, 10.0, scrollWidgetHeight)

            drawer.drawStyle.clip = actionBounds

            entriesInView = articles.asSequence().withIndex().associate { (i, article) ->
                var itemBox = Rectangle(actionBounds.corner, 0.0, 0.0)

                itemBox = Rectangle(
                    actionBounds.corner.x,
                    actionBounds.corner.y + i * 55.0 + yOffset,
                    actionBounds.width,
                    55.0
                )

                if (itemBox.y < actionBounds.y + actionBounds.height && itemBox.y + itemBox.height> actionBounds.y) {

                    drawer.isolated {
                        drawer.shadeStyle = linearGradient(ColorRGBa.BLACK.opacify(0.5), ColorRGBa.BLACK)
                        drawer.rectangle(itemBox)
                    }


                    drawer.writer {
                        gaplessNewLine()
                        drawer.fontMap = publicationFm
                        val tw = textWidth(article.title)
                        box = Rectangle(0.0, 0.0, 1.0E10, 1.0E20)

                        val seconds = (System.currentTimeMillis() - initialT) / 1000.0
                        val xOffset = (tw - actionBounds.width + actionBounds.x).coerceAtLeast(0.0) / 2.0
                        val t = (sin(seconds * 0.5) * 0.5 + 0.5) * xOffset

                        val color = article.faculty.facultyColor()
                        cursor.x = itemBox.corner.x
                        cursor.y = itemBox.center.y + publicationFm.height / 2.0

                        val div = article.title.split("|")
                        div.take(2).forEachIndexed { i, item ->
                            val cc = if (i == 0) color else color.mix(ColorRGBa.GRAY, 0.65)
                            item.forEach { c ->
                                drawer.fill =  ColorRGBa.WHITE
                                text(c.toString())
                            }
                        }

                        drawer.fontMap = publicationFs
                        newLine()
                        cursor.x = itemBox.corner.x
                        text("${article.year}")
                        cursor.x = itemBox.corner.x + itemBox.width/2.0
                        text("${article.author}")
                    }
                }

                (itemBox.corner + actionBounds.corner) to i
            }
            drawer.drawStyle.clip = null
        }

    }
}
