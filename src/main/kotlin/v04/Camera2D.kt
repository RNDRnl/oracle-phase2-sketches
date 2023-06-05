package v04

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.events.Event
import org.openrndr.extra.camera.ChangeEvents
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.map
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.log2

/**
 * The [Camera2D] extension enables panning, rotating and zooming the view
 * with the mouse:
 * - left click and drag to **pan**
 * - right click and drag to **rotate**
 * - use the mouse wheel to **zoom** in and out
 *
 * Usage: `extend(Camera2D())`
 */
class Camera2D : Extension, ChangeEvents {
    override var enabled = true

    val zoomSpeed = 0.1

    var minZoom = 1.0
    var maxZoom = 60.0

    var mappedZoom = 0.0

    var view = Matrix44.IDENTITY
        set(value) {
            val scale = (value * Vector4(1.0, 1.0, 0.0, 0.0).normalized).xy.length

            if (scale in minZoom..maxZoom) {
                mappedZoom = ln(scale).map(ln(minZoom), ln(maxZoom), 0.0, 1.0)
                field = value
            }
        }

    var inUiElement = false

    override val changed = Event<Unit>()

    var dirty = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
            }
            field = value
        }
    override val hasChanged: Boolean
        get() = dirty


    fun dragged(mouse: MouseEvent) {
        if(!inUiElement) {
            view = buildTransform {
                translate(mouse.dragDisplacement)
            } * view
            dirty = true
        }

    }

    fun scrolled(mouse: MouseEvent) { // this
        val scaleFactor = 1.0 - mouse.rotation.y * zoomSpeed

        view = buildTransform {
            translate(mouse.position)
            scale(scaleFactor)
            translate(-mouse.position)
        } * view
        dirty = true
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()
        drawer.ortho(RenderTarget.active)
        drawer.view = view
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        dirty = false
        drawer.popTransforms()
    }
}

