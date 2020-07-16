package venusbackend.riscv.insts.dsl.impls

import venusbackend.riscv.MachineCode
import venusbackend.simulator.Simulator

object NoImplementation : InstructionImplementation {
    override suspend operator fun invoke(mcode: MachineCode, sim: Simulator) =
            throw NotImplementedError("no implementation available")
}
