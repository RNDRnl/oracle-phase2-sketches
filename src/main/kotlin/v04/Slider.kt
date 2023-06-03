package v04

import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.math.mix
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle


class Slider(val pos: Vector2): Animatable() {

    var visible = true

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

    var current = 0.0
        set(value) {
            if(field != value) {
                if(fader == 0.0) focus() else unfocus()
            }
            field = value.coerceIn(0.0, 1.0)
        }

    var bounds = Rectangle.fromCenter(pos, 350.0, 80.0)

    val fm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 20.0)
    fun draw(drawer: Drawer) {
        if(visible) {
            updateAnimation()
            drawer.stroke = null

            val h = 50.0 + (30.0 * fader)
            bounds = Rectangle.fromCenter(pos, 250.0 + (100.0 * fader), h)
            drawer.fill = ColorRGBa.WHITE.opacify(0.3 + (0.4 * fader))
            drawer.roundedRectangle(bounds.offsetEdges(10.0).toRounded(1999.0))

            drawer.fill = ColorRGBa.WHITE.opacify(0.7 + 0.3 * fader)
            val rail = LineSegment(bounds.x + h / 2.0, bounds.y + bounds.height / 2.0,
                bounds.x + bounds.width - (h / 2.0), bounds.y + bounds.height / 2.0)
            val pos = rail.position(current)
            drawer.circle(pos, h / 2.0)

            drawer.fill = ColorRGBa.BLACK
            drawer.fontMap = fm
            val n = current.toString().take(3)
            val textWidth = n.fold(0.0) { acc, next -> fm.characterWidth(next) + acc }
            drawer.text(n, pos.x - (textWidth / 2.0), pos.y + (fm.height / 2.0))
        }
    }

}