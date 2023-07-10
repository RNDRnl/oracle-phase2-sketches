package v05

import kotlinx.coroutines.yield
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blur.GaussianBlur
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.math.*
import org.openrndr.math.transforms.project
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import v05.extensions.IdleDetector
import v05.filters.*
import v05.libs.UIManager
import v05.libs.watchProperty
import kotlin.random.Random


fun Program.pc05(data: DataModel, state: State) {

    val camera = Camera2D()

    val slider = Slider(Vector2(width / 2.0, height - 60.0))

    val discover = Discover(state)
    val discoverSelector = DiscoverSelector()

    val topGraduates = TopGraduates(state)
    val topGraduatesSelector = TopGraduatesSelector()

    val facultyFilterModel = FacultyFilterModel()
    val facultyFilter = FacultyFilter(drawer, facultyFilterModel)

    val topicFilterModel = TopicFilterModel()
    val topicFilter = TopicFilter(drawer, topicFilterModel)

    val dateFilterModel = DateFilterModel()
    val dateFilter = DateFilter(drawer, dateFilterModel)

    val articleFilter = ArticleFilter(drawer, data.articles)

    state.facultyFilter = facultyFilterModel
    state.topicFilter = topicFilterModel
    state.dateFilter = dateFilterModel



    val labelPoints = data.points.shuffled(Random(0)).take(40)

    val uiManager = UIManager(window, mouse)
    var uiManagerExport: UIManager by userProperties
    uiManagerExport = uiManager

    val uiElements =
        listOf(camera, slider,
            discover, discoverSelector,
            topGraduates, topGraduatesSelector,
            facultyFilter, topicFilter, dateFilter, articleFilter)

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

    facultyFilterModel.filterChanged.listen {
        state.filterChanged()
        // FIXME this should not be necessary, without invoking trigger we end up with a bugged point cloud view
        camera.changed.trigger(Unit)
    }

    topicFilterModel.filterChanged.listen {
        state.filterChanged()
        // FIXME this should not be necessary, without invoking trigger we end up with a bugged point cloud view
        camera.changed.trigger(Unit)
    }

    dateFilterModel.filterChanged.listen {
        state.filterChanged()
        // FIXME this should not be necessary, without invoking trigger we end up with a bugged point cloud view
        camera.changed.trigger(Unit)
    }

    articleFilter.articleSelected.listen {
        if (articleFilter.currentArticle != null) {
            val pos = data.articlesToPoints[articleFilter.currentArticle]
            if (pos != null) {
                discoverSelector.current = 1
                camera.centerAt(pos)
            }
        }
    }

    watchProperty(discover::expanded).listen {
        if (!it) {
            state.filterSet = FilterSet.EMPTY
        } else {
            state.filterChanged()
        }
    }

    watchProperty(discover::active).listen {
        if(it) {
            topGraduates.active = false
            topGraduates.expanded = false
        }
        listOf(facultyFilter, topicFilter, dateFilter, articleFilter).map { f -> f.visible = it }
    }

    watchProperty(topGraduates::active).listen {
        if(it) {
            discover.active = false
        }
        discover.expanded = it
    }

    state.changed.listen {
        if (articleFilter.visible) {
            articleFilter.articles = state.filtered.values.toList()
        }
    }

    camera.changed.listen {
        slider.current = camera.mappedZoom

        state.apply {
            zoom = camera.mappedZoom
            radius = 40.0 / camera.view.c0r0
            lookAt = (camera.view.inversed * drawer.bounds.center.xy01).xy
            state.changed.trigger(Unit)
        }

    }

    slider.valueChanged.listen {
        camera.setNormalizedScale(it)
    }

    val pointCloudShadeStyle = shadeStyle {
        fragmentTransform = """float dx = cos(v_worldPosition.x*10.0)*2.0+2.0; 
            float dy = cos(v_worldPosition.y*10.0)*2.0+2.0;
float c = 0.5 + 0.5 * cos(va_texCoord0.y * 3.1415 * dx + dy + p_time) * sin(va_texCoord0.x * 3.1415 * dy + dx + p_time);
x_fill.rgb *= c;                             
        """
    }



    val c = compose {
        layer {// Background
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
        layer {// Point cloud

            val fm = loadFont("data/fonts/Roboto-Regular.ttf", 36.0)
            val fms = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 24.0)

            draw {
                drawer.strokeWeight = 0.2
                drawer.isolated {
                    pointCloudShadeStyle.parameter("time", seconds)
                    drawer.shadeStyle = pointCloudShadeStyle
                    drawer.rectangles {
                        for ((point, article) in data.pointsToArticles) {
                            val opacity = if (state.filtered[point] != null) 1.0 else 0.2
                            this.stroke = if (state.activePoints[point] != null) article.faculty.facultyColor().mix(
                                ColorRGBa.WHITE, 0.75
                            ) else null

                            val size = if (state.filtered[point] != null) 6 else 2

                            this.fill = article.faculty.facultyColor().opacify(opacity)
                            this.rectangle(Rectangle.fromCenter(point, 0.25 * size, 0.45 * size))
                        }
                    }
                }
                //drawer.circles(labelPoints, 3.0)

                val filteredPoints = labelPoints.filter { it in state.filtered.keys }

                val projectedPoints = filteredPoints.map { project(it.xy0, drawer.projection, drawer.view*drawer.model, drawer.width, drawer.height).xy }
                drawer.defaults()
                val labelTexts = filteredPoints.map { data.pointsToArticles[it]!!.topic }
                drawer.fontMap = fms
                drawer.texts(labelTexts, projectedPoints)

                drawer.fontMap = fm
                drawer.fill = ColorRGBa.WHITE
                when (slider.current) {
                    in 0.8..1.0 -> {
                        for ((p, a) in state.activePoints) {
                            drawer.text(a.title, p.transform(camera.view))
                        }
                    }
                }
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.circle(state.lookAt, 40.0)

            }
        }
        layer {
            draw { // UI elements

                drawer.isolated {
                    drawer.defaults()

                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE.opacify(discover.animations.expandT)
                    drawer.shadeStyle = linearGradient(ColorRGBa.BLACK, ColorRGBa.TRANSPARENT, rotation = -90.0)
                    drawer.rectangle(
                        0.0,
                        0.0,
                        discover.actionBounds.width + (discover.actionBounds.width * discover.animations.expandT),
                        height * 1.0
                    )
                    drawer.shadeStyle = null

                    discover.let {
                        drawer.drawStyle.clip = if (!it.expanded) it.actionBounds else it.actionBounds.copy(width = it.actionBounds.width * 2.0)

                        it.draw(drawer)

                        if(it.expanded && it.active) {

                            discoverSelector.apply {
                                visible = it.expanded
                                if (it.expanded) draw(drawer, it.actionBounds)
                            }

                            facultyFilter.apply {
                                visible = it.expanded
                                actionBounds = Rectangle(
                                    it.actionBounds.x + (50.0 * (1.0 - animations.slider)),
                                    it.actionBounds.y + 140.0,
                                    it.actionBounds.width - 50.0,
                                    it.actionBounds.height - 180.0 - 150.0
                                )
                                isMinimized = discoverSelector.current == 1 || discoverSelector.current == 2
                                draw()
                            }

                            topicFilter.apply {
                                visible = it.expanded && (discoverSelector.current == 1 || discoverSelector.current == 2)
                                actionBounds = Rectangle(
                                    it.actionBounds.x + 50.0,
                                    it.actionBounds.y + 140.0,
                                    it.actionBounds.width - 50.0,
                                    facultyFilter.actionBounds.height + 60.0
                                )
                                draw()
                            }

                            dateFilter.apply {
                                visible = it.expanded
                                actionBounds = Rectangle(
                                    topicFilter.actionBounds.x,
                                    it.actionBounds.y + 730.0,
                                    facultyFilter.actionBounds.width,
                                    110.0
                                )
                                draw()
                            }

                            articleFilter.apply {
                                visible = it.expanded && discoverSelector.current == 2
                                actionBounds =
                                    topicFilter.actionBounds.movedBy(Vector2(facultyFilter.actionBounds.width, 0.0))
                                draw()
                            }

                        }

                        drawer.drawStyle.clip = null
                    }

                    topGraduates.let {

                        it.draw(drawer)

                        topGraduatesSelector.apply {
                            visible = it.expanded
                            if (it.expanded) draw(drawer, it.actionBounds)
                        }

                    }



                    slider.draw(drawer)
                }

            }
        }
        layer {// Decoration

            val titleFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 40.0)

            draw {

                drawer.isolated {
                    drawer.defaults()

                    drawer.fontMap = titleFm
                    drawer.fill = ColorRGBa.WHITE
                    drawer.text("ORACLE", 25.0, 50.0)
                }
            }
        }
    }
    launch {
        while (true) {
            uiManager.update()
            yield()
        }
    }


    window.presentationMode = PresentationMode.MANUAL

    extend(camera)
    extend {

        c.draw(drawer)
      //  uiManager.drawDebugBoxes(drawer)

    }

}

inline infix fun <T> T?.checkNullOr(predicate: (T) -> Boolean): Boolean = if (this != null) predicate(this) else true