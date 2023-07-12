package v05

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment

class Viewfinder(val state: State, val camera: Camera2D): Animatable() {

    var radius = 100.0
    var oldRadius = radius
    var pos = state.lookAt
        set(value) {
            field = value
            cancel()
            ::posFader.animate(1.0, 650, Easing.CubicOut).completed.listen {
                oldPos = value
                oldRadius = radius
                posFader = 0.0
            }
        }
    var oldPos = pos
    var posFader = 0.0

    var fader = 0.0

    fun fadeIn() {
        cancel()
        ::fader.animate(1.0, 750, Easing.CubicInOut)
    }

    fun fadeOut() {
        cancel()
        ::fader.animate(0.0, 350, Easing.CubicInOut)
    }


    fun moveTo(target: Vector2) {
        radius = state.lookAt.distanceTo(state.furthest!!)
        pos = target
    }

    var zoom = 0.0
        set(value) {
            if (field != value ) {
                if(value in CLOSER && fader == 0.0) {
                    fadeIn()
                } else if((value in CLOSEST || value in FURTHEST) && fader == 1.0) {
                    fadeOut()
                }
            }


            field = value
        }

    fun draw(z: Double, drawer: Drawer) {
        updateAnimation()

        drawer.isolated {
            //drawer.defaults()
            zoom = z

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(fader)
            drawer.strokeWeight = 1.0 /  view.scale()

            val newPos = oldPos.mix(pos, posFader)

            state.furthest?.let {
                drawer.circle(newPos, radius * posFader + oldRadius * (1.0 - posFader))
            }

            val offset =  30.0 * (1.0 - fader) / view.scale()
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 1.5 / view.scale()
            drawer.lineSegment(LineSegment(newPos.x, newPos.y - offset, newPos.x, newPos.y + offset))
            drawer.lineSegment(LineSegment(newPos.x - offset, newPos.y, newPos.x + offset, newPos.y))
        }
    }

}