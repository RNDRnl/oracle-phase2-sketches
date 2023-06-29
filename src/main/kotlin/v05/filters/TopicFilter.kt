package v05.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.topicNames

class TopicFilterModel: FilterModel() {

    override val list = topicNames
    override val states = list.map { ToggleFilterState() }
    val filteredList: List<String>
        get() = filter()

    fun filter(): List<String> {
        return list.filterIndexed { i, _ -> states[i].visible  }
    }

    init {
        states.forEach {
            it.stateChanged.listen {
                if (states.none { it.visible }) {
                    states.forEach {
                        it.visible = true
                    }
                }
                filterChanged.trigger(Unit)
            }
        }
    }
}

class TopicFilterNew(val drawer: Drawer, val model: TopicFilterModel): FilterNew() {

    override var title = "TOPICS"

    override var headerBox = Rectangle(200.0, 90.0, 460.0 * 0.4, 32.0)
    override val bounds = Rectangle(80.0, 90.0 + 32.0, 460.0, 32.0)

    val topicFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw() {

        drawer.stroke = ColorRGBa.WHITE
        drawer.fill = if(isCurrent) ColorRGBa.WHITE else null
        drawer.rectangle(headerBox)

        if(isVisible) {
            var topicBoxTracker = bounds.corner
            model.states.forEachIndexed { i, state ->

                val item = model.list[i]

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = topicFm
                    val tw = textWidth(item) + 40.0
                    val itemBox = Rectangle(topicBoxTracker.x, topicBoxTracker.y, tw, 40.0).run {
                        val final: Rectangle
                        val isIn = x + width < bounds.corner.x + bounds.width - 100.0
                        if(isIn) {
                            final = movedTo(topicBoxTracker)
                            topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                        } else {
                            topicBoxTracker = Vector2(bounds.corner.x, topicBoxTracker.y + 50.0)
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

            }
        }

    }
}

class TopicFilter(list: List<String>): Filter(list) {

    override var title = "TOPICS"

    val topicFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw(drawer: Drawer, bounds: Rectangle) {
        headerBox = Rectangle(80.0 + (bounds.width - 80.0) * 0.3, 90.0, (bounds.width - 80.0) * 0.3, 32.0)

        val c = if(isCurrent && boundingBox != headerBox) ColorRGBa.WHITE else null
        drawer.stroke = ColorRGBa.WHITE
        drawer.fill = c
        drawer.rectangle(headerBox)

        beforeDraw(drawer, bounds.movedBy(Vector2(70.0, 0.0)))


        if(isVisible) {
            var topicBoxTracker = origin
            entriesInView = list.withIndex().associate { (i, item) ->
                var itemBox = Rectangle(origin, 0.0, 0.0)

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = topicFm
                    val tw = textWidth(item) + 40.0
                    itemBox = Rectangle(topicBoxTracker.x, topicBoxTracker.y, tw, 40.0).run {
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

                    if(itemBox.y < bounds.height && itemBox.y > 80.0) {
                        drawer.fill = if(activeList.size != list.size && activeList.contains(i)) ColorRGBa.WHITE else ColorRGBa.TRANSPARENT
                        drawer.stroke = ColorRGBa.WHITE
                        drawer.roundedRectangle(itemBox.toRounded(900.0))

                        drawer.fill = if(activeList.size != list.size && activeList.contains(i)) ColorRGBa.BLACK else  ColorRGBa.WHITE
                        cursor.x = itemBox.center.x - tw / 2.0
                        cursor.y = itemBox.center.y + topicFm.height / 2.0
                        text(item.uppercase())
                    }
                }


                (itemBox.center + bounds.corner) to i
            }
        }

    }
}