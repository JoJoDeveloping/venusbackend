package venusbackend.simulator.comm.listeners

import venusbackend.simulator.SimulatorState
import venusbackend.simulator.SpecialRegisters
import venusbackend.simulator.comm.Message

class InterruptConnectionListener: IConnectionListener() {

    override suspend fun interruptRequest(message: Message, simulatorState: SimulatorState) {
        simulatorState.setSReg(SpecialRegisters.MIP.address, 0)
    }

}
