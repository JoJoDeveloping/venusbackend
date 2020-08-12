package venusbackend.simulator

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import venusbackend.riscv.MemorySegments
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.cache.CacheHandler
import kotlin.coroutines.EmptyCoroutineContext

private val sregs64 = mutableMapOf<Int, SimulatorState64.CSR64>()
private val semaphore64: Semaphore = Semaphore(1)
private val context64 = EmptyCoroutineContext

class SimulatorState64(override var mem: Memory = MemoryMap()) : SimulatorState {
    private val regs64 = Array(32) { 0.toLong() }
    private val fregs = Array(32) { Decimal() }
    private var pc: Long = 0
    private var maxpc: Long = MemorySegments.TEXT_BEGIN.toLong()
    private var heapEnd = MemorySegments.HEAP_BEGIN.toLong()

    override val registerWidth = 64
    override var cache = CacheHandler(1)

    init {
        /**
         * Add CSRs here. Take the number from the "Currently allocated RISC-V x-level CSR addresses" table
         */
        sregs64[SpecialRegisters.MSTATUS.address] = CSR64(8, Privilege.MRW) // mstatus CSR with the mie bit set
        sregs64[SpecialRegisters.MIE.address] = CSR64(0, Privilege.MRW) // mie CSR
        sregs64[SpecialRegisters.MIP.address] = CSR64(0, Privilege.MRW) // mip CSR
        sregs64[SpecialRegisters.MEPC.address] = CSR64(0, Privilege.MRW) // mepc CSR
        sregs64[SpecialRegisters.MCAUSE.address] = CSR64(0, Privilege.MRW) // mcause CSR
        sregs64[SpecialRegisters.MTVEC.address] = CSR64(0, Privilege.MRW) // mtvec CSR
    }

    override fun setCacheHandler(ch: CacheHandler) {
        cache = ch
    }
    override fun setPC(location: Number) {
        this.pc = location.toLong()
    }
    override fun getPC(): Number {
        return this.pc
    }
    override fun incPC(amount: Number) {
        this.pc += amount.toLong()
    }
    override fun setMaxPC(location: Number) {
        this.maxpc = location.toLong()
    }
    override fun getMaxPC(): Number {
        return this.maxpc
    }
    override fun incMaxPC(amount: Number) {
        this.maxpc = (this.maxpc + amount.toLong())
    }
    override fun getReg(i: Int) = regs64[i]
    override fun setReg(i: Int, v: Number) { if (i != 0) regs64[i] = v.toLong() }
    override fun getFReg(i: Int) = fregs[i]
    override fun setFReg(i: Int, v: Decimal) { fregs[i] = v }
    override suspend fun getSReg(i: Int): Number {
        semaphore64.acquire()
        val result: Long
        withContext(context64) {
            result = sregs64[i]!!.content
        }
        semaphore64.release()
        return result
    }

    override suspend fun setSReg(i: Int, v: Number) {
        semaphore64.acquire()
        if (sregs64[i]!!.privilege == Privilege.MRW) { // Checking just machine Read/Write privilege because we only have machine mode
            withContext(context64) {
                sregs64[i]!!.content = v.toLong()
            }
        }
        semaphore64.release()
    }

    override fun getHeapEnd(): Number {
        return heapEnd
    }

    override fun setHeapEnd(i: Number) {
        heapEnd = i.toLong()
    }

    override fun incHeapEnd(amount: Number) {
        heapEnd += amount.toLong()
    }

    override fun reset() {
        this.cache.reset()
    }
    class CSR64(var content: Long, val privilege: Privilege)
    class SpecialRegisterSetter64(val i: Int, val v: Number)
}
