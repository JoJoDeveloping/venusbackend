package venusbackend.simulator

import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.cache.CacheHandler

interface SimulatorState {
    var mem: Memory
    var cache: CacheHandler
    fun setCacheHandler(ch: CacheHandler)
    val registerWidth: Int
    fun getPC(): Number
    fun setPC(location: Number)
    fun incPC(amount: Number)
    fun getMaxPC(): Number
    fun setMaxPC(location: Number)
    fun incMaxPC(amount: Number)
    fun getReg(i: Int): Number
    fun setReg(i: Int, v: Number)
    fun getFReg(i: Int): Decimal
    fun setFReg(i: Int, v: Decimal)
    suspend fun getSReg(i: Int): Number
    suspend fun setSReg(i: Int, v: Number)
    fun getHeapEnd(): Number
    fun incHeapEnd(amount: Number)
    fun setHeapEnd(i: Number)
    fun reset()
}

enum class Privilege {
    URW, URO, SRW, MRO, MRW, DRW
}

enum class SpecialRegisters(val address: Int, val regName: String) {
    MSTATUS(0x300, "mstatus"),
    MIE(0x304, "mie"),
    MTVEC(0x305, "mtvec"),
    MEPC(0x341, "mepc"),
    MCAUSE(0x342, "mcause"),
    MIP(0x344, "mip")
}
