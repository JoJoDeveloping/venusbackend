package venusbackend.simulator

import com.soywiz.klock.TimeSpan
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.withTimeout
import com.soywiz.korio.net.ws.readBinary
import venusbackend.and
import venusbackend.riscv.MemSize
import venusbackend.simulator.comm.Message
import venusbackend.simulator.comm.MessageFactory
import venusbackend.simulator.comm.MotherboardConnection
import kotlin.coroutines.EmptyCoroutineContext

class MemoryVMB(private val connection: MotherboardConnection) : Memory {

    /**
     * It "removes" a byte by zero-ing it.
     * @param address The address of the Byte to be removed
     */
    override fun removeByte(address: Number) {
        storeByte(address, 0.toByte())
    }

    override suspend fun loadByte(address: Number): Int {
        return read(address, MemSize.BYTE).toInt().and(0xff)
    }

    override suspend fun loadHalfWord(addr: Number): Int {
        return read(addr, MemSize.HALF).toInt().and(0xffff)
    }

    override suspend fun loadWord(addr: Number): Int {
        return read(addr, MemSize.WORD).toInt()
    }

    override suspend fun loadLong(addr: Number): Long {
        return read(addr, MemSize.LONG).toLong()
    }

    /**
     * Checks if the given number fits in a byte and writes the byte in the memory.
     * If the number doesn't fit in a byte, it throws a NumberNotValidError.
     */
    override fun storeByte(addr: Number, value: Number) {
        val translatedAddress = translate(addr)
        val tmp = value and 0xFF
        val message = MessageFactory.createWriteByteMessage(translatedAddress, tmp.toByte())
        launch(EmptyCoroutineContext) {
            connection.send(message)
        }
    }

    override fun storeHalfWord(addr: Number, value: Number) {
        if (translate(addr) % MemSize.HALF.size != 0L) {
            throw AlignmentError()
        }
        val translatedAddress = translate(addr)
        val tmp = value and 0xFFFF
        val message = MessageFactory.createWriteHalfMessage(translatedAddress, tmp.toInt())
        launch(EmptyCoroutineContext) {
            connection.send(message)
        }
    }

    override fun storeWord(addr: Number, value: Number) {
        val translatedAddress = translate(addr)
        var tmp = value
        if (value is Long) {
            tmp = value and 0xFFFFFFFFL
        }
        val message = MessageFactory.createWriteWordMessage(translatedAddress, tmp.toInt())
        launch(EmptyCoroutineContext) {
            connection.send(message)
        }
    }

    override fun storeLong(addr: Number, value: Number) {
        val translatedAddress = translate(addr)
        val message = MessageFactory.createWriteLongMessage(translatedAddress, value.toLong())
        launch(EmptyCoroutineContext) {
            connection.send(message)
        }
    }

    /**
     * Dummy translate function for the future
     * TODO: Implement so that it really translates the address
     */
    private fun translate(addr: Number): Long {
        return addr.toLong() or 0x0000_0001_0000_0000 // and 0x0FFFFFFF or 0x100000000 //0000000100000000
    }

    /**
     * It loads a byte from the memory and returns it as a number.
     * The function waits for the response from the motherboard for 1 second, if no response is given it throws an SimulatorError
     * @param address the address of the byte
     * @return the data at the given address
     */
    private suspend fun read(address: Number, size: MemSize): Number {
        val translatedAddress = translate(address)
        var message: Message? = null
        when (size) {
            MemSize.BYTE -> message = MessageFactory.createReadByteMessage(translatedAddress)
            MemSize.HALF -> message = MessageFactory.createReadHalfMessage(translatedAddress)
            MemSize.WORD -> message = MessageFactory.createReadWordMessage(translatedAddress)
            MemSize.LONG -> message = MessageFactory.createReadLongMessage(translatedAddress)
        }
        if (message == null) {
            throw SimulatorError()
        }
        if (size != MemSize.LONG) {
            message.finalizePayloadAndSize()
        }
        val listener = connection.getReadListener()
        launch(connection.context) {
            connection.send(message)
        }
        var payload: ByteArray = byteArrayOf()
        try {
            withTimeout(TimeSpan(1000.0)) {
                payload = connection.webSocketClient!!.readBinary()
            }
        } catch (e: Exception) {
            println(e.message)
        }

        val messageFromVMB = Message().setup(payload)
        listener.readData(messageFromVMB)
        var tmp: Number? = null
        when (size) {
            MemSize.BYTE -> {
                tmp = listener.byte
                listener.byte = null
            }
            MemSize.HALF -> {
                tmp = listener.half
                listener.half = null
            }
            MemSize.WORD -> {
                tmp = listener.word
                listener.word = null
            }
            MemSize.LONG -> {
                tmp = listener.long
                listener.long = null
            }
        }
        if (tmp == null) {
            throw SimulatorError("The motherboard didn't answer in time! Please check the connection to the motherboard!")
        }
        return tmp
    }
}