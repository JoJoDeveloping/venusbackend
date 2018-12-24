package venusbackend.riscv.insts.integer.base.i

import venus.Renderer
import venusbackend.riscv.InstructionField
import venusbackend.riscv.MemorySegments
import venusbackend.riscv.Registers
import venusbackend.riscv.insts.dsl.Instruction
import venusbackend.riscv.insts.dsl.disasms.RawDisassembler
import venusbackend.riscv.insts.dsl.formats.FieldEqual
import venusbackend.riscv.insts.dsl.formats.InstructionFormat
import venusbackend.riscv.insts.dsl.impls.NoImplementation
import venusbackend.riscv.insts.dsl.impls.RawImplementation
import venusbackend.riscv.insts.dsl.parsers.DoNothingParser
import venusbackend.simulator.FilesHandler
import venusbackend.simulator.Simulator

val ecall = Instruction(
        name = "ecall",
        format = InstructionFormat(4,
                listOf(FieldEqual(InstructionField.ENTIRE, 0b000000000000_00000_000_00000_1110011))
        ),
        parser = DoNothingParser,
        impl16 = NoImplementation,
        impl32 = RawImplementation { mcode, sim ->
            val whichCall = sim.getReg(10)
            when (whichCall) {
                1 -> printInteger(sim)
                4 -> printString(sim)
                9 -> sbrk(sim)
                10 -> exit(sim)
                11 -> printChar(sim)
                13 -> openFile(sim)
                14 -> readFile(sim)
                15 -> writeFile(sim)
                16 -> closeFile(sim)
                17 -> exitWithCode(sim)
                18 -> fflush(sim)
                19 -> feof(sim)
                20 -> ferror(sim)
                34 -> printHex(sim)
                else -> Renderer.printConsole("Invalid ecall $whichCall")
            }
            if (!(whichCall == 10 || whichCall == 17)) {
                sim.incrementPC(mcode.length)
            }
        },
        impl64 = NoImplementation,
        impl128 = NoImplementation,
        disasm = RawDisassembler { "ecall" }
)

// All file operations will return -1 if the file descriptor is not found.
private fun openFile(sim: Simulator) {
    /**
     * Attempt to open the file with the lowest number to return first. If cannot open file, return -1.
     * Look here for the permissionbits:https://en.cppreference.com/w/c/io/fopen
     *
     * a0=13,a1=filename,a2=permissionbits -> a0=filedescriptor
     */
    val filenameAddress = sim.getReg(Registers.a1)
    val permissions = sim.getReg(Registers.a2)
    val filename = getString(sim, filenameAddress)
    val fdID = sim.filesHandler.openFile(sim, filename, permissions)
    sim.setReg(Registers.a0, fdID)
}

private fun readFile(sim: Simulator) {
    /**
     * Check file descriptor and check if we have the valid permissions.
     * If we can read from the file, start reading at the offset (default=0)
     * and increment the offset by the bytes read. Return the number of bytes which were read.
     * User will call feof(fd) or ferror(fd) for if the output is not equal to the length.
     *
     * a0=14, a1=filedescriptor, a2=where to store data, a3=amt to read -> a0=Number of items read
     */
    val fdID = sim.getReg(Registers.a1)
    val bufferAddress = sim.getReg(Registers.a2)
    val size = sim.getReg(Registers.a3)
    val data = sim.filesHandler.readFileDescriptor(fdID, size)
    var offset = 0
    if (data != null) {
        for (c in data) {
            sim.storeBytewCache(bufferAddress + offset, c.toInt())
            offset++
        }
        sim.setReg(Registers.a0, offset)
    } else {
        sim.setReg(Registers.a0, FilesHandler.EOF)
    }
}

