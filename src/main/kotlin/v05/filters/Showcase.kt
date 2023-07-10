package v05.filters

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import v05.Article
import v05.facultyColor
import v05.libs.UIElementImpl

class Showcase(val title: String, val articles: List<Article>, var frame: Rectangle): Animatable() {



    var toggleButton = Rectangle(
        frame.x + frame.width - 140.0,
        frame.y + 20.0,
    140.0, 20.0)

    var outerFrame = frame
    var fader = 0.0

    var visible = false
    var expanded = false
        set(value) {
            if(value) {
                ::fader.animate(1.0, 1000, Easing.CubicInOut)
            } else {
                ::fader.animate(0.0, 500, Easing.CubicInOut)
            }
            field = value
        }

    val titleFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 36.0)
    val buttonFm = loadFont("data/fonts/Roboto-Regular.ttf", 17.0)
    fun draw(drawer: Drawer) {
        updateAnimation()

        if(visible) {
            val frameCopy = frame
            frame = Rectangle(
                frameCopy.corner.mix(outerFrame.corner + Vector2(10.0, 80.0), fader),
                frameCopy.width,
                frameCopy.height + (outerFrame.height - frameCopy.height) * fader
            )

            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = null
            drawer.roundedRectangle(frame.toRounded(8.0))

            toggleButton = Rectangle(
                    frame.x + frame.width - 140.0,
            frame.y + 35.0,
            100.0, 25.0)

            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = if(expanded) ColorRGBa.TRANSPARENT else ColorRGBa.WHITE
            drawer.roundedRectangle(toggleButton.toRounded(999.0))

            drawer.fontMap = buttonFm
            drawer.fill = if(expanded) ColorRGBa.WHITE else ColorRGBa.BLACK
            val text = if(expanded) "BACK" else "EXPLORE"
            drawer.writer {
                box = toggleButton
                newLine()
                cursor.x = toggleButton.center.x - textWidth(text) / 2.0
                cursor.y = toggleButton.center.y + buttonFm.height / 2.0
                text(text)

            }

            drawer.fontMap = titleFm
            drawer.fill = ColorRGBa.WHITE
            drawer.text(title, frame.corner + Vector2(20.0, 50.0))

            drawer.stroke = null
            drawer.rectangles {
                articles.forEachIndexed { i, it ->
                    val w = (frame.width - 120.0) / articles.size
                    this.fill = it.faculty.facultyColor()
                    this.rectangle(40.0 + i * w + (w / 5.0) * i, frame.y + 80.0, w, 60.0 - (58.0 * fader))
                }
            }

        }
    }

}