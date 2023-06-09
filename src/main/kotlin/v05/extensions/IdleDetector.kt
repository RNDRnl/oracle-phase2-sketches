package v05.extensions

import kotlinx.coroutines.yield
import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.events.listen
import org.openrndr.launch


private val logger = KotlinLogging.logger {}

class IdleDetector : Extension{
    override var enabled = true
    var lastInteraction = System.currentTimeMillis()
    var idle = true
    val idleStarted = Event<Unit>("idle-started")
    val idleEnded = Event<Unit>("idle-ended")
    override fun setup(program: Program) {
        listOf(program.mouse.moved, program.mouse.dragged, program.mouse.buttonDown, program.mouse.buttonDown, program.mouse.scrolled).listen {
            lastInteraction = System.currentTimeMillis()
            if (idle) {
                logger.info { "activity detected, ending idle mode" }
                idle = false
                idleEnded.trigger(Unit)
            }
        }
        program.launch {
            while (true) {
                if (System.currentTimeMillis() - lastInteraction > 60_000) {
                    if (!idle) {
                        logger.info { "no activity detected for 60.0s, starting idle mode" }
                        idle = true
                        idleStarted.trigger(Unit)
                    }
                }
                yield()
            }
        }
    }

}