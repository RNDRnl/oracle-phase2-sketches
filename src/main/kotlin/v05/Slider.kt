package v05

import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import v05.libs.UIElementImpl


class Slider(val pos: Vector2): UIElementImpl() {

    inner class Animations: Animatable() {
        var fader = 0.0

        var timer = 0.0
        fun focus() {
            ::fader.animate(1.0, 350, Easing.CubicInOut)
        }

        fun unfocus() {
            ::timer.cancel()
            ::timer.animate(1.0, 1500).completed.listen {
                ::fader.animate(0.0, 500, Easing.CubicInOut)
            }
        }

    }
    val animations = Animations()

    val valueChanged = Event<Double>()

    var current = 0.0
        set(value) {
            val safeValue = value.coerceIn(0.0, 1.0)
            if(field != safeValue) {
                if(animations.fader == 0.0) animations.focus() else animations.unfocus()
                field = safeValue
                valueChanged.trigger(field)
            }
        }


    var bounds = Rectangle.fromCenter(pos, 350.0, 80.0)
    var acceptDragging = false

    init {
        actionBounds = bounds
        buttonDown.listen {
            it.cancelPropagation()
        }

        dragged.listen {
            current = it.position.x.map(
                actionBounds.position(0.0, 0.0).x,
                actionBounds.position(1.0, 0.0).x,
                0.0,
                1.0,
                clamp = true
            )
        }
    }


    fun buttonDown(it: MouseEvent) {
        if(visible && it.position in bounds.offsetEdges(65.0, 0.0)) {
            acceptDragging = true
            current = map(
                bounds.x,
                bounds.x + bounds.width,
                0.0,
                1.1,
                it.position.x.coerceIn(bounds.x, bounds.x + bounds.width))
        } else {
            acceptDragging = false
        }
    }

    fun buttonUp(it: MouseEvent) {
        acceptDragging = false
    }


    fun dragged(it: MouseEvent) {
        if (!it.propagationCancelled) {
            if (visible && acceptDragging) {
                it.cancelPropagation()
                current = map(
                    bounds.x,
                    bounds.x + bounds.width,
                    0.0,
                    1.1,
                    it.position.x.coerceIn(bounds.x, bounds.x + bounds.width)
                )
            }
        }
    }




    val fm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 20.0)
    fun draw(drawer: Drawer) {
        if(visible) {
            animations.updateAnimation()
            drawer.stroke = null

            val h = 50.0 + (30.0 * animations.fader)
            bounds = Rectangle.fromCenter(pos, 250.0 + (100.0 * animations.fader), h)
            drawer.fill = ColorRGBa.WHITE.opacify(0.3 + (0.4 * animations.fader))
            drawer.roundedRectangle(bounds.offsetEdges(10.0).toRounded(1999.0))

            drawer.fill = ColorRGBa.WHITE.opacify(0.7 + 0.3 * animations.fader)
            val rail = LineSegment(bounds.x + h / 2.0, bounds.y + bounds.height / 2.0,
                bounds.x + bounds.width - (h / 2.0), bounds.y + bounds.height / 2.0)
            val pos = rail.position(current)

            drawer.fill = ColorRGBa.WHITE.opacify(0.7 + 0.3 * animations.fader).opacify(0.5)
            drawer.circle(pos, h/2.0)
            drawer.fill = ColorRGBa.WHITE.opacify(0.7 + 0.3 * animations.fader)
            drawer.circle(pos, current.map(0.0, 1.0, (h / 2.0), (h/8.0)))

            drawer.fill = ColorRGBa.BLACK
            drawer.fontMap = fm
        }
    }

}