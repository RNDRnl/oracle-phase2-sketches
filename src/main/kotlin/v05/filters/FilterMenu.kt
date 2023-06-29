package v05.filters

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
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
import v05.facultyNames
import v05.libs.UIElementImpl

open class FilterMenu: UIElementImpl() {

    inner class Animations: Animatable() {
        var expandT = 0.0

        fun expand() {
            ::expandT.animate(1.0, 1000, Easing.CubicInOut)
        }

        fun compress() {
            ::expandT.animate(0.0, 1000, Easing.CubicInOut)
        }
    }
    val animations = Animations()

    var expanded = false
        set(value) {
            animations.cancel()
            if (!field && value) animations.expand() else if (field && !value) animations.compress()

            field = value
        }


    val boundsWidth = 460.0
    val boundsHeight = 80.0

    var icon = loadSVG("<svg></svg>")
    var title = ""
    var subtitle = ""

    val titleFm = loadFont("data/fonts/Roboto-Regular.ttf", 28.0)
    val subtitleFm = loadFont("data/fonts/Roboto-Regular.ttf", 12.0)
/*
    open fun dragged(e: MouseEvent) {
        e.cancelPropagation()

            if(e.position in dateFilter.headerBox.movedBy(bounds.corner)) {
                e.cancelPropagation()

                val mappedPosition = map(dateFilter.boundingBox.x, dateFilter.boundingBox.x + dateFilter.boundingBox.width, 0.0, 1.0, e.position.x)
                dateFilter.closestSelector?.pos = mappedPosition.coerceIn(0.0, 1.0)
            }

    }

    open fun buttonDown(e: MouseEvent) {
        if(e.position in dateFilter.headerBox.movedBy(bounds.corner)) {
            e.cancelPropagation()

            dateFilter.closestSelector = dateFilter.selectors.minBy { dateFilter.boundingBox.position(it.pos, dateFilter.boundingBox.center.y).distanceTo(e.position)}
        }

    }

    open fun buttonUp(e: MouseEvent) {
        val target = filters.firstOrNull { e.position in it.headerBox.movedBy(bounds.corner) }

        if(target != null) {
            if(!target.isVisible) {
                target.isVisible = true
                filters.minus(setOf(target, dateFilter)).onEach { it.isVisible = false }
            }
        } else {
            filters.minus(dateFilter).firstOrNull { it.isVisible }?.lastPos = e.position
        }
    }*/

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

        drawer.fontMap = subtitleFm
        drawer.text(subtitle, actionBounds.corner + Vector2(80.0,  45.0 + titleFm.height))

    }

    open fun draw(drawer: Drawer) {}

}