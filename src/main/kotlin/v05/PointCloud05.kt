package v05

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle
import v05.filters.FilterSet
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread


const val IDLE = 1
const val NAVIGATE = 2
data class EventObject(val screenMode: Int, val articleIndexes: List<Int>, val zoom: Double, val filterSet: FilterSet = FilterSet.EMPTY) : Serializable


fun main() = application {
    val appMode = when (val mode=System.getProperty("appMode")) {
        "production" -> AppMode.Production
        "prototype" -> AppMode.Prototype
        else -> AppMode.Debug
    }
    configure {
        when (appMode) {
            AppMode.Debug -> {

            }
            AppMode.Prototype -> {
                hideWindowDecorations = true
                windowAlwaysOnTop = true
            }
            AppMode.Production -> {
                hideWindowDecorations = true
                windowAlwaysOnTop = true
            }
        }
        width = 1280
        height = 1024
    }
    program {
        val ipAddress = System.getProperty("screenIP") ?: "192.168.1.158"

        val data = DataModel(Rectangle.fromCenter(drawer.bounds.center, height * 1.0, height * 1.0))
        val state = State(data)
        val pc = viewBox(drawer.bounds) { pc05(data, state) }

        val sendChannel = Channel<EventObject>(10000)

        thread(isDaemon = true) {
            val socket = DatagramSocket()
            val address = InetSocketAddress(InetAddress.getByName(ipAddress), 9002)
            socket.soTimeout = 10


            fun send(state: EventObject) {
                val baos = ByteArrayOutputStream(1024)
                val oos = ObjectOutputStream(baos).use {
                    it.writeUnshared(state)
                    val dt = baos.toByteArray()
                    val p = DatagramPacket(dt, dt.size, address)
                    socket.send(p)
                }
            }

            while (true) {
                runBlocking {
                    println("sending to ${ipAddress}:9002")
                    val eo = sendChannel.receive()
                    send(eo)
                }
            }


        }
        state.changed.listen {

            val indices: List<Int> = state.activePoints.map { data.pointsToArticleIndices[it.key]!! }
            println("total number of indicies: ${indices.size}")
            runBlocking {
                sendChannel.send(EventObject(if (state.idle) IDLE else NAVIGATE, indices, state.zoom, state.filterSet))
            }
            //println("this is the filterset: ${state.filterSet.faculties}")
            //send()
            //Thread.sleep(100)
        }

        extend {
            pc.draw()

            drawer.circles(pointers.pointers.map { it.position }, 10.0)
        }
    }
}