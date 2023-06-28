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
import v05.DataModel
import v05.facultyNames

open class FilterMenu(articles: List<Article>): Animatable() {

    val changed = Event<Unit>()


    val facultyFilter = FacultyFilter(facultyNames)
    val dateFilter = DateFilter((1900..2023).map { it.toString() })
    open val filters = listOf(facultyFilter, dateFilter)

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
            if(!target.isActive) {
                target.isActive = true
                filters.minus(setOf(target, dateFilter)).onEach { it.isActive = false }
            }
        } else {
            filters.first { it.isActive }.lastPos = e.position
        }
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

    }

    open fun draw(drawer: Drawer) {}

}