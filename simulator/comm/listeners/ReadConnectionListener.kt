package venusbackend.simulator.comm.listeners

import venusbackend.and
import venusbackend.simulator.comm.Message
import venusbackend.simulator.comm.MotherboardConnection
import java.util.concurrent.CountDownLatch

class ReadConnectionListener(private var countDownLatch: CountDownLatch, private val delay : Long = 1000, private val connection: MotherboardConnection) : IConnectionListener() {
    var byte : Int? = null
    var half : Int? = null
    var word : Int? = null
    var long : Long? = null


    fun getCountDownLatch() : CountDownLatch {
        return countDownLatch
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
            when(message.id) {
                Message.ID_BYTEREPLY -> byte = (message.getPayload()[0] and 0xFF).toInt()
                Message.ID_WYDEREPLY -> half = ((message.getPayload()[1] and 0xFF).toInt() shl 8) or (message.getPayload()[0] and 0xFF).toInt()
                Message.ID_TETRAREPLY -> {
                    val byte1 = (message.getPayload()[3] and 0xFF).toInt()
                    val byte2 = (message.getPayload()[2] and 0xFF).toInt()
                    val byte3 = (message.getPayload()[1] and 0xFF).toInt()
                    val byte4 = (message.getPayload()[0] and 0xFF).toInt()
                    word = (((((byte1 shl 8) or byte2) shl 8) or byte3) shl 8) or byte4
                }
                Message.ID_READREPLY -> {
                    for (i in message.getPayload().size downTo 0 ) {
                        long = long!! shl 8 or (message.getPayload()[i] and 0xFF).toLong()
                    }
                }
            }
            countDownLatch.countDown()
            countDownLatch = CountDownLatch(1)
        } else {
            byte = null
            half = null
            word = null
            long = null
            countDownLatch.countDown()
            countDownLatch = CountDownLatch(1)
        }
    }
}