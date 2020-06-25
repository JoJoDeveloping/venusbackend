package venusbackend.simulator.comm

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korio.async.launch
import com.soywiz.klogger.Logger
import com.soywiz.korio.async.delay
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.net.ws.readString
import com.soywiz.korio.util.encoding.Base64
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
    var webSocketClient: WebSocketClient? = null

    init {
        logger.output = object : Logger.Output {
            override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
                println("${logger.name}: $level: $msg")
            }
        }
        logger.level = Logger.Level.INFO
    }

    suspend fun connectToVMB(host: String, port: Int): Boolean {
        try {
            webSocketClient = WebSocketClient("ws://$host:$port")
            val message = "{\"command\": \"connect\", \"host\": \"127.0.0.1\", \"port\":9002}"
            webSocketClient!!.send(message)
            val response = webSocketClient!!.readString()
            val regex = Regex("\"success\": true")
            return regex.containsMatchIn(response)
        } catch (e: Exception) {
            logger.fatal { e.message }
            return false
        }
    }

    private fun busyWait(timeout: Long = 1000, functionToBeExecuted: suspend () -> Unit) {
        var done = false
        val afterTimeout = DateTime.now() + timeout.toDouble().milliseconds
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
        var isConnected = false
        busyWait {
            isConnected = connectToVMB(host, port)
        }
        if (!isConnected) {
            throw ConnectionError("Could not connect to the motherboard!")
        }
        connectionListeners.add(readListener)
        connectionListeners.add(LoggingConnectionListener())

        launch(EmptyCoroutineContext) {
            watchForMessages()
        }
        register()
        // Wait for 30 milliseconds so that the motherboard has time to send the power on/off signal
        busyWait(100) {
            delay(TimeSpan(100.0))
        }
    }

    /*override suspend fun readByte(): Byte {
        val r = clientAsynch!!.read()
        if (r < 0) {
            throw ConnectionError("End of stream reached")
        }
        return (r and 0xFF).toByte()
    }*/

    fun getReadListener(): ReadConnectionListener {
        return readListener
    }

    suspend fun watchForMessages() {
        while (true) {
            try {
                val rcv = webSocketClient!!.readString()
                val successRegex = Regex("\"success\": true")
                val dataRegex = Regex("\"recv data\"")
                if (successRegex.containsMatchIn(rcv) && dataRegex.containsMatchIn(rcv)) {
                    var tmp = rcv.replace(Regex("\\{.*\"data\": "), "")
                    tmp = tmp.replace("\"", "")
                    tmp = tmp.replace("}", "")
                    val payload = Base64.decode(tmp)
                    val message = Message()
                    message.setType(payload[0])
                    message.size = payload[1]
                    message.slot = payload[2]
                    message.id = payload[3]
                    if (message.hasTimeStamp()) {
                        message.readTimeStampFromByte(copyOfRange(4, 8, payload))
                    }
                    if (!message.hasTimeStamp() && message.hasAddress()) {
                        message.readAddressFromByte(payload[4])
                    }
                    if (message.hasTimeStamp() && message.hasAddress()) {
                        message.readAddressFromByte(payload[5])
                    }
                    if (message.hasPayload() && !message.hasTimeStamp()) {
                        message.readPayloadFromArray(copyOfRange(12, payload.size - 1, payload))
                    } else if (message.hasPayload() && message.hasTimeStamp()) {
                        message.readPayloadFromArray(copyOfRange(16, payload.size - 1, payload))
                    }
                    dispatchMessage(message)
                }
            } catch (e: Exception) {
                println(e.message)
                return
            }
        }
    }

    fun copyOfRange(begin: Int, end: Int, array: ByteArray): ByteArray {
        var result: ByteArray = byteArrayOf()
        if (begin < 0 || end >= array.size) {
            return result
        }
        result = ByteArray(end - begin + 1)
        for ((i, x) in (begin..end).withIndex()) {
            result[i] = array[x]
        }
        return result
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
        send(MessageFactory.createRegistrationMessage(startAddress, startAddress + size, -1L, "RISC-V CPU"), true)
    }

    fun send(message: Message, register: Boolean = false) {
        if (isOn || register) {
            val reg = message.toByteArray()
            val base64Message = Base64.encode(message.toByteArray())
            var mess = ""
            for (i in reg.indices) {
                mess += reg[i].toUByte()
                if (i != reg.size - 1) {
                    mess += ";"
                }
            }
            launch(EmptyCoroutineContext) {
                webSocketClient!!.send("{\"command\": \"sendb\", \"data\": \"$base64Message\"}")
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
        webSocketClient!!.close()
    }
}