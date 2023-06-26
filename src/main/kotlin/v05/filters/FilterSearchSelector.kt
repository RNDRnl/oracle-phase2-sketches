package v05.filters

import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.RoundedRectangle
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.math.smoothstep
import org.openrndr.shape.Rectangle
import v05.facultyColor
import v05.facultyNames
import v05.libs.mix
import kotlin.math.abs
import kotlin.math.sin

enum class FilterSearchSelectors {
    FACULTY,
    TOPIC,
    PUBLICATION
}

class FilterSearchSelector(val r: Rectangle, val type: FilterSearchSelectors): Animatable() {

    var list: List<String> = listOf()
        set(value) {
            field = value.sorted()
        }

    val x = r.x
    val y = r.y
    val width = r.width
    val height = r.height

    var current: String? = null

    val h = if(type == FilterSearchSelectors.FACULTY) 70.0 else 52.0
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

            currentFaculty = if(type == FilterSearchSelectors.FACULTY && field != null) {
                current!!
            } else {
                facultyNames.last()
            }
        }

    var currentFaculty = v05.facultyNames.last()

    var initialT = System.currentTimeMillis()
    fun draw(drawer: Drawer, slider: Double) {

        drawer.drawStyle.clip = r

        drawer.fill = ColorRGBa.WHITE.opacify(slider)
        drawer.stroke = null

        val toTrim = ((slider - 1) * (FilterSearchSelectors.values().indexOf(type) + 1))
        println((toTrim * 8).toInt().coerceIn(0, 8))
        val title = ("choose a ".drop((toTrim * 8).toInt().coerceIn(0, 8)) + "${type.name.lowercase()} (${list.size})").uppercase()

        drawer.roundedRectangle(x, y + 10.0,width + 40.0, 50.0, 999.0)
        drawer.fill = ColorRGBa.BLACK
        drawer.fontMap = sectionFm
        drawer.text(title.take((title.length * slider).toInt()), x + 20.0, (y + 45.0))


        drawer.fontMap = if(type == FilterSearchSelectors.FACULTY) facultyFm else entriesFm

        currentlyInView = list.withIndex().associate { (i, item) ->
            val iy = (250.0 + i * h) + yOffset

            if(iy < height && iy > y + 140.0) {
                val o = if(iy < y + 130.0) 0.0 else smoothstep(1.0, 0.8, iy / height)

                var stroke = ColorRGBa.TRANSPARENT
                var bgColor = ColorRGBa.TRANSPARENT
                var textColor = ColorRGBa.WHITE

                when(type) {
                    FilterSearchSelectors.FACULTY -> {
                        stroke = ColorRGBa.TRANSPARENT
                        if(currentSelected == i) {
                            textColor = ColorRGBa.WHITE
                            bgColor = v05.facultyToColor[item]?: v05.facultyColors.last()
                        } else {
                            textColor = v05.facultyToColor[item]?: v05.facultyColors.last()
                            bgColor = ColorRGBa.TRANSPARENT
                        }
                    }
                    FilterSearchSelectors.TOPIC -> {
                        textColor = currentFaculty.facultyColor().opacify(o)
                        if(currentSelected == i){
                            stroke = currentFaculty.facultyColor()
                            bgColor = ColorRGBa.TRANSPARENT
                        } else {
                            stroke = ColorRGBa.TRANSPARENT
                            bgColor = ColorRGBa.TRANSPARENT
                        }
                    }
                    FilterSearchSelectors.PUBLICATION -> {
                        stroke = ColorRGBa.TRANSPARENT
                        if(currentSelected == i) {
                            bgColor = currentFaculty.facultyColor()
                            textColor = ColorRGBa.WHITE
                        } else {
                            textColor = currentFaculty.facultyColor().opacify(o)
                            bgColor = ColorRGBa.TRANSPARENT
                        }
                    }
                }

                drawer.fill = textColor
                drawer.writer {

                    val text = if(type == FilterSearchSelectors.FACULTY) v05.facultyAbbreviations[i].mix(item, 2.0 - slider) else item
                    val tw = textWidth(text)
                    val seconds = (System.currentTimeMillis() - initialT) / 1000.0
                    val xOffset = (tw - r.width + r.x).coerceAtLeast(0.0) / 2.0
                    val t = (sin(seconds * 0.4) * 0.5 + 0.5) * xOffset

                    val rect = RoundedRectangle(x, iy - (h / 2.0), tw + 20.0, h * 0.75, 999.0)
                    drawer.stroke = stroke
                    drawer.fill = bgColor
                    drawer.roundedRectangle(rect)

                    cursor.x = if(tw > r.width) x + 10.0 - t else x + 10.0
                    cursor.y = iy

                    if(type == FilterSearchSelectors.PUBLICATION) {
                        val div = text.split("|")
                        div.take(2).forEachIndexed { i, item ->
                            val cc = if(i == 0) textColor else textColor.mix(ColorRGBa.GRAY, 0.65)
                            item.take((slider * item.length).toInt()).forEach { c ->
                                val op = if(tw > r.width) smoothstep(0.95, 0.8, cursor.x / r.width) else 1.0
                                drawer.fill = cc
                                text(c.toString())
                            }
                        }
                    } else {
                        text.take((slider * item.length).toInt()).forEach { c ->
                            val op = if(tw > r.width) smoothstep(0.95, 0.8, cursor.x / r.width) else 1.0
                            drawer.fill = textColor.opacify(op)
                            text(c.toString())
                        }
                    }

                }





            }

            iy to i

        }

        drawer.drawStyle.clip = null
    }
}