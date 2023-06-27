package v05.screens

import org.openrndr.Clock
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.grid
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import v05.Article
import v05.ScreenDrawer
import kotlin.math.cos
import kotlin.math.sin

class IdleMode(val allArticles: List<Article>) : ScreenDrawer{

    var points = emptyList<Vector2>()

    override fun update() {

    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {

        if (points.isEmpty()) {
            points = drawer.bounds.scatter(20.0).take(200)
        }

        val px = cos(clock.seconds*0.01) * 200.0
        val py = sin(clock.seconds*0.01) * 200.0


        val rects = drawer.bounds.offsetEdges(200.0).grid(drawer.width/128, drawer.height/128).flatten()


        drawer.translate(px, py)
        drawer.isolated {
            drawer.stroke = ColorRGBa.WHITE.shade(0.1)
            drawer.fill = null
            drawer.rectangles(rects)
        }
        //drawer.circles(points, 4.0)
        drawer.texts( allArticles.take(200).map { it.title }, points)
    }
}