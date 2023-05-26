package v04

import org.openrndr.MouseEventType
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import v03.Article
import v03.screenTest03

fun main() = application {
    val debug = true

    configure {
        if(debug) {
            width = (2560 * 4) / 5
            height = (1080 * 3) / 5
            position = IntVector2(0, 250)
        } else {
            width = (2560 * 4)
            height = (1080 * 3)
            position = IntVector2(0, 0)
        }
        hideWindowDecorations = true
        windowAlwaysOnTop = false
    }
    program {

        val frames = Rectangle(0.0, 0.0, 2560 * 4.0, 1080.0 * 3).grid(4, 3).flatten()

        val screens = frames.slice(setOf(0, 1, 4, 5, 6, 7, 8, 9)).mapIndexed { i, r ->
            val vb = viewBox(Rectangle(0.0, 0.0, 2560.0, 1080.0)) { screenTest03(i, r)  }
            val update: (met: MouseEventType, articlesToColors: List<Article>, zoomLevel: Int)->Unit by vb.userProperties

            vb to update
        }
/*

        val receiver = Receiver()

        receiver.stateReceived.listen {

            val zoomLevel = when (it.zoom) {
                in 0.0..0.33 -> 0
                in 0.33..0.8 -> 1
                else -> 2
            }

            val deserialized = it.indexesToColors.map { (i, c) ->
                Article(articles[i], c.run { ColorRGBa(r, g, b) })
            }
            val deserializedSorted = deserialized.sortedBy { d -> d.ad.faculty }

            launch {
                if(zoomLevel < 2) {
                    val chunks = HashMap<Int, MutableList<Article>>(8)

                    var currentScreen = 0
                    val list = if(zoomLevel == 1) deserialized else deserializedSorted
                    for(article in list) {
                        val c = chunks.getOrPut(currentScreen) { mutableListOf() }

                        if(c.size < 1000) {
                            c.add(article)
                        } else break

                        currentScreen = if(currentScreen == 7) 0 else currentScreen + 1
                    }

                    for(chunk in chunks) {
                        val updateFunc = screens[chunk.key].second
                        updateFunc(it.type, chunk.value.toList(), zoomLevel)
                    }
                } else {
                    for((_, updateFunc) in screens) {
                        updateFunc(it.type, deserialized, zoomLevel)
                    }
                }
            }
        }

        thread (isDaemon = true) {
            while (true) {
                receiver.work()
            }
        }
*/


        extend {

            if(debug) drawer.scale(1.0 / 5.0)

            (screens zip frames).forEach { (screen, rect) ->
                screen.first.update()
                drawer.image(screen.first.result, screen.first.result.bounds, rect)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.rectangle(rect)
            }



        }
    }
}
