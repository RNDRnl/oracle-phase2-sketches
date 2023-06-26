package v05

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.listen
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blur.GaussianBlur
import org.openrndr.extra.noise.uniform
import org.openrndr.math.*
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import v04.checkNullOr
import v05.filters.FilterMenu


fun Program.pc05(data: DataModel) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))
    val filterMenu = FilterMenu(data, drawer.bounds.offsetEdges(-20.0), mouse)
    val carousel = Carousel(data)

    filterMenu.filterChanged.listen { filters ->
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
        if(slider.visible) {
            val obst = obstacles.firstOrNull { o -> it.position in o.offsetEdges(5.0) }
            camera.inUiElement = obst != null
        }
        // val e = EventObject(it.type, data.activePoints, camera.mappedZoom)
    }

    mouse.buttonDown.listen {
        //filter.buttonDown(it)
    }

    mouse.buttonUp.listen {
        //filter.buttonUp(it)
        filterMenu.buttonUpDown(it)
        data.changed.trigger(Unit)
    }

    mouse.scrolled.listen {
        camera.scrolled(it)
    }

    mouse.dragged.listen {
        camera.dragged(it)
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

    slider.valueChanged.listen {
        camera.setNormalizedScale(it)
    }

    val c = compose {
        layer {
            val poisson = PoissonFill()
            val blur = GaussianBlur().apply {
                sigma = 25.0
                spread = 4.0
                window = 25
            }

            val mul = 2
            val bg = drawImage(width * mul, height * mul) {

                val circles = facultyColors.map {
                    it.shade(0.2) to Circle(Vector2.uniform(drawer.bounds), Double.uniform(80.0, 200.0))
                }

                drawer.stroke = null
                circles.forEach {
                    drawer.fill = it.first
                    drawer.circle(it.second)
                }
                drawer.fill = ColorRGBa.BLACK
                drawer.circle(drawer.bounds.center, 400.0)
            }
            poisson.apply(bg, bg)
            blur.apply(bg, bg)

            draw {
                drawer.translate(-bg.width / (mul * 2.0), -bg.height / (mul * 2.0))
                drawer.image(bg)
            }
        }
        layer {

            val fm = loadFont("data/fonts/Roboto-Regular.ttf", 36.0)
            val titleFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 40.0)

            draw {
                drawer.strokeWeight = 0.05
                drawer.rectangles {
                    for ((point, article) in data.pointsToArticles) {
                        val opacity = if(data.filtered[point] != null) 1.0 else 0.2
                        this.stroke = if (data.activePoints[point] != null) ColorRGBa.YELLOW else null

                        val size = if(data.filtered[point] != null) 6 else 2

                        this.fill = article.faculty.facultyColor().opacify(opacity)
                        this.rectangle(Rectangle.fromCenter(point, 0.25 * size, 0.45 * size))
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

                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(data.lookAt, 40.0)


                drawer.fontMap = titleFm
                drawer.fill = ColorRGBa.WHITE
                drawer.text("ORACLE", 25.0, 50.0)

                slider.draw(drawer)
            }

            layer {
                draw {
                    filterMenu.draw(drawer)
                }
            }
        }
    }

    extend(camera)
    extend {

        c.draw(drawer)

    }

}

inline infix fun <T> T?.checkNullOr(predicate: (T) -> Boolean): Boolean = if (this != null) predicate(this) else true