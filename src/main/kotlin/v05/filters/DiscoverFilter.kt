package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.*
import java.io.File
import kotlin.math.sin

class DiscoverFilter: Filter() {

    init {
        icon = loadSVG(File("data/icons/discoverIcon.svg"))
        title = "DISCOVER & FILTER"
        subtitle = "FACULTIES, TOPICS, TITLES & AUTHORS"
    }


    var articles = listOf<Article>()
        set(value) {
            field = value
            titlesToAuthors = articles.map { it.title + " | " + it.author }
        }

    val faculties = facultyNames
    val topics = topicNames
    var titlesToAuthors = listOf<String>()

    val fullLists = listOf(::faculties, ::topics, ::titlesToAuthors)

    var activeFaculties = mutableMapOf<Int, String>()
    var activeTopics = mutableMapOf<Int, String>()
    var activeArticles = mutableMapOf<Int, String>()

    val activeLists = listOf(::activeFaculties, ::activeTopics, ::activeArticles)

    var selectorBoxes = listOf(
        Rectangle(80.0, 90.0, (bounds.width - 80.0) * 0.3, 32.0),
        Rectangle(80.0 + (bounds.width - 80.0) * 0.3, 90.0, (bounds.width - 80.0) * 0.3, 32.0),
        Rectangle(80.0 + (bounds.width - 80.0) * 0.6, 90.0, (bounds.width - 80.0) * 0.4, 32.0)
    )
    var currentBox = 0

    fun reset(n: Int? = null) {
        when(n) {
            0 -> activeFaculties = faculties.withIndex().associate { it.index to it.value }.toMutableMap()
            1 -> activeTopics = topics.withIndex().associate { it.index to it.value }.toMutableMap()
            2 -> activeArticles = titlesToAuthors.withIndex().associate { it.index to it.value }.toMutableMap()
            else -> {
                reset(0)
                reset(1)
                reset(2)
            }
        }
    }

    var yOffset = 0.0
    var entriesInView = mapOf<Vector2, Int>()
    override var lastPos = Vector2.ZERO
        set(value) {
            field = value
            if(visible) {
                val selected = entriesInView.minByOrNull { it.key.distanceTo(field) }

                selected?.value.let { index ->
                    val i = index!!
                    val al = activeLists[currentBox].get()
                    val fl = fullLists[currentBox].get()

                    if(al.size == fl.size) {
                        al.clear()
                        al[i] = fl[i]
                    } else {
                        if(al[i].isNullOrBlank()) {
                            al[i] = fl[i]
                        } else {
                            al.remove(i)
                            if(al.isEmpty()) {
                                reset(currentBox)
                            }
                        }
                    }

                }

                changed.trigger(Unit)
            }
        }

    override fun dragged(e: MouseEvent) {
        println("dragged")
        if(currentBox == 2) {
            yOffset += e.dragDisplacement.y
        }
    }

    override fun buttonUp(e: MouseEvent) {
        println("buttonup")
        val target = selectorBoxes.firstOrNull { e.position in it.movedBy(bounds.corner) }
        if(target != null) {
            currentBox = selectorBoxes.indexOf(target)
        } else {
            lastPos = e.position
        }
    }

