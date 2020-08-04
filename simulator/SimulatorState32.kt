package venusbackend.simulator

import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.addSuspend
import kotlinx.coroutines.sync.Mutex
import venusbackend.riscv.MemorySegments
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.cache.CacheHandler

val mutex32: Mutex = Mutex()
val signal32 = Signal<SimulatorState32.SpecialRegisterSetter32>()

class SimulatorState32(override var mem: Memory = MemoryMap()) : SimulatorState {
    private val regs32 = Array(32) { 0 }
    private val sregs32 = mutableMapOf<Int, CSR32>()
    private val fregs = Array(32) { Decimal() }
    private var pc: Int = 0
    private var maxpc: Int = MemorySegments.TEXT_BEGIN
    private var heapEnd = MemorySegments.HEAP_BEGIN

    init {
        /**
         * Add CSRs here. Take the number from the "Currently allocated RISC-V x-level CSR addresses" table
         */
        sregs32[SpecialRegisters.MSTATUS.address] = CSR32(8, Privilege.MRW) // mstatus CSR with the mie bit set
        sregs32[SpecialRegisters.MIE.address] = CSR32(0, Privilege.MRW) // mie CSR
        sregs32[SpecialRegisters.MIP.address] = CSR32(0, Privilege.MRW) // mip CSR
        sregs32[SpecialRegisters.MEPC.address] = CSR32(0, Privilege.MRW) // mepc CSR
        sregs32[SpecialRegisters.MCAUSE.address] = CSR32(0, Privilege.MRW) // mcause CSR
        sregs32[SpecialRegisters.MTVEC.address] = CSR32(0, Privilege.MRW) // mtvec CSR
    }

    override val registerWidth = 32
    override var cache = CacheHandler(1)
    override fun setCacheHandler(ch: CacheHandler) {
        cache = ch
    }
    override fun setPC(location: Number) {
        this.pc = location.toInt()
    }
    override fun getPC(): Number {
        return this.pc
    }
    override fun incPC(amount: Number) {
        this.pc += amount.toInt()
    }
    override fun setMaxPC(location: Number) {
        this.maxpc = location.toInt()
    }
    override fun getMaxPC(): Number {
        return this.maxpc
    }
    override fun incMaxPC(amount: Number) {
        this.maxpc = (this.maxpc + amount.toInt())
    }
    override fun getReg(i: Int) = regs32[i]
    override fun setReg(i: Int, v: Number) { if (i != 0) regs32[i] = v.toInt() }
    override fun getFReg(i: Int) = fregs[i]
    override fun setFReg(i: Int, v: Decimal) { fregs[i] = v }
    override fun getSReg(i: Int): Number {
        return sregs32[i]!!.content
    }

    override suspend fun setSReg(i: Int, v: Number) {
        if (signal32.listenerCount == 0) {
            signal32.addSuspend {
                setSpecialRegister(it.i,it.v)
            }
        }
        signal32(SpecialRegisterSetter32(i, v))
    }

    private suspend fun setSpecialRegister(i: Int, v: Number) {
        mutex32.lock()
        if (sregs32[i]!!.privilege == Privilege.MRW) {
            sregs32[i]!!.content = v.toInt()
        }
        mutex32.unlock()
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
    class CSR32(var content: Int, val privilege: Privilege)
    class SpecialRegisterSetter32(val i: Int, val v: Number)
}
