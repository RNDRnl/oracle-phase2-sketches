package v04

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.listen
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.GaussianBlur
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.*
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import v02.transform

fun Program.pointCloud04(data: DataModelNew) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))
    val filter = FilterSearch(data, drawer.bounds.offsetEdges(-20.0), mouse)

    val obstacles = listOf(slider.bounds)

    listOf(mouse.buttonDown, mouse.buttonUp).listen {
        if(slider.visible) {
            val obst = obstacles.firstOrNull { o -> it.position in o.offsetEdges(5.0) }
            camera.inUiElement = obst != null
        }
        // val e = EventObject(it.type, data.activePoints, camera.mappedZoom)
    }

    mouse.dragged.listen {
        if(slider.visible && it.position in slider.bounds.offsetEdges(65.0, 0.0)) {
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

    val g = GUI()

    val c = compose {
        layer {
            val circles = facultyColors.map {
               it.shade(0.2) to Circle(Vector2.uniform(drawer.bounds), Double.uniform(80.0, 200.0))
            }

            draw {
                drawer.stroke = null
                circles.forEach {
                    drawer.fill = it.first
                    drawer.circle(it.second)
                }
                drawer.fill = ColorRGBa.BLACK
                drawer.circle(drawer.bounds.center, 400.0)
            }
            post(PoissonFill())
            post(GaussianBlur()) {
                sigma = 25.0
                spread = 4.0
                window = 25
            }
        }
        layer {

            val fm = loadFont("data/fonts/Roboto-Regular.ttf", 18.0)
            val titleFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 40.0)

            draw {

                drawer.translate(filter.timeline.pcx * filter.visibleSlider * width, 0.0)
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

                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(data.lookAt, 40.0)

                drawer.defaults()
                drawer.fontMap = titleFm
                drawer.fill = ColorRGBa.WHITE
                drawer.text("ORACLE", 25.0, 50.0)

               /* drawer.fill = null
                drawer.stroke = ColorRGBa.YELLOW
                drawer.contours(data.fakeTriangulation.map { it.first.transform(camera.view) })

                drawer.view = camera.view
                drawer.fill = ColorRGBa.YELLOW
                drawer.stroke = null
                for((c, text) in data.fakeTriangulation) {
                    val points = c.segments.map { it.start }
                    val triangle = Triangle(points[0],points[1],points[2])
                    drawer.text(text,triangle.centroid - Vector2(fm.textWidth(text) / 2.0, 0.0))
                }*/

                slider.draw(drawer)
            }
            if(filter.visible) {
                post(GaussianBlur()) {
                    this.sigma = 5.0
                    this.spread = 20.0
                    this.window = 12
                }
            }

        }
        layer {
            draw {
                filter.draw(drawer)
            }
        }
    }


    extend(camera) {
        enabled = !filter.visible
    }
    //extend(g)
    extend {
        slider.visible = !filter.visible
        c.draw(drawer)
        //searchBar.draw(drawer)

    }

}

fun main() = application {
    configure {
        width = 1920
        height = 1080
        position = IntVector2(0, -1200)
    }
    program {

        val data = DataModelNew(Rectangle.fromCenter(drawer.bounds.center, height * 1.0, height * 1.0))
        val pc = viewBox(drawer.bounds) { pointCloud04(data) }


        extend {
            pc.draw()

        }
    }
}