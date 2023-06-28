package v05.extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.events.listen

class IdleDetector : Extension{
    override var enabled = true
    var lastInteraction = 0.0
    val idleDetected = Event<Unit>("idle-detected")
    override fun setup(program: Program) {
        listOf(program.mouse.moved, program.mouse.dragged, program.mouse.buttonDown, program.mouse.buttonDown).listen {
            lastInteraction = program.seconds
        }
    }
    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (program.seconds - lastInteraction > 60.0) {
            idleDetected.trigger(Unit)
        }
    }
}