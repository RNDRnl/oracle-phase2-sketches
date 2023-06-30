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
                    if(model.states.all { it.visible }) {
                        model.states[i].visible = true
                        model.states.minus(model.states[i]).forEach { s ->s.visible = false }
                    } else {
                        model.states[i].visible = !model.states[i].visible
                    }
                }
            }
        }
        dragged.listen {
            for (i in model.states.indices) {
                if (it.position in (itemBoxes[i] ?: Rectangle.EMPTY)) {
                    model.states[i].visible = true
                }
            }
        }
    }

    var itemBoxes = mapOf<Int, Rectangle>()

    val topicFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 19.0)

    override fun draw() {

        if(visible) {
            var topicBoxTracker = actionBounds.corner
            itemBoxes = model.states.withIndex().associate { (i, state) ->
                var itemBox = Rectangle(0.0, 0.0,100.0, 100.0)
                val item = model.list[i].uppercase()

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = topicFm
                    val tw = textWidth(item) + 40.0
                    itemBox = Rectangle(topicBoxTracker.x, topicBoxTracker.y, tw + 10.0, 41.0).run {
                        val final: Rectangle
                        val isIn = x + width < actionBounds.corner.x + actionBounds.width
                        if(isIn) {
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw + 10.0, topicBoxTracker.y)
                        } else {
                            topicBoxTracker = Vector2(actionBounds.corner.x, topicBoxTracker.y + 44.0)
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw + 10.0, topicBoxTracker.y)
                        }
                        final
                    }

                    drawer.fill = if(state.visible) ColorRGBa.GRAY.mix(ColorRGBa.WHITE, 0.5) else ColorRGBa.TRANSPARENT
                    drawer.stroke = ColorRGBa.GRAY.mix(ColorRGBa.WHITE, 0.5)
                    val offset = itemBox.offsetEdges(-5.0, -4.0)
                    drawer.roundedRectangle(offset.toRounded(900.0))

                    drawer.fill = if(state.visible) ColorRGBa.BLACK else  ColorRGBa.WHITE
                    cursor.x = offset.center.x - (tw / 2.0) + 20.0
                    cursor.y = offset.center.y + topicFm.height / 2.0
                    text(item)
                }


                i to itemBox
            }
        }

    }
}
