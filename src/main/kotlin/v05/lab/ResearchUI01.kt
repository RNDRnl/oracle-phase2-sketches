package v05.lab

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.noise.uniform
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import v05.Camera2D
import v05.extensions.PinchDetector
import v05.extensions.PinchEvent
import v05.libs.UIElement
import v05.libs.UIElementImpl
import v05.libs.UIManager

fun main() {
    application {
        program {


            val pinchDetector = extend(PinchDetector())

            class Slider(area: Rectangle) : UIElementImpl() {
                var value = 0.0

                init {
                    actionBounds = area
                    buttonDown.listen {
                        it.cancelPropagation()
                    }

                    dragged.listen {

                        value = it.position.x.map(
                            actionBounds.position(0.0, 0.0).x,
                            actionBounds.position(1.0, 0.0).x,
                            0.0,
                            1.0,
                            clamp = true
                        )
                    }
                }

                fun draw() {
                    drawer.isolated {
                        drawer.fill = ColorRGBa.GRAY
                        drawer.rectangle(actionBounds)

                        drawer.fill = ColorRGBa.GRAY.shade(0.5)
                        val drawBounds = actionBounds.offsetEdges(-actionBounds.height / 2.0, 0.0)
                        drawer.rectangle(drawBounds)

                        drawer.fill = ColorRGBa.RED
                        drawer.circle(
                            value.map(
                                0.0,
                                1.0,
                                drawBounds.position(0.0, 0.0).x,
                                drawBounds.position(1.0, 0.0).x
                            ), actionBounds.position(0.0, 0.5).y, drawBounds.height / 2.0
                        )
                    }
                }
            }


            val uiManager = UIManager(window, mouse, pinchDetector.pinchChanged)

            val sliders = (0 until 10).map {
                Slider(
                    Rectangle(
                        Double.uniform(50.0, width - 200.0),
                        Double.uniform(50.0, height - 200.0),
                        Double.uniform(200.0, 300.0),
                        Double.uniform(20.0, 70.0)
                    )
                )
            }

            val c = extend(Camera2D())
            uiManager.elements.add(c)
            for (slider in sliders) {
                //uiManager.elements.add(slider)
            }

            extend {
                for (slider in sliders) {
                    slider.draw()
                }
            }
        }
    }
}