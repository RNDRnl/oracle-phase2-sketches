package v05

import lib.cwriter
import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.openrndr.Clock
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.grid
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import v01.simScale
import v01.toVec2
import v05.model.FilteredDataModel
import kotlin.random.Random

class ArticleBody(val frame: Rectangle, val body: Body)

interface ScreenDrawer {
    fun update()

    fun processMessage(message: ScreenMessage)

    fun draw(clock: Clock, drawer: Drawer, circle: Circle)
}


abstract class ZoomLevel(
    val screenId: Int,
    val bounds: Rectangle,
    val dataModel: DataModel,
    val filteredDataModel: FilteredDataModel
) : ScreenDrawer {
    open fun clear() {
        animations.fadeOut()
    }

    inner class Animations : Animatable() {
        var fade = 0.0
        var textFade = 0.0

        fun fadeIn() {
            ::fade.animate(1.0, 1000, Easing.SineInOut)
            ::textFade.animate(1.0, 1500, Easing.SineInOut)
        }

        fun fadeOut() {
            ::textFade.cancel()
            ::fade.cancel()
            ::fade.animate(0.0, 250, Easing.SineInOut).completed.listen { fade = 0.0 }
            ::textFade.animate(0.0, 350, Easing.SineInOut).completed.listen { textFade = 0.0 }
        }
    }
    val animations = Animations()

    override fun update() {
        animations.updateAnimation()
    }

    val fs = loadFont("data/fonts/Roboto-Regular.ttf", 30.0, contentScale = 1.0)
    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 60.0, contentScale = 1.0)
    val tfm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 200.0, contentScale = 1.0)
    val stfm = loadFont("data/fonts/default.otf", 80.0, contentScale = 1.0)
}

