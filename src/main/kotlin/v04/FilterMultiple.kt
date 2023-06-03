package v04

import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.math.smoothstep
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.reflect.KProperty0


class Box(val r: Rectangle, val content: KProperty0<List<String>>) {

    val list = content()

    val x = r.x
    val y = r.y
    val width = r.width
    val height = r.height

    var yOffset = 0.0
        set(value) {
            field = value.coerceIn((-35.0 * list.size) + 200, 0.0)
        }

    val sectionFm = loadFont("data/fonts/default.otf", 30.0)
    val entriesFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 28.0)

    var lastY = 0.0
        set(value) {
            field = value
            val closest = currentlyInView.minBy { abs(it.key - field) }.value

            if(currentSelected.contains(closest)) {
                currentSelected.remove(closest)
            } else {
                currentSelected.add(closest)
            }
        }

    var currentlyInView = mapOf<Double, Int>()
    var currentSelected = mutableListOf<Int>()

    var faculties = if(content.name == "faculties") {
        list.map { Faculty.fromName(it) }
    } else listOf()

    fun draw(drawer: Drawer) {

        drawer.stroke = ColorRGBa.WHITE
        drawer.fill = ColorRGBa.TRANSPARENT
        drawer.rectangle(x, y, width, 80.0)

        drawer.fill = ColorRGBa.WHITE
        drawer.stroke = null
        drawer.fontMap = sectionFm
        drawer.text("${content.name.uppercase()} (${list.size})", x + 10.0, (y + 45.0))

        drawer.fontMap = entriesFm

        currentlyInView = list.withIndex().associate { (i, item) ->
            val iy = (200.0 + i * 35.0) + yOffset
            val c = if(currentSelected.contains(i)) ColorRGBa.RED else ColorRGBa.WHITE

            if(iy < height && iy > y + 90.0) {
                val o = if(iy < y + 80.0) 0.0 else smoothstep(1.0, 0.8, iy / height)
                drawer.fill = if(faculties.isEmpty()) c.opacify(o) else faculties[i].color
                drawer.text(item, x + 10.0, iy)
            }

            iy to i
        }


    }
}

class FilterMultiple(val data: DataModelNew, frame: Rectangle, val mouse: MouseEvents) {

    var visible = true
    val queryChanged = Event<List<Set<String>>>()
    val currentFilter = ""

    val faculties = facultyNames
    val topics = data.articles.map { it.topic }
    val titles = data.articles.map { it.title }
    val authors = data.articles.map { it.author }

    val facultyBox = Box(Rectangle(frame.x, frame.y + 60.0, frame.width * 0.3, frame.height - 120.0), ::faculties)
    val topicsBox = Box(Rectangle(facultyBox.x + facultyBox.width, facultyBox.y, frame.width * 0.15, facultyBox.height), ::topics)
    val titlesBox = Box(Rectangle(topicsBox.x + topicsBox.width, facultyBox.y, frame.width * 0.35, facultyBox.height), ::titles)
    val authorsBox = Box(Rectangle(titlesBox.x + titlesBox.width, facultyBox.y, frame.width * 0.2, facultyBox.height), ::authors)

    private fun setupListeners() {

        var lastEvent = MouseEventType.BUTTON_UP

        mouse.dragged.listen { e ->
            e.cancelPropagation()
            if(e.dragDisplacement.y.absoluteValue > 1.0) {
                lastEvent = e.type

                val last = listOf(topicsBox, titlesBox, authorsBox).firstOrNull { e.position in it.r }
                if(last != null) {
                    last.yOffset += e.dragDisplacement.y
                }
            }
        }

        mouse.buttonUp.listen { e ->
            if(lastEvent == MouseEventType.BUTTON_DOWN) {
                lastEvent = e.type

                val last = listOf(topicsBox, titlesBox, authorsBox).firstOrNull { e.position in it.r }
                if(last != null) {
                    last.lastY = e.position.y
                }
            }
        }

        mouse.buttonDown.listen {
            lastEvent = MouseEventType.BUTTON_DOWN
        }
    }

    fun draw(drawer: Drawer) {

        drawer.fill = null
        drawer.stroke = ColorRGBa.YELLOW
        drawer.rectangles(listOf(facultyBox, topicsBox, titlesBox, authorsBox).map { it.r })

        listOf(facultyBox, topicsBox, titlesBox, authorsBox).forEach {
            drawer.isolated {
                it.draw(drawer)
            }
        }
    }

    init {
        setupListeners()
    }

}