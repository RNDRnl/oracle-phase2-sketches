package v05

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import v05.libs.UIElement
import v05.libs.UIElementImpl
import v05.model.SelectedDataModel
import kotlin.math.atan2

class ViewfinderFinder: UIElementImpl() {

    val returnEvent = Event<Unit>()
    init {
        actionBounds = Rectangle(0.0, 0.0, 50.0, 50.0)
        buttonDown.listen {
            it.cancelPropagation()
        }

        buttonUp.listen {
            returnEvent.trigger(Unit)
        }
    }

    fun draw(drawer: Drawer, view: Matrix44, newPos: Vector2) {
        drawer.isolated {
            drawer.defaults()
            val p1 = newPos.transform(view)

            if(!drawer.bounds.offsetEdges(-30.0).contains(p1)) {
                val p0 = drawer.bounds.center
                val c = LineSegment(p0, p1).contour

                val p = c.intersections(drawer.bounds.offsetEdges(-40.0).contour)

                if (p.isNotEmpty()) {
                    val pos = p.first().position
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.fill = ColorRGBa.WHITE.opacify(0.3)
                    val circle = Circle(pos, 25.0)
                    drawer.circle(circle)

                    actionBounds = circle.contour.bounds

                    drawer.fill = ColorRGBa.WHITE
                    val angle = Math.toDegrees(atan2(pos.y - p0.y, pos.x - p0.x))
                    drawer.contour(regularPolygon(3, pos, 10.0, angle))
                }
            }

        }
    }
}
class Viewfinder(val state: State, val selectedModel: SelectedDataModel): Animatable() {

    val finder = ViewfinderFinder()

    private var radius = 100.0
    private var oldRadius = radius

    private var facultyQuadrant: List<Pair<String, List<Article>>> = listOf()
                set(value) {
                    field = value.sortedBy { it.first.first() }
                }
    private var oldFacultyQuadrant = facultyQuadrant
    private var pos = state.lookAt
        set(value) {
            field = value
            cancel()
            ::posFader.animate(1.0, 800, Easing.SineInOut).completed.listen {
                oldPos = value
                oldRadius = radius
                oldFacultyQuadrant = facultyQuadrant
                posFader = 0.0
            }
        }
    private var oldPos = pos

    var posFader = 0.0
    var fader = 0.0

    fun fadeIn() {
        cancel()
        ::fader.animate(1.0, 750, Easing.CubicInOut)
    }

    fun fadeOut() {
        cancel()
        ::fader.animate(0.0, 350, Easing.CubicInOut)
    }


    fun positionChanged() {
        radius = state.lookAt.distanceTo(state.furthest!!)
        facultyQuadrant = selectedModel.groupedByFaculty.toList()

        pos = state.lookAt
    }

    var zoom = 0.0
        set(value) {
            if (field != value ) {
                if(value in CLOSER && fader == 0.0) {
                    fadeIn()
                } else if((value in CLOSEST || value in FURTHEST) && fader == 1.0) {
                    fadeOut()
                }
            }


            field = value
        }


    fun draw(z: Double, drawer: Drawer) {
        updateAnimation()

        drawer.isolated {
            //drawer.defaults()
            zoom = z

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(fader)
            drawer.strokeWeight = 1.0 /  view.scale()

            val newPos = oldPos.mix(pos, posFader)

            state.furthest?.let {
                val c = Circle(newPos, radius * posFader + oldRadius * (1.0 - posFader))
                drawer.circle(c)

                val facultiesQuadrant = c.copy(radius = c.radius + 3.0).contour.sub(0.2, 0.6)
                drawer.contour(facultiesQuadrant)

                drawer.strokeWeight= 2.0
                drawer.lineCap = LineCap.SQUARE

                var oldAcc = 0.0
                val oldIntervals = oldFacultyQuadrant.map { (_, articles) ->
                    val new = articles.size.toDouble() / selectedModel.articles.size
                    oldAcc += new
                    oldAcc
                }

                var newAcc = 0.0
                val newIntervals = facultyQuadrant.map { (_, articles) ->
                    val new = articles.size.toDouble() / selectedModel.articles.size
                    newAcc += new
                    newAcc
                }

                val intervals = (oldIntervals zip newIntervals).map {
                    it.first * (1.0 - posFader) + it.second * posFader
                }

                intervals.foldIndexed(0.0) { i, acc, new ->
                    val fac = facultyQuadrant[i].first
                    drawer.stroke = fac.facultyColor()
                    drawer.contour(facultiesQuadrant.sub(acc, acc + new))
                    acc + new
                }

                //val
            }

            val offset =  30.0 * (1.0 - fader) / view.scale()
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 1.5 / view.scale()
            drawer.lineSegment(LineSegment(newPos.x, newPos.y - offset, newPos.x, newPos.y + offset))
            drawer.lineSegment(LineSegment(newPos.x - offset, newPos.y, newPos.x + offset, newPos.y))

            val camview = view

            finder.draw(drawer, camview, newPos)
        }
    }

}