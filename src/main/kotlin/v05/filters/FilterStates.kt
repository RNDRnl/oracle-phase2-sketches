package v05.filters

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event

abstract class FilterState: Animatable() {
    val stateChanged = Event<Unit>()

    var fade: Double = 1.0
}

class ToggleFilterState: FilterState() {

    var visible: Boolean = true
        set(value) {
            if (value != field) {
                field = value

                cancel()
                if (value) {
                    ::fade.animate(1.0, 500, Easing.QuadInOut)
                }

                if (!value) {
                    ::fade.animate(0.0, 500, Easing.QuadInOut)
                }
                stateChanged.trigger(Unit)
            }
        }


}

class DateFilterState(year: Int): FilterState() {

    var animatedYear = year.toDouble()
    var year = year.toDouble()
        set(value) {
            if (value != field) {

                cancel()
                if (value > field)
                    animatedYear = value -1.0
                if (value < field) {
                    animatedYear = value+1.0
                }
                ::animatedYear.animate(value, 500, Easing.QuadInOut)

                field = value

                stateChanged.trigger(Unit)
            }
        }
}

