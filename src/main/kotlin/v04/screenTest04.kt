package v04

import classes.ArticleData
import orbox.matrix44
import orbox.polygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.openrndr.Program
import org.openrndr.MouseEventType
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.writer
import org.openrndr.extra.color.presets.DARK_ORANGE
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import origin
import v01.Book
import v01.simScale
import v01.toVec2
import kotlin.random.Random

fun Program.screenTest04(i: Int, rect: Rectangle) {

    var articles = listOf<Article>()

    class Controller: Animatable() {
        var showTimer = 0.0
        var hideTimer = 0.0

        fun fadeIn() {
            ::hideTimer.cancel()
            hideTimer = 0.0

            showTimer = 0.0
            ::showTimer.cancel()
            ::showTimer.animate(1.0, 3200L, Easing.SineOut)
        }

    }
    val controller = Controller()

    val zoomLevels = listOf(::Zoom0, ::Zoom1, ::Zoom2).map { it(i, rect, drawer) }
    var currentZoom = 1


    var update: (articlesToColors: MutableList<Article>, zoomLevel: Int)->Unit by this.userProperties
    update = { newArticles, zoomlv ->

        controller.fadeIn()
        currentZoom = zoomlv
        zoomLevels[currentZoom].populate(articles)

        articles = newArticles
    }

    extend {
        controller.updateAnimation()
        zoomLevels[currentZoom].update()

        drawer.clear(ColorRGBa.BLACK.shade(0.35))

        drawer.defaults()

        val circle = Circle(origin, 7200.0 * controller.showTimer)

        drawer.shadeStyle = shadeStyle {
            fragmentTransform = """
                float dist = distance(c_boundsPosition.xy, vec2(0.5));
                vec3 green = vec3(0.058, 0.9, 0.53);
                vec3 c = mix(vec3(0.0), green, smoothstep(0.0, 1.0, dist));
                x_fill = vec4(c, 1.0 - p_t);
            """.trimIndent()
            parameter("t", controller.showTimer)
        }
        drawer.translate(-(rect.x), -(rect.y))
        drawer.fill = ColorRGBa.WHITE
        drawer.stroke = null

        drawer.circle(circle)
        drawer.shadeStyle = null

        drawer.defaults()
        zoomLevels[currentZoom].draw(circle)

    }
}