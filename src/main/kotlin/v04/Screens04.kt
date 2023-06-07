package v04

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorRGBa.Companion.BLUE
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import kotlin.concurrent.thread

fun main() = application {
    val debug = true
    val scale = 3

    configure {
        if(debug) {
            width = (2560 * 4) / scale
            height = (1080 * 3) / scale
            position = IntVector2(-300, -1800)
        } else {
            width = (2560 * 4)
            height = (1080 * 3)
            position = IntVector2(0, 0)
        }
        hideWindowDecorations = true
        windowAlwaysOnTop = false
    }
    program {

        val data = DataModelNew(Rectangle(Vector2.ZERO, 1920.0, 1080.0))
        val frames = Rectangle(0.0, 0.0, 2560 * 4.0, 1080.0 * 3)
            .grid(4, 3)
            .flatten()
            .slice(setOf(0, 1, 4, 5, 6, 7, 8, 9))

        val screens = frames.mapIndexed { i, r ->
            val vb = viewBox(Rectangle(0.0, 0.0, 2560.0, 1080.0)) { screenTest04(i, r)  }
            val update: (articles: MutableList<Article>, zoomLevel: Int)->Unit by vb.userProperties

            vb to update
        }


        val receiver = Receiver()


        receiver.stateReceived.listen { e ->
            val zoomLevel = when (e.zoom) {
                in 0.0..0.33 -> 0
                in 0.2..0.8 -> 1
                else -> 2
            }

            val newArticles = mutableListOf<Article>()
            for(i in e.articleIndexes) {
                newArticles.add(data.articles[i])
            }


            println(newArticles.size)
            launch {
                if(zoomLevel < 2) {
                    val chunks = HashMap<Int, MutableList<Article>>(8)

                    var currentScreen = 0
                    for(article in newArticles) {
                        val c = chunks.getOrPut(currentScreen) { mutableListOf() }

                        if(c.size < 1000) {
                            c.add(article)
                        } else break

                        currentScreen = if(currentScreen == 7) 0 else currentScreen + 1
                    }

                    for(chunk in chunks) {
                        val updateFunc = screens[chunk.key].second
                        updateFunc(chunk.value, zoomLevel)
                    }
                } else {
                    for((_, updateFunc) in screens) {
                        updateFunc(newArticles.toMutableList(), zoomLevel)
                    }
                }
            }
        }


        thread (isDaemon = true) {
            while (true) {
                receiver.work()
            }
        }

        extend {

            if(debug) drawer.scale(1.0 / scale)

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
