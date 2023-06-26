package v05

import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import origin
import v05.screens.IdleMode


class ScreenState(var mode: Int = IDLE, var zoomLevel: Int = 0)

fun Program.screenProgram(i: Int, rect: Rectangle) {

    var articles = listOf<Article>()

    class Controller: Animatable() {
        var showTimer = 0.0
        var hideTimer = 0.0

        fun fadeIn() {
            ::hideTimer.cancel()
            hideTimer = 0.0

            showTimer = 0.0
            ::showTimer.cancel()
            ::showTimer.animate(1.0, 3200L, Easing.SineOut)
        }
    }
    val controller = Controller()

    val state = ScreenState()

    val zoomLevels = listOf(::Zoom0, ::Zoom1, ::Zoom2).map { it(i, rect, drawer) }
    val idleMode = IdleMode(drawer)


    var update: (mode: Int, articles: MutableList<Article>, zoomLevel: Int)->Unit by this.userProperties
    update = { mode, newArticles, zoomLevel ->
        println("setting mode to ${mode} (from ${state.mode}")
        state.mode = mode
        controller.fadeIn()
        state.zoomLevel = zoomLevel
        zoomLevels[zoomLevel].populate(newArticles)
        articles = newArticles
    }

    extend {
        controller.updateAnimation()

        if (state.mode != IDLE) {
            zoomLevels[state.zoomLevel].update()
        }

        drawer.clear(ColorRGBa.BLACK.shade(0.35))

        drawer.defaults()

        val circle = Circle(origin, 7200.0 * controller.showTimer)
        drawer.defaults()
        drawer.isolated {
            when (state.mode) {
                IDLE -> idleMode.draw(circle)
                NAVIGATE -> zoomLevels[state.zoomLevel].draw(circle)
            }
        }


    }
}