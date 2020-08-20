package venusbackend.simulator.comm

import com.soywiz.klogger.Logger
import com.soywiz.korio.async.launch
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.net.ws.readBinary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import venusbackend.simulator.SimulatorState
import venusbackend.simulator.comm.listeners.IConnectionListener
import venusbackend.simulator.comm.listeners.ReadConnectionListener
import kotlin.coroutines.EmptyCoroutineContext

class MotherboardConnection(private val startAddress: Long, private val size: Int, private val simulatorState: SimulatorState) : IConnection {
    companion object {
        val connectionListeners: MutableList<IConnectionListener> = mutableListOf()
        private val readListener: ReadConnectionListener = ReadConnectionListener()
        private var logger: Logger = Logger("MotherboardConnection Logger")
        val context = EmptyCoroutineContext
        private val messageChannel = Channel<Message>(Channel.UNLIMITED)
        var isOn: Boolean = false
        var isConnected = false
        var webSocketClient: WebSocketClient? = null
        private val connectionSemaphore = Semaphore(1)
    }

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
            logger.fatal {
                "Could not connect to motherboard. Error message: ${e.message}"
            }
            false
        }
    }

    override suspend fun establishConnection(host: String, port: Int) {
        println("Establishing connection...")
        val connected = connectToVMB(host, port)
        if (!connected) {
            println("Could not connect to the motherboard!")
        } else {
            isConnected = true
            println("Connected!")
        }
        connectionListeners.add(readListener)
        register()
    }

    fun getReadListener(): ReadConnectionListener {
        return readListener
    }

    suspend fun watchForMessages() {
        launch(Dispatchers.Default){
            dispatchAll()
        }
        while (true) {
            try {
                val payload = webSocketClient!!.readBinary()
                val message = Message().setup(payload)
                launch(Dispatchers.Default) {
                    messageChannel.send(message)
                }
            } catch (e: Exception) {
                logger.fatal {
                    "Error trying to read the message: ${e.message}"
                }
                return
            }
        }
    }

    suspend fun dispatchAll() {
        while (true){
            val message : Message = messageChannel.receive()
            launch(Dispatchers.Default) {
                dispatchMessage(message)
            }
        }
    }

    suspend fun dispatchMessage(message: Message) {
        connectionSemaphore.acquire()
        for (listener in connectionListeners) {
            if (message.isBusMessage) {
                when (message.id) {
                    Message.ID_POWERON -> {
                        isOn = true
                        println("ON")
                        listener.powerOn()
                    }
                    Message.ID_POWEROFF -> {
                        listener.powerOff()
                        isOn = false
                    }
                    Message.ID_INTERRUPT -> {
                        listener.interruptRequest(message, simulatorState)
                    }
                    Message.ID_RESET -> listener.reset()
                    Message.ID_TERMINATE -> listener.terminate()
                    else -> logger.warn {
                        println("Unhandled bus message id ${message.id}")
                    }
                }
            } else {
                when (message.id) {
                    Message.ID_WRITETETRA -> listener.dataReceived((message.address - startAddress).toInt(), message.getPayload())
                    Message.ID_BYTEREPLY -> {
                        listener.readData(message)
                    }
                    Message.ID_WYDEREPLY -> {
                        listener.readData(message)
                    }
                    Message.ID_TETRAREPLY -> {
                        listener.readData(message)
                    }
                    Message.ID_READREPLY -> {
                        listener.readData(message)
                    }
                    else -> logger.warn {
                        println("Unhandled message id ${message.id}")
                    }
                }
            }
        }
        connectionSemaphore.release()
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