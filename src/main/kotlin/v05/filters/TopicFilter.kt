package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle


class TopicFilter(val drawer: Drawer, val model: TopicFilterModel): Filter() {

    override var title = "TOPICS"
    override var visible = false

    init {
        actionBounds = Rectangle(80.0, 90.0 + 32.0, 460.0, 600.0)
        buttonDown.listen {
            it.cancelPropagation()

            for (i in model.states.indices) {
                if (itemBoxes[i]?.contains(it.position) == true) {
                    model.states[i].visible = !model.states[i].visible
                }
            }
        }
    }

    var itemBoxes = mapOf<Int, Rectangle>()

    val topicFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw() {

        if(visible) {
            var topicBoxTracker = actionBounds.corner
            itemBoxes = model.states.withIndex().associate { (i, state) ->
                var itemBox = Rectangle(0.0, 0.0,100.0, 100.0)
                val item = model.list[i]

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = topicFm
                    val tw = textWidth(item) + 40.0
                    itemBox = Rectangle(topicBoxTracker.x, topicBoxTracker.y, tw, 40.0).run {
                        val final: Rectangle
                        val isIn = x + width < actionBounds.corner.x + actionBounds.width
                        if(isIn) {
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                        } else {
                            topicBoxTracker = Vector2(actionBounds.corner.x, topicBoxTracker.y + 50.0)
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                        }
                        final
                    }

                    drawer.fill = if(state.visible) ColorRGBa.WHITE else ColorRGBa.TRANSPARENT
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.roundedRectangle(itemBox.toRounded(900.0))

                    drawer.fill = if(state.visible) ColorRGBa.BLACK else  ColorRGBa.WHITE
                    cursor.x = itemBox.center.x - tw / 2.0
                    cursor.y = itemBox.center.y + topicFm.height / 2.0
                    text(item.uppercase())
                }


                i to itemBox
            }
        }

    }
}
