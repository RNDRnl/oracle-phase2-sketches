package v05

import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.openrndr.Clock
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.grid
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import v01.simScale
import v01.toVec2
import kotlin.random.Random

class ArticleBody(val frame: Rectangle, val body: Body)

interface ScreenDrawer {
    fun update()
    fun draw(clock: Clock, drawer: Drawer, circle: Circle)
}


abstract class ZoomLevel(val i: Int, val bounds: Rectangle) : ScreenDrawer {

    open fun populate(articles: List<Article>) { }

    open fun clear() { animations.fadeOut() }

    inner class Animations: Animatable() {
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

    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 60.0, contentScale = 1.0)
    val tfm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 200.0, contentScale = 1.0)
    val stfm = loadFont("data/fonts/default.otf", 80.0, contentScale = 1.0)


}

class Zoom0(i: Int, rect: Rectangle) : ZoomLevel(i, rect) {

    var rects = listOf<Rectangle>()
    var color = ColorRGBa.GRAY

    override fun populate(articles: List<Article>) {

        if (articles.isNotEmpty()) {

            color = articles[0].faculty.facultyColor()
            rects = Rectangle(0.0, 0.0, bounds.width, bounds.height).grid(articles.size, 1).flatten()
            animations.fadeIn()
        } else {
            println("error: asked to be populated from an empty list ${articles}")
        }
    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {
        drawer.isolated {
            fill = ColorRGBa.BLUE
            for(rect in rects.take((animations.fade * rects.size).toInt())) {
                drawer.fill = color
                drawer.rectangle(rect)
            }
        }
    }
}

class Zoom1(i: Int, bounds: Rectangle) : ZoomLevel(i, bounds) {

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

        addStaticBox(Rectangle(0.0, bounds.height-10.0,  bounds.width*1.0, 10.0))
        addStaticBox(Rectangle(0.0, 0.0, 10.0, bounds.height-10.0))
        addStaticBox(Rectangle(bounds.width-10.0, 0.0, 10.0, bounds.height-10.0))
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
            position.set(pos.toVec2().mul((1/ simScale).toFloat()))
            this.angle = Random.nextDouble(-Math.PI / 6, Math.PI / 6).toFloat()
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)

        body.linearVelocity = Vec2(Random.nextFloat() * 50.0f  -25.0f, Random.nextFloat() * 50.0f - 25.0f).mul((1/ simScale).toFloat())
        body.angularVelocity = (Random.nextFloat() * 100.0f - 50.0f) / 100.0f

        return body
    }

    override fun populate(articles: List<Article>) {
        animations.fadeOut()
        for(body in articleBodies.map { it.value.body }) {
            world.destroyBody(body)
        }

        articleBodies.clear()

        var previousX = 0.0

        for((index, article) in articles.withIndex()) {
            val boxWidth = article.title.length.toDouble().map(0.0, 300.0, bounds.width / 50.0, bounds.width / 12.0)
            val boxHeight = bounds.height * Double.uniform(0.75, 0.85)

            if(previousX >= bounds.width) break
            else {
                val box = Rectangle.fromCenter(
                    Vector2.ZERO,
                    boxWidth,
                    boxHeight)

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
            world.step(1.0f/200.0f, 100, 100)

            drawer.fontMap = fm


            for ((article, body) in articleBodies) {

                if(body.frame.center.transform(body.body.transform.matrix44()) + this@Zoom1.bounds.corner in circle) {
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
                            box = Rectangle(body.frame.y, body.frame.x, body.frame.height, body.frame.width).offsetEdges(-2.0)
                            newLine()
                            text(article.title.uppercase())
                        }
                    }
                }

            }
        }
    }

}

class Zoom2(i: Int, rect: Rectangle) : ZoomLevel(i, rect) {

    var currentArticle: Article? = null
    var sameFaculty = listOf<Article>()
    var sameTopic = listOf<Article>()

    override fun clear() {
        animations.fadeOut()
    }

    override fun populate(articles: List<Article>) {
        animations.fadeIn()
        currentArticle = if(articles.isNotEmpty()) articles.first() else null
        if(currentArticle != null) {
            sameFaculty = List(10) { articles.filter { it.faculty == currentArticle!!.faculty }.random() }
            sameTopic = List(10) { articles.random() }
        }
    }

