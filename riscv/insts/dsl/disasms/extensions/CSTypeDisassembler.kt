package venusbackend.riscv.insts.dsl.disasms.extensions

import venusbackend.riscv.InstructionField
import venusbackend.riscv.MachineCode
import venusbackend.riscv.insts.dsl.Instruction
import venusbackend.riscv.insts.dsl.disasms.InstructionDisassembler

object CSTypeDisassembler : InstructionDisassembler {
    override fun invoke(mcode: MachineCode): String {
        val name = Instruction[mcode].name
        val rd = mcode[InstructionField.RDP]
        val rs1 = mcode[InstructionField.RS1P]
        val rs2 = mcode[InstructionField.RS2P]
        return "$name x$rd x$rs1 x$rs2"
    }
}
