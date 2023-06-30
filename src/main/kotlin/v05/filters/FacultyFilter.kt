package v05.filters

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.math.mod
import org.openrndr.shape.Rectangle
import v05.*

class FacultyFilter(val drawer: Drawer, val model: FacultyFilterModel): Filter() {


    inner class Animations: Animatable() {
        var slider = 0.0

        fun minimize() {
            ::slider.animate(1.0, 750, Easing.CubicInOut)
        }

        fun maximize() {
            ::slider.animate(0.0, 750, Easing.CubicInOut)
        }
    }
    val animations = Animations()

    override var isCurrent = true
    var isMinimized = false
        set(value) {
            if(!field && value) {
                animations.minimize()
            } else if (field && !value) {
                animations.maximize()
            }
            field = value
        }
    override var title = "FACULTIES"

    init {
        actionBounds = Rectangle(10.0, 0.0, 460.0, 600.0)

        buttonDown.listen {
            it.cancelPropagation()
            for (i in model.states.indices) {
                if (it.position in itemBox(i)) {
                     if(model.states.all { it.visible }) {
                        model.states[i].visible = true
                        model.states.minus(model.states[i]).forEach { s ->s.visible = false }
                    } else {
                        model.states[i].visible = !model.states[i].visible
                    }
                }
            }
        }

        dragged.listen {
            for (i in model.states.indices) {
                if (it.position in itemBox(i)) {
                    model.states[i].visible = true
                }
            }
        }
    }

    fun itemBox(i: Int): Rectangle {
        val offsetX = animations.slider * 45.0

        return Rectangle(
            actionBounds.x + 5.0,
            i * 30.0 + (25.0 * i) + actionBounds.y + 10.0,
            80.0 - offsetX,
            30.0
        )
    }

    val facultyAbbrFm = loadFont("data/fonts/Roboto-Regular.ttf", 17.0)
    val facultyFm = loadFont("data/fonts/ArchivoNarrow-SemiBold.ttf", 16.0)

    override fun draw() {

        if(visible) {
            animations.updateAnimation()
            model.states.forEachIndexed { i, state ->

                val item = model.list[i]
                val itemBox = itemBox(i)

                drawer.writer {
                    gaplessNewLine()

                    drawer.fontMap = facultyAbbrFm
                    drawer.fill = if (state.visible) facultyColors[i] else ColorRGBa.TRANSPARENT
                    drawer.stroke = facultyColors[i]
                    drawer.roundedRectangle(itemBox.toRounded(999.0))

                    val t0 = item.facultyAbbreviation()
                    drawer.fill =  if (state.visible) ColorRGBa.BLACK else facultyColors[i]
                    cursor.x = itemBox.center.x - textWidth(t0) / 2.0
                    cursor.y = itemBox.center.y + facultyAbbrFm.height / 2.0
                    text(t0)

                    drawer.fontMap = facultyFm
                    drawer.fill = ColorRGBa.WHITE.opacify(1.0 - animations.slider)
                    cursor.x = itemBox.corner.x + itemBox.width + 5.0
                    text(item.uppercase())


                }

            }
        }

    }
}
