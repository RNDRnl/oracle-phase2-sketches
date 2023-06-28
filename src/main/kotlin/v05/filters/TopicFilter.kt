package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.facultyAbbreviation
import v05.facultyColor
import v05.facultyColors
import kotlin.math.sin

class TopicFilter(list: List<String>): Filter(list) {

    override var title = "TOPICS"

    val topicFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw(drawer: Drawer, bounds: Rectangle) {
        headerBox = Rectangle(80.0 + (bounds.width - 80.0) * 0.3, 90.0, (bounds.width - 80.0) * 0.3, 32.0)
        beforeDraw(drawer, bounds)

        if(isActive) {
            var topicBoxTracker = origin
            entriesInView = list.withIndex().associate { (i, item) ->
                var bbox = Rectangle(origin, 0.0, 0.0)

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = topicFm
                    val tw = textWidth(item) + 40.0
                    bbox = Rectangle(topicBoxTracker.x, topicBoxTracker.y, tw, 40.0).run {
                        val final: Rectangle
                        val isIn = x + width < origin.x + bounds.width - 100.0
                        if(isIn) {
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                        } else {
                            topicBoxTracker = Vector2(origin.x, topicBoxTracker.y + 50.0)
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                        }
                        final
                    }

                    if(bbox.y < bounds.height && bbox.y > 80.0) {
                        drawer.fill = if(activeList.size != list.size && activeList.contains(i)) ColorRGBa.WHITE else ColorRGBa.TRANSPARENT
                        drawer.stroke = ColorRGBa.WHITE
                        drawer.roundedRectangle(bbox.toRounded(900.0))

                        drawer.fill = if(activeList.size != list.size && activeList.contains(i)) ColorRGBa.BLACK else  ColorRGBa.WHITE
                        cursor.x = bbox.center.x - tw / 2.0
                        cursor.y = bbox.center.y + topicFm.height / 2.0
                        text(item.uppercase())
                    }
                }


                (bbox.center + bounds.corner) to i
            }
        }


    }
}