package v04

import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.extra.shapes.RoundedRectangle
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.sin

class FilterSearchTimeline(): Keyframer() {
    val pcscale by DoubleChannel("pc_scale")
    val pcx by DoubleChannel("pc_x")
}

enum class SelectorType {
    FACULTY,
    TOPIC,
    PUBLICATION
}

class Selector(val r: Rectangle, val type: SelectorType): Animatable() {

    var list: List<String> = listOf()
        set(value) {
            field = value.sorted()
        }

    val x = r.x
    val y = r.y
    val width = r.width
    val height = r.height

    var current: String? = null

    val h = if(type == SelectorType.FACULTY) 70.0 else 52.0
    var yOffset = 0.0
        set(value) {
            field = value.coerceIn((-h * list.size) + 200, 0.0)
        }

    val sectionFm = loadFont("data/fonts/default.otf", 30.0)
    val entriesFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 32.0)
    val facultyFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 36.0)

    var lastY = 0.0
        set(value) {
            field = value
            val selected = currentlyInView.minByOrNull { abs(it.key - field) }
            selected?.let { currentSelected = it.value }
        }

    var currentlyInView = mapOf<Double, Int>()
    var currentSelected: Int? = null
        set(value) {
            field = if(field == value) {
                null
            } else {
                value
            }

            current = if(field != null) list[field!!] else null

            currentFaculty = if(type == SelectorType.FACULTY && field != null) {
                Faculty.fromName(current!!)
            } else {
                Faculty.fromName(facultyNames.last())
            }
        }

    var currentFaculty = Faculty.fromName(facultyNames.last())

    var initialT = System.currentTimeMillis()
    fun draw(drawer: Drawer) {

        drawer.drawStyle.clip = r

        drawer.fill = ColorRGBa.WHITE
        drawer.stroke = null

        val title = "choose a ${type.name.lowercase()} (${list.size})".uppercase()
        drawer.roundedRectangle(x, y + 10.0, sectionFm.textWidth(title) + 40.0, 50.0, 999.0)
        drawer.fill = ColorRGBa.BLACK
        drawer.fontMap = sectionFm
        drawer.text(title, x + 20.0, (y + 45.0))


        drawer.fontMap = if(type == SelectorType.FACULTY) facultyFm else entriesFm

        currentlyInView = list.withIndex().associate { (i, item) ->
            val iy = (250.0 + i * h) + yOffset

            if(iy < height && iy > y + 140.0) {
                val o = if(iy < y + 130.0) 0.0 else smoothstep(1.0, 0.8, iy / height)

                var stroke = ColorRGBa.TRANSPARENT
                var bgColor = ColorRGBa.TRANSPARENT
                var textColor = ColorRGBa.WHITE

                when(type) {
                    SelectorType.FACULTY -> {
                        stroke = ColorRGBa.TRANSPARENT
                        if(currentSelected == i) {
                            textColor = ColorRGBa.WHITE
                            bgColor = faculties[i].color
                        } else {
                            textColor = faculties[i].color
                            bgColor = ColorRGBa.TRANSPARENT
                        }
                    }
                    SelectorType.TOPIC -> {
                        textColor = currentFaculty.color.opacify(o)
                        if(currentSelected == i){
                            stroke = currentFaculty.color
                            bgColor = ColorRGBa.TRANSPARENT
                        } else {
                            stroke = ColorRGBa.TRANSPARENT
                            bgColor = ColorRGBa.TRANSPARENT
                        }
                    }
                    SelectorType.PUBLICATION -> {
                        stroke = ColorRGBa.TRANSPARENT
                        if(currentSelected == i) {
                            bgColor = currentFaculty.color
                            textColor = ColorRGBa.WHITE
                        } else {
                            textColor = currentFaculty.color.opacify(o)
                            bgColor = ColorRGBa.TRANSPARENT
                        }
                    }
                }

                drawer.fill = textColor
                drawer.writer {
                    val tw = textWidth(item)
                    val seconds = (System.currentTimeMillis() - initialT) / 1000.0
                    val xOffset = (tw - r.width + r.x).coerceAtLeast(0.0) / 2.0
                    val t = (sin(seconds * 0.4) * 0.5 + 0.5) * xOffset

                    val rect = RoundedRectangle(x, iy - (h / 2.0), tw + 20.0, h * 0.75, 999.0)
                    drawer.stroke = stroke
                    drawer.fill = bgColor
                    drawer.roundedRectangle(rect)

                    cursor.x = if(tw > r.width) x + 10.0 - t else x + 10.0
                    cursor.y = iy

                    if(type == SelectorType.PUBLICATION) {

                        val div = item.split("|")
                        div.take(2).forEachIndexed { i, item ->
                            val cc = if(i == 0) textColor else textColor.mix(ColorRGBa.GRAY, 0.65)
                            item.forEach { c ->
                                val op = if(tw > r.width) smoothstep(0.95, 0.8, cursor.x / r.width) else 1.0
                                drawer.fill = cc
                                text(c.toString())
                            }
                        }
                    } else {
                        item.forEach { c ->
                            val op = if(tw > r.width) smoothstep(0.95, 0.8, cursor.x / r.width) else 1.0
                            drawer.fill = textColor.opacify(op)
                            text(c.toString())
                        }
                    }

                }





            }

            iy to i

        }

        drawer.stroke = ColorRGBa.YELLOW
        drawer.fill = null
        drawer.rectangle(r)

        drawer.drawStyle.clip = null
    }
}

