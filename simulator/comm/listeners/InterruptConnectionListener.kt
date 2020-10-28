package venusbackend.simulator.comm.listeners

import com.soywiz.korio.async.Signal
import venusbackend.or
import venusbackend.simulator.SimulatorState
import venusbackend.simulator.SpecialRegisters
import venusbackend.simulator.comm.Message

val simulatorInterruptSignal = Signal<Boolean>()

class InterruptConnectionListener: IConnectionListener() {

    override suspend fun interruptRequest(message: Message, simulatorState: SimulatorState) {
        simulatorState.setSReg(SpecialRegisters.MIP.address, simulatorState.getSReg(SpecialRegisters.MIP.address) or 0b100000000000) // set the machine external interrupt bit in mip
        var mcause: Number = 0
        when(simulatorState.registerWidth) { // set interrupt bit
            32 -> mcause = 1 shl 31
            64 -> mcause = 1L shl 63
        }
        mcause = mcause or message.slot.toInt()
        simulatorState.setSReg(SpecialRegisters.MCAUSE.address, mcause)
        simulatorInterruptSignal(true)
    }

}
