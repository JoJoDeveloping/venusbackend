package venusbackend.riscv.insts.dsl.relocators

import venusbackend.riscv.MachineCode

object NoRelocator64 : Relocator64 {
    override operator fun invoke(mcode: MachineCode, pc: Long, target: Long) =
            throw NotImplementedError("no relocator64 for $mcode")
}
