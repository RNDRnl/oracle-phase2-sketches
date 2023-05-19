package v01

import classes.ArticleData
import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.openrndr.Program
import org.openrndr.MouseEventType
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.color.spaces.toOKHSLa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.smoothstep
import org.openrndr.shape.Rectangle
import kotlin.random.Random

const val simScale = 100.0

class Book(val frame: Rectangle, val data: ArticleData, val color: ColorRGBa, val body: Body)

fun Program.screenTest01() {

    val frame = drawer.bounds

    val world = World(Vec2(0.0f, .81f))

    var articles = listOf<Pair<ArticleData, ColorRGBa>>()

    val books = mutableListOf<Book>()

    fun createBookBody(box: Rectangle, index: Int): Body {
        val fixtureDef = FixtureDef().apply {
            shape = box.contour.polygonShape(simScale)
            density = 1.0f
            friction = 0.3f
            restitution = 0.0f
        }

        val pos = Vector2((index.toDouble() / articles.size.toDouble()) * (frame.width - 50.0) + Double.uniform(-5.0, 5.0) + 50.0,
                            Double.uniform(0.1, 0.5) * (frame.height - 50.0))


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

    fun show(zoom: Double) {
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
        for(body in books.map { it.body }) {
            if(body.type == BodyType.DYNAMIC) {
                world.destroyBody(body)
            }
        }
        articles = listOf()
        books.clear()
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


    val staticBodies = mutableListOf<Body>()

    fun addStaticBox(rect: Rectangle) {
        val fixtureDef = FixtureDef().apply {
            shape = rect.contour.polygonShape()
            density = 1.0f
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

    addStaticBox(Rectangle(0.0, height-10.0,  width*1.0, 10.0))
    addStaticBox(Rectangle(0.0, 0.0, 10.0, height-10.0))
    addStaticBox(Rectangle(width-10.0, 0.0, 10.0, height-10.0))

    val fm = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 80.0, contentScale = 4.0)

    extend {

        drawer.clear(ColorRGBa.BLUE.shade(0.35))

        world.gravity = Vec2(0.0f, 100.81f)
        world.step(1.0f/200.0f, 100, 100)

        drawer.fontMap = fm

        for (book in books) {

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

        drawer.fill = null
        drawer.stroke = ColorRGBa.PINK
        drawer.strokeWeight = 8.0
        drawer.rectangle(frame)


    }
}

fun Vec2.toVector2(): Vector2 {
    return Vector2(this.x.toDouble(), this.y.toDouble())
}


fun Vector2.toVec2(): Vec2 {
    return Vec2(this.x.toFloat(), this.y.toFloat())
}