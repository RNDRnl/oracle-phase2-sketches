package v05.libs

import org.openrndr.*
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import v05.extensions.PinchEvent
import kotlin.random.Random

interface UIElement: MouseEvents {
    val zOrder: Int
    val actionBounds: Rectangle
    val visible: Boolean
    val pinched: Event<PinchEvent>
}

open class UIElementImpl : UIElement {

    override var zOrder = 0
    override var actionBounds = Rectangle(0.0, 0.0, 100.0, 100.0)
    override var visible = true

    override val buttonDown: Event<MouseEvent> = Event("ui-element-button-down")
    override val buttonUp: Event<MouseEvent> = Event("ui-element-button-up")
    override val dragged: Event<MouseEvent> = Event("ui-element-dragged")
    override val pinched: Event<PinchEvent> = Event("ui-element-pinched")

    override val entered: Event<MouseEvent> = Event("ui-element-entered")
    override val exited: Event<MouseEvent> = Event("ui-element-exited")
    override val moved: Event<MouseEvent> = Event("ui-element-moved")

    override val position: Vector2
        get() = error("don't use 'position'")

    override val scrolled: Event<MouseEvent> = Event("ui-element-scrolled")
}

class UIManager(val window: Window, mouseEvents: MouseEvents, pinchEvent: Event<PinchEvent>) {
    var activeElement: UIElement? = null
    var dragElement: UIElement? = null
    var postDragElement: UIElement? = null

    private var lastMousePosition = Vector2(0.0, 0.0)
    private var velocity = Vector2(0.0, 0.0)
    private var potentialVelocity = Vector2(0.0, 0.0)

    var lastAction = System.currentTimeMillis()

    fun requestDraw() {
        lastAction = System.currentTimeMillis()
        window.requestDraw()
    }

    val postDragEnded = Event<Unit>()
    val clicked = Event<Unit>()

    var dragged = false

    init {
        mouseEvents.buttonDown.listen { event ->
            lastMousePosition = event.position
            if (!event.propagationCancelled) {
                for (e in elements.filter { it.visible && event.position in it.actionBounds }.sortedBy { it.zOrder }) {
                    e.buttonDown.trigger(event)
                    if (event.propagationCancelled) {
                        potentialVelocity = Vector2.ZERO
                        velocity = Vector2.ZERO
                        requestDraw()
                        activeElement = e
                        dragElement = null
                    }
                }
            }
        }

        mouseEvents.dragged.listen { event ->
            if (!event.propagationCancelled) {

                if (dragElement != null) {
                    potentialVelocity = Vector2.ZERO
                }

                dragElement = activeElement

                if (dragElement != null) {
                    requestDraw()
                    potentialVelocity = event.position - lastMousePosition
                    lastMousePosition = event.position
                }

                dragElement?.dragged?.trigger(event)
            }
        }

        mouseEvents.buttonUp.listen { event ->
            lastMousePosition = event.position

            activeElement?.buttonUp?.trigger(event)
            if (activeElement != null && dragElement == null) {
                clicked.trigger(Unit)
            }

            if (dragElement != null) {
                velocity = potentialVelocity
                postDragElement = dragElement
                dragElement = null
            }
        }

        pinchEvent.listen {
            activeElement?.pinched?.trigger(it)
        }

        mouseEvents.scrolled.listen {
            activeElement?.scrolled?.trigger(it)
            if (activeElement != null) {
                requestDraw()
            }
        }
    }
    val elements = mutableListOf<UIElement>()

    fun update() {

        if (System.currentTimeMillis() - lastAction < 2000) {
            window.requestDraw()
        }

        velocity *= 0.9
        if (velocity.length < 0.01 && postDragElement != null) {
            velocity = Vector2.ZERO
            postDragElement = null
            postDragEnded.trigger(Unit)
        } else {
            if (postDragElement != null) {
                requestDraw()
                lastMousePosition += velocity
                postDragElement?.dragged?.trigger(MouseEvent(lastMousePosition, Vector2.ZERO, velocity, MouseEventType.DRAGGED, MouseButton.NONE, emptySet()))
            }
        }
    }

    fun drawDebugBoxes(drawer: Drawer) {
        drawer.isolated {
            drawer.defaults()
            drawer.fill = null
            drawer.rectangles {
                elements.filter { it.visible }.mapIndexed { i, it ->
                    this.stroke = ColorHSLa(0.5, 0.5, 0.5).shiftHue(360.0 * Double.uniform(0.0, 1.0, Random(i) )).toRGBa()
                    this.rectangle(it.actionBounds)
                }
            }

            drawer.fill = ColorRGBa.GREEN
            for(e in elements.filter { it.visible }) {
                val rect = e.actionBounds
                e::class.simpleName?.let { drawer.text(it, rect.center) }
            }

            if (dragElement != null) {
                drawer.fill = ColorRGBa.WHITE.opacify(0.2)
                drawer.rectangle(dragElement!!.actionBounds)
            }

            drawer.fill = ColorRGBa.PINK
            if(postDragElement != null) {
                postDragElement!!::class.simpleName?.let { drawer.text("last dragged: $it", 20.0, 20.0) }
            }
            if(activeElement != null) {
                activeElement!!::class.simpleName?.let { drawer.text("last clicked: $it", 20.0, 40.0) }
            }

            drawer.fill = ColorRGBa.RED
        }
    }
}

