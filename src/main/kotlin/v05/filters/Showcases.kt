package v05.filters

import org.openrndr.MouseEventType
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.writer
import org.openrndr.events.Event
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.State
import java.io.File
import kotlin.math.exp

class Showcases(state: State): FilterMenu(state) {

    val showcaseSelected = Event<Unit>()

    val boundsWidth = 510.0
    val boundsHeight = 80.0

    val rects = List(state.model.showcases.size) { i -> Rectangle(20.0, 260.0 + i * 180.0 + (20.0 * i), boundsWidth - 40.0, 180.0) }
    val showcases = state.model.showcases.mapIndexed {  i, it ->
        Showcase(it.first, it.second.sortedBy { it.faculty }, rects[i])
    }

    var filter = ArticleFilter(listOf())

    var current: Showcase? = null
        set(value) {
             if(value != null) {
                 if(value == field) {
                     value.expanded = false
                     showcases.map { it.visible = true }
                     field = null
                 } else {
                     filter.articles = value.articles
                     value.expanded = true
                     showcases.minus(value).map {
                         it.visible = false
                         it.expanded = false
                     }
                     field = value
                 }
             } else {
                 showcases.map {
                     it.expanded = false
                     field = null
                 }
             }
            showcaseSelected.trigger(Unit)
        }

    var lastEventType: MouseEventType? = null

    var lastPos = Vector2.ZERO
        set(value) {
            if (visible) {
                val selected = showcases.filter { it.visible }.firstOrNull { it.toggleButton.contains(value) }
                if (selected != null) {
                    current = selected
                }
            }

            field = value
        }

    var yOffset = 0.0
        set(value) {
            field = value.coerceIn(-rects.sumOf { it.height } + actionBounds.y, 0.0)
        }

    init {
        icon = loadSVG(File("data/icons/topGraduatesIcon.svg"))
        title = "SHOWCASES"
        subtitle = "discover 170+ top graduates dissertations".uppercase()
        zOrder = 5

        actionBounds = Rectangle(10.0, 80.0, boundsWidth, boundsHeight)

        buttonDown.listen {
            it.cancelPropagation()
            lastEventType = it.type
        }

        buttonUp.listen {
            val header = actionBounds.copy(height =  80.0)

            if (it.position in header) {
                active = !active
                showcases.map { s -> s.visible = active }
            } else {
                if (lastEventType == MouseEventType.BUTTON_DOWN) {
                    lastPos = it.position
                }
            }

        }

        dragged.listen {
            if(it.dragDisplacement.squaredLength > 4.0) lastEventType = it.type
            if(current == null) yOffset += it.dragDisplacement.y
        }
    }

    override fun draw(drawer: Drawer) {

        val expandedY = drawer.height * 0.75 * animations.expandT
        val abHeight = if(showcases.any { it.expanded }) 200.0 else boundsHeight + expandedY
        actionBounds = Rectangle(
            10.0,
            80.0 + (drawer.height - boundsHeight * 2) - (drawer.height * 0.75 * animations.expandT),
            boundsWidth,
            abHeight)

        drawBasics(drawer)

        drawer.fontMap = subtitleFm
        drawer.fill = ColorRGBa.WHITE
        drawer.writer {
            cursor.x = actionBounds.x + 78.0
            cursor.y = actionBounds.y + 45.0 + titleFm.height
            val type = if(current == showcases[0]) "SHOWCASE" else "SHELF"
            val text = if(current != null) "EXPLORING THE ${current!!.title} $type" else "EXPLORE CURATED COLLECTION AND PHYSICAL SHELVES"

            text(text)
        }


        drawer.drawStyle.clip = Rectangle(Vector2(actionBounds.x, actionBounds.y + 80.0), boundsWidth, (boundsHeight + expandedY) - 80.0)

        showcases.forEachIndexed { i, it->
            it.frame = rects[i].movedBy(Vector2(0.0, yOffset))
            it.outerFrame = actionBounds.copy(height = boundsHeight + expandedY)
            it.draw(drawer)

            if(it.expanded && active) {
                filter.visible = true
                filter.actionBounds = Rectangle(it.frame.x, it.frame.y + 120.0, it.frame.width, it.frame.height - 120.0)
                filter.draw(drawer)
            }
        }

        drawer.drawStyle.clip = null
        Driver.instance.setState(drawer.drawStyle)
    }
}

