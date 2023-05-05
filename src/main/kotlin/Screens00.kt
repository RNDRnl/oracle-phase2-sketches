import classes.Receiver
import org.openrndr.MouseEventType
import org.openrndr.application
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
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

        val frames = drawer.bounds.grid(2, 4).flatten()

        val widescreenFrame = Rectangle(0.0, 0.0, 3440.0, 2560.0)

        val screens = List(8) {
            val vb = viewBox(widescreenFrame).apply { screenTest() }
            val update: (met: MouseEventType)->Unit by vb.userProperties
            vb to update
        }

        val receiver = Receiver()
        receiver.stateReceived.listen {
            println("received")
            for((vb, update) in screens) {
                update(it.type)
            }
        }

        thread {
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
