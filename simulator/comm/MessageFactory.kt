package venusbackend.simulator.comm

import venusbackend.and
import venusbackend.riscv.MemSize
import kotlin.experimental.or

object MessageFactory {
    fun createRegistrationMessage(address: Long, limit: Long, mask: Long, name: String?): Message {
        val message = Message()
        message.setType((Message.TYPE_BUS or Message.TYPE_PAYLOAD))
        message.appendToPayload(address)
        message.appendToPayload(limit)
        message.appendToPayload(mask)
        if (name != null) {
            message.appendToPayload(name)
        }
        message.finalizePayloadAndSize()
        message.id = Message.ID_REGISTER
        return message
    }

    fun createReadByteMessage(address: Long) : Message {
        return createReadMessage(address, Message.ID_READBYTE)
    }

    fun createReadHalfMessage(address: Long) : Message {
        return createReadMessage(address, Message.ID_READWYDE)
    }

    fun createReadWordMessage(address: Long) : Message {
        return createReadMessage(address, Message.ID_READTETRA)
    }

    fun createReadLongMessage(address: Long) : Message {
        val message = createReadMessage(address, Message.ID_READ)
        message.size = 0
        return message
    }

    fun createWriteByteMessage(address: Long, byte : Byte) : Message {
        return createWriteMessage(address, byte, Message.ID_WRITEBYTE, MemSize.BYTE)
    }

    fun createWriteHalfMessage(address: Long, wyde : Int) : Message {
        return createWriteMessage(address, wyde, Message.ID_WRITEWYDE, MemSize.HALF)
    }

    fun createWriteWordMessage(address: Long, tetra : Int) : Message {
        return createWriteMessage(address, tetra, Message.ID_WRITETETRA, MemSize.WORD)
    }

    fun createWriteLongMessage(address: Long, octa : Long) : Message {
        return createWriteMessage(address, octa, Message.ID_WRITE, MemSize.LONG)
    }

    private fun createWriteMessage(address: Long, value : Number, id : Byte, size : MemSize) : Message {
        val message = Message()
        message.setType((Message.TYPE_ADDRESS or Message.TYPE_PAYLOAD))
        when(size) {
            MemSize.BYTE -> message.appendToPayload(value.toByte())
            MemSize.HALF -> {
                val lsb = (value and 0xFF).toByte()
                val msb = ((value.toInt() shr 8) and 0xFF).toByte()
                val half = (((0 or lsb.toInt()) shl 8) or msb.toInt()) shl 16
                message.appendToPayload(half)

            }
            MemSize.WORD -> {
                var word = 0
                for (index in 0..2) {
                    word = word or ((value.toInt() shr index*8) and 0xFF)
                    word = word shl 8
                }
                message.appendToPayload(word)
            }
            MemSize.LONG -> {
                var long = 0L
                for (index in 0..6) {
                    long = long or ((value.toLong() shr index*8) and 0xFF)
                    long = long shl 8
                }
                message.appendToPayload(long)
            }
        }
        message.finalizePayloadAndSize()
        message.id = id
        message.address = address
        return message
    }

    private fun createReadMessage(address: Long, id: Byte) : Message {
        val message = Message()
        message.setType((Message.TYPE_ADDRESS or Message.TYPE_REQUEST))
        message.id = id
        message.address = address
        return message
    }
}