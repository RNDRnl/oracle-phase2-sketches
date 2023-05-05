import classes.smoothStaggered
import org.openrndr.Program
import org.openrndr.MouseEventType
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.shape.Rectangle



fun Program.screenTest() {

    fun show(): Array<Double> {
       return Array(14) { Double.uniform(0.5, 1.0) }
    }

    fun hide(): Array<Double> {
        return Array(14) { 0.0 }
    }

    val controller = object {
        var values = show()
    }

    var update: (met: MouseEventType)->Unit by this.userProperties
    update = { met ->
        if(met == MouseEventType.BUTTON_DOWN) {
            controller.values = hide()
        } else
        {
            controller.values = show()
        }
    }

    val sx by smoothStaggered(controller::values, 0.55, 0.55)

    extend {

        drawer.rectangles(controller.values.indices.map {
            val w = width / controller.values.size * 1.0
            val h = height * sx[it]
            Rectangle(it * w, height - h, w,  h).offsetEdges(-10.0)
        })

        drawer.stroke = ColorRGBa.PINK
        drawer.strokeWeight = 4.0
        drawer.fill = null
        drawer.rectangle(drawer.bounds)

    }
}

