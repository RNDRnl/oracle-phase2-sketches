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
import v05.filters.FilterSet
import v05.screens.IdleMode


class ScreenState(var mode: Int = IDLE, var zoomLevel: Int = 0)

class ScreenMessage(val mode:Int, val articles:List<Article>, val zoomLevel: Int, val filters: FilterSet)

fun Program.screenProgram(i: Int, rect: Rectangle, dataModel: DataModel) {

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

    val state = ScreenState(NAVIGATE, 0)

    val zoomLevels = listOf(::Zoom0, ::Zoom1, ::Zoom2).map { it(i, rect, dataModel) }
    val idleMode = IdleMode(dataModel.articles)


    var update: (message: ScreenMessage)->Unit by this.userProperties
    update = { m ->
        println("setting mode to ${m.mode} (from ${state.mode}")
        state.mode = m.mode
        controller.fadeIn()
        state.zoomLevel = m.zoomLevel
        zoomLevels[m.zoomLevel].processMessage(m)
        articles = m.articles
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
                IDLE -> idleMode.draw(this@screenProgram, drawer, circle)
                NAVIGATE -> zoomLevels[state.zoomLevel].draw(this@screenProgram, drawer, circle)
            }
        }
    }
}