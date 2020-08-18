package venusbackend.simulator.comm.listeners

import venusbackend.or
import venusbackend.simulator.SimulatorState
import venusbackend.simulator.SpecialRegisters
import venusbackend.simulator.comm.Message

class InterruptConnectionListener: IConnectionListener() {

    override suspend fun interruptRequest(message: Message, simulatorState: SimulatorState) {
        simulatorState.setSReg(SpecialRegisters.MIP.address, simulatorState.getSReg(SpecialRegisters.MIP.address) or 0b100000000000) // set the machine external interrupt bit in mip
        var mcause: Number = 0
        when(simulatorState.registerWidth) { // set interrupt bit
            32 -> mcause = 1 shl 31
            64 -> mcause = 1L shl 63
        }
        mcause = when(message.slot.toInt()) {
            44 -> mcause or 7 // machine timer interrupt
            else -> mcause or 11  // machine external interrupt
        }
        simulatorState.setSReg(SpecialRegisters.MCAUSE.address, mcause)
    }

}
