package venusbackend.riscv.insts.dsl.impls

import venusbackend.and
import venusbackend.or
import venusbackend.riscv.MachineCode
import venusbackend.shl
import venusbackend.shr
import venusbackend.simulator.Simulator
import venusbackend.simulator.SpecialRegisters

class TRTypeImplementation: InstructionImplementation {
    override suspend fun invoke(mcode: MachineCode, sim: Simulator) {
        val last7BitsOfMstatus = sim.getSReg(SpecialRegisters.MSTATUS.address) and 0x7F // Save last 7 bits
        val mpieBit = sim.getSReg(SpecialRegisters.MSTATUS.address) and 0x80 // Take only mpie bit from mstatus
        val mieBitMask = mpieBit shr 4 // build the mask for the mie bit in mstatus
        /**
         * Zeroes out the last 8 bits then recreates the last 7 bits (just making sure the mpie bit is 0)
         * Then sets the mie bit to the value that was in mpie.
         */
        val mstatusNewValue = (((((sim.getSReg(SpecialRegisters.MSTATUS.address) shr 8) shl 8) or last7BitsOfMstatus) or mieBitMask))
        sim.setSReg(SpecialRegisters.MSTATUS.address, mstatusNewValue)
        // mpp bit in mstatus will not be set because currently there's only machine mode
        val mip = sim.getSReg(SpecialRegisters.MIP.address)
        val mie = sim.getSReg(SpecialRegisters.MIE.address)
        val meieBit = mie and 0x800 shr 11 // Machine external interrupt enable bit
        val meipBit = mip and 0x800 shr 11 // Machine external interrupt pending bit
        val mtieBit = mie and 0x80 shr 7   // Machine timer interrupt enable bit
        val mtipBit = mip and 0x80 shr 7   // Machine timer interrupt pending bit
        val msieBit = mie and 0x8 shr 3    // Machine software interrupt enable bit
        val msipBit = mip and 0x8 shr 3    // Machine software interrupt pending bit
        val newPC = sim.getSReg(SpecialRegisters.MEPC.address)
        sim.setPC(newPC)
    }
}