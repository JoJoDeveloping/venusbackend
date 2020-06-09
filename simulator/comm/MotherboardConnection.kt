package venusbackend.simulator.comm

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korio.async.launch
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.net.createTcpClient
import com.soywiz.klogger.Logger
import com.soywiz.korio.async.delay
import venusbackend.simulator.ConnectionError
import venusbackend.simulator.SimulatorError
import venusbackend.simulator.comm.listeners.IConnectionListener
import venusbackend.simulator.comm.listeners.LoggingConnectionListener
import venusbackend.simulator.comm.listeners.ReadConnectionListener
import kotlin.coroutines.EmptyCoroutineContext

class MotherboardConnection(private val startAddress: Long, private val size: Int) : IConnection {
    private val connectionListeners: MutableList<IConnectionListener> = mutableListOf()
    private val readListener: ReadConnectionListener = ReadConnectionListener()
    private var logger: Logger = Logger("MotherboardConnection Logger")
    var isOn: Boolean = false
    var clientAsynch: AsyncClient? = null

    init {
        logger.output = object : Logger.Output {
            override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
                println("${logger.name}: $level: $msg")
            }
        }
        logger.level = Logger.Level.INFO
    }

    suspend fun connectToVMB(host: String, port: Int): Boolean {
        val client: AsyncClient
        try {
            client = createTcpClient(host, port, false)
        } catch (e: Exception) {
            println(e.message)
            return false
        }
        clientAsynch = client
        return true
    }

    private fun busyWait(timeout: Long = 1000, functionToBeExecuted: suspend () -> Unit) {
        var done = false
        val afterTimeout = DateTime.now() + timeout.milliseconds

        launch(EmptyCoroutineContext) {
            functionToBeExecuted()
        }.invokeOnCompletion {
            done = true
        }
        while (!done) {
            if (DateTime.now() >= afterTimeout) {
                break
            }
        }
    }

    override fun establishConnection(host: String, port: Int) {
        busyWait {
            connectToVMB(host, port)
        }

        if (clientAsynch == null || !clientAsynch!!.connected) {
            throw ConnectionError("Could not connect to the motherboard!")
        }

        connectionListeners.add(readListener)
        connectionListeners.add(LoggingConnectionListener())

        launch(EmptyCoroutineContext) {
            watchForMessages()
        }
        register()
        // Wait for 30 milliseconds so that the motherboard has time to send the power on/off signal
        busyWait {
            delay(TimeSpan(30.0))
        }
    }

    override suspend fun readByte(): Byte {
        val r = clientAsynch!!.read()
        if (r < 0) {
            throw ConnectionError("End of stream reached")
        }
        return (r and 0xFF).toByte()
    }

    fun getReadListener(): ReadConnectionListener {
        return readListener
    }

    suspend fun watchForMessages() {
        while (clientAsynch!!.connected) {
            try {
                dispatchMessage(Message().setup(this@MotherboardConnection))
            } catch (e: Exception) {
                println(e.message)
                return
            }
        }
    }

    /*
     * This message will only be called from the receiver thread.
     */
    private fun dispatchMessage(message: Message) {
        for (listener in connectionListeners) {
            if (message.isBusMessage) {
                when (message.id) {
                    Message.ID_POWERON -> {
                        isOn = true
                        listener.powerOn()
                    }
                    Message.ID_POWEROFF -> {
                        listener.powerOff()
                        isOn = false
                    }
                    Message.ID_INTERRUPT -> listener.interruptRequest(message.slot.toInt())
                    Message.ID_RESET -> listener.reset()
                    Message.ID_TERMINATE -> listener.terminate()
                    else -> logger.warn {
                        println("Unhandled message id ${message.id}")
                    }
                }
            } else {
                when (message.id) {
                    Message.ID_WRITETETRA -> listener.dataReceived((message.address - startAddress).toInt(), message.getPayload())
                    Message.ID_BYTEREPLY -> listener.readData(message)
                    Message.ID_WYDEREPLY -> listener.readData(message)
                    Message.ID_TETRAREPLY -> listener.readData(message)
                    Message.ID_READREPLY -> listener.readData(message)
                    else -> logger.warn {
                        println("Unhandled message id ${message.id}")
                    }
                }
            }
        }
    }

    private fun register() {
        launch(EmptyCoroutineContext) {
            clientAsynch!!.write(MessageFactory.createRegistrationMessage(startAddress, startAddress + size, -1L, "RISC-V CPU").toByteArray())
        }
    }

    fun send(message: Message) {
        if (isOn) {
            launch(EmptyCoroutineContext) {
                clientAsynch!!.write(message.toByteArray())
            }
        } else {
            throw SimulatorError("The motherboard is not on !")
        }
    }

    fun unregister() {}

    override fun addConnectionListener(listener: IConnectionListener?) {
        if (listener != null) {
            connectionListeners.add(listener)
        }
    }

    override suspend fun shutDown() {
        clientAsynch!!.close()
    }
}