package v05.filters

import org.openrndr.events.Event
import v05.libs.UIElementImpl


abstract class Filter: UIElementImpl() {

    open var isCurrent = false

    open val title = ""

    open fun draw() {


    }
}

abstract class FilterModel {

    val filterChanged = Event<Unit>()

    open val list = listOf<Any>()
    open val states = listOf<FilterState>()


    fun update() {
        for (state in states) {
            state.updateAnimation()
        }
    }
}
