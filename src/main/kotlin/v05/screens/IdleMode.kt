package v05.screens

import org.openrndr.Clock
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.grid
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import v05.Article
import v05.ScreenDrawer
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

class IdleMode(val allArticles: List<Article>) : ScreenDrawer{

    var points = emptyList<Vector2>()

    override fun update() {

    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {
        if (points.isEmpty()) {
            points = drawer.bounds.scatter(20.0).take(200)
        }

        val px = round(cos(clock.seconds*0.1) * 200.0)
        val py = round(sin(clock.seconds*0.1) * 200.0)

        val rects = drawer.bounds.offsetEdges(drawer.bounds.width/8, drawer.bounds.height/8).grid(drawer.width/128, drawer.height/128).flatten()
        drawer.fontMap = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 32.0)


        drawer.translate(px, py)
        drawer.isolated {
            drawer.stroke = ColorRGBa.WHITE.shade(0.1)
            drawer.fill = null
            drawer.rectangles(rects)
        }
        //drawer.circles(points, 4.0)
        drawer.texts( allArticles.take(200).mapIndexed() { index, it ->
            val t = it.title
            val d = points[index].distanceTo(Vector2(-px+drawer.width/2.0, -py+drawer.height/2.0))
            val s = 1.0 + t.length * smoothstep(400.0, 0.0, d)
            t.take(s.toInt())


         }, points)

    }
}