package v05

import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil

class FilterSearch(val data: DataModel, val frame: Rectangle, val mouse: MouseEvents): Animatable() {

    val filterChanged = Event<List<String?>>()
    var visible = false
        set(value) {
            if(!field && value) {
                show()
                if (timelineSlider == 0.0) step(1.0)
            } else {
                hide()
            }
            field = value
        }
    var visibleSlider = 0.0

    val timeline = FilterSearchTimeline()
    var timelineSlider = 0.0
        set(value) {
            field = value.coerceIn(0.0, 4.0)
        }

    val toggleBox = Rectangle(frame.width - 60.0, 20.0, 60.0, 60.0)
    val resetBox = Rectangle.fromCenter(Vector2(frame.width / 2.0, frame.height - 20.0), 275.0, 60.0)

    fun show() {
        ::visibleSlider.cancel()
        ::visibleSlider.animate(1.0, 1500, Easing.SineInOut)
    }

    fun hide() {
        ::visibleSlider.cancel()
        ::visibleSlider.animate(0.0, 1500, Easing.SineInOut)
    }


    val articleToAuthor = data.articles.map { it.title + " | " + it.author }

    val fRect = Rectangle(frame.x, frame.y + 114.0, frame.width * 0.25, frame.height - 120.0)
    val tRect = Rectangle(fRect.x + fRect.width, fRect.y, frame.width * 0.175, frame.height - 200.0)
    val aRect = Rectangle(tRect.x + tRect.width, tRect.y, frame.width * 0.325, frame.height - 200.0)

    val facultyBox = FilterSearchSelector(fRect, FilterSearchSelectors.FACULTY)
    val topicsBox = FilterSearchSelector(tRect, FilterSearchSelectors.TOPIC)
    val articlesBox = FilterSearchSelector(aRect, FilterSearchSelectors.PUBLICATION)

    fun reset() {
        ::timelineSlider.cancel()
        ::timelineSlider.animate(1.0, 1500, Easing.SineInOut)


        listOf(facultyBox, topicsBox, articlesBox).onEach { it.currentSelected = null }
        topicsBox.list = topicNames
        articlesBox.list = articleToAuthor
        filterChanged.trigger(List(3) { null })
    }

    fun step(to: Double) {

        if(timelineSlider < to) {
            ::timelineSlider.cancel()
            ::timelineSlider.animate(to, 800, Easing.SineInOut)
        }

        when(to) {
            2.0 -> {
                val art = data.articles.filter {
                    it.faculty == facultyBox.current
                }
                topicsBox.list = art.map { it.topic }
                articlesBox.list = art.map { it.title + " | " + it.author }
            }
            3.0 -> articlesBox.list = data.articles.filter {
                        it.faculty == facultyBox.current &&
                        it.topic  == topicsBox.current
            }.map { it.title + " | " + it.author }
            else -> { println("") }
        }
    }


    var lastMouseEvent = MouseEventType.BUTTON_UP
    fun buttonDown(e: MouseEvent) {
        if(e.position in toggleBox) {
            visible = !visible
        }
        if(visible) {
            lastMouseEvent = MouseEventType.BUTTON_DOWN
            if(mouse.position in resetBox) {
                reset()
            }
        }
    }

    fun buttonUp(e: MouseEvent) {
        if(visible && lastMouseEvent == MouseEventType.BUTTON_DOWN) {
            lastMouseEvent = e.type

            val all = listOf(facultyBox, topicsBox, articlesBox)
            val last = all.firstOrNull { e.position in it.r }
            if(last != null) {
                last.lastY = e.position.y
                filterChanged.trigger(all.map { it.current })

                step(all.indexOf(last).toDouble() + 2.0)
            }
        }
    }

    fun dragged(e: MouseEvent) {
        e.cancelPropagation()
        if(visible && e.dragDisplacement.y.absoluteValue > 1.0) {
            lastMouseEvent = e.type

            val last = listOf(facultyBox, topicsBox, articlesBox).firstOrNull { e.position in it.r }
            if(last != null) {
                last.yOffset += e.dragDisplacement.y
            }
        }
    }


    val default = loadFont("data/fonts/default.otf", 35.0)
    val coverFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 37.0)
    fun draw(drawer: Drawer) {
        updateAnimation()
        timeline(timelineSlider)

        drawer.isolated {
            defaults()
            toggleBox.run {
                drawer.strokeWeight = 4.0
                drawer.stroke = ColorRGBa.WHITE
                drawer.fill = null

                if(!visible) {
                    val c = Circle(center - 5.0, 15.0)
                    drawer.circle(c)
                    drawer.lineSegment(Polar(45.0, 15.0).cartesian + c.center, Polar(45.0, 30.0).cartesian + c.center)
                } else {
                    toggleBox.offsetEdges(-10.0).run {
                        drawer.lineSegment(corner, corner + dimensions)
                        drawer.lineSegment(Vector2(x + width, y), Vector2(x, y + height))
                    }
                }
            }

            if(visible) {

                topicsBox.currentFaculty = facultyBox.currentFaculty
                articlesBox.currentFaculty = facultyBox.currentFaculty

                listOf(facultyBox, topicsBox, articlesBox).take(ceil(timelineSlider).toInt()).map {
                    it.draw(drawer, timelineSlider)
                }

                drawer.strokeWeight = 1.0
                drawer.stroke = ColorRGBa.WHITE
                drawer.fill = ColorRGBa.TRANSPARENT
                drawer.roundedRectangle(resetBox.toRounded(999.0))

                drawer.fontMap = default
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.TRANSPARENT
                drawer.text("RESET", resetBox.center - Vector2(default.textWidth("RESET") / 2.0, -default.height * 0.5))

                if (timelineSlider > 3.01 && articlesBox.current != null) {
                    val o = abs(3.0 - timelineSlider)
                    val rect = Rectangle(articlesBox.x + articlesBox.width + 80.0, articlesBox.y - 20.0, 300.0, 470.0)
                    drawer.fill = facultyBox.currentFaculty.facultyColor().opacify(o)
                    drawer.stroke = ColorRGBa.WHITE.opacify(o)
                    drawer.rectangle(rect)
                    drawer.stroke = null
                    drawer.fill = facultyBox.currentFaculty.facultyColor().shade(0.8).opacify(o)
                    drawer.circle(rect.center, rect.width * 0.4)

                    drawer.fontMap = coverFm
                    drawer.fill = ColorRGBa.WHITE
                    drawer.writer {
                        box = rect
                        newLine()
                        text(articlesBox.current!!)
                    }

                    drawer.fill = null
                    drawer.stroke = ColorRGBa.WHITE
                    val saBox = Rectangle(rect.x, rect.y + rect.height + 30.0, rect.width, 60.0)
                    drawer.roundedRectangle(saBox.toRounded(999.0) )
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = default
                    drawer.writer {
                        box = saBox
                        newLine()
                        text("  SEE ARTICLE ->")
                    }

                }

            }
        }

    }

    init {

        timeline.loadFromJson(File("data/timelines/filter-search-timeline.json"))

        facultyBox.list = facultyNames
        topicsBox.list = topicNames
        articlesBox.list = articleToAuthor

    }

}