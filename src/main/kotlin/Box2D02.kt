import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.drawImage
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.color.spaces.toOKHSLa
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Transform
import java.io.File
import kotlin.random.Random

fun main() = application {
    val div = 4

    configure {
        width = 1920
        height = 1080
    }

    program {
        val world = World(Vec2(0.0f, .81f))
        val bodies = mutableListOf<Body>()
        val simScale = 100.0

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
            bodies.add(body)
        }

        val images = (2..9).map { loadImage(File("data/images/spines1/Group $it.png")) }.map {
            drawImage(it.height, it.width) {
                drawer.clear(ColorRGBa.BLUE)
                drawer.view = Matrix44.fromColumnVectors(
                    Vector4.UNIT_Y, Vector4.UNIT_X, Vector4.UNIT_Z, Vector4.UNIT_W)


                drawer.image(it)
            }.also { c -> c.flipV =true }
        }
        val box = Rectangle.fromCenter(Vector2(0.0, 0.0), 80.0, 800.0).contour
        fun addBox(p:Vector2) {
            val fixtureDef = FixtureDef().apply {
                shape = box.polygonShape(simScale)
                density = 1.0f
                friction = 0.3f
                restitution = 0.0f

            }
            val bodyDef = BodyDef().apply {
                type = BodyType.DYNAMIC
                position.set(Vec2(p.x.toFloat(), p.y.toFloat()).mul((1/simScale).toFloat()))
                //position.set( Vec2(320.0f + Random.nextFloat()*100.0f - 50.0f, 240.0f + Random.nextFloat()*100.0f - 50.0f).mul((1/simScale).toFloat()))
                this.angle = Random.nextDouble(-Math.PI, Math.PI).toFloat()
            }
            val body = world.createBody(bodyDef)
            body.createFixture(fixtureDef)

            body.linearVelocity = Vec2(Random.nextFloat() * 50.0f  -25.0f, Random.nextFloat() * 50.0f - 25.0f).mul((1/simScale).toFloat())
            body.angularVelocity = (Random.nextFloat() * 100.0f - 50.0f) / 100.0f
            bodies.add(body)
        }

        mouse.buttonDown.listen {
            for (i in 0 until 1)
            addBox(it.position)
        }

        addStaticBox(Rectangle(0.0, height-10.0,  width*1.0, 10.0))
        addStaticBox(Rectangle(0.0, 0.0, 10.0, height-10.0))
        addStaticBox(Rectangle(width-10.0, 0.0, 10.0, height-10.0))

        extend {

            drawer.clear(ColorRGBa.PINK)
            drawer.fill = ColorRGBa.PINK.shade(0.5)

            val inset = drawer.bounds.offsetEdges(-100.0)

            drawer.rectangle(inset)
            drawer.lineSegment(drawer.bounds.position(0.0, 0.0), inset.position(0.0, 0.0))
            drawer.lineSegment(drawer.bounds.position(0.0, 1.0), inset.position(0.0, 1.0))
            drawer.lineSegment(drawer.bounds.position(1.0, 1.0), inset.position(1.0, 1.0))
            drawer.lineSegment(drawer.bounds.position(1.0, 0.0), inset.position(1.0, 0.0))

            drawer.stroke = null
            drawer.fill = ColorRGBa.BLACK

            drawer.fill = null

            world.gravity = Vec2(0.0f, 90.81f)
            world.step(1.0f/200.0f, 100, 100)
            for ((index, body) in bodies.drop(3).withIndex()) {
                drawer.fill = ColorRGBa.RED.toOKHSLa().shiftHue(index*10.0).toRGBa()
                drawer.stroke = null

                drawer.isolated {
                    drawer.model = body.transform.matrix44()
                    val box = Rectangle.fromCenter(Vector2(0.0, 0.0), 70.0, 790.0)
                    drawer.contour(box.contour)
                    drawer.image(images.random(Random(index)), -40.0, -400.0)
                    drawer.fill = ColorRGBa.WHITE
                 /*   val box2 = Rectangle.fromCenter(Vector2(0.0, 0.0), 60.0, 90.0).movedBy(Vector2(0.0, 345.0))
                    drawer.contour(box2.contour.contour)*/

                }
            }

        }
    }
}