class FilterSearch(val data: DataModelNew, val frame: Rectangle, val mouse: MouseEvents): Animatable() {

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
            field = value.coerceIn(0.0, 3.0)
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

    val facultyBox = Selector(Rectangle(frame.x, frame.y + 114.0, frame.width * 0.25, frame.height - 120.0), SelectorType.FACULTY)
    val topicsBox = Selector(Rectangle(facultyBox.x + facultyBox.width, facultyBox.y, frame.width * 0.175, facultyBox.height), SelectorType.TOPIC)
    val articlesBox = Selector(Rectangle(topicsBox.x + topicsBox.width, facultyBox.y, frame.width * 0.3, facultyBox.height), SelectorType.PUBLICATION)

    fun reset() {
        ::timelineSlider.cancel()
        ::timelineSlider.animate(1.0, 1500, Easing.SineInOut)

        listOf(facultyBox, topicsBox, articlesBox).onEach { it.currentSelected = null }
        topicsBox.list = topics
        articlesBox.list = articleToAuthor

    }

    fun step(to: Double) {

        if(timelineSlider < to) {
            ::timelineSlider.cancel()
            ::timelineSlider.animate(to, 800, Easing.SineInOut)
        }

        when(to) {
            2.0 -> {
                val art = data.articles.filter {
                    it.faculty.name == facultyBox.current
                }
                topicsBox.list = topics.filter { t -> art.map { it.topic }.contains(t) }
                articlesBox.list = art.map { it.title + " | " + it.author }
            }
            3.0 -> articlesBox.list = data.articles.filter {
                        it.faculty.name == facultyBox.current &&
                        it.topic == topicsBox.current
            }.map { it.title + " | " + it.author }
            else -> { println("") }
        }
    }


    fun setup() {
        timeline.loadFromJson(File("data/timelines/filter-search-timeline.json"))

        facultyBox.list = facultyNames
        topicsBox.list = topics
        articlesBox.list = articleToAuthor

        var lastEvent = MouseEventType.BUTTON_UP

        mouse.buttonDown.listen {
            if(it.position in toggleBox) {
                visible = !visible
            }
            if(visible) {
                lastEvent = MouseEventType.BUTTON_DOWN
                if(it.position in resetBox) {
                    reset()
                }
            }
        }

        mouse.dragged.listen { e ->
            e.cancelPropagation()
            if(visible && e.dragDisplacement.y.absoluteValue > 1.0) {
                lastEvent = e.type

                val last = listOf(facultyBox, topicsBox, articlesBox).firstOrNull { e.position in it.r }
                if(last != null) {
                    last.yOffset += e.dragDisplacement.y
                }
            }
        }

        mouse.buttonUp.listen { e ->
            if(visible && lastEvent == MouseEventType.BUTTON_DOWN) {
                lastEvent = e.type

                val all = listOf(facultyBox, topicsBox, articlesBox)
                val last = all.firstOrNull { e.position in it.r }
                if(last != null) {
                    last.lastY = e.position.y

                    step(all.indexOf(last).toDouble() + 2.0)
                }
            }
        }

    }


    val default = loadFont("data/fonts/default.otf", 35.0)

    fun draw(drawer: Drawer) {

        drawer.isolated {
            drawer.defaults()
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

            updateAnimation()
            timeline(timelineSlider)

            if(visible) {

                topicsBox.currentFaculty = facultyBox.currentFaculty
                articlesBox.currentFaculty = facultyBox.currentFaculty

                listOf(facultyBox, topicsBox, articlesBox).take(ceil(timelineSlider).toInt()).map {
                    it.draw(drawer)
                }

                drawer.strokeWeight = 1.0
                drawer.stroke = ColorRGBa.WHITE
                drawer.fill = ColorRGBa.TRANSPARENT
                drawer.roundedRectangle(resetBox.toRounded(999.0))

                drawer.fontMap = default
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.TRANSPARENT
                drawer.text("RESET", resetBox.center - Vector2(default.textWidth("RESET") / 2.0, -default.height * 0.5))
            }
        }


        //println("currentStage $currentStage")

    }

    init {

        setup()

    }

}