class Zoom0(i: Int, rect: Rectangle, dataModel: DataModel, filteredDataModel: FilteredDataModel) :
    ZoomLevel(i, rect, dataModel, filteredDataModel) {

    private var rects = listOf<Rectangle>()
    private var color = ColorRGBa.GRAY
    private val slots = mutableListOf<Vector2>()
    private var articlesSorted = dataModel.articlesSorted[SortMode.FACULTY_YEAR]!!

    private var highlighted = mutableListOf<Article>()
    private var highlightedFaculties = mutableListOf<String>()
    private var highlightedYears = mutableListOf<Int>()

    init {
        for (x in 0..(rect.width.toInt() - 20) / 10) {
            for (y in 0..((rect.height).toInt() - 20) / 42) {
                slots.add(Vector2(x * 10.0, y * 42.0).plus(Vector2(5.0, 2.0)))
            }
        }
    }

    override fun processMessage(message: ScreenMessage) {
        highlighted = message.articles.toMutableList()
        highlightedFaculties = message.filters.faculties.toMutableList()
        for (y in message.filters.dates.first until message.filters.dates.second) {
            highlightedYears.add(y)
        }
        if (message.articles.isNotEmpty()) {
            color = message.articles[0].faculty.facultyColor()
            rects = Rectangle(0.0, 0.0, bounds.width, bounds.height).grid(message.articles.size, 1).flatten()
            animations.fadeIn()
        } else {
            println("error: asked to be populated from an empty list ${message.articles}")
        }
    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {
//        val sortingSpeed = 10
//        for(i in 0 until sortingSpeed) {
//            sortingStep()
//        }
        drawer.isolated {
            drawer.rectangles {
                articlesSorted.forEachIndexed { index, article ->
                    val cIndex = (index + (screenId * slots.size))
                    if(cIndex < articlesSorted.size && index < slots.size) {
                        val highlighted = highlightedFaculties.contains(articlesSorted[cIndex].faculty) && highlightedYears.contains(articlesSorted[cIndex].year.toInt())
                        this.stroke = null
                        if (highlighted) {
                            this.fill = articlesSorted[cIndex].faculty.facultyColor()
                        } else {
                            this.fill = articlesSorted[cIndex].faculty.facultyColor().shade(0.15)
                        }
                        val position = slots[index]
                        if (highlighted) {
                            this.rectangle(Rectangle(position, 10.0 * 0.45, 42.0 * 0.85))
                        } else {
                            this.rectangle(Rectangle(position, 10.0, 42.0))
                        }
                    }
                }
            }
        }

        drawer.isolated {
            var lastX = -100.0
            articlesSorted.forEachIndexed { index, article ->
                val cIndex = (index + (screenId * slots.size))
                if(cIndex < articlesSorted.size && index < slots.size) {
                    val position = slots[index]
                    val label = articlesSorted[cIndex].label

                    if(label.isNotEmpty()) {
                        drawer.isolated {
                            val d = position.x - lastX
                            if(d > 50) {
                                drawer.fill = ColorRGBa.WHITE
                                drawer.rectangle(Rectangle(position.x, 0.0, 4.0, bounds.height))
                                drawer.fontMap = stfm
                                val yl = dataModel.yearLabels
                                val yOffset = map(yl.min().toDouble(), yl.max().toDouble(), 0.0, bounds.height-160.0, label.toDouble())

                                drawer.translate(position.x, 150.0 + yOffset)
                                drawer.rotate(-90.0)
                                drawer.text(label, 0.0, 55.0)
                                lastX = position.x
                            }
                        }
                    }
                }
            }
        }
    }
}

class Zoom1(i: Int, bounds: Rectangle, dataModel: DataModel, filteredDataModel: FilteredDataModel) :
    ZoomLevel(i, bounds, dataModel, filteredDataModel) {

    val world = World(Vec2(0.0f, .81f))
    val articleBodies = mutableMapOf<Article, ArticleBody>()
    val staticBodies = mutableListOf<Body>()

    init {
        fun addStaticBox(rect: Rectangle) {
            val fixtureDef = FixtureDef().apply {
                shape = rect.contour.polygonShape()
                density = 2.0f
                friction = 0.01f
                restitution = 0.1f
            }
            val bodyDef = BodyDef().apply {
                type = BodyType.STATIC
                position.set(Vec2(0.0f, 0.0f))

            }
            val body = world.createBody(bodyDef)
            body.createFixture(fixtureDef)
            staticBodies.add(body)
        }

        addStaticBox(Rectangle(0.0, bounds.height - 10.0, bounds.width * 1.0, 10.0))
        addStaticBox(Rectangle(0.0, 0.0, 10.0, bounds.height - 10.0))
        addStaticBox(Rectangle(bounds.width - 10.0, 0.0, 10.0, bounds.height - 10.0))
    }

    fun createArticleBody(pos: Vector2, box: Rectangle): Body {
        val fixtureDef = FixtureDef().apply {
            shape = box.contour.polygonShape(simScale)
            density = 1.0f
            friction = 0.9f
            restitution = 0.0f
        }


        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(pos.toVec2().mul((1 / simScale).toFloat()))
            this.angle = Random.nextDouble(-Math.PI / 6, Math.PI / 6).toFloat()
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)

        body.linearVelocity =
            Vec2(Random.nextFloat() * 50.0f - 25.0f, Random.nextFloat() * 50.0f - 25.0f).mul((1 / simScale).toFloat())
        body.angularVelocity = (Random.nextFloat() * 100.0f - 50.0f) / 100.0f

        return body
    }

    override fun processMessage(message: ScreenMessage) {
        animations.fadeOut()
        for (body in articleBodies.map { it.value.body }) {
            world.destroyBody(body)
        }

        articleBodies.clear()

        var previousX = 0.0

        for ((index, article) in message.articles.withIndex()) {
            val boxWidth = article.title.length.toDouble().map(0.0, 300.0, bounds.width / 50.0, bounds.width / 12.0)
            val boxHeight = bounds.height * Double.uniform(0.75, 0.85)

            if (previousX >= bounds.width) break
            else {
                val box = Rectangle.fromCenter(
                    Vector2.ZERO,
                    boxWidth,
                    boxHeight
                )

                val pos = Vector2(previousX + 80.0 * index, bounds.height - boxHeight - 10.0)

                val body = createArticleBody(pos + Vector2(100.0, 0.0), box)
                val articleBody = ArticleBody(box, body)

                articleBodies[article] = articleBody
                previousX = boxWidth + 80.0 * index
            }
        }

    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {
        drawer.isolated {

            world.gravity = Vec2(0.0f, 900.81f)
            world.step(1.0f / 200.0f, 100, 100)

            drawer.fontMap = fm
            for ((article, body) in articleBodies) {

                if (body.frame.center.transform(body.body.transform.matrix44()) + this@Zoom1.bounds.corner in circle) {
                    drawer.isolated {
                        drawer.fill = article.faculty.facultyColor()
                        drawer.stroke = null
                        drawer.model = body.body.transform.matrix44()
                        drawer.contour(body.frame.offsetEdges(2.0).contour)

                        drawer.fill = ColorRGBa.BLACK
                        drawer.translate(body.frame.center)
                        drawer.rotate(-90.0)
                        drawer.translate(-body.frame.center)
                        writer {
                            box = Rectangle(
                                body.frame.y,
                                body.frame.x,
                                body.frame.height,
                                body.frame.width
                            ).offsetEdges(-2.0)
                            newLine()
                            text(article.title.uppercase())
                        }
                    }
                }

            }
        }
    }

}

class Zoom2(i: Int, rect: Rectangle, dataModel: DataModel, filteredDataModel: FilteredDataModel) :
    ZoomLevel(i, rect, dataModel, filteredDataModel) {

    var currentArticle: Article? = null
    var sameFaculty = listOf<Article>()
    var sameTopic = listOf<Article>()

    override fun clear() {
        animations.fadeOut()
    }

    override fun processMessage(message: ScreenMessage) {
        animations.fadeIn()
        currentArticle = if (message.articles.isNotEmpty()) message.articles.first() else null
        if (currentArticle != null) {
            sameFaculty = List(10) { message.articles.filter { it.faculty == currentArticle!!.faculty }.random() }
            sameTopic = List(10) { message.articles.random() }
        }
    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {
        drawer.isolated {
            if (currentArticle != null) {
                val article = currentArticle!!

                drawer.fill = when (screenId) {
                    0, 2, 6 -> article.faculty.facultyColor()
                    else -> ColorRGBa.BLACK
                }.opacify(animations.fade)
                drawer.stroke = null
                drawer.rectangle(bounds)

                drawer.fill = ColorRGBa.WHITE
                when (screenId) {
                    0 -> singleColumnText(drawer, article.title)
                    1 -> infoText(drawer, article)
                    2 -> singleColumnText(drawer, article.department)
                    3 -> multiColumnText(drawer, "ABSTRACT", article.abstract, 3)
                    4 -> books(drawer, sameFaculty, "FROM THE SAME FACULTY (${currentArticle!!.faculty})")
                    5 -> books(drawer, sameTopic, "FROM THE SAME TOPIC (${currentArticle!!.topic})")
                    6 -> singleColumnText(drawer, article.year)
                    7 -> timeline(drawer)
                }
            }
        }
    }


    private fun infoText(drawer: Drawer, ad: Article) {

        val rects = Rectangle(50.0, 50.0, drawer.width - 100.0, drawer.height - 200.0)
            .grid(
                3, 1,
                20.0,
                30.0,
                100.0
            ).flatten()

        val authorsBox = Rectangle(rects[0].x, rects[0].y + (stfm.height * 2.0), rects[0].width, 350.0)
        drawer.fontMap = stfm
        drawer.text("AUTHORS", rects[0].corner + Vector2(0.0, stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = authorsBox
            newLine()
            text(ad.author.take((animations.textFade * ad.author.length).toInt()))
        }

        val dateBox = Rectangle(rects[1].x, rects[1].y + (stfm.height * 2.0), rects[1].width, 150.0)
        drawer.fontMap = stfm
        drawer.text("DATE", rects[1].corner + Vector2(0.0, stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = dateBox
            newLine()
            text(ad.year.take((animations.textFade * ad.year.length).toInt()))
        }

        val facultyBox = Rectangle(rects[1].x, dateBox.y + dateBox.height + (stfm.height * 2.0), rects[1].width, 250.0)
        drawer.fontMap = stfm
        drawer.text("FACULTY", dateBox.corner + Vector2(0.0, dateBox.height + stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = facultyBox
            newLine()
            text(ad.faculty.take((animations.textFade * ad.faculty.length).toInt()))
        }

        val departmentBox =
            Rectangle(rects[1].x, facultyBox.y + facultyBox.height + (stfm.height * 2.0), rects[1].width, 250.0)
        drawer.fontMap = stfm
        drawer.text("DEPARTMENT", facultyBox.corner + Vector2(0.0, facultyBox.height + stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = departmentBox
            newLine()
            text(ad.department.take((animations.textFade * ad.department.length).toInt()))
        }


        val infoBox = Rectangle(rects[2].x, rects[2].y + (stfm.height * 2.0), rects[2].width, rects[2].height)
        drawer.fontMap = stfm
        drawer.text("INFO", rects[2].corner + Vector2(0.0, stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = infoBox
            newLine()
            text(lipsum.take((animations.textFade * lipsum.length).toInt()))
        }

    }

    private fun multiColumnText(drawer: Drawer, title: String, text: String, cols: Int = 2) {
        val area = drawer.bounds.grid(1, 1, 40.0, 40.0).flatten().first()

        val rows = area.grid(1, 5, 0.0, 0.0, 100.0, 30.0).flatten()
        val header = rows[0]
        val body = rows.drop(1).bounds.grid(3, 1, 0.0, 0.0, 90.0, 0.0).flatten()

        drawer.isolated {
            drawer.fill = null
            drawer.rectangles(rows)
            drawer.rectangles(body)
        }

        drawer.fontMap = stfm
        drawer.writer {
            box = header
            newLine()
            text(title)
        }

        drawer.fontMap = fs
        drawer.cwriter {
            boxes = body
            newLine()
            text(text)
        }
    }

    private fun singleColumnText(drawer: Drawer, text: String) {
        val area = drawer.bounds.grid(1, 1, 40.0, 40.0).flatten().first()
        drawer.fontMap = tfm
        drawer.writer {
            box = area
            newLine()
            text(text.take((animations.textFade * text.length).toInt()))
        }
    }

    private fun books(drawer: Drawer, bookList: List<Article>, text: String = "") {
        val area = drawer.bounds.grid(1, 1, 40.0, 40.0).flatten().first()
        val rows = area.grid(1, 5, 0.0, 0.0, 100.0, 30.0).flatten()
        val header = rows[0]
        val body = rows.drop(1).bounds.grid(3, 1, 0.0, 0.0, 90.0, 0.0).flatten()

        drawer.stroke = null

        drawer.fontMap = stfm
        drawer.writer {
            box = header
            newLine()
            text(text)
        }

        var acc = 0.0
        drawer.translate(area.x, area.position(Vector2.ONE).y - 380.0)
        drawer.fontMap = fm
        for ((i, sf) in bookList.take((bookList.size * animations.fade).toInt()).withIndex()) {
            val w = Double.uniform(80.0, 160.0, Random(i))
            val r = Rectangle(acc, 0.0, w, 380.0)
            drawer.fill = sf.faculty.facultyColor()
            drawer.rectangle(r)

            drawer.fill = ColorRGBa.BLACK

            drawer.isolated {
                drawer.translate(r.x + 50.0, r.y + r.height - 20.0)
                drawer.rotate(-90.0)
                drawer.translate(-r.x, -(r.y + r.height))
                drawer.text(
                    sf.title.uppercase().take(
                        (r.width / fm.characterWidth('"')).toInt()
                    ), r.x, r.y + r.height
                )
            }

            acc += w + 10.0
        }
        drawer.isolated {
            drawer.defaults()
            val brackets = filteredDataModel.yearBrackets
            drawer.translate(area.x, 0.0)
            drawer.translate((brackets.first().start - 1900) * 20.0, 0.0)
            for (bracket in brackets) {
                val max = brackets.maxBy { it.count }.count.toDouble()
                val width = (bracket.endInclusive - bracket.start) * 20.0
                val height = ((bracket.count / max) * 500.0) / (width / 20.0) + 20.0
                drawer.rectangle(0.0, 600.0, width, -height)
                drawer.text("${bracket.start}", 0.0, 630.0)
                drawer.translate(width, 0.0)

            }
        }

    }

    val images = listOf(loadImage("data/images/placeholders/img.png"), loadImage("data/images/placeholders/img_1.png"), loadImage("data/images/placeholders/img_2.png"))
    private fun timeline(drawer: Drawer) {
        val area = drawer.bounds.grid(1, 1, 40.0, 40.0).flatten().first()

        val rows = area.grid(1, 5, 0.0, 0.0, 100.0, 30.0).flatten()
        val header = rows[0]
        val cells  = rows.drop(1).bounds.grid(3, 1, 0.0, 0.0, 90.0, 0.0).flatten()


        for ((index, cell) in cells.withIndex()) {

            drawer.imageFit(images[index], bounds = cell, fitMethod =  FitMethod.Contain)

        }


    }


}

