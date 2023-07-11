package v05.extensions

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Vector2

data class PinchEvent(val center: Vector2, val scale: Double, val scaleDelta: Double)
class PinchDetector : Extension {
    override var enabled: Boolean = true
    var pinching = false
    val pinchStarted = Event<Unit>()
    val pinchEnded = Event<Unit>()
    val pinchChanged = Event<PinchEvent>()
    var startDistance = 1E3
    var lastScale = 1.0
    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (program.pointers.pointers.size == 2 && !pinching) {
            pinching = true
            pinchStarted.trigger(Unit)
            startDistance = program.pointers.pointers[0].position.distanceTo(
                program.pointers.pointers[1].position
            )
            lastScale = 1.0
        } else if (pinching && program.pointers.pointers.size < 2) {
            pinching = false
            pinchEnded.trigger(Unit)
        }
        if (pinching) {
            val center = (program.pointers.pointers[0].position + program.pointers.pointers[1].position) / 2.0
            val currentDistance = program.pointers.pointers[0].position.distanceTo(
                program.pointers.pointers[1].position
            )

            val delta = currentDistance / startDistance - lastScale

            if (delta > 1E-4) {
                pinchChanged.trigger(
                    PinchEvent(
                        center,
                        currentDistance / startDistance,
                        delta
                    )
                )
            }
            lastScale = currentDistance / startDistance
        }
    }
}