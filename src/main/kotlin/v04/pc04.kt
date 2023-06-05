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
import org.openrndr.extra.noise.uniform
import org.openrndr.math.*
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import org.openrndr.shape.bounds
import v02.transform

fun Program.pc04(data: DataModelNew) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))
    val filter = FilterSearch(data, drawer.bounds.offsetEdges(-20.0), mouse)
    val carousel = Carousel(data)

    filter.filterChanged.listen { filters ->
        if(filters.all { it == null }) {
            data.filtered = data.pointsToArticles
        } else {
            data.filtered = data.pointsToArticles.filter { a ->
                filters[0] checkNullOr { a.value.faculty == filters[0] } &&
                        filters[1] checkNullOr { a.value.topic == filters[1] } &&
                        filters[2] checkNullOr { a.value.title + " | " + a.value.author == filters[2] }
            }
        }

    }

    val obstacles = listOf(slider.bounds)

    listOf(mouse.buttonDown, mouse.buttonUp).listen {
        if(slider.visible && !filter.visible) {
            val obst = obstacles.firstOrNull { o -> it.position in o.offsetEdges(5.0) }
            camera.inUiElement = obst != null
        }
        // val e = EventObject(it.type, data.activePoints, camera.mappedZoom)
    }

    mouse.buttonDown.listen {
        filter.buttonDown(it)
    }

    mouse.buttonUp.listen {
        filter.buttonUp(it)
    }

    mouse.scrolled.listen {
        if(!filter.visible) {
            camera.scrolled(it)
        }
    }

    mouse.dragged.listen {
        if(!filter.visible) {
            camera.dragged(it)
        } else {
            filter.dragged(it)
        }
        slider.dragged(it, mouse)
    }

    camera.changed.listen {
        slider.current = camera.mappedZoom

        data.apply {
            zoom = camera.mappedZoom
            radius = 40.0 / camera.view.c0r0
            lookAt = (camera.view.inversed * drawer.bounds.center.xy01).xy
        }
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

            val fm = loadFont("data/fonts/Roboto-Regular.ttf", 36.0)
            val titleFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 40.0)

            draw {
                val tr = Vector2(filter.timeline.pcx * filter.visibleSlider * width, filter.timeline.pcy * filter.visibleSlider * width)

                drawer.translate(data.points.bounds.center + tr)
                drawer.scale(1.0 - (filter.timeline.pcscale * filter.visibleSlider))
                drawer.translate(-data.points.bounds.center)

                drawer.strokeWeight = 0.05
                drawer.rectangles {
                    for ((point, article) in data.pointsToArticles) {
                        val opacity = if(data.filtered[point] != null) 1.0 else 0.2
                        this.stroke = if (data.activePoints[point] != null && !filter.visible) ColorRGBa.YELLOW else null

                        val size = if(filter.visible && filter.timelineSlider > 1.0 && data.filtered[point] != null) 6 else 2

                        this.fill = article.faculty.facultyColor().opacify(opacity)
                        this.rectangle(Rectangle.fromCenter(point, 0.65 * size, 1.0 * size))
                    }
                }

                if(filter.visible) {
                    if(filter.timelineSlider in 0.0..2.99) {
                        carousel.draw(drawer)
                    } else if(filter.timelineSlider in 3.0..3.99) {
                        drawer.fill = null
                        drawer.stroke = ColorRGBa.WHITE
                        drawer.contours(data.fakeTriangulation.map { it.first })

                        for((c, text) in data.fakeTriangulation) {
                            val points = c.segments.map { it.start }
                            val triangle = Triangle(points[0],points[1],points[2])

                            if(filter.topicsBox.current == text) {
                                drawer.fill = null
                                drawer.stroke = filter.facultyBox.currentFaculty.facultyColor()
                                drawer.strokeWeight = 3.0
                                drawer.contour(triangle.contour)
                            }

                            drawer.fill = ColorRGBa.WHITE
                            drawer.stroke = null
                            drawer.text(text,triangle.centroid - Vector2(fm.textWidth(text) / 2.0, 0.0))
                        }
                    } else {
                        drawer.stroke = null
                        drawer.fill = ColorRGBa.WHITE
                        for((p, a) in data.filtered) {
                            drawer.circle(p, 20.0)
                        }
                    }

                }


                drawer.defaults()

                drawer.fontMap = fm
                drawer.fill = ColorRGBa.WHITE
                when(slider.current) {
                    in 0.8..1.0 -> {
                        for((p, a) in data.activePoints) {
                            drawer.text(a.title, p.transform(camera.view))
                        }
                    }
                }

                if(!filter.visible) {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.circle(data.lookAt, 40.0)
                }


                drawer.fontMap = titleFm
                drawer.fill = ColorRGBa.WHITE
                drawer.text("ORACLE", 25.0, 50.0)

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

inline infix fun <T> T?.checkNullOr(predicate: (T) -> Boolean): Boolean = if (this != null) predicate(this) else true