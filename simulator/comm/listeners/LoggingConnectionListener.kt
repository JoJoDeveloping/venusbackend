package venusbackend.simulator.comm.listeners

import com.soywiz.klogger.Logger
import venusbackend.simulator.SimulatorState
import venusbackend.simulator.comm.Message

class LoggingConnectionListener : IConnectionListener() {
    var logger: Logger = Logger("Logger Listener")

    init {
        logger.output = object : Logger.Output {
            override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
                println("${logger.name}: $level: $msg")
            }
        }
        logger.level = Logger.Level.INFO
    }

    override fun dataReceived(offset: Int, payload: ByteArray?) {
        logger.info {
            "Data received: Offset = " + toHexString(byteArrayOf(offset.toByte())) + "   " + payload?.let { toHexString(it) }
        }
    }

    override suspend fun interruptRequest(message: Message, simulatorState: SimulatorState) {
        logger.info { "Irq ${message.slot.toInt()}" }
    }

    override fun powerOff() {
        logger.info { "Power Off" }
    }

    override fun powerOn() {
        logger.warn { "Power On" }
    }

    override fun readRequest() {
        logger.info { "Read" }
    }

    override fun reset() {
        logger.info { "Reset" }
    }

    override fun writeRequest() {
        logger.info { "Write" }
    }

    override fun terminate() {
        logger.info { "Terminate" }
    }

    companion object {
        /**
         * to hex converter
         */
        private val toHex = charArrayOf(
                '0', '1', '2', '3', '4', '5', '6',
                '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f')

        /**
         * Converts b[] to hex string.
         * @param b the byte array to convert
         * @return a Hex representation of b.
         */
        private fun toHexString(b: ByteArray): String {
            var pos = 0
            val c = CharArray(b.size * 3)
            for (i in b.indices) {
                c[pos++] = toHex[b[i].toInt() shr 4 and 0x0F]
                c[pos++] = toHex[b[i].toInt() and 0x0f]
                c[pos++] = ' '
            }
            return String(c)
        }
    }
}