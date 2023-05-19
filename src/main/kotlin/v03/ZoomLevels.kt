package v03

import classes.ArticleData
import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.grid
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import v01.Book
import v01.simScale
import v01.toVec2
import kotlin.random.Random

open class ZoomLevel(val i: Int, val rect: Rectangle, val drawer: Drawer) {

    open fun populate(articles: List<Pair<ArticleData, ColorRGBa>>) { }

    open fun clear() { }

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
            ::fade.animate(0.0, 250, Easing.SineInOut)
            ::textFade.animate(0.0, 350, Easing.SineInOut)
        }
    }
    val animations = Animations()

    fun update() {
        animations.updateAnimation()
    }

    val fm = loadFont("data/fonts/Roboto-Regular.ttf", 60.0, contentScale = 1.0)
    val tfm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 200.0, contentScale = 1.0)
    val stfm = loadFont("data/fonts/default.otf", 80.0, contentScale = 1.0)

    open fun draw(circle: Circle) { }
}

class Zoom0(i: Int, rect: Rectangle, drawer: Drawer) : ZoomLevel(i, rect, drawer) {

    override fun draw(circle: Circle) {
        drawer.isolated {
            fill = ColorRGBa.BLUE
            circle(bounds.center, 100.0)
        }
    }
}

class Zoom1(i: Int, rect: Rectangle, drawer: Drawer) : ZoomLevel(i, rect, drawer) {

    val world = World(Vec2(0.0f, .81f))
    val books = mutableListOf<Book>()
    val staticBodies = mutableListOf<Body>()

    init {
        fun addStaticBox(rect: Rectangle) {
            val fixtureDef = FixtureDef().apply {
                shape = rect.contour.polygonShape()
                density = 2.0f
                friction = 0.01f
                restitution = 0.1f

                /*    if (floor) {
                        filter.categoryBits = controller.floorBits
                        filter.maskBits = controller.bookBits
                    }*/

            }
            val bodyDef = BodyDef().apply {
                type = BodyType.STATIC
                position.set(Vec2(0.0f, 0.0f))

            }
            val body = world.createBody(bodyDef)
            body.createFixture(fixtureDef)
            staticBodies.add(body)
        }

        addStaticBox(Rectangle(0.0, drawer.height-10.0,  drawer.width*1.0, 10.0))
        addStaticBox(Rectangle(0.0, 0.0, 10.0, drawer.height-10.0))
        addStaticBox(Rectangle(drawer.width-10.0, 0.0, 10.0, drawer.height-10.0))
    }

