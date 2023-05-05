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
            val update: (met: MouseEventType, articlesToColors: List<Pair<ArticleData, ColorRGBa>>)->Unit by vb.userProperties
            vb to update
        }

        val receiver = Receiver()

        receiver.stateReceived.listen {
            launch {
                println("received ${it.indexesToColors.size}")
                val n = it.indexesToColors.size / screens.size

                if(n != 0) {
                    screens.forEachIndexed { i, (vb, update) ->
                        val chunk = it.indexesToColors.chunked(n)[i].take(18)
                        val ad = chunk.map { articles[it.first] }
                        val colors = chunk.map {
                            val c = it.second
                            ColorRGBa(c.r, c.g, c.b) }
                        update(it.type, ad zip colors)
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

            for((screen, rect) in screens zip frames) {
                screen.first.update()
                drawer.image(screen.first.result, widescreenFrame, rect)
            }

        }
    }
}
