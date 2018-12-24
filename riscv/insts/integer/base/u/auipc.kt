package venusbackend.riscv.insts.integer.base.u

import venusbackend.riscv.InstructionField
import venusbackend.riscv.insts.dsl.UTypeInstruction
import venusbackend.riscv.insts.dsl.impls.NoImplementation

val auipc = UTypeInstruction(
        name = "auipc",
        opcode = 0b0010111,
        impl16 = NoImplementation::invoke,
        impl32 = { mcode, sim ->
            val offset = mcode[InstructionField.IMM_31_12] shl 12
            sim.setReg(mcode[InstructionField.RD], sim.getPC() + offset)
            sim.incrementPC(mcode.length)
        },
        impl64 = NoImplementation::invoke,
        impl128 = NoImplementation::invoke
)
