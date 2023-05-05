import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.MouseEventType
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

const val divider = 4

fun main() = application {
    configure {
        width = 1920 / divider
        height = 1080 / divider
        hideWindowDecorations = true
        windowAlwaysOnTop = true
        position = IntVector2(1200, 250)
    }
    program {

        val points = csvReader().readAllWithHeader(File("data/data-umap-highlight-v1.csv")).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }.toSphere()

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
            },
            points.size
        ).apply {
            put {
                for (position in points) {
                    write(position)
                }
            }
        }


        val ss = shadeStyle {
            vertexTransform = """
                    vec3 voffset = (x_viewMatrix * vec4(i_offset, 1.0)).xyz;
                    x_viewMatrix = mat4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
                    float size = 0.05;
                    x_position.xyz *= vec3(size - 0.01, size + 0.015, size);
                    x_position.xyz += voffset;
                """.trimIndent()
        }
        val orb = Orbital().apply {
            eye = Vector3.UNIT_Z * -14.0
        }



        thread {
            val socket = DatagramSocket()
            val address = InetSocketAddress(InetAddress.getByName("192.168.1.103"), 9002)

            fun send(state: MouseEventType) {
                val baos = ByteArrayOutputStream(1024)
                val oos = ObjectOutputStream(baos)
                oos.writeUnshared(state)
                val data = baos.toByteArray()
                val p = DatagramPacket(data, data.size, address)
                socket.send(p)
            }

            mouse.buttonUp.listen {
                launch {
                    send(it.type)
                }
            }

            mouse.buttonDown.listen {
                launch {
                    send(it.type)
                }
            }
        }


        extend(orb)
        extend {

            drawer.view = orb.camera.viewMatrix()
            drawer.perspective(90.0, width*1.0/height*1.0, 0.1, 100.0)

            drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
            drawer.depthWrite = true

            drawer.shadeStyle = ss
            drawer.vertexBufferInstances(listOf(quad), listOf(offsets), DrawPrimitive.TRIANGLE_STRIP, offsets.vertexCount)



        }
    }
}


fun List<Vector2>.toSphere(radius: Double = 10.0): List<Vector3> {
    val llbounds = Rectangle(-240.0, 10.0, 480.0, 160.0) // ?
    val latlon = map(bounds, llbounds)


    return latlon.map { Spherical(it.x, it.y, radius).cartesian }
}
