package venusbackend.simulator.comm.listeners

import com.soywiz.klock.TimeSpan
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.withTimeout
import kotlinx.coroutines.channels.Channel
import venusbackend.and
import venusbackend.simulator.comm.Message

class ReadConnectionListener : IConnectionListener() {
    var byte: Int? = null
    var half: Int? = null
    var word: Int? = null
    var long: Long? = null
    //private val readChannel = Channel<Message>(Channel.RENDEZVOUS)
    private val readSignal = Signal<Boolean>()

    suspend fun waitForReadResponse() {
        withTimeout(TimeSpan(10000.0)) {
            //receivedMessage = readChannel.receive()
            readSignal.waitOneBase()
        }
    }

    override suspend fun readData(message: Message) {
        //readChannel.send(message)
        setData(message)
        readSignal(true)
    }
    /**
     * Receives a byte in the payload and saves it in the b variable.
     */
    private fun setData(message: Message?) {
        if (message != null) {
            byte = 0
            half = 0
            word = 0
            long = 0
            when (message.id) {
                Message.ID_BYTEREPLY -> {
                    byte = (message.getPayload()[0] and 0xFF).toInt()
                }
                Message.ID_WYDEREPLY -> half = ((message.getPayload()[1] and 0xFF).toInt() shl 8) or (message.getPayload()[0] and 0xFF).toInt()
                Message.ID_TETRAREPLY -> {
                    for (i in message.getPayload().size - 1 downTo 0) {
                        word = word!! or (message.getPayload()[i] and 0xFF).toInt()
                        if (i != 0) {
                            word = word!! shl 8
                        }
                    }
                }
                Message.ID_READREPLY -> {
                    for (i in message.getPayload().size - 1 downTo 0) {
                        long = long!! or (message.getPayload()[i] and 0xFF).toLong()
                        if (i != 0) {
                            long = long!! shl 8
                        }
                    }
                }
            }
        } else {
            byte = null
            half = null
            word = null
            long = null
        }
    }
}