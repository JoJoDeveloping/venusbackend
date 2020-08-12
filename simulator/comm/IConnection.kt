package venusbackend.simulator.comm

import venusbackend.simulator.comm.listeners.IConnectionListener

interface IConnection {
    suspend fun establishConnection(host: String, port: Int)
    fun addConnectionListener(listener: IConnectionListener?)
    suspend fun shutDown()
}
