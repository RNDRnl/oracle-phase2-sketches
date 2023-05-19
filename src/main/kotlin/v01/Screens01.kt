package v01

import classes.ArticleData
import classes.Data
import classes.Receiver
import divider
import org.openrndr.MouseEventType
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle
import kotlin.concurrent.thread

fun main() = application {
    configure {
        width = (1920 * 2) / divider
        height = (1080 * 4) / divider
        hideWindowDecorations = true
        windowAlwaysOnTop = true
        position = IntVector2(2, 2)
    }
    program {

        val articles = Data().articles
        val frames = drawer.bounds.grid(2, 4).flatten()

        val widescreenFrame = Rectangle(0.0, 0.0, 3440.0, 2560.0)

        val screens = List(8) {
            val vb = viewBox(widescreenFrame) { screenTest01()  }
            val update: (met: MouseEventType, articlesToColors: List<Pair<ArticleData, ColorRGBa>>, zoom: Double)->Unit by vb.userProperties
            vb to update
        }

        val receiver = Receiver()

        receiver.stateReceived.listen {
            launch {
                val chunks = HashMap<Int, MutableList<Pair<ArticleData, ColorRGBa>>>(8)

                println(it.indexesToColors.size)

                var currentScreen = 0
                for((i, rgb) in it.indexesToColors) {
                    val color = rgb.run { ColorRGBa(r, g, b) }
                    val c = chunks.getOrPut(currentScreen) { mutableListOf() }

                    if(c.size < 14) {
                        c.add(articles[i] to color)
                    } else break

                    currentScreen = if(currentScreen == 7) 0 else currentScreen + 1
                }

                for(chunk in chunks) {

                    val updateFunc = screens[chunk.key].second
                    updateFunc(it.type, chunk.value.toList(), it.zoom)
                }


            }
         }

        thread (isDaemon = true) {
            while (true) {
                receiver.work()
            }
        }

        extend {

            for((screen, rect) in screens zip frames) {
                screen.first.update()
                drawer.image(screen.first.result, widescreenFrame, rect)
            }

        }
    }
}
