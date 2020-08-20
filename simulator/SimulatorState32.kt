package venusbackend.simulator

import com.soywiz.korio.concurrent.atomic.KorAtomicInt
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import venusbackend.riscv.MemorySegments
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.cache.CacheHandler
import kotlin.coroutines.EmptyCoroutineContext

class SimulatorState32(override var mem: Memory = MemoryMap()) : SimulatorState {
    private val regs32 = Array(32) { 0 }
    private val fregs = Array(32) { Decimal() }
    private var heapEnd = MemorySegments.HEAP_BEGIN
    companion object {
        private val semaphore32: Semaphore = Semaphore(1)
        private val context32 = EmptyCoroutineContext
        private val sregs32 = mutableMapOf<Int, CSR32>()
        private var pc: Int = 0
        private var maxpc: Int = MemorySegments.TEXT_BEGIN
    }
    init {
        /**
         * Add CSRs here. Take the number from the "Currently allocated RISC-V x-level CSR addresses" table
         */
        sregs32[SpecialRegisters.MSTATUS.address] = CSR32(KorAtomicInt(8), Privilege.MRW) // mstatus CSR
        sregs32[SpecialRegisters.MIE.address] = CSR32(KorAtomicInt(0b100010001000), Privilege.MRW) // mie CSR
        sregs32[SpecialRegisters.MIP.address] = CSR32(KorAtomicInt(0), Privilege.MRW) // mip CSR
        sregs32[SpecialRegisters.MEPC.address] = CSR32(KorAtomicInt(0), Privilege.MRW) // mepc CSR
        sregs32[SpecialRegisters.MCAUSE.address] = CSR32(KorAtomicInt(0), Privilege.MRW) // mcause CSR
        sregs32[SpecialRegisters.MTVEC.address] = CSR32(KorAtomicInt(0x10000000), Privilege.MRW) // mtvec CSR
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
    override suspend fun getSReg(i: Int): Number {
        semaphore32.acquire()
        val result : Int
        withContext(context32) {
            result = sregs32[i]!!.content.value
        }
        semaphore32.release()
        return result
    }

    override suspend fun setSReg(i: Int, v: Number) {
        semaphore32.acquire()
        if (sregs32[i]!!.privilege == Privilege.MRW) { // Checking just machine Read/Write privilege because we only have machine mode
            withContext(context32) {
                sregs32[i]!!.content.value = v.toInt()
            }
        }
        semaphore32.release()
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
    class CSR32(var content: KorAtomicInt, val privilege: Privilege)
}
