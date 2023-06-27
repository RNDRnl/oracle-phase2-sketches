package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG

open class Filter: Animatable() {

    val changed = Event<Unit>()
    var list: List<String> = listOf()
        set(value) {
            field = value.sorted()
        }

    var current: String? = null

    var visible = false
    var expanded = false
        set(value) {
            cancel()
            if (!field && value) expand() else if (field && !value) compress()

            field = value
        }
    var expandT = 0.0

    private fun expand() {
        ::expandT.animate(1.0, 1000, Easing.CubicInOut)
    }

    private fun compress() {
        ::expandT.animate(0.0, 1000, Easing.CubicInOut)
    }

    val boundsWidth = 460.0
    val boundsHeight = 80.0
    var bounds = Rectangle(0.0, 0.0, boundsWidth, boundsHeight)

    var icon = loadSVG("<svg></svg>")
    var title = ""
    var subtitle = ""

    val titleFm = loadFont("data/fonts/Roboto-Regular.ttf", 28.0)
    val subtitleFm = loadFont("data/fonts/Roboto-Regular.ttf", 12.0)

    open var lastPos = Vector2.ZERO

    open fun dragged(e: MouseEvent) {

    }

    open fun buttonUp(e: MouseEvent) {

    }

    fun drawBasics(drawer: Drawer) {

        drawer.stroke = ColorRGBa.RED
        drawer.fill = null

        drawer.translate(bounds.corner)

        drawer.stroke = ColorRGBa.WHITE.opacify(0.4)
        drawer.fill = null
        drawer.lineSegment(Vector2.ZERO, Vector2(bounds.width, 0.0))

        icon.root.transform = buildTransform {
            translate(40.0, 40.0)
            scale(0.5)
            translate(-icon.root.bounds.center)
        }
        drawer.composition(icon)

        drawer.fill = ColorRGBa.WHITE.opacify(0.8)
        drawer.stroke = null
        drawer.fontMap = titleFm
        drawer.text(title, 80.0, 40.0)

        drawer.fontMap = subtitleFm
        drawer.text(subtitle, 80.0, 45.0 + titleFm.height)
/*
        drawer.stroke = ColorRGBa.RED
        drawer.fill = null

        drawer.rectangle(0.0, 0.0, bounds.width, bounds.height)*/
    }

    open fun draw(drawer: Drawer) {}

}