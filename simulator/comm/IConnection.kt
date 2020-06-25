package venusbackend.simulator.comm

import venusbackend.simulator.comm.listeners.IConnectionListener

interface IConnection {
    fun establishConnection(host: String, port: Int)

    // suspend fun readByte(): Byte
    fun addConnectionListener(listener: IConnectionListener?)

    suspend fun shutDown()
}