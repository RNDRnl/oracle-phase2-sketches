import classes.ArticleData
import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.contacts.CircleContact
import org.openrndr.Program
import org.openrndr.MouseEventType
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.random.Random

fun Program.screenTest02(i: Int, rect: Rectangle) {

    val world = World(Vec2(0.0f, .81f))

    var articles = listOf<Pair<ArticleData, ColorRGBa>>()

    val books = mutableListOf<Book>()


    class Controller: Animatable() {
        var showTimer = 0.0
        var hideTimer = 0.0

        var fallTimer = 0.0

        //categories
        var bookBits = 0
        var floorBits = 1
    }
    val controller = Controller()


    fun createBookBody(box: Rectangle, index: Int): Body {
        val fixtureDef = FixtureDef().apply {
            shape = box.contour.polygonShape(simScale)
            density = 1.0f
            friction = 0.3f
            restitution = 0.0f
/*
            filter.categoryBits = controller.bookBits
            filter.maskBits = controller.floorBits*/

        }

        val pos = Vector2((index.toDouble() / articles.size.toDouble()) * (width - 50.0) + Double.uniform(-5.0, 5.0) + 50.0,
                            Double.uniform(0.3, 0.7) * (height - 50.0))


        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(pos.toVec2().mul((1/simScale).toFloat()))
            this.angle = Random.nextDouble(-Math.PI / 6, Math.PI / 6).toFloat()
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)

        body.linearVelocity = Vec2(Random.nextFloat() * 50.0f  -25.0f, Random.nextFloat() * 50.0f - 25.0f).mul((1/simScale).toFloat())
        body.angularVelocity = (Random.nextFloat() * 100.0f - 50.0f) / 100.0f

        return body
    }

    val staticBodies = mutableListOf<Body>()

    fun addStaticBox(rect: Rectangle, floor: Boolean = false) {
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

    addStaticBox(Rectangle(0.0, height-10.0,  width*1.0, 10.0), true)
    addStaticBox(Rectangle(0.0, 0.0, 10.0, height-10.0))
    addStaticBox(Rectangle(width-10.0, 0.0, 10.0, height-10.0))

    fun show(zoom: Double) {
        println("hello")
        books.clear()
        controller.apply {
            ::hideTimer.cancel()
            hideTimer = 0.0

            showTimer = 0.0
            ::showTimer.cancel()
            ::showTimer.animate(1.0, 3200L, Easing.SineOut)
        }

        for((index, article) in articles.withIndex()) {

            val t = smoothstep(0.75, 1.0, Double.uniform(0.0, 1.0))
            val boxWidth = if(t < zoom) 180.0 else 600.0

            val box = Rectangle.fromCenter(
                Vector2.ZERO,
                boxWidth * Double.uniform(0.85, 1.15),
                1600.0 * Double.uniform(0.85, 1.15))

            val body = createBookBody(box, index)
            val book = Book(box, article.first, article.second, body)
            books.add(book)
        }
    }

    fun hide() {
        articles = listOf()
        for(body in books.map { it.body }) {
            if(body.type == BodyType.DYNAMIC) {
                world.destroyBody(body)
            }
        }

        controller.apply {
            ::showTimer.cancel()
            ::hideTimer.animate(1.0, 150L, predelayInMs = i * 200L).completed.listen {
                books.clear()
            }
        }

    }

    var update: (met: MouseEventType, articlesToColors: List<Pair<ArticleData, ColorRGBa>>, zoom: Double)->Unit by this.userProperties
    update = { met, atc, zoom ->
        if(met == MouseEventType.BUTTON_DOWN) {
            hide()
        } else
        {
            articles = atc
            show(zoom)
        }
    }

    val fm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 80.0, contentScale = 4.0)

    extend {

        val circle = Circle(origin, 7200.0 * controller.showTimer)

        controller.updateAnimation()

        drawer.clear(ColorRGBa.BLUE.shade(0.35))

        world.gravity = Vec2(0.0f, 180.81f)
        world.step(1.0f/200.0f, 100, 100)

        drawer.fontMap = fm

        val t0 = (controller.showTimer * books.size).toInt()
        val t1 = (controller.hideTimer * t0).toInt()
        //.reversed().take(t0).drop(t1)
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

        drawer.fill = null
        drawer.stroke = ColorRGBa.PINK
        drawer.strokeWeight = 8.0
        drawer.rectangle(drawer.bounds)

        drawer.defaults()
        drawer.translate(-(rect.x), -(rect.y))
        drawer.fill = null
        drawer.stroke = ColorRGBa.YELLOW

        drawer.circle(circle)

    }
}
