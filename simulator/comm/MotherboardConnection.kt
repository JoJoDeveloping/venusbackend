package venusbackend.simulator.comm

import com.soywiz.klogger.Logger
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.net.ws.readBinary
import venusbackend.simulator.comm.listeners.IConnectionListener
import venusbackend.simulator.comm.listeners.LoggingConnectionListener
import venusbackend.simulator.comm.listeners.ReadConnectionListener
import kotlin.coroutines.EmptyCoroutineContext

class MotherboardConnection(private val startAddress: Long, private val size: Int) : IConnection {
    private val connectionListeners: MutableList<IConnectionListener> = mutableListOf()
    private val readListener: ReadConnectionListener = ReadConnectionListener()
    private var logger: Logger = Logger("MotherboardConnection Logger")
    val context = EmptyCoroutineContext
    var isOn: Boolean = false
    var webSocketClient: WebSocketClient? = null

    init {
        logger.output = object : Logger.Output {
            override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
                println("${logger.name}: $level: $msg")
            }
        }
        logger.level = Logger.Level.INFO
    }

    private suspend fun connectToVMB(host: String, port: Int): Boolean {
        return try {
            webSocketClient = WebSocketClient("ws://$host:$port")
            true
        } catch (e: Exception) {
            logger.fatal { e.message }
            false
        }
    }

    override suspend fun establishConnection(host: String, port: Int) {
        println("Establishing connection...")
        val isConnected = connectToVMB(host, port)
        println("Done!")
        if (!isConnected) {
            println("Could not connect to the motherboard!")
        }
        connectionListeners.add(readListener)
        connectionListeners.add(LoggingConnectionListener())
        register()
    }

    fun getReadListener(): ReadConnectionListener {
        return readListener
    }

    suspend fun watchForMessages() {
        while (true) {
            try {
                val payload = webSocketClient!!.readBinary()
                val message = Message().setup(payload)
                dispatchMessage(message)
            } catch (e: Exception) {
                logger.fatal {
                    e.message
                }
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

    private suspend fun register() {
        send(MessageFactory.createRegistrationMessage(startAddress, startAddress + size, -1L, "RISC-V CPU"), true)
    }

    suspend fun send(message: Message, register: Boolean = false) {
        if (isOn || register) {
            val messageToBeSent = message.toByteArray()
            try {
                webSocketClient!!.send(messageToBeSent)
            } catch (e: Exception) {
                logger.fatal {
                    "Could not send message error: ${e.message}"
                }
            }
        } else {
            println("The motherboard is not on !")
        }
    }

    fun unregister() {}

    override fun addConnectionListener(listener: IConnectionListener?) {
        if (listener != null) {
            connectionListeners.add(listener)
        }
    }

    override suspend fun shutDown() {
        webSocketClient!!.close()
    }
}