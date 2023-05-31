package v04

import classes.*
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.events.listen
import org.openrndr.extra.camera.ParametricOrbital
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.*
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import v02.transform
import java.lang.StringBuilder
import kotlin.math.sign
import kotlin.math.sin

class SearchBar(val keyboard: KeyEvents, val frame: Rectangle) {

    val queryChanged = Event<String>()
    var currentText: String = ""
        set(value) {
            field = value
            currentTextWidth = fm.textWidth(currentText)
            queryChanged.trigger(value)
        }

    var isBackspace = false
    var currentTextWidth = 0.0
        set(value) {
            field = value
            if(value > frame.width + frame.x) {
                val amt = fm.glyphMetrics[currentText.last()]?.advanceWidth ?: 0.0
                if (!isBackspace) {
                    offset -= amt
                } else {
                    offset += amt
                }
            }
        }
    var offset = 0.0

    fun setupListeners() {
        keyboard.character.listen {
            currentText += it.character
        }

        listOf(keyboard.keyDown, keyboard.keyRepeat).listen {
            if (it.key == KEY_BACKSPACE && currentText.isNotEmpty()) {
                isBackspace = true
                currentText = currentText.dropLast(1)
            } else {
                isBackspace = false
            }
        }
    }


    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 42.0)
    private val t = System.currentTimeMillis()

    fun draw(drawer: Drawer) = drawer.run {

        drawer.fontMap = fm
        drawer.stroke = null
        drawer.fill = ColorRGBa.WHITE


        text(currentText, frame.x + offset, frame.y + fm.height)

        // cursor
        drawer.fill = ColorRGBa.WHITE.opacify(sin((System.currentTimeMillis() - t) / 1000 * 20.0) *0.5 + 0.5)
        drawer.rectangle(frame.x + currentTextWidth, frame.y, 2.0, fm.height)

    }

    init {
        setupListeners()
    }

}

fun Program.pointCloud04(data: DataModelNew) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))
    val searchBar = SearchBar(keyboard, Rectangle(10.0, 10.0, width*1.0, 128.0))

    val obstacles = listOf(slider.bounds)

    listOf(mouse.buttonDown, mouse.buttonUp).listen {
        val obst = obstacles.firstOrNull { o -> it.position in o.offsetEdges(5.0) }
        camera.inUiElement = obst != null
        // val e = EventObject(it.type, data.activePoints, camera.mappedZoom)
    }

    mouse.dragged.listen {
        if(it.position in slider.bounds.offsetEdges(65.0, 0.0)) {
            val old = slider.current

            slider.current = map(
                slider.bounds.x,
                slider.bounds.x + slider.bounds.width,
                0.0,
                1.1,
                it.position.x.coerceIn(slider.bounds.x, slider.bounds.x + slider.bounds.width))

            mouse.scrolled.trigger(
                MouseEvent(drawer.bounds.center,Vector2.UNIT_Y * (old - slider.current), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, setOf())
            )
        }
    }

    camera.changed.listen {
        slider.current = camera.mappedZoom

        data.radius = 40.0 / camera.view.c0r0
        data.lookAt = (camera.view.inversed * drawer.bounds.center.xy01).xy
    }

    searchBar.queryChanged.listen { s ->
        data.changed.trigger(Unit)
        data.filtered = data.pointsToArticles.filter { it.value.title.contains(s) }
    }

    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)

    extend(camera)
    extend {

        drawer.strokeWeight = 0.05
        drawer.rectangles {
            for ((point, article) in data.pointsToArticles) {
                val opacity = if(data.filtered[point] != null) 1.0 else 0.0
                this.stroke = if (point in data.activePoints) ColorRGBa.YELLOW else null

                this.fill = article.faculty.color.opacify(opacity)
                this.rectangle(Rectangle.fromCenter(point, 0.65, 1.0))
            }
        }

        drawer.defaults()

        drawer.fontMap = fm
        drawer.fill = ColorRGBa.WHITE
      /*  when(slider.current) {
            in 0.8..1.0 -> {
                for(i in data.activePoints.filter { data.filteredIndices[data.points[it]] != null }) {
                    val p = data.points[i].transform(camera.view)
                    drawer.text(data.articles[i].title, p)
                }
            }
        }*/

        slider.draw(drawer)

        searchBar.draw(drawer)

        drawer.fill = null
        drawer.stroke = ColorRGBa.WHITE
        drawer.circle(data.lookAt, 40.0)


    }

}

fun main() = application {
    configure {
        width = 900
        height = 900
    }
    program {

        val data = DataModelNew(drawer.bounds)
        val pc = viewBox(drawer.bounds) { pointCloud04(data) }

        extend {
            pc.draw()
        }
    }
}