package v04

import classes.*
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.events.listen
import org.openrndr.extra.camera.ParametricOrbital
import org.openrndr.extra.hashgrid.filter
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.*
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import org.openrndr.shape.contains
import v02.transform
import java.lang.StringBuilder
import kotlin.math.sign
import kotlin.math.sin

fun Program.pointCloud04(data: DataModelNew) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))

    /*val searchBar = SearchBar(keyboard, Rectangle(10.0, 10.0, width*1.0, 128.0))

    searchBar.queryChanged.listen { s ->
        data.changed.trigger(Unit)
        data.filtered = data.pointsToArticles.filter { it.value.title.contains(s) }
    }*/

    val obstacles = listOf(slider.bounds)

    listOf(mouse.buttonDown, mouse.buttonUp).listen {
        val obst = obstacles.firstOrNull { o -> it.position in o.offsetEdges(5.0) }
        camera.inUiElement = obst != null
        // val e = EventObject(it.type, data.activePoints, camera.mappedZoom)
    }

    mouse.dragged.listen {
        if(it.position in slider.bounds.offsetEdges(65.0, 0.0)) {
            val old = slider.current

            slider.current = map(
                slider.bounds.x,
                slider.bounds.x + slider.bounds.width,
                0.0,
                1.1,
                it.position.x.coerceIn(slider.bounds.x, slider.bounds.x + slider.bounds.width))

            mouse.scrolled.trigger(
                MouseEvent(drawer.bounds.center,Vector2.UNIT_Y * (old - slider.current), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, setOf())
            )
        }
    }

    camera.changed.listen {
        slider.current = camera.mappedZoom

        data.radius = 40.0 / camera.view.c0r0
        data.lookAt = (camera.view.inversed * drawer.bounds.center.xy01).xy
    }


    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)

    extend(camera)
    extend {

        drawer.strokeWeight = 0.05
        drawer.rectangles {
            for ((point, article) in data.pointsToArticles) {
                val opacity = if(data.filtered[point] != null) 1.0 else 0.0
                this.stroke = if (data.activePoints[point] != null) ColorRGBa.YELLOW else null

                this.fill = article.faculty.color.opacify(opacity)
                this.rectangle(Rectangle.fromCenter(point, 0.65 * 2, 1.0 * 2))
            }
        }

        drawer.defaults()

        drawer.fontMap = fm
        drawer.fill = ColorRGBa.WHITE
        when(slider.current) {
            in 0.8..1.0 -> {
                for((p, a) in data.activePoints) {
                    drawer.text(a.topic, p.transform(camera.view))
                }
            }
        }

        slider.draw(drawer)

        //searchBar.draw(drawer)

        drawer.fill = null
        drawer.stroke = ColorRGBa.WHITE
        drawer.circle(data.lookAt, 40.0)

        drawer.fill = null
        drawer.stroke = ColorRGBa.YELLOW
        drawer.contours(data.fakeTriangulation.map { it.first.transform(camera.view) })

        drawer.view = camera.view
        drawer.fill = ColorRGBa.YELLOW
        drawer.stroke = null
        for((c, text) in data.fakeTriangulation) {
            val points = c.segments.map { it.start }
            val triangle = Triangle(points[0],points[1],points[2])
            drawer.text(text,triangle.centroid - Vector2(fm.textWidth(text) / 2.0, 0.0))
        }

    }

}

fun main() = application {
    configure {
        width = 900
        height = 900
    }
    program {

        val data = DataModelNew(drawer.bounds)
        val pc = viewBox(drawer.bounds) { pointCloud04(data) }

        extend {
            pc.draw()
        }
    }
}