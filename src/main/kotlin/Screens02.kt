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
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt


var origin = Vector2(x=6400.0, y=2730.0)

fun main() = application {
    configure {
        width = (2560 * 4) / 5
        height = (1080 * 3) / 5
        hideWindowDecorations = true
        windowAlwaysOnTop = false
        position = IntVector2(0, 250)
    }
    program {

        val articles = Data().articles
        val active = setOf(0, 1, 4, 5, 6, 7, 8, 9)


        val frames = Rectangle(0.0, 0.0, 2560 * 4.0, 1080.0 * 3).grid(4, 3)
            .flatten()
            .slice(active)
            .sortedBy { it.center.distanceTo(origin) }



        val widescreenFrame = Rectangle(0.0, 0.0, 2560.0, 1080.0)

        val screens = frames.map {
            val i = ceil(origin.distanceTo(it.center) / it.diagonalLength).toInt() - 1


            val vb = viewBox(widescreenFrame) { screenTest02(i, it)  }
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

        val screensToFrames = (screens zip frames)

        extend {

            //origin = mouse.position * 5.0
            //println(origin)

            drawer.scale(1.0 / 5.0)
            screensToFrames.forEach { (screen, rect) ->
                screen.first.update()
                drawer.image(screen.first.result, screen.first.result.bounds, rect)
            }

            drawer.fill = ColorRGBa.RED
            drawer.circle(origin, 10.0)
        }
    }
}

val Rectangle.diagonalLength: Double
    get() = sqrt(width.pow(2) + height.pow(2))