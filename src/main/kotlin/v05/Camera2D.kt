package v05

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
import kotlin.math.*

class Camera2D : Extension, ChangeEvents {
    override var enabled = true

    val zoomSpeed = 0.1

    var minZoom = 0.5
    var maxZoom = 60.0

    var mappedZoom = 0.0
    var zoomPosition = Vector2(0.0, 0.0)
    lateinit var program: Program
    var view = Matrix44.IDENTITY
        set(value) {
            val scale = (value * Vector4(1.0, 1.0, 0.0, 0.0).normalized).xy.length

            if (scale in minZoom..maxZoom) {
                mappedZoom = toNormalizedScale(scale)
                field = value
            }
        }

    var inUiElement = false

    override val changed = Event<Unit>()

    fun quantize(x: Double): Double {
        return floor(x * 1000.0) / 1000.0
    }

    fun Matrix44.scale(): Double = (this * Vector4(1.0, 1.0, 0.0, 0.0).normalized).xy.length

    fun toNormalizedScale(scale: Double): Double {
        return quantize(ln(scale).map(ln(minZoom), ln(maxZoom), 0.0, 1.0, clamp = true))
    }

    fun toExpScale(scale: Double): Double {
        val lnScale = scale.map(0.0, 1.0, ln(minZoom), ln(maxZoom))
        return exp(lnScale)
    }

    fun centerAt(targetPosition: Vector2) {
        val w = RenderTarget.active.width
        val h = RenderTarget.active.height
        view = buildTransform {
            translate(-targetPosition + Vector2(w/2.0, h/2.0))
        }
    }

    fun setNormalizedScale(targetScaleNormalized: Double) {
        val targetScale = toExpScale(quantize(targetScaleNormalized))
        val currentScale = view.scale()
        val factor = targetScale / currentScale
        zoomPosition = program.drawer.bounds.center
        view = buildTransform {
            translate(zoomPosition)
            scale(factor)
            translate(-zoomPosition)
        } * view
        dirty = true

    }


    var dirty = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
            }
            field = value
        }
    override val hasChanged: Boolean
        get() = dirty


    private var lastMousePosition = Vector2(0.0, 0.0)
    private var velocity = Vector2(0.0, 0.0)
    private var potentialVelocity = Vector2(0.0, 0.0)

    fun buttonDown(mouse: MouseEvent) {
        if (!inUiElement) {
            lastMousePosition = mouse.position
        }
        potentialVelocity = Vector2.ZERO
    }
    fun buttonUp(mouse: MouseEvent) {
        if (!inUiElement) {
            velocity = potentialVelocity
            view = buildTransform {
                translate(velocity)
            } * view
        }
    }

    fun dragged(mouse: MouseEvent) {
        /*if (!mouse.propagationCancelled) {

        }*/

        if (!inUiElement) {
            potentialVelocity =mouse.position - lastMousePosition
            lastMousePosition = mouse.position
            view = buildTransform {
                translate(mouse.dragDisplacement)
            } * view
            dirty = true
        }
    }

    fun scrolled(mouse: MouseEvent) { // this
        val scaleFactor = 1.0 - mouse.rotation.y * zoomSpeed
        zoomPosition = mouse.position

        view = buildTransform {
            translate(mouse.position)
            scale(scaleFactor)
            translate(-mouse.position)
        } * view
        dirty = true
    }

    override fun setup(program: Program) {
        this.program = program
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        view = buildTransform {
            translate(velocity)
        } * view

        velocity *= 0.9
        if (velocity.length < 0.01) {
            velocity = Vector2.ZERO
        }
        drawer.pushTransforms()
        drawer.ortho(RenderTarget.active)
        drawer.view = view
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        dirty = false
        drawer.popTransforms()
    }
}

