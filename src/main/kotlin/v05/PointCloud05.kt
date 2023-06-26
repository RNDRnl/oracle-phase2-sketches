package v05

import org.openrndr.application
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

data class EventObject(val articleIndexes: List<Int>, val zoom: Double): Serializable

fun main() = application {
    configure {
        width = 1280
        height = 1024
    }
    program {

        val ipAddress = "192.168.1.62"

        val data = DataModel(Rectangle.fromCenter(drawer.bounds.center, height * 1.0, height * 1.0))
        val pc = viewBox(drawer.bounds) { pc05(data) }

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

            data.changed.listen {
                println("sending")
                val indices = data.activePoints.map { data.articles.indexOf(it.value) }
                send(EventObject(indices, data.zoom))
            }
        }

        extend {
            pc.draw()
        }
    }
}