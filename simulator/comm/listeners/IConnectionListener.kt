package venusbackend.simulator.comm.listeners

import venusbackend.simulator.SimulatorState
import venusbackend.simulator.comm.Message

abstract class IConnectionListener {
    open fun dataReceived(offset: Int, payload: ByteArray?) {}
    open suspend fun readData(message: Message) {}
    open fun powerOn() {}
    open fun powerOff() {}
    open fun reset() {}
    open fun readRequest() {}
    open fun writeRequest() {}
    open suspend fun interruptRequest(message: Message, simulatorState: SimulatorState) {}
    open fun terminate() {}
}