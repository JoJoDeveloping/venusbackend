package venusbackend.assembler.pseudos

import venusbackend.assembler.AssemblerPassOne
import venusbackend.assembler.DebugInfo
import venusbackend.assembler.LineTokens
import venusbackend.assembler.PseudoWriter
import venusbackend.riscv.insts.dsl.relocators.PCRelHiRelocator
import venusbackend.riscv.insts.dsl.relocators.PCRelLoRelocator

/**
 * Writes pseudoinstruction `tail label`.
 */
object TAIL : PseudoWriter() {
    override operator fun invoke(args: LineTokens, state: AssemblerPassOne, dbg: DebugInfo): List<LineTokens> {
        checkArgsLength(args, 2, dbg)

        val auipc = listOf("auipc", "x6", "0")
        state.addRelocation(PCRelHiRelocator, state.getOffset(), args[1], dbg = dbg)

        val jalr = listOf("jalr", "x0", "x6", "0")
        state.addRelocation(PCRelLoRelocator, state.getOffset() + 4, args[1], dbg = dbg)

        return listOf(auipc, jalr)
    }
}
