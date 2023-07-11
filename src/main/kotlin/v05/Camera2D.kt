package v05

import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.events.Event
import org.openrndr.extra.camera.ChangeEvents
import org.openrndr.math.*
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import v05.libs.UIElementImpl
import kotlin.math.*

class Camera2D : Extension, ChangeEvents, UIElementImpl() {
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
        return floor(x * 10000.0) / 10000.0
    }

    fun Matrix44.scale(): Double = (this * Vector4(1.0, 1.0, 0.0, 0.0).normalized).xy.length

    fun toNormalizedScale(scale: Double): Double {
        return quantize(ln(scale).map(ln(minZoom), ln(maxZoom), 0.0, 1.0, clamp = true))
    }

    fun toExpScale(scale: Double): Double {
        val lnScale = scale.map(0.0, 1.0, ln(minZoom), ln(maxZoom))
        return exp(lnScale)
    }

    var panJob: Job? = null
    fun centerAtSlow(targetPosition: Vector2) {
        val w = RenderTarget.active.width
        val h = RenderTarget.active.height
        val currentPosition = (-(view * Vector4.UNIT_W).xy)
        println("current position = ${currentPosition}, target position = ${targetPosition}")
        panJob?.cancel()
        panJob = program.launch {
            for (i in 0 until 120) {
                centerAt(currentPosition.mix(-targetPosition, smoothstep(0.0, 1.0, i / 119.0)))
                yield()
            }
        }
    }

    fun centerAt(targetPosition: Vector2) {
        val w = RenderTarget.active.width
        val h = RenderTarget.active.height
        val currentPosition = ((view * Vector4.UNIT_W).xy)
        view = buildTransform {
            translate((targetPosition - currentPosition))
        } * view

    }

    fun getNormalizedScale(): Double {
        return toNormalizedScale(view.scale())
    }

    fun setNormalizedScaleSlow(targetScaleNormalized: Double) {
        program.launch {
            val cs = getNormalizedScale()
            for (i in 0 until 120) {
                setNormalizedScale(mix(cs, targetScaleNormalized, i / 119.0))
                yield()
            }
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

    init {
        val w = RenderTarget.active.width.toDouble()
        val h = RenderTarget.active.height.toDouble()
        actionBounds = Rectangle(0.0, 0.0, w, h)


        buttonDown.listen {
            it.cancelPropagation()
        }


        dragged.listen {
            view = buildTransform {
                translate(it.dragDisplacement)
            } * view
            dirty = true
        }

        pinched.listen {
            println("scale ${it.scale}, delta ${it.scaleDelta}")
            view = buildTransform {
                translate(it.center)
                scale(1.0 + it.scaleDelta)
                translate(-it.center)
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
        drawer.pushTransforms()
        drawer.ortho(RenderTarget.active)
        drawer.view = view
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        dirty = false
        drawer.popTransforms()
        val currentPosition = ((view.inversed * Vector4.UNIT_W).div.xy)
    }
}

