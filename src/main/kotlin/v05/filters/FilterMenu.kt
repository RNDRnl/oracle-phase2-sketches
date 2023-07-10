package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.svg.loadSVG
import v05.Article
import v05.State
import v05.facultyNames
import v05.libs.UIElementImpl

open class FilterMenu(val state: State): UIElementImpl() {

    inner class Animations: Animatable() {
        var expandT = 0.0

        fun expand() {
            ::expandT.animate(1.0, 1000, Easing.CubicInOut)
        }

        fun compress(): PropertyAnimationKey<Double> {
            return ::expandT.animate(0.0, 500, Easing.CubicInOut)
        }
    }
    val animations = Animations()

    var active = false
        set(value) {
            field = value
            expanded = value
        }
    var expanded = false
        set(value) {
            animations.cancel()
            if (!field && value) {
                animations.expand()
                field = true
            } else if (field && !value) {
                animations.compress().completed.listen {
                    field = false
                }
            }

        }

    var icon = loadSVG("<svg></svg>")
    var title = ""
    var subtitle = ""

    val titleFm = loadFont("data/fonts/Roboto-Regular.ttf", 28.0)
    val subtitleFm = loadFont("data/fonts/Roboto-Regular.ttf", 13.0)

    fun drawBasics(drawer: Drawer) {

        animations.updateAnimation()

        drawer.stroke = ColorRGBa.RED
        drawer.fill = null

        drawer.stroke = ColorRGBa.WHITE.opacify(0.4)
        drawer.fill = null
        drawer.lineSegment(actionBounds.corner, Vector2(actionBounds.corner.x + actionBounds.width, actionBounds.corner.y))

        icon.root.transform = buildTransform {
            translate(actionBounds.corner + (icon.bounds.dimensions / 4.0))
            scale(0.5)
        }
        drawer.composition(icon)

        drawer.fill = ColorRGBa.WHITE.opacify(0.8)
        drawer.stroke = null
        drawer.fontMap = titleFm

        drawer.text(title, actionBounds.corner + Vector2(80.0, 40.0))


    }

    open fun draw(drawer: Drawer) {}

}