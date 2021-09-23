package venusbackend.riscv.insts.integer.base.i.trap

import venusbackend.riscv.insts.dsl.disasms.RawDisassembler
import venusbackend.riscv.insts.dsl.formats.OpcodeFunct12Format
import venusbackend.riscv.insts.dsl.impls.NoImplementation
import venusbackend.riscv.insts.dsl.impls.RawImplementation
import venusbackend.riscv.insts.dsl.parsers.DoNothingParser
import venusbackend.riscv.insts.dsl.types.Instruction

val wfi = Instruction(
        name = "wfi",
        format = OpcodeFunct12Format(0b1110011, 0b000100000101),
        parser = DoNothingParser,
        disasm = RawDisassembler { "wfi" },
        impl16 = NoImplementation,
        impl128 = NoImplementation,
        impl32 = RawImplementation {machineCode, simulator ->
//            simulatorInterruptSignal.waitOneBase() // TODO: Implement alternative to Signal
        },
        impl64 = RawImplementation{machineCode, simulator ->
//            simulatorInterruptSignal.waitOneBase() // TODO: Implement alternative to Signal
        }
)