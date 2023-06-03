package v04

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.math.Polar
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import kotlin.random.Random

class Carousel(val data: DataModelNew): Animatable() {

    val articles = data.articles
    val current = MutableList(5) { articles.random() }

    var fade = 0.0
    var timer = 0.0

    init {
        cycle() // this should start only if the filter is visible
    }

    fun cycle() {
        fade = 0.0
        current.add(articles.random())
        ::timer.animate(1.0, 4000).completed.listen {
            ::fade.animate(1.0, 2000, Easing.SineInOut).completed.listen {
               current.remove(current.first())
                cycle()
            }
        }

    }

    val font = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 28.0)



    fun draw(drawer: Drawer) {

        updateAnimation()
        drawer.fontMap = font
        drawer.strokeWeight = 3.0

        current.forEach {
            val pos = data.articlesToPoints[it]!!
            val pol = Polar(45.0, 90.0).cartesian + pos
            val textBox = Rectangle(pol, 280.0, 400.0)

            when (it) {
                current.first() -> {

                    drawer.stroke = ColorRGBa.WHITE
                    drawer.contour(LineSegment(pos, pol).contour.sub(0.0, 1.0 - fade))
                    drawer.stroke = null

                    drawer.writer {
                        box = textBox
                        drawer.fill = ColorRGBa.WHITE
                        text(it.title.take(((1.0 - fade) * it.title.length).toInt()))
                        newLine()
                    }

                    drawer.circle(data.articlesToPoints[it]!!, 10.0 * (1.0 - fade))
                }
                current.last() -> {

                    drawer.stroke = ColorRGBa.WHITE
                    drawer.contour(LineSegment(pos, pol).contour.sub(0.0, fade))
                    drawer.stroke = null

                    drawer.writer {
                        box = textBox
                        drawer.fill = ColorRGBa.WHITE
                        text(it.title.take(((fade) * it.title.length).toInt()))
                    }

                    drawer.circle(data.articlesToPoints[it]!!, 10.0 * fade)
                }
                else -> {

                    drawer.stroke = ColorRGBa.WHITE
                    drawer.contour(LineSegment(pos, pol).contour)
                    drawer.stroke = null

                    drawer.writer {
                        box = textBox
                        drawer.fill = ColorRGBa.WHITE
                        text(it.title)
                    }
                    drawer.circle(data.articlesToPoints[it]!!, 10.0)
                }
            }

        }
    }

}