package venusbackend.simulator.comm.listeners

import venusbackend.or
import venusbackend.simulator.SimulatorState
import venusbackend.simulator.SpecialRegisters
import venusbackend.simulator.comm.Message

class InterruptConnectionListener: IConnectionListener() {

    override suspend fun interruptRequest(message: Message, simulatorState: SimulatorState) {
        simulatorState.setSReg(SpecialRegisters.MIP.address, simulatorState.getSReg(SpecialRegisters.MIP.address) or 0b100000000000) // set the machine external interrupt bit in mip
        simulatorState.setSReg(SpecialRegisters.MCAUSE.address, message.slot.toInt())
        //println("Now this is the value of mip register: ${simulatorState.getSReg(SpecialRegisters.MIP.address)}")
    }

}
