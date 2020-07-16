package venusbackend.riscv.insts.dsl.types.extensions.floating

import venusbackend.riscv.insts.dsl.types.Instruction
import venusbackend.riscv.insts.dsl.disasms.extensions.floating.FITypeDisassembler
import venusbackend.riscv.insts.dsl.formats.base.ITypeFormat
import venusbackend.riscv.insts.dsl.impls.NoImplementation
import venusbackend.riscv.insts.dsl.impls.extensions.floating.b32.FITypeImplementation32
import venusbackend.riscv.insts.dsl.parsers.extensions.floating.FITypeParser
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.Simulator

open class FITypeInstruction(
    name: String,
    opcode: Int,
    funct3: Int,
    eval32: suspend (Int, Simulator) -> Decimal
) : Instruction(
        name = name,
        format = ITypeFormat(opcode, funct3),
        parser = FITypeParser,
        impl16 = NoImplementation,
        impl32 = FITypeImplementation32(eval32),
        impl64 = NoImplementation,
        impl128 = NoImplementation,
        disasm = FITypeDisassembler
)