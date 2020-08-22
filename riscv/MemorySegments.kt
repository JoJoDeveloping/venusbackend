package venusbackend.riscv

import venusbackend.simulator.SimulatorError

/** A singleton containing constants which say where various segments start */
object MemorySegments {
    /** Memory address where the stack segment starts (growing downwards) */
    const val STACK_BEGIN: Int = 0x1FFF0FFF // 0x40003FFF
    /** Memory address where the heap segment starts */
    const val HEAP_BEGIN: Int = 0x11000800 // 0x40002000
    /** Memory address where the data segment starts */
    var STATIC_BEGIN: Int = 0x10000800 // 0x4000000 // should be 0x8000_0000
    /**
     * Memory address where the text segment starts
     */
    public var TEXT_BEGIN: Int = 0x10000000 // 0x20000000

    /**
     * Memory address where the BIOS starts
     */
    public var BIOS_BEGIN: Int = 0x00020000


    fun setTextBegin(i: Int) {
        if (i < 0) {
            throw SimulatorError("The text location must be a positive number!")
        } else if (i >= MemorySegments.STATIC_BEGIN) {
            /*
            * @todo add check to see if text plus program is above stack. Should error as well.
            */
            throw SimulatorError("The text location in memory cannot be larger than the static!")
        }
        MemorySegments.TEXT_BEGIN = i
    }
}
