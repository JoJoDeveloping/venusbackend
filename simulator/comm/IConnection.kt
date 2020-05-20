package venusbackend.simulator.comm

import venusbackend.simulator.comm.listeners.IConnectionListener
import java.io.IOException
import java.net.InetAddress
import kotlin.jvm.Throws

interface IConnection {
    @Throws(IOException::class)
    fun establishConnection(address: InetAddress?, port: Int)

    @Throws(IOException::class)
    fun readByte(): Byte
    fun addConnectionListener(listener: IConnectionListener?)

    @Throws(IOException::class)
    fun shutDown()
}