    val selectorBoxesFm = loadFont("data/fonts/default.otf", 22.0)
    val facultyAbbrFm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)
    val facultyTopicFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)
    val publicationFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 27.0)

    var initialT = System.currentTimeMillis()
    override fun draw(drawer: Drawer) {

        updateAnimation()
        drawer.drawStyle.clip = bounds

        val expandedY = drawer.height * 0.635 * expandT
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

            drawer.fontMap = selectorBoxesFm
            val texts = listOf("FACULTIES", "TOPICS", "PUBLICATIONS")
            texts.forEachIndexed { i, text ->
                drawer.fill = if(i == currentBox) ColorRGBa.BLACK else ColorRGBa.WHITE
                drawer.writer {
                    box = selectorBoxes[i]
                    cursor.x = selectorBoxes[i].center.x - textWidth(text) / 2.0
                    cursor.y = selectorBoxes[i].center.y + selectorBoxesFm.height / 2.0
                    text(text)
                }
            }

            val currentList = fullLists[currentBox].get()

            var topicBoxTracker = Vector2(selectorBoxes[0].x, 160.0)
            entriesInView = currentList.withIndex().associate { (i, item) ->
                    var bbox = Rectangle(selectorBoxes[0].x, 160.0, 0.0, 0.0)

                    drawer.writer {
                        gaplessNewLine()

                        when(currentBox) {
                            0 -> { // faculties
                                bbox = Rectangle(
                                    selectorBoxes[0].x,
                                    i * 30.0 + (25.0 * i) + 160.0,
                                    80.0,
                                    30.0
                                )

                                if(bbox.y < bounds.height && bbox.y > 80.0) {

                                    drawer.fontMap = facultyAbbrFm
                                    drawer.fill =
                                        if (activeFaculties.size != faculties.size && activeFaculties.contains(i)) facultyColors[i] else ColorRGBa.TRANSPARENT
                                    drawer.stroke = facultyColors[i]
                                    drawer.roundedRectangle(bbox.toRounded(999.0))

                                    val t0 = item.facultyAbbreviation()
                                    drawer.fill =
                                        if (activeFaculties.size != faculties.size && activeFaculties.contains(i)) ColorRGBa.BLACK else facultyColors[i]
                                    cursor.x = bbox.center.x - textWidth(t0) / 2.0
                                    cursor.y = bbox.center.y + facultyAbbrFm.height / 2.0
                                    text(t0)

                                    drawer.fontMap = facultyTopicFm
                                    drawer.fill = ColorRGBa.WHITE.opacify(0.8)
                                    cursor.x = bbox.corner.x + bbox.width + 5.0
                                    text(item.uppercase())
                                }
                            }
                            1 -> { // topics
                                drawer.fontMap = facultyTopicFm
                                val tw = textWidth(item) + 40.0
                                bbox = Rectangle(topicBoxTracker.x, topicBoxTracker.y, tw, 40.0).run {
                                    val final: Rectangle
                                    val isIn = x + width < selectorBoxes[0].x + bounds.width - 100.0
                                    if(isIn) {
                                        final = movedTo(topicBoxTracker)
                                        topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                                    } else {
                                        topicBoxTracker = Vector2(selectorBoxes[0].x, topicBoxTracker.y + 50.0)
                                        final = movedTo(topicBoxTracker)
                                        topicBoxTracker = Vector2(topicBoxTracker.x + tw, topicBoxTracker.y)
                                    }
                                    final
                                }

                                if(bbox.y < bounds.height && bbox.y > 80.0) {
                                    drawer.fill = if(activeTopics.size != topics.size && activeTopics.contains(i)) ColorRGBa.WHITE else ColorRGBa.TRANSPARENT
                                    drawer.stroke = ColorRGBa.WHITE
                                    drawer.roundedRectangle(bbox.toRounded(900.0))

                                    drawer.fill = if(activeTopics.size != topics.size && activeTopics.contains(i)) ColorRGBa.BLACK else  ColorRGBa.WHITE
                                    cursor.x = bbox.center.x - tw / 2.0
                                    cursor.y = bbox.center.y + facultyTopicFm.height / 2.0
                                    text(item.uppercase())
                                }
                            }
                            2 -> { // articles

                                bbox = Rectangle(
                                    selectorBoxes[0].x,
                                    i * 30.0 + (25.0 * i) + 160.0  + yOffset,
                                    80.0,
                                    29.0
                                )

                                if(bbox.y < bounds.height && bbox.y > 80.0) {
                                    drawer.fontMap = publicationFm
                                    val tw = textWidth(item)

                                    val seconds = (System.currentTimeMillis() - initialT) / 1000.0
                                    val xOffset = (tw - bounds.width + bounds.x).coerceAtLeast(0.0) / 2.0
                                    val t = (sin(seconds * 0.5) * 0.5 + 0.5) * xOffset

                                    val article = articles[i]
                                    val color = article.faculty.facultyColor()

                                    cursor.x = if(tw < bounds.width) bbox.corner.x else bbox.corner.x - t
                                    cursor.y = bbox.center.y + facultyTopicFm.height / 2.0

                                    val div = item.split("|")
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


                (bbox.center + bounds.corner) to i
            }


            drawer.drawStyle.clip = null
        }

    }

}