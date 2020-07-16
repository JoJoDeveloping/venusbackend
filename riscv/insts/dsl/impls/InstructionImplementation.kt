package venusbackend.riscv.insts.dsl.impls

import venusbackend.riscv.MachineCode
import venusbackend.simulator.Simulator

interface InstructionImplementation {
    suspend operator fun invoke(mcode: MachineCode, sim: Simulator)
}
