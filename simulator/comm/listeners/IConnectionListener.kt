package venusbackend.simulator.comm.listeners

import venusbackend.simulator.comm.Message

abstract class IConnectionListener {
    open fun dataReceived(offset: Int, payload: ByteArray?) {}
    open fun readData(message: Message?) {}
    open fun powerOn() {}
    open fun powerOff() {}
    open fun reset() {}
    open fun readRequest() {}
    open fun writeRequest() {}
    open fun interruptRequest(irqNumber: Int) {}
    open fun terminate() {}
}