package v05

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer

class Viewfinder(val state: State, val camera: Camera2D): Animatable() {

    var fader = 0.0

    fun fadeIn() {
        cancel()
        ::fader.animate(1.0, 750, Easing.CubicInOut)
    }

    fun fadeOut() {
        cancel()
        ::fader.animate(0.0, 350, Easing.CubicInOut)
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

        zoom = z

        drawer.fill = null
        drawer.stroke = ColorRGBa.WHITE.opacify(fader)

        drawer.circle(state.lookAt, state.lookAt.distanceTo(state.furthest.transform(camera.view)))

        drawer.fill = ColorRGBa.WHITE
        drawer.circle(state.lookAt, 1.0)
    }

}