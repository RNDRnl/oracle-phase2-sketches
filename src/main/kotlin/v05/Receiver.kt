package v05

import org.openrndr.events.Event
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.net.*

class Receiver {
    val stateReceived = Event<EventObject>()
    val quit = false

    val networkInterface = NetworkInterface.getNetworkInterfaces().toList().find {
        !it.isVirtual && it.isUp && !it.isVirtual && it.supportsMulticast() && it.interfaceAddresses.find {
            it.address.hostAddress.contains("192.168.1")
            //true
        }!= null }.also { println(it) }

    fun work() {

        val serverSocket: DatagramSocket
        try {
            val addr = networkInterface!!.inetAddresses.nextElement()
            serverSocket = DatagramSocket(InetSocketAddress(addr, 9002)).also { println(addr) }

        } catch (e1: SocketException) {
            e1.printStackTrace()
            return
        }

        val receiveData = ByteArray(65536)

        while (!quit) {
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            try {
                serverSocket.receive(receivePacket)
                val `is` = ByteArrayInputStream(receiveData)
                val ois = ObjectInputStream(`is`)
                val `object` = ois.readObject()

                if (`object` is EventObject) {
                    stateReceived.trigger((`object`))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
    }

}