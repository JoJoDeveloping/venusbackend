package venusbackend.simulator

//import com.soywiz.korio.concurrent.atomic.KorAtomicInt
//import kotlinx.coroutines.sync.Semaphore
//import kotlinx.coroutines.withContext
import venus.Renderer
import venusbackend.riscv.MemorySegments
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.cache.CacheHandler
import kotlin.coroutines.EmptyCoroutineContext

class SimulatorState32(override var mem: Memory = MemoryAsMap()) : SimulatorState {
    private val regs32 = Array(32) { 0 }
    private val fregs = Array(32) { Decimal() }
    private var heapEnd = MemorySegments.HEAP_BEGIN
    companion object {
        private val sregs32 = mutableMapOf<Int, CSR32>()
        private var pc: Int = 0
        private var maxpc: Int = MemorySegments.TEXT_BEGIN
    }
    init {
        /**
         * Add CSRs here. Take the number from the "Currently allocated RISC-V x-level CSR addresses" table
         */
        sregs32[SpecialRegisters.MSTATUS.address] = CSR32(0, SpecialRegisterRights.MRW) // mstatus CSR
        sregs32[SpecialRegisters.MIE.address] = CSR32(0, SpecialRegisterRights.MRW) // mie CSR
        sregs32[SpecialRegisters.MIP.address] = CSR32(0, SpecialRegisterRights.MRW) // mip CSR
        sregs32[SpecialRegisters.MEPC.address] = CSR32(0, SpecialRegisterRights.MRW) // mepc CSR
        sregs32[SpecialRegisters.MCAUSE.address] = CSR32(0, SpecialRegisterRights.MRW) // mcause CSR
        sregs32[SpecialRegisters.MTVEC.address] = CSR32(0, SpecialRegisterRights.MRW) // mtvec CSR
        sregs32[SpecialRegisters.MTIME.address] = CSR32(0, SpecialRegisterRights.MRW)
        sregs32[SpecialRegisters.MTIMECMP.address] = CSR32(0, SpecialRegisterRights.MRW)
    }

    override val registerWidth = 32
    override var cache = CacheHandler(1)
    override fun setCacheHandler(ch: CacheHandler) {
        cache = ch
    }
    override fun setPC(location: Number) {
        pc = location.toInt()
    }
    override fun getPC(): Number {
        return pc
    }
    override fun incPC(amount: Number) {
        pc += amount.toInt()
    }
    override fun setMaxPC(location: Number) {
        maxpc = location.toInt()
    }
    override fun getMaxPC(): Number {
        return maxpc
    }
    override fun incMaxPC(amount: Number) {
        maxpc = (maxpc + amount.toInt())
    }
    override fun getReg(i: Int) = regs32[i]
    override fun setReg(i: Int, v: Number) { if (i != 0) regs32[i] = v.toInt() }
    override fun getFReg(i: Int) = fregs[i]
    override fun setFReg(i: Int, v: Decimal) { fregs[i] = v }
    override fun getSReg(i: Int)= sregs32[i]!!.content

    override fun setSReg(i: Int, v: Number) {
        if (sregs32[i]!!.specialRegisterRights == SpecialRegisterRights.MRW) { // Checking just machine Read/Write privilege because we only have machine mode. TODO: check rights correctly here
                sregs32[i]!!.content = v.toInt()
        }
    }

    override fun getHeapEnd(): Number {
        return heapEnd
    }

    override fun setHeapEnd(i: Number) {
        heapEnd = i.toInt()
    }

    override fun incHeapEnd(amount: Number) {
        heapEnd += amount.toInt()
    }

    override fun reset() {
        this.cache.reset()
    }
    private data class CSR32(var content: Int, val specialRegisterRights: SpecialRegisterRights)
}
