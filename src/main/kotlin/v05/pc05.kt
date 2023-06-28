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
import v05.extensions.IdleDetector
import v05.filters.Sidebar


fun Program.pc05(data: DataModel, state: State) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))
    val sidebar = Sidebar(data, state, drawer.bounds.offsetEdges(-20.0), mouse)

    val idleDetector = extend(IdleDetector())

    idleDetector.idleStarted.listen {
        state.idle = true
    }

    idleDetector.idleEnded.listen {
        state.idle = false
    }


    sidebar.filtersChanged.listen { fe ->
        state.filterSet = fe
        if(sidebar.currentArticle != null) {
            val v = data.articlesToPoints[sidebar.currentArticle]
            v?.let { camera.centerAt(it) }
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
        slider.buttonDown(it)
        camera.buttonDown(it)
        sidebar.buttonDown(it)
        //filter.buttonDown(it)
    }

    mouse.buttonUp.listen {

        slider.buttonUp(it)

        //filter.buttonUp(it)
        if(it.position in sidebar.bounds) {
            sidebar.buttonUp(it)
        } else {
            state.changed.trigger(Unit)
        }
        camera.buttonUp(it)
    }

    mouse.scrolled.listen {
        //camera.scrolled(it)
    }

    mouse.dragged.listen {
        if(sidebar.opened && it.position in sidebar.bounds) {
            sidebar.dragged(it)
        } else {
            camera.dragged(it)
            slider.dragged(it)
        }
    }

    camera.changed.listen {
        slider.current = camera.mappedZoom

        state.apply {
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
                drawer.clear(ColorRGBa.TRANSPARENT)
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
                        val opacity = if(state.filtered[point] != null) 1.0 else 0.2
                        this.stroke = if (state.activePoints[point] != null) ColorRGBa.YELLOW else null

                        val size = if(state.filtered[point] != null) 6 else 2

                        this.fill = article.faculty.facultyColor().opacify(opacity)
                        this.rectangle(Rectangle.fromCenter(point, 0.25 * size, 0.45 * size))
                    }
                }

                drawer.defaults()

                drawer.fontMap = fm
                drawer.fill = ColorRGBa.WHITE
                when(slider.current) {
                    in 0.8..1.0 -> {
                        for((p, a) in state.activePoints) {
                            drawer.text(a.title, p.transform(camera.view))
                        }
                    }
                }

                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(state.lookAt, 40.0)

                slider.draw(drawer)
            }

            layer {
                draw {
                    sidebar.draw(drawer)
                }
            }
            layer {
                draw {

                    drawer.fontMap = titleFm
                    drawer.fill = ColorRGBa.WHITE
                    drawer.text("ORACLE", 25.0, 50.0)
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