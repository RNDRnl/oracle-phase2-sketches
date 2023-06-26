package v05.screens

import org.openrndr.draw.Drawer
import org.openrndr.extra.noise.scatter
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import v05.ScreenDrawer

class IdleMode : ScreenDrawer{

    var points = emptyList<Vector2>()

    override fun update() {

    }

    override fun draw(drawer: Drawer, circle: Circle) {
        if (points.isEmpty()) {
            points = drawer.bounds.scatter(20.0).take(200)
        }
        //drawer.circles(points, 4.0)
        drawer.texts(points.map { "Here is the idle mode" },  points)
    }
}