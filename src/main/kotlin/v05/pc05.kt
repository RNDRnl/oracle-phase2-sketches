package v05

import kotlinx.coroutines.yield
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blur.GaussianBlur
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.math.*
import org.openrndr.math.transforms.project
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import v05.extensions.IdleDetector
import v05.extensions.PinchDetector
import v05.filters.*
import v05.fx.GradientFilter
import v05.fx.LocalMaximaFilter
import v05.libs.UIManager
import v05.libs.equidistantFlowlines
import v05.libs.watchProperty
import v05.views.PointCloud
import kotlin.math.atan2
import kotlin.random.Random


fun Program.pc05(data: DataModel, state: State) {

    val camera = Camera2D()

    val pointCloudView = PointCloud(drawer, this, state, data)

    val viewfinder = Viewfinder(state, camera)

    val discover = Discover(state)
    val discoverSelector = DiscoverSelector()

    val showcases = Showcases(state)

    val facultyFilterModel = FacultyFilterModel()
    val facultyFilter = FacultyFilter(facultyFilterModel)

    val topicFilterModel = TopicFilterModel()
    val topicFilter = TopicFilter(topicFilterModel)

    val dateFilterModel = DateFilterModel()
    val dateFilter = DateFilter(dateFilterModel)

    val articleFilter = ArticleFilter(data.articles)

    state.facultyFilter = facultyFilterModel
    state.topicFilter = topicFilterModel
    state.dateFilter = dateFilterModel


    val pinchDetector = extend(PinchDetector())
    mouse.dragged.listen {
        if (pinchDetector.pinching) {
            it.cancelPropagation()
        }
    }

    mouse.scrolled.listen {
        if (pinchDetector.pinching) {
            it.cancelPropagation()
        }
    }

    val uiManager = UIManager(window, mouse, pinchDetector.pinchChanged)
    var uiManagerExport: UIManager by userProperties
    uiManagerExport = uiManager

    val uiElements =
        listOf(camera,
            showcases, showcases.filter,
            discover, discoverSelector,
            facultyFilter, topicFilter, dateFilter, articleFilter
        )

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

//    watchProperty(articleFilter::visible).listen {
//        if (it) {
//            println("here we are now")
//            camera.setNormalizedScaleSlow(1.0)
//        } else {
//            println("entartain us")
//            camera.setNormalizedScaleSlow(0.0)
//        }
//    }

    articleFilter.articleSelected.listen {
        if (articleFilter.currentArticle != null) {
            val pos = data.articlesToPoints[articleFilter.currentArticle]
            if (pos != null) {
                discoverSelector.current = 1
                camera.centerAtSlow(pos)
            }
        }
    }

    showcases.showcaseSelected.listen {
        //showcases don't provide a filterset
        if(showcases.current != null) {
            state.filtered = data.pointsToArticles.filter {
                it.value in showcases.current!!.articles
            }
        } else {
            state.filtered = data.pointsToArticles
        }
        state.changed.trigger(Unit)
    }

    showcases.filter.articleSelected.listen {
        if (showcases.filter.currentArticle != null) {
            val pos = data.articlesToPoints[showcases.filter.currentArticle]
            if (pos != null) {
                camera.centerAtSlow(pos)
            } else {
                println()
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
            showcases.active = false
            showcases.expanded = false
        }
        listOf(facultyFilter, topicFilter, dateFilter, articleFilter).map { f -> f.visible = it }
    }

    watchProperty(showcases::active).listen {
        if (it) {
            showcases.current = null
            discover.active = false
        }
        discover.expanded = it
    }

    state.changed.listen {
        if (articleFilter.visible) {
            articleFilter.articles = state.filtered.values.toList()
        }
    }


    fun updateState() {
        state.zoom = camera.mappedZoom
        state.changed.trigger(Unit)
    }

    camera.clicked.listen {
        if(!pinchDetector.pinching) {
            state.lookAt = (camera.view.inversed * it.position.xy01).xy
            updateState()
            viewfinder.moveTo(state.lookAt)
        }
     /*   if (state.zoom in CLOSER && pointCloudView.contains(it.position.transform(camera.view))) {

            //pointCloudView.moveTo(it.position)
        }*/
    }

    camera.changed.listen {
        updateState()
    }

    val pointCloudDensity = drawImage(width, height, type = ColorType.FLOAT32) {
        drawer.clear(ColorRGBa.BLACK)
        drawer.drawStyle.blendMode = BlendMode.ADD

        drawer.shadeStyle = shadeStyle {
            fragmentTransform = """float d = length(2.0 * (va_texCoord0.xy - vec2(0.5) ));
                float ed = smoothstep(1.0, 0.0, d);
                 x_fill.rgb *= ed;
            """.trimMargin()
        }
        drawer.stroke = null
        drawer.fill = ColorRGBa.WHITE
        val size = 160.0
        drawer.rectangles {
            for (i in data.points.indices) {
                rectangle(data.points[i] - Vector2(size / 2.0, size / 2.0), size, size)
            }
        }
    }
    val gradientFilter = GradientFilter()
    val pointCloudGradient = pointCloudDensity.createEquivalent()
    gradientFilter.apply(pointCloudDensity, pointCloudGradient)

    val pcds = pointCloudDensity.shadow
    pcds.download()

    for (i in data.points.indices) {
        val ix = data.points[i].x.toInt().coerceIn(0, width - 1)
        val iy = data.points[i].y.toInt().coerceIn(0, height - 1)
        val c = pcds[ix, iy].r
        println(c)
        data.densities[i] = c
    }
    val pcgs = pointCloudGradient.shadow
    pcgs.download()
    for (i in data.points.indices) {
        val ix = data.points[i].x.toInt().coerceIn(0, width - 1)
        val iy = data.points[i].y.toInt().coerceIn(0, height - 1)
        val c = pcgs[ix, iy].toVector4().xy
        val r = atan2(c.x, c.y)
        data.rotations[i] = r.asDegrees


    }
    pcgs.destroy()

    val pointCloudLocalMaxima = pointCloudGradient.createEquivalent(type = ColorType.UINT8)
    val localMaximaFilter = LocalMaximaFilter()
    localMaximaFilter.apply(pointCloudDensity, pointCloudLocalMaxima)

    val pclms = pointCloudLocalMaxima.shadow
    pclms.download()
    val labelPointsCandidates = mutableListOf<Vector2>()
    for (y in 0 until pointCloudLocalMaxima.height) {
        for (x in 0 until pointCloudLocalMaxima.width) {
            val c = pclms[x, y].r
            if (c > 0.5) {
                labelPointsCandidates.add(Vector2(x.toDouble(), y.toDouble()))
            }
        }
    }
    pclms.destroy()
    pointCloudLocalMaxima.destroy()

    pointCloudGradient.shadow.download()
    //val lines = equidistantFlowlines(pointCloudGradient, drawer.bounds.center, random = Random(0))
    val flowLines = equidistantFlowlines(pointCloudGradient, drawer.bounds.center, distance0 = 20.0, distance1 = 30.0, random = Random(0))

    val labelPoints = labelPointsCandidates.map { state.kdtree.findNearest(it)!! }

    val c = compose {
        layer {// Background
            val poisson = PoissonFill()
            val blur = GaussianBlur().apply {
                sigma = 25.0
                spread = 4.0
                window = 25
            }

            val mul = 2
            val bg = drawImage(width * mul, height * mul, type = ColorType.FLOAT32) {
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.stroke = null
                drawer.points {
                    for (i in data.articles.indices) {
                        fill = data.articles[i].faculty.facultyColor().shade(0.2)
                        point(data.points[i] + Vector2(width / 4.0, height / 4.0))
                    }
                }
                drawer.points {
                    fill = ColorRGBa.BLACK
                    for (x in 0 until width) {
                        this.point(x.toDouble(), 1.0)
                        this.point(x.toDouble(), height - 2.0)
                    }
                    for (y in 0 until height) {
                        this.point(1.0, y.toDouble())
                        this.point(width - 2.0, y.toDouble())
                    }
                }
            }
            poisson.apply(bg, bg)
            blur.apply(bg, bg)

            draw {
                drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(0.5))
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
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
                    drawer.strokeWeight = 0.25
                    drawer.lineCap = LineCap.ROUND
                    drawer.fill = null


                    drawer.contours(flowLines)
                }
                pointCloudView.draw()
                viewfinder.draw(state.zoom, drawer)

                //drawer.circles(labelPoints, 3.0)

                val filteredPoints = labelPoints.filter { it in state.filtered.keys }

                val projectedPoints = filteredPoints.map {
                    project(
                        it.xy0,
                        drawer.projection,
                        drawer.view * drawer.model,
                        drawer.width,
                        drawer.height
                    ).xy
                }

                drawer.defaults()
                val labelTexts = filteredPoints.map { data.pointsToArticles[it]!!.faculty }
                drawer.fontMap = fms
                drawer.texts(labelTexts, projectedPoints)

//                val p = Vector2(state.closest.x + 3.0, state.closest.y)
//                if(p in pointCloudView.currentlySelected.first && pointCloudView.closestArticle != null && state.zoom in CLOSEST) {
//                    drawer.fontMap = fm
//                    drawer.fill = ColorRGBa.WHITE
//                    drawer.text(pointCloudView.closestArticle!!.title, p.transform(camera.view))
//                }
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
                        drawer.drawStyle.clip =
                            if (!it.expanded) it.actionBounds else it.actionBounds.copy(width = it.actionBounds.width * 2.0)

                        it.draw(drawer)

                        if (it.expanded && it.active) {

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
                                draw(drawer)
                            }

                            topicFilter.apply {
                                visible =
                                    it.expanded && (discoverSelector.current == 1 || discoverSelector.current == 2)
                                actionBounds = Rectangle(
                                    it.actionBounds.x + 50.0,
                                    it.actionBounds.y + 140.0,
                                    it.actionBounds.width - 50.0,
                                    facultyFilter.actionBounds.height + 60.0
                                )
                                draw(drawer)
                            }

                            dateFilter.apply {
                                visible = it.expanded
                                actionBounds = Rectangle(
                                    topicFilter.actionBounds.x,
                                    it.actionBounds.y + 730.0,
                                    facultyFilter.actionBounds.width,
                                    110.0
                                )
                                draw(drawer)
                            }

                            articleFilter.apply {
                                visible = it.expanded && discoverSelector.current == 2
                                actionBounds =
                                    topicFilter.actionBounds.movedBy(Vector2(facultyFilter.actionBounds.width, 0.0))
                                draw(drawer)
                            }

                        }

                        drawer.drawStyle.clip = null
                    }

                   showcases.draw(drawer)

                    showcases.draw(drawer)

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
                    drawer.text("ORACLE", 10.0, 37.0)
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
        //uiManager.drawDebugBoxes(drawer)

//        drawer.defaults()
//        drawer.image(pointCloudDensity)
//        drawer.shadeStyle = shadeStyle {
//            fragmentTransform = """x_fill.rg = x_fill.rg * 0.5 + 0.5;"""
//        }
//        drawer.image(pointCloudGradient)
    }


}
