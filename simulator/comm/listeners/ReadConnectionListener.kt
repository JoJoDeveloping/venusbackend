package venusbackend.simulator.comm.listeners

import venusbackend.simulator.comm.Message
import java.util.concurrent.CountDownLatch

class ReadConnectionListener(private var countDownLatch: CountDownLatch, private val delay : Long = 1000) : IConnectionListener() {
    var byte : Int? = null
    var half : Int? = null
    var word : Int? = null
    var long : Long? = null

    init {
        Thread(Runnable {
            Thread.sleep(delay)
            if (countDownLatch.count != 0L) {
                readData(null)
            }
        }).start()
    }

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
                Message.ID_BYTEREPLY -> byte = message.getPayload()[0].toInt().and(0xFF)
                Message.ID_WYDEREPLY -> half = (message.getPayload()[0].toInt().and(0xFF) shl 8) or (message.getPayload()[1].toInt().and(0xFF))
                Message.ID_TETRAREPLY -> {
                    val byte1 = message.getPayload()[0].toInt().and(0xFF)
                    val byte2 = message.getPayload()[1].toInt().and(0xFF)
                    val byte3 = message.getPayload()[2].toInt().and(0xFF)
                    val byte4 = message.getPayload()[3].toInt().and(0xFF)
                    word = (((((byte1 shl 8) or byte2) shl 8) or byte3) shl 8) or byte4
                }
                Message.ID_READREPLY -> {
                    for (el in message.getPayload()) {
                        long = long!! shl 8 or el.toLong().and(0xFF)
                    }
                }
            }
            countDownLatch.countDown()
            countDownLatch = CountDownLatch(1)
        } else {
            countDownLatch.countDown()
            countDownLatch = CountDownLatch(1)
        }
    }
}