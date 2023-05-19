package v03

import classes.ArticleData
import classes.Data
import classes.Receiver
import org.openrndr.MouseEventType
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle
import origin
import kotlin.concurrent.thread

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

        val articles = Data().articles
        val active = setOf(0, 1, 4, 5, 6, 7, 8, 9)

        val frames = Rectangle(0.0, 0.0, 2560 * 4.0, 1080.0 * 3).grid(4, 3)
            .flatten()
            .slice(active)

        val widescreenFrame = Rectangle(0.0, 0.0, 2560.0, 1080.0)

        val screens = frames.mapIndexed { i, r ->
            val vb = viewBox(widescreenFrame) { screenTest03(i, r)  }
            val update: (met: MouseEventType, articlesToColors: List<Pair<ArticleData, ColorRGBa>>, zoomLevel: Int)->Unit by vb.userProperties

            vb to update
        }

        val receiver = Receiver()

        receiver.stateReceived.listen {

            val zoomLevel = when (it.zoom) {
                in 0.0..0.33 -> 0
                in 0.33..0.8 -> 1
                else -> 2
            }
            val deserialized = it.indexesToColors.map { (i, c) ->
                articles[i] to c.run { ColorRGBa(r, g, b) }
            }

            launch {
                if(zoomLevel < 2) {
                    val chunks = HashMap<Int, MutableList<Pair<ArticleData, ColorRGBa>>>(8)

                    var currentScreen = 0
                    for((ad, color) in deserialized) {
                        val c = chunks.getOrPut(currentScreen) { mutableListOf() }

                        if(c.size < 300) {
                            c.add(ad to color)
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

        val screensToFrames = (screens zip frames)

        extend {

            //origin = mouse.position * 5.0
            //println(origin)

            if(debug) drawer.scale(1.0 / 5.0)

            screensToFrames.forEach { (screen, rect) ->
                screen.first.update()
                drawer.image(screen.first.result, screen.first.result.bounds, rect)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.rectangle(rect)
            }

            drawer.fill = ColorRGBa.RED
            drawer.circle(origin, 10.0)
        }
    }
}
