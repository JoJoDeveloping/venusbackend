package venusbackend.riscv.insts.dsl.types.base

import venusbackend.riscv.MachineCode
import venusbackend.riscv.insts.dsl.disasms.base.UTypeDisassembler
import venusbackend.riscv.insts.dsl.formats.base.UTypeFormat
import venusbackend.riscv.insts.dsl.impls.RawImplementation
import venusbackend.riscv.insts.dsl.parsers.base.UTypeParser
import venusbackend.riscv.insts.dsl.types.Instruction
import venusbackend.simulator.Simulator

class UTypeInstruction(
    name: String,
    opcode: Int,
    impl16: suspend (MachineCode, Simulator) -> Unit = { _, _ -> throw NotImplementedError("no rv16") },
    impl32: suspend (MachineCode, Simulator) -> Unit,
    impl64: suspend (MachineCode, Simulator) -> Unit = { _, _ -> throw NotImplementedError("no rv64") },
    impl128: suspend (MachineCode, Simulator) -> Unit = { _, _ -> throw NotImplementedError("no rv128") }
) : Instruction(
        name = name,
        format = UTypeFormat(opcode),
        parser = UTypeParser,
        impl16 = RawImplementation(impl16),
        impl32 = RawImplementation(impl32),
        impl64 = RawImplementation(impl64),
        impl128 = RawImplementation(impl128),
        disasm = UTypeDisassembler
)
