package v05.libs

import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.MouseEvents
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.random.Random

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

    private var lastMousePosition = Vector2(0.0, 0.0)
    private var velocity = Vector2(0.0, 0.0)
    private var potentialVelocity = Vector2(0.0, 0.0)


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
            if (dragElement != null) {
                potentialVelocity = event.position - lastMousePosition
                lastMousePosition = event.position
            }

            dragElement?.dragged?.trigger(event)
        }

        mouseEvents.buttonUp.listen { event ->
            activeElement?.buttonUp?.trigger(event)

            if (dragElement != null) {
                velocity = potentialVelocity
                //dragElement = null
            }
        }
    }
    val elements = mutableListOf<UIElement>()

    fun update() {
        velocity *= 0.9
        if (velocity.length < 0.01) {
            velocity = Vector2.ZERO
        } else {
            if (dragElement != null) {
                lastMousePosition += velocity
                dragElement?.dragged?.trigger(MouseEvent(lastMousePosition, Vector2.ZERO, velocity, MouseEventType.DRAGGED, MouseButton.NONE, emptySet()))
            }
        }
    }

    fun drawDebugBoxes(drawer: Drawer) {
        drawer.isolated {
            drawer.defaults()
            drawer.fill = null
            drawer.rectangles {
                elements.mapIndexed { i, it ->
                    val v = Vector3.uniformRing(0.0, 1.0, Random(i))
                    this.stroke = ColorRGBa.fromVector(v)
                    this.rectangle(it.actionBounds)
                }
            }

            drawer.fill = ColorRGBa.GREEN
            for(e in elements) {
                val rect = e.actionBounds
                e::class.simpleName?.let { drawer.text(it, rect.center) }
            }

            if (dragElement != null) {
                drawer.fill = ColorRGBa.WHITE.opacify(0.2)
                drawer.rectangle(dragElement!!.actionBounds)
            }
        }
    }
}

