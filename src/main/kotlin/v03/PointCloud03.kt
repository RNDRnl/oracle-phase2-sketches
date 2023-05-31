package v03

import classes.*
import divider
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.GaussianBlur
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.math.*
import v02.ss
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

fun main() = application {
    configure {
        width = 1280
        height = 1024
        hideWindowDecorations = true
        windowAlwaysOnTop = true
        //position = IntVector2(1020, 680)
    }

    program {

        val ipAddress = "192.168.0.172"

        val data = Data()
        val positions = data.points
        val qcam = QuaternionCameraSimple().apply {
            bounds = drawer.bounds.scaledBy(1.0, 0.8, 0.0, 0.0)
        }

        val quad = vertexBuffer(
            vertexFormat {
                position(3)
            },
            4
        ).apply {
            put {
                write(Vector3(-1.0, -1.0, 0.0))
                write(Vector3(1.0, -1.0, 0.0))
                write(Vector3(-1.0, 1.0, 0.0))
                write(Vector3(1.0, 1.0, 0.0))
            }
        }

        val offsets = vertexBuffer(
            vertexFormat {
                attribute("offset", VertexElementType.VECTOR3_FLOAT32)
                attribute("color", VertexElementType.VECTOR4_FLOAT32)
                attribute("fac0", VertexElementType.VECTOR4_FLOAT32)
                attribute("fac1", VertexElementType.VECTOR4_FLOAT32)
                attribute("fac2", VertexElementType.VECTOR4_FLOAT32)
            },
            positions.size
        ).apply {
            put {
                for ((index, position) in positions.withIndex()) {
                    write(position)
                    val f = position.length.map(10.0, 12.0, 0.0, 1.0)

                    val activeFaculty = data.facultyIndexes[index].let { if (it == -1) 9 else it }
                    write(facultyColors.getOrNull(activeFaculty)?: ColorRGBa.WHITE)

                    for (j in 0 until 12) {
                        val value = if (j == activeFaculty && j < 10) 1 else if (j == activeFaculty && j > 10) 0 else 0
                        write(value.toFloat())
                    }
                }
            }
        }


        qcam.orientationChanged.listen {
            data.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
        }

        qcam.zoomChanged.listen {
            data.zoom = qcam.zoom
            data.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
        }


        val slider = Slider(Vector2(width - 220.0, height - 30.0), 180.0, "ZOOM")

        mouse.dragged.listen {
            val p = it.position
            if(p in slider.bounds.offsetEdges(15.0)) {
                slider.current = map(slider.bounds.x, slider.bounds.x + slider.width, 0.0, 1.0, p.x)
                qcam.zoom = slider.current
            }
        }


        val gui = GUI()
        val c = compose {
            val a = aside {
                draw {
                    drawer.isolated {
                        drawer.shadeStyle = ss
                        drawer.vertexBufferInstances(
                            listOf(quad),
                            listOf(offsets),
                            DrawPrimitive.TRIANGLE_STRIP,
                            offsets.vertexCount
                        )
                    }
                }
            }
            layer {
                draw {
                    drawer.defaults()
                    drawer.image(a.result)
                }
                post(GaussianBlur()).addTo(gui)
                post(ColorCorrection()).addTo(gui)
            }
            layer {
                draw {
                    drawer.defaults()
                    drawer.image(a.result)
                }
            }
        }

        thread {
            val socket = DatagramSocket()
            val address = InetSocketAddress(InetAddress.getByName(ipAddress), 9002)

            fun send(state: EventObject) {
                val baos = ByteArrayOutputStream(1024)
                val oos = ObjectOutputStream(baos)
                oos.writeUnshared(state)
                val dt = baos.toByteArray()
                val p = DatagramPacket(dt, dt.size, address)
                socket.send(p)
            }


            mouse.buttonUp.listen {
                val ee = data.activePoints zip data.activeFacultyColors.map { c -> Color(c.r, c.g, c.b) }
                send(EventObject(it.type, ee, qcam.zoom))
            }

            mouse.buttonDown.listen {
                val ee = data.activePoints zip data.activeFacultyColors.map { c -> Color(c.r, c.g, c.b) }
                send(EventObject(it.type, ee, qcam.zoom))
            }
        }

        extend(gui)
        extend(qcam)
        extend {
            gui.visible = false
            drawer.isolated {
                c.draw(drawer)
            }

            drawer.defaults()

            slider.draw(drawer)

            drawer.fill = ColorRGBa.WHITE
            drawer.text(data.activePoints.size.toString(), 10.0, 18.0)
            for((i, a) in data.activePoints.withIndex()) {
                drawer.fill = data.activeFacultyColors[i]
                drawer.text(a.toString(), 10.0, 13.0 * i + 36.0)
            }

            drawer.fill = null
            drawer.circle(drawer.bounds.center, 12.0)
        }
    }
}