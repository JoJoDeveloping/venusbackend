package venusbackend.simulator.comm

import venusbackend.toHex
import kotlin.experimental.and

class Message {
    private var type: Byte = 0
    var size: Byte = 0
    var slot: Byte = 0
    var id: Byte = 0
    var timeStamp = 0
        private set

    /**
     * @return the address
     */
    var address: Long = 0
    private val payload: ArrayList<Byte?> = ArrayList()

    fun setup(payload: ByteArray): Message {
        type = payload[0]
        size = payload[1]
        slot = payload[2]
        id = payload[3]
        if (hasTimeStamp()) {
            readTimeStampFromByte(copyOfRange(4, 8, payload))
        }
        if (!hasTimeStamp() && hasAddress()) {
            readAddressFromByte(copyOfRange(4, 11, payload))
            // println("1. Response from address: ${toHex(address)}")
        }
        if (hasTimeStamp() && hasAddress()) {
            readAddressFromByte(copyOfRange(5, 12, payload))
            // println("2. Response from address: ${toHex(address)}")
        }
        if (hasPayload() && !hasTimeStamp() && !hasAddress()) {
            readPayloadFromArray(copyOfRange(4, payload.size - 1, payload))
        } else if (hasPayload() && !hasTimeStamp() && hasAddress()) {
            readPayloadFromArray(copyOfRange(12, payload.size - 1, payload))
        } else if (hasPayload() && hasTimeStamp() && hasAddress()) {
            readPayloadFromArray(copyOfRange(16, payload.size - 1, payload))
        }
        return this
    }

    private fun copyOfRange(begin: Int, end: Int, array: ByteArray): ByteArray {
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

    private fun readPayloadFromArray(array: ByteArray) {
        for (i in 0 until payloadSize()) {
            payload.add(array[i])
        }
    }

    private fun readAddressFromByte(bytes: ByteArray) {
        address = 0
        for (i in 0..7) {
            address = (address shl 8) + (bytes[i].toLong() and 0xffL)
        }
    }

    private fun readTimeStampFromByte(bytes: ByteArray) {
        timeStamp = 0
        for (i in 0..3) {
            timeStamp = (timeStamp shl 8) + bytes[i]
        }
    }

    val isBusMessage: Boolean
        get() = type and TYPE_BUS != 0.toByte()

    fun hasTimeStamp(): Boolean {
        return type and TYPE_TIME != 0.toByte()
    }

    fun hasAddress(): Boolean {
        return type and TYPE_ADDRESS != 0.toByte()
    }

    fun hasRoute(): Boolean {
        return type and TYPE_ROUTE != 0.toByte()
    }

    fun hasPayload(): Boolean {
        return type and TYPE_PAYLOAD != 0.toByte()
    }

    fun setType(type: Byte) {
        this.type = type
    }

    val isRequest: Boolean
        get() = type and 0x04 != 0.toByte()

    fun hasLock(): Boolean {
        return type and 0x02 != 0.toByte()
    }

    fun toByteArray(): ByteArray {
        var arraySize = 4
        if (hasTimeStamp()) {
            arraySize += 4
        }
        if (hasAddress()) {
            arraySize += 8
        }
        if (hasPayload()) {
            arraySize += payloadSize()
        }
        val message: ArrayList<Byte?> = ArrayList()
        message.add(type)
        message.add(size)
        message.add(slot)
        message.add(id)
        if (hasTimeStamp()) {
            append(timeStamp, message)
        }
        if (hasAddress()) {
            append(address, message)
        }
        if (hasPayload()) {
            append(payload, message)
        }
        val m = ByteArray(message.size)
        var i = 0
        for (b in message) {
            if (b != null) {
                m[i++] = b
            }
        }
        return m
    }

    private fun payloadSize(): Int {
        return (size + 1) * 8
    }

    fun appendToPayload(l: Long) {
        append(l, payload)
    }

    fun appendToPayload(i: Int) {
        append(i, payload)
    }

    fun appendToPayload(list: ArrayList<Byte?>) {
        append(list, payload)
    }

    fun appendToPayload(b: Byte) {
        payload.add(b)
    }

    fun appendToPayload(s: String) {
        for (i in s.indices) {
            payload.add((s[i].toInt() and 0xFF).toByte())
        }
    }

    fun setAutoSize() {
        size = payload.size.toByte()
    }

    fun finalizePayloadAndSize() {
        while (payload.size % 8 != 0) {
            payload.add(0.toByte())
        }
        size = (payload.size / 8 - 1).toByte()
    }

    fun getPayload(): ByteArray {
        val pl = ByteArray(payloadSize())
        for (i in 0 until payloadSize()) {
            pl[i] = payload[i]!!
        }
        return pl
    }

    companion object {
        const val TYPE_BUS = 0x80.toByte()
        const val TYPE_TIME = 0x40.toByte()
        const val TYPE_ADDRESS = 0x20.toByte()
        const val TYPE_ROUTE = 0x10.toByte()
        const val TYPE_PAYLOAD = 0x08.toByte()
        const val TYPE_REQUEST = 0x04.toByte()
        const val TYPE_LOCK = 0x02.toByte()
        const val ID_TERMINATE = 0xF9.toByte()
        const val ID_REGISTER = 0xFA.toByte()
        const val ID_UNREGISTER = 0xFB.toByte()
        const val ID_INTERRUPT = 0xFC.toByte()
        const val ID_RESET = 0xFD.toByte()
        const val ID_POWEROFF = 0xFE.toByte()
        const val ID_POWERON = 0xFF.toByte()
        const val ID_IGNORE = 0.toByte()
        const val ID_READ = 1.toByte()
        const val ID_WRITE = 2.toByte()
        const val ID_READREPLY = 3.toByte()
        const val ID_NOREPLY = 4.toByte()
        const val ID_READBYTE = 5.toByte()
        const val ID_READWYDE = 6.toByte()
        const val ID_READTETRA = 7.toByte()
        const val ID_WRITEBYTE = 8.toByte()
        const val ID_WRITEWYDE = 9.toByte()
        const val ID_WRITETETRA = 10.toByte()
        const val ID_BYTEREPLY = 11.toByte()
        const val ID_WYDEREPLY = 12.toByte()
        const val ID_TETRAREPLY = 13.toByte()
        fun append(value: Int, a: ArrayList<Byte?>) {
            var valueTmp = value
            for (i in 3 downTo 0) {
                a.add((value ushr 8 * i and 0xFF).toByte())
                valueTmp = valueTmp ushr 8
            }
        }

        fun append(value: Long, a: ArrayList<Byte?>) {
            for (i in 7 downTo 0) {
                a.add((value ushr 8 * i and 0xFF).toByte())
            }
        }

        fun append(value: ArrayList<Byte?>, a: ArrayList<Byte?>) {
            for (i in 0 until value.size) {
                a.add(value[i])
            }
        }
    }
}