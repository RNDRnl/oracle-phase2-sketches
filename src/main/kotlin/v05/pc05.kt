package v05

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
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
import v05.filters.*
import v05.libs.UIManager


fun Program.pc05(data: DataModel, state: State) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))
    val discover = Discover()
    val selectorBoxes = SelectorBoxes()

    val facultyFilterModel = FacultyFilterModel()
    val facultyFilter = FacultyFilter(drawer, facultyFilterModel)

    val topicFilterModel = TopicFilterModel()
    val topicFilter = TopicFilter(drawer, topicFilterModel).apply { visible = false }

    val dateFilterModel = DateFilterModel()
    val dateFilter = DateFilter(drawer, dateFilterModel).apply { visible = false }

    val articleFilter = ArticleFilter(drawer, data.articles)

    state.facultyFilter = facultyFilterModel
    state.topicFilter = topicFilterModel
    state.dateFilter = dateFilterModel

    val uiManager = UIManager(mouse)
    val uiElements = listOf(camera, slider, discover, selectorBoxes, facultyFilter, topicFilter, dateFilter, articleFilter)

    uiElements.forEach {
        uiManager.elements.add(it)
    }

    val idleDetector = extend(IdleDetector())

    idleDetector.idleStarted.listen {
        state.idle = true
    }

    idleDetector.idleEnded.listen {
        state.idle = false
    }


/*    sidebar.filtersChanged.listen { fe ->
        state.filterSet = fe
        if(sidebar.currentArticle != null) {
            val v = data.articlesToPoints[sidebar.currentArticle]
            v?.let { camera.centerAt(it) }
        }
    }*/

    facultyFilterModel.filterChanged.listen {
        state.filterChanged()
    }

    topicFilterModel.filterChanged.listen {
        state.filterChanged()
    }

    dateFilterModel.filterChanged.listen {
        state.filterChanged()
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

            }

            layer {
                draw {

                    val filters = listOf(facultyFilter, topicFilter, dateFilter, articleFilter)

                    discover.draw(drawer)

                    facultyFilter.actionBounds = Rectangle(discover.actionBounds.x + 50.0, discover.actionBounds.y + 140.0, discover.actionBounds.width - 50.0, discover.actionBounds.height - 180.0 - 150.0 )
                    topicFilter.actionBounds = facultyFilter.actionBounds
                    dateFilter.actionBounds = Rectangle(facultyFilter.actionBounds.x, facultyFilter.actionBounds.y + facultyFilter.actionBounds.height, facultyFilter.actionBounds.width, 150.0)
                    articleFilter.actionBounds = facultyFilter.actionBounds.movedBy(Vector2(facultyFilter.actionBounds.width, 0.0))

                    selectorBoxes.draw(drawer, discover.actionBounds)

                    when(selectorBoxes.current) {
                        0 -> {
                            facultyFilter.isMinimized = false
                            facultyFilter.visible = true
                            topicFilter.visible = false
                            articleFilter.visible = false
                        }
                        1 -> {
                            facultyFilter.visible = true
                            facultyFilter.isMinimized = true
                            topicFilter.visible = true
                            articleFilter.visible = false
                        }
                        2 -> {
                            facultyFilter.isMinimized = true
                            facultyFilter.visible = true
                            topicFilter.visible = true
                            articleFilter.visible = true
                        }
                    }

                    filters.forEachIndexed { i, it ->
                        it.draw()
                    }

                    slider.draw(drawer)
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
       // uiManager.drawDebugBoxes(drawer)

    }

}

inline infix fun <T> T?.checkNullOr(predicate: (T) -> Boolean): Boolean = if (this != null) predicate(this) else true