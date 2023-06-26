package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.*
import java.io.File

class DiscoverFilter: Filter() {

    init {
        icon = loadSVG(File("data/icons/discoverIcon.svg"))
        title = "DISCOVER & FILTER"
        subtitle = "FACULTIES, TOPICS, TITLES & AUTHORS"
    }


    lateinit var articles: List<Article>

    val faculties = facultyNames
    val topics = topicNames
    var titlesToAuthors = listOf<String>()

    val fullList = listOf(::faculties, ::topics, ::titlesToAuthors)

    var activeFaculties = mutableListOf<Int>()
    var activeTopics = mutableListOf<Int>()
    var activeArticles = mutableListOf<Int>()

    val activeList = listOf(::activeFaculties, ::activeTopics, ::activeArticles)

    var selectorBoxes = listOf(
        Rectangle(110.0, 90.0, (bounds.width - 110.0) * 0.3, 25.0),
        Rectangle(110.0 + (bounds.width - 110.0) * 0.3, 90.0, (bounds.width - 110.0) * 0.3, 25.0),
        Rectangle(110.0 + (bounds.width - 110.0) * 0.6, 90.0, (bounds.width - 110.0) * 0.4, 25.0)
    )
    var currentBox = 0

    var entriesInView = mapOf<Vector2, Int>()
    var lastPos = Vector2.ZERO
        set(value) {
            field = value
            if(visible) {
                val selected = entriesInView.minByOrNull { it.key.distanceTo(field) }

                selected?.let {
                   val l = activeList[currentBox].get()
                    if(l.contains(it.value)) {
                        l.remove(it.value)
                    } else {
                        l.add(it.value)
                    }

                }
            }
        }

    fun reset() {
        activeFaculties = faculties.indices.toMutableList()
        activeTopics = topics.indices.toMutableList()
        activeArticles = articles.indices.toMutableList()
    }

    override fun buttonUp(e: MouseEvent) {
        val target = selectorBoxes.firstOrNull { e.position in it.movedBy(bounds.corner) }
        if(target != null) {
            currentBox = selectorBoxes.indexOf(target)
        } else {
            lastPos = e.position
        }
    }

    override fun draw(drawer: Drawer) {

        updateAnimation()

        val expandedY = drawer.height * 0.6 * expandT
        bounds = Rectangle(
            10.0,
            (drawer.height - boundsHeight * 3 - 10.0) - expandedY,
            boundsWidth,
            boundsHeight + expandedY)

        drawBasics(drawer)

        if(visible) {
            drawer.strokeWeight = 0.2
            drawer.stroke = ColorRGBa.WHITE
            selectorBoxes.forEachIndexed { i, it ->
                drawer.fill = if(i == currentBox) ColorRGBa.WHITE else null
                drawer.rectangle(it)
            }

            val texts = listOf("FACULTIES", "TOPICS", "PUBLICATIONS")
            texts.forEachIndexed { i, text ->
                drawer.fill = if(i == currentBox) ColorRGBa.BLACK else ColorRGBa.WHITE
                drawer.text(text, selectorBoxes[i].corner + Vector2(8.0, 15.0))
            }

            val currentList = fullList[currentBox].get()

            entriesInView = currentList.withIndex().associate { (i, item) ->
                val box = Rectangle(110.0, i * 30.0 + (25.0 * i) + 160.0, 80.0, 30.0)
                val pos = box.center + bounds.corner


                if(box.y < bounds.height && box.y > 80.0) {

                    drawer.writer {
                        val text = item
                        val tw = textWidth(text)

                        this.box = box

                        when(currentBox) {
                            0 -> {
                                drawer.fill = if(i in activeFaculties) facultyColors[i] else ColorRGBa.TRANSPARENT
                                drawer.stroke = facultyColors[i]
                                drawer.roundedRectangle(box.toRounded(999.0))

                                drawer.fill = if(i in activeFaculties) ColorRGBa.BLACK else facultyColors[i]
                                drawer.text(item.facultyAbbreviation(), box.corner + Vector2(box.width / 2.0, box.height / 2.0))

                                drawer.fill = ColorRGBa.WHITE.opacify(0.8)
                                drawer.text(item.uppercase(), box.corner + Vector2(box.width, 5.0))
                            }
                            1 -> {
                                //TODO
                            }
                            2 -> {

                                val article = articles[i]
                                val color = article.faculty.facultyColor()

                                val div = text.split("|")
                                div.take(2).forEachIndexed { i, item ->
                                    val cc = if(i == 0) color else color.mix(ColorRGBa.GRAY, 0.65)
                                    item.forEach { c ->
                                        drawer.fill = cc
                                        text(c.toString())
                                    }
                                }
                            }
                        }

                    }
                }


                pos to i
            }


        }

    }

}