package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.Article

open class Filter(val list: List<String>, var articles: List<Article>? = null): Animatable() {

    val changed = Event<Unit>()
    open var isActive = false

    var activeList = reset()

    var entriesInView = mapOf<Vector2, Int>()

    open var boundingBox = Rectangle(0.0,0.0, 100.0, 100.0)
    open var headerBox = Rectangle(0.0,0.0, 100.0, 100.0)
    open val title = ""

    open var lastPos = Vector2.ZERO
        set(value) {
            field = value
            if(isActive) {
                val selected = entriesInView.minByOrNull { it.key.distanceTo(field) }

                selected?.value.let { index ->
                    val i = index!!

                    if(activeList.size == list.size) {
                        activeList.clear()
                        activeList[i] = list[i]
                    } else {
                        if(activeList[i].isNullOrBlank()) {
                            activeList[i] = list[i]
                        } else {
                            activeList.remove(i)
                            if(activeList.isEmpty()) {
                                activeList = reset()
                            }
                        }
                    }

                }
                println(activeList)
                changed.trigger(Unit)
            }
        }



    open fun buttonDown(e: MouseEvent) {

    }

    open fun buttonUp(e: MouseEvent) {

    }

    open fun dragged(e: MouseEvent) {

    }

    fun reset(): MutableMap<Int, String> {
        return list.withIndex().associate { it.index to it.value }.toMutableMap()
    }

    fun beforeDraw(drawer: Drawer, b: Rectangle) {
        updateAnimation()
        boundingBox = b

        val c = if(isActive && boundingBox != headerBox) ColorRGBa.WHITE else null
        drawer.stroke = c
        drawer.fill = c
        drawer.rectangle(headerBox)

        drawer.fontMap = selectorBoxesFm
        drawer.fill = if(isActive) ColorRGBa.BLACK else ColorRGBa.WHITE
        drawer.writer {
            box = headerBox
            cursor.x = headerBox.center.x - textWidth(title) / 2.0
            cursor.y = headerBox.center.y + selectorBoxesFm.height / 2.0
            text(title)
        }
    }

    val origin = Vector2(80.0, 160.0)
    val selectorBoxesFm = loadFont("data/fonts/default.otf", 22.0)

    var initialT = System.currentTimeMillis()

    open fun draw(drawer: Drawer, bounds: Rectangle) {
        beforeDraw(drawer, bounds)
    }
}