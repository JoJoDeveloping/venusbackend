package venusbackend.simulator.comm

import venusbackend.simulator.comm.listeners.IConnectionListener
import venusbackend.simulator.comm.listeners.LoggingConnectionListener
import venusbackend.simulator.comm.listeners.ReadConnectionListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.CountDownLatch
import java.util.logging.Logger

class MotherboardConnection(private val startAddress: Long, private val size: Int) : IConnection {
    private var s: Socket? = null
    private var sa: SocketAddress? = null
    private var outputStream: OutputStream? = null
    private var `in`: InputStream? = null
    private val connectionListeners: MutableList<IConnectionListener> = mutableListOf()
    private val readListener : ReadConnectionListener = ReadConnectionListener()
    private var logger: Logger = Logger.getLogger(LoggingConnectionListener::class.java.toString())
    var countDownLatch : CountDownLatch = CountDownLatch(1)
    var isOn : Boolean = false


    @Throws(IOException::class)
    override fun establishConnection(address: InetAddress?, port: Int) {
        s = Socket()
        sa = InetSocketAddress(address, port)
        s!!.tcpNoDelay = true
        s!!.connect(sa)
        outputStream = s!!.getOutputStream()
        `in` = s!!.getInputStream()
        connectionListeners.add(readListener)
        connectionListeners.add(LoggingConnectionListener())
        register()
        Thread(object : Runnable {
            override fun run() {
                val logger: Logger = Logger.getLogger(this.toString())
                while (s!!.isConnected) {
                    try {
                        dispatchMessage(Message(this@MotherboardConnection))
                    } catch (e: IOException) {
                        logger.warning(e.toString())
                        return
                    }
                }
            }
        }).start()
    }

    /* (non-Javadoc)
     * @see venusbackend.simulator.comm.IConnection#readByte()
     */
    @Throws(IOException::class)
    override fun readByte(): Byte {
        val r = `in`?.read()
        if (r != null) {
            if (r < 0) {
                throw IOException("End of stream reached");
            }
            return (r and 0xFF).toByte()
        } else {
            throw IOException("End of stream reached");
        }
    }

    fun getReadListener() : ReadConnectionListener {
        return readListener
    }

    fun waitForMotherBoardPower() {
        if (countDownLatch.count == 1L){
            print("Power...")
            countDownLatch.await()
            println("ON")
            isOn = true
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
                        countDownLatch.countDown()
                        isOn = true
                        listener.powerOn()
                    }
                    Message.ID_POWEROFF -> {
                        listener.powerOff()
                        isOn = false
                        countDownLatch = CountDownLatch(1)
                    }
                    Message.ID_INTERRUPT -> listener.interruptRequest(message.slot.toInt())
                    Message.ID_RESET -> listener.reset()
                    Message.ID_TERMINATE -> listener.terminate()
                    else -> logger.warning("unhandeled message id " + message.id)
                }
            } else {
                when (message.id) {
                    Message.ID_WRITETETRA -> listener.dataReceived((message.address - startAddress).toInt(), message.getPayload())
                    Message.ID_BYTEREPLY -> listener.readData(message)
                    Message.ID_WYDEREPLY -> listener.readData(message)
                    Message.ID_TETRAREPLY -> listener.readData(message)
                    Message.ID_READREPLY -> listener.readData(message)
                    else -> logger.warning("unhandeled message id " + message.id)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun register() {
        send(MessageFactory.createRegistrationMessage(startAddress, startAddress + size, -1L, "RISC-V CPU"))
    }

    @Throws(IOException::class)
    fun send(message: Message) {
        outputStream?.write(message.toByteArray())
    }

    fun unregister() {}

    override fun addConnectionListener(listener: IConnectionListener?) {
        if (listener != null) {
            connectionListeners.add(listener)
        }
    }

    fun removeConnectionListener(listener: IConnectionListener) {
        connectionListeners.remove(listener)
    }


    @Throws(IOException::class)
    override fun shutDown() {
        s?.close()
    }
}