    fun createBookBody(pos: Vector2, box: Rectangle, index: Int): Body {
        val fixtureDef = FixtureDef().apply {
            shape = box.contour.polygonShape(simScale)
            density = 1.0f
            friction = 0.3f
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


    override fun populate(articles: List<Pair<ArticleData, ColorRGBa>>) {
        books.clear()

        for((index, article) in articles.withIndex()) {

            val t = smoothstep(0.75, 1.0, Double.uniform(0.0, 1.0))
            val boxWidth = if(t < 0.5) 180.0 else 600.0

            val box = Rectangle.fromCenter(
                Vector2.ZERO,
                boxWidth * Double.uniform(0.85, 1.15),
                1600.0 * Double.uniform(0.85, 1.15))

            val pos = Vector2((index.toDouble() / articles.size.toDouble()) * (drawer.width - 50.0) + Double.uniform(-5.0, 5.0) + 50.0,
                Double.uniform(0.3, 0.7) * (drawer.height - 50.0))

            val body = createBookBody(pos, box, index)
            val book = Book(box, article.first, article.second, body)
            books.add(book)
        }
    }

    override fun clear() {
        for(body in books.map { it.body }) {
            world.destroyBody(body)
        }
    }

    override fun draw(circle: Circle) {
        drawer.isolated {

            world.gravity = Vec2(0.0f, 180.81f)
            world.step(1.0f/200.0f, 100, 100)

            drawer.fontMap = fm


            for (book in books) {

                fun Vector2.transform(m : Matrix44) : Vector2 {
                    return (m * this.xy01).xy
                }

                if(book.frame.center.transform(book.body.transform.matrix44()) + rect.corner in circle) {
                    drawer.isolated {
                        drawer.fill = book.color
                        drawer.stroke = null
                        drawer.model = book.body.transform.matrix44()
                        drawer.contour(book.frame.offsetEdges(2.0).contour)

                        drawer.fill = ColorRGBa.BLACK
                        drawer.translate(book.frame.center)
                        drawer.rotate(-90.0)
                        drawer.translate(-book.frame.center)
                        writer {
                            box = Rectangle(book.frame.y, book.frame.x, book.frame.height, book.frame.width).offsetEdges(-2.0)
                            newLine()
                            text(book.data.title.uppercase())
                        }
                    }
                }

            }
        }
    }

}

class Zoom2(i: Int, rect: Rectangle, drawer: Drawer) : ZoomLevel(i, rect, drawer) {

    var currentArticle: Pair<ArticleData, ColorRGBa>? = null
    var sameFaculty = listOf<Pair<ArticleData, ColorRGBa>>()
    var sameTopic = listOf<Pair<ArticleData, ColorRGBa>>()

    override fun clear() {
        animations.fadeOut()
    }

    override fun populate(articles: List<Pair<ArticleData, ColorRGBa>>) {
        animations.fadeIn()
        currentArticle = if(articles.isNotEmpty()) articles.first() else null
        if(currentArticle != null) {
            sameFaculty = List(10) { articles.filter { it.first.faculty == currentArticle!!.first.faculty }.random() }
            sameTopic = List(10) { articles.random() }
        }
    }

    override fun draw(circle: Circle) {
        drawer.isolated {
            if(currentArticle != null) {
                val (ad, color) = currentArticle!!

                drawer.fill = when(i) { 0, 2, 6 -> color else -> ColorRGBa.BLACK }.opacify(animations.fade)
                drawer.stroke = null
                drawer.rectangle(bounds)

                drawer.fill = ColorRGBa.WHITE
                when(i) {
                    0 -> singleColumnText(ad.title)
                    1 -> infoText(ad)
                    2 -> singleColumnText(ad.department)
                    3 -> multiColumnText("ABSTRACT", lipsum, 3)
                    4 -> books(sameFaculty)
                    5 -> books(sameTopic)
                    6 -> singleColumnText(ad.date)
                    7 -> timeline()
                }
            }
        }
    }


    private fun infoText(ad: ArticleData) {

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
            println(ad.author)
            text(ad.author)
        }

        val dateBox = Rectangle(rects[1].x, rects[1].y + (stfm.height * 2.0), rects[1].width, 150.0)
        drawer.fontMap = stfm
        drawer.text("DATE", rects[1].corner + Vector2(0.0, stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = dateBox
            newLine()
            text(ad.date)
        }

        val facultyBox = Rectangle(rects[1].x,dateBox.y + dateBox.height + (stfm.height * 2.0), rects[1].width, rects[1].height)
        drawer.fontMap = stfm
        drawer.text("FACULTY", dateBox.corner +  Vector2(0.0, dateBox.height + stfm.height))
        drawer.fontMap = fm
        drawer.writer {
            box = facultyBox
            newLine()
            text(ad.faculty)
        }


        val departmentBox = Rectangle(rects[1].x, rects[1].y + facultyBox.height, rects[1].width, rects[1].height)
        drawer.fontMap = stfm
        drawer.text("DEPARTMENT", rects[1].corner + stfm.height)
        drawer.fontMap = fm
        drawer.writer {
            box = departmentBox
            newLine()
            text(ad.department)
        }

        /*
        val infoBox = Rectangle(rects[2].x, rects[2].y + (stfm.height * 2.0), rects[2].width, 150.0)
        drawer.fontMap = stfm
        drawer.text("INFO", rects[2].corner + stfm.height)
        drawer.fontMap = fm
        drawer.writer {
            box = infoBox
            newLine()
            text(ad.department)
        }*/

//        drawer.strokeWeight = 4.0
//        drawer.stroke = ColorRGBa.RED
//        drawer.fill = null
//        drawer.rectangles(listOf(authorsBox, dateBox, departmentBox, infoBox))

    }

    private fun multiColumnText(title: String, text: String, cols: Int = 2) {
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

    private fun singleColumnText(text: String) {
        drawer.fontMap = tfm
        drawer.writer {
            box = drawer.bounds.offsetEdges(-20.0)
            newLine()
            text(text.take((animations.textFade * text.length).toInt()))
        }
    }

    private fun books(bookList: List<Pair<ArticleData, ColorRGBa>>) {
        drawer.stroke = null
        for((i, sf) in bookList.withIndex()) {
            drawer.fill = sf.second
            drawer.rectangle(i * 10.0, 100.0, 10.0, 200.0)
        }
    }

    private fun timeline() {
        drawer.stroke = ColorRGBa.WHITE
        drawer.lineSegment(0.0, 100.0, drawer.width * 1.0, 100.0)
    }


}

val lipsum = "Lorem ipsum dolor sit amet. " +
        "Quo quia delectus sed iste eaque nam deleniti " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! " +
        "asperiores et temporibus illo quo aliquid doloremque " +
        "sit explicabo recusandae? Ad quibusdam consectetur est " +
        "doloremque molestiae qui quidem perspiciatis ut odit " +
        "galisum 33 asperiores illo nam nostrum eius eum beatae " +
        "voluptatem! "