private fun writeFile(sim: Simulator) {
    /**
     * a0=15, a1=filedescriptor, a2=buffer to read data, a3=amt to write, a4=size of each item -> a0=Number of items written
     */
    val fdID = sim.getReg(Registers.a1)
    val bufferAddress = sim.getReg(Registers.a2)
    val size = sim.getReg(Registers.a3)
    val sizeOfItem = sim.getReg(Registers.a4)
    var offset = 0
    val sb = StringBuilder()
    while (offset < size) {
        sb.append(sim.loadBytewCache(bufferAddress + offset).toChar())
        offset++
    }
    val result = sim.filesHandler.writeFileDescriptor(fdID, sb.toString())
    sim.setReg(Registers.a0, result)
}

private fun closeFile(sim: Simulator) {
    /**
     * Flush the data written to the file back to where it came from.
     * a0=16, a1=filedescriptor -> ​0​ on success, EOF otherwise
     */
    val fdID = sim.getReg(Registers.a1)
    val a0 = sim.filesHandler.closeFileDescriptor(fdID)
    sim.setReg(Registers.a0, a0)
}

private fun fflush(sim: Simulator) {
    /**
     * Returns zero on success. Otherwise EOF is returned and the error indicator of the file stream is set.
     * a0=19, a1=filedescriptor -> a0=if end of file
     */
    val fdID = sim.getReg(Registers.a1)
    val a0 = sim.filesHandler.flushFileDescriptor(fdID)
    sim.setReg(Registers.a0, a0)
}

private fun feof(sim: Simulator) {
    /**
     * Will return nonzero value if the end of the stream has been reached, otherwise ​0​
     *
     * a0=19, a1=filedescriptor -> a0=if end of file
     */
    val fdID = sim.getReg(Registers.a1)
    val a0 = sim.filesHandler.getFileDescriptorEOF(fdID)
    sim.setReg(Registers.a0, a0)
}

private fun ferror(sim: Simulator) {
    /**
     * Will return Nonzero value if the file stream has errors occurred, ​0​ otherwise
     *
     * a0=20, a1=filedescriptor -> a0=if error occured
     */
    val fdID = sim.getReg(Registers.a1)
    val a0 = sim.filesHandler.getFileDescriptorError(fdID)
    sim.setReg(Registers.a0, a0)
}

private fun printHex(sim: Simulator) {
    val arg = sim.getReg(11)
    sim.ecallMsg = Renderer.toHex(arg)
    Renderer.printConsole(sim.ecallMsg)
}

private fun printInteger(sim: Simulator) {
    val arg = sim.getReg(11)
    sim.ecallMsg = arg.toString()
    Renderer.printConsole(sim.ecallMsg)
}

private fun printString(sim: Simulator) {
    val arg = sim.getReg(11)
    val s = getString(sim, arg)
        sim.ecallMsg += s
        Renderer.printConsole(s)
}

private fun sbrk(sim: Simulator) {
    val bytes = sim.getReg(11)
    if (bytes < 0) return
    sim.setReg(10, sim.getHeapEnd())
    sim.addHeapSpace(bytes)
}

private fun exit(sim: Simulator) {
    sim.setPC(MemorySegments.STATIC_BEGIN)
    // sim.ecallMsg = "exiting the venusbackend.simulator"
}

private fun printChar(sim: Simulator) {
    val arg = sim.getReg(11)
    sim.ecallMsg = (arg.toChar()).toString()
    Renderer.printConsole(arg.toChar())
}

private fun exitWithCode(sim: Simulator) {
    sim.setPC(MemorySegments.STATIC_BEGIN)
    val retVal = sim.getReg(11)
    sim.ecallMsg = "\nExited with error code $retVal"
    Renderer.printConsole("\nExited with error code $retVal\n")
}

private fun getString(sim: Simulator, address: Int): String {
    var addr = address
    val s = StringBuilder()
    var c = sim.loadByte(address)
    addr++
    while (c != 0) {
        s.append(c.toChar())
        c = sim.loadByte(addr)
        addr++
    }
    return s.toString()
}
