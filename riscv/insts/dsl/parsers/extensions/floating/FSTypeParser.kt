package venusbackend.riscv.insts.dsl.parsers.extensions.floating

import venusbackend.assembler.DebugInfo
import venusbackend.riscv.InstructionField
import venusbackend.riscv.MachineCode
import venusbackend.riscv.Program
import venusbackend.riscv.insts.dsl.getImmediate
import venusbackend.riscv.insts.dsl.parsers.InstructionParser
import venusbackend.riscv.insts.dsl.parsers.checkArgsLength
import venusbackend.riscv.insts.dsl.parsers.regNameToNumber

/**
 * Created by thaum on 8/6/2018.
 */
object FSTypeParser : InstructionParser {
    const val S_TYPE_MIN = -2048
    const val S_TYPE_MAX = 2047
    override operator fun invoke(prog: Program, mcode: MachineCode, args: List<String>, dbg: DebugInfo) {
        checkArgsLength(args.size, 3, dbg)

        val imm = getImmediate(args[1], S_TYPE_MIN, S_TYPE_MAX, dbg)
        mcode[InstructionField.RS1] = regNameToNumber(args[2], dbg = dbg)
        mcode[InstructionField.RS2] = regNameToNumber(args[0], false, dbg)
        mcode[InstructionField.IMM_4_0] = imm
        mcode[InstructionField.IMM_11_5] = imm shr 5
    }
}