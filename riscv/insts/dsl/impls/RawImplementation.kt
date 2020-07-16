package venusbackend.riscv.insts.dsl.impls

import venusbackend.riscv.MachineCode
import venusbackend.simulator.Simulator

class RawImplementation(private val eval: suspend (MachineCode, Simulator) -> Unit) : InstructionImplementation {
    override suspend operator fun invoke(mcode: MachineCode, sim: Simulator) = eval(mcode, sim)
}
