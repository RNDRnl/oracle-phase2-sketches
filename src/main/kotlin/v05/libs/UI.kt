package v05.libs

import org.openrndr.MouseEvent
import org.openrndr.MouseEvents
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

interface UIElement: MouseEvents {
    val zOrder: Int
    val actionBounds: Rectangle
    val visible: Boolean
}

open class UIElementImpl : UIElement {

    override var zOrder = 0
    override var actionBounds = Rectangle(0.0, 0.0, 100.0, 100.0)
    override var visible = true

    override val buttonDown: Event<MouseEvent> = Event("ui-element-button-down")
    override val buttonUp: Event<MouseEvent> = Event("ui-element-button-up")
    override val dragged: Event<MouseEvent> = Event("ui-element-dragged")

    override val entered: Event<MouseEvent> = Event("ui-element-entered")
    override val exited: Event<MouseEvent> = Event("ui-element-exited")
    override val moved: Event<MouseEvent> = Event("ui-element-moved")

    override val position: Vector2
        get() = error("don't use 'position'")

    override val scrolled: Event<MouseEvent> = Event("ui-element-scrolled")
}

class UIManager(mouseEvents: MouseEvents) {
    var activeElement: UIElement? = null
    var dragElement: UIElement? = null

    init {
        mouseEvents.buttonDown.listen { event ->
            if (!event.propagationCancelled) {
                for (e in elements.filter { it.visible && event.position in it.actionBounds }.sortedBy { it.zOrder }) {
                    e.buttonDown.trigger(event)
                    if (event.propagationCancelled) {
                        activeElement = e
                        dragElement = e
                    }
                }
            }
        }

        mouseEvents.dragged.listen { event ->
            dragElement?.dragged?.trigger(event)
        }

        mouseEvents.buttonUp.listen { event ->
            activeElement?.buttonUp?.trigger(event)
            dragElement = null
        }
    }
    val elements = mutableListOf<UIElement>()
}

