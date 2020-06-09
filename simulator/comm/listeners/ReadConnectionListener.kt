package venusbackend.simulator.comm.listeners

import com.soywiz.klock.DateTime
import com.soywiz.klock.milliseconds
import venusbackend.and
import venusbackend.simulator.comm.Message

class ReadConnectionListener : IConnectionListener() {
    var byte: Int? = null
    var half: Int? = null
    var word: Int? = null
    var long: Long? = null
    var done: Boolean = false

    fun waitForReadingResponse(timeout: Long = 1000) {
        val afterTimeout = DateTime.now() + timeout.milliseconds
        while (!done) {
            if (DateTime.now() >= afterTimeout) {
                break
            }
        }
        done = false
    }

    /**
     * Receives a byte in the payload and saves it in the b variable.
     */
    override fun readData(message: Message?) {
        if (message != null) {
            byte = 0
            half = 0
            word = 0
            long = 0
            when (message.id) {
                Message.ID_BYTEREPLY -> byte = (message.getPayload()[0] and 0xFF).toInt()
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
            done = true
        } else {
            byte = null
            half = null
            word = null
            long = null
            done = true
        }
    }
}