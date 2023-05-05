package classes

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.mix
import org.openrndr.math.transforms.perspective
import org.openrndr.shape.Rectangle

class QuaternionCameraSimple : Extension {

    var orientation = Quaternion.IDENTITY

    var dragStart = Vector2(0.0, 0.0)

    var bounds = Rectangle.EMPTY

    var maxZoomOut = 90.0

    var orientationChanged: Event<Quaternion> = Event()
    var zoomChanged: Event<Quaternion> = Event()

    var buttonDown = false

    var zoom = 0.5
        set(value) {
            zoomChanged.trigger(orientation)
            field = value
        }

    override fun setup(program: Program) {
        program.mouse.buttonDown.listen {
            if (!it.propagationCancelled && it.position in bounds) {
                buttonDown = true
                dragStart = it.position
            }
        }


        program.mouse.dragged.listen {
            if (!it.propagationCancelled && it.position in bounds) {
                if (!buttonDown) {
                    return@listen
                }
                val sensitivity = mix(1.0 / 100.0, 1.0 / 10.0, zoom)

                orientation = Quaternion.fromAngles(
                    it.dragDisplacement.x * sensitivity,
                    it.dragDisplacement.y * sensitivity,
                    0.0
                ) * orientation
                orientationChanged.trigger(orientation)

            }
        }


        program.mouse.buttonUp.listen {
            if (!it.propagationCancelled && it.position in bounds) {
                buttonDown = false
            }
        }
    }

    override var enabled: Boolean = true

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()
        val fov = (zoom * 76.0 + 12.0).coerceAtMost(maxZoomOut)
        drawer.projection = perspective(fov, 2880.0/1920.0, 0.1, 50.0)
        drawer.view = orientation.matrix.matrix44
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }
}