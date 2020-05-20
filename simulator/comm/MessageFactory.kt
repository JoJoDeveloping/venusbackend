package venusbackend.simulator.comm

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
        return createReadMessage(address, Message.ID_READ)
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
            MemSize.HALF -> message.appendToPayload(value.toInt() shl 16)
            MemSize.WORD -> message.appendToPayload(value.toInt())
            MemSize.LONG -> message.appendToPayload(value.toLong())
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