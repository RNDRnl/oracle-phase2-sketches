package v05.libs;

import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.launch
import kotlin.reflect.KMutableProperty0

class PropertyWatcher<T>(val program: Program, val watchProperty: KMutableProperty0<T>) {
    var currentValue = watchProperty.get()
    val event = Event<T>("${watchProperty.name}-changed")

    init {
        program.launch {
            while (true) {
                val candidateValue = watchProperty.get()
                if (candidateValue != currentValue) {
                    currentValue = candidateValue
                    event.trigger(candidateValue)
                }
                yield()
            }
        }
    }
}

fun <T> Program.watchProperty(property: KMutableProperty0<T>): Event<T> {
    val pw = PropertyWatcher(this, property)
    return pw.event
}
