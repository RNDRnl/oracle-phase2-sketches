package v05.filters

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Composition
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG

open class Filter: Animatable() {

    val filterChanged = Event<List<String?>>()
    var expanded = false
        set(value) {
            cancel()
            if (!field && value) expand() else compress()

            field = value
        }
    var expandT = 0.0

    var bounds = Rectangle(0.0, 0.0, 360.0, 80.0)

    var icon = loadSVG("<svg></svg>")
    var title = ""
        set(value) {
            field = value.uppercase()
        }
    var subtitle = ""
        set(value) {
            field = value.uppercase()
        }

    val titleFm = loadFont("data/fonts/Roboto-Regular.ttf", 28.0)
    val subtitleFm = loadFont("data/fonts/Roboto-Regular.ttf", 12.0)

    private fun expand() {
        ::expandT.animate(1.0, 3000, Easing.CubicInOut)
    }

    private fun compress() {
        ::expandT.animate(0.0, 3000, Easing.CubicInOut)
    }

    fun drawBasics(drawer: Drawer) {

        drawer.stroke = ColorRGBa.WHITE.opacify(0.4)
        drawer.fill = null
        drawer.lineSegment(bounds.corner, Vector2(bounds.x + bounds.width, bounds.y))

        icon.root.transform = buildTransform {
            translate(50.0, bounds.height / 2.0)
            scale(0.75)
            translate(-icon.root.bounds.center)
        }
        drawer.composition(icon)

        drawer.fill = ColorRGBa.WHITE.opacify(0.8)
        drawer.stroke = null
        drawer.fontMap = titleFm
        drawer.text(title, 110.0, 40.0)

        drawer.fontMap = subtitleFm
        drawer.text(subtitle, 110.0, 45.0 + titleFm.height)
    }

    open fun draw(drawer: Drawer) {}

}