    override fun draw(clock: Clock, drawer: Drawer, circle: Circle) {
        drawer.isolated {
            if(currentArticle != null) {
                val article = currentArticle!!

                drawer.fill = when(i) { 0, 2, 6 -> article.faculty.facultyColor()
                    else -> ColorRGBa.BLACK }.opacify(animations.fade)
                drawer.stroke = null
                drawer.rectangle(bounds)

                drawer.fill = ColorRGBa.WHITE
                when(i) {
                    0 -> singleColumnText(drawer, article.title)
                    1 -> infoText(drawer, article)
                    2 -> singleColumnText(drawer, article.department)
                    3 -> multiColumnText(drawer, "ABSTRACT", article.abstract, 3)
                    4 -> books(drawer, sameFaculty, "FROM THE SAME FACULTY")
                    5 -> books(drawer, sameTopic, "FROM THE SAME TOPIC")
                    6 -> singleColumnText(drawer, article.year)
                    7 -> timeline(drawer)
                }
            }
        }
    }


    private fun infoText(drawer: Drawer, ad: Article) {

        val rects = Rectangle(50.0, 50.0, drawer.width - 100.0, drawer.height - 200.0)
            .grid(3, 1,
                20.0,
                30.0,
                100.0).flatten()

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

        val facultyBox = Rectangle(rects[1].x,dateBox.y + dateBox.height + (stfm.height * 2.0), rects[1].width, 250.0)
        drawer.fontMap = stfm
        drawer.text("FACULTY", dateBox.corner + Vector2(0.0, dateBox.height + stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = facultyBox
            newLine()
            text(ad.faculty.take((animations.textFade * ad.faculty.length).toInt()))
        }

        val departmentBox = Rectangle(rects[1].x, facultyBox.y + facultyBox.height + (stfm.height * 2.0), rects[1].width, 250.0)
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
        drawer.fontMap = stfm
        drawer.text(title, drawer.bounds.corner + Vector2(60.0, 120.0))

        val rects = Rectangle(50.0, 200.0, drawer.width - 100.0, drawer.height - 200.0)
            .grid(cols, 1,
                20.0,
                30.0,
                100.0).flatten()
        var currentRect = 0

        drawer.fontMap = fm
        drawer.writer {
            box = rects[currentRect]
            for(char in text.take((animations.textFade * text.length).toInt())) {
                if(cursor.y > rects[currentRect].height - 20.0 && currentRect < rects.size - 1) {
                    currentRect++
                    box = rects[currentRect]
                }
                text(char.toString())
            }
        }
    }

    private fun singleColumnText(drawer: Drawer, text: String) {
        drawer.fontMap = tfm
        drawer.writer {
            box = drawer.bounds.offsetEdges(-20.0)
            newLine()
            text(text.take((animations.textFade * text.length).toInt()))
        }
    }

    private fun books(drawer: Drawer, bookList: List<Article>, text: String = "") {
        drawer.stroke = null

        drawer.fontMap = stfm
        drawer.text(text, drawer.bounds.corner + Vector2(60.0, 120.0))

        var acc = 0.0
        drawer.translate(60.0, 0.0)
        drawer.fontMap = fm
        for((i, sf) in bookList.take((bookList.size * animations.fade).toInt()).withIndex()) {
            val w = Double.uniform(80.0, 160.0, Random(i))
            val r = Rectangle(acc, drawer.height - 480.0, w,380.0)
            drawer.fill = sf.faculty.facultyColor()
            drawer.rectangle(r)

            drawer.fill = ColorRGBa.BLACK

            drawer.pushTransforms()
            drawer.translate(r.x + 50.0, r.y + r.height - 20.0)
            drawer.rotate(-90.0)
            drawer.translate(-r.x, -(r.y + r.height))
            drawer.text(sf.title.uppercase().take(
                (r.width / fm.characterWidth('"')).toInt()
            ), r.x, r.y + r.height)
            drawer.popTransforms()

            acc += w + 10.0
        }
    }

    private fun timeline(drawer: Drawer) {
        drawer.stroke = ColorRGBa.WHITE
        drawer.lineSegment(0.0, 100.0, drawer.width * 1.0, 100.0)
    }


}

