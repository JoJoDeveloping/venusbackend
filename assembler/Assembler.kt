package venusbackend.assembler
/* ktlint-disable no-wildcard-imports */
import venusbackend.riscv.InitInstructions
import venusbackend.assembler.pseudos.checkArgsLength
import venus.Renderer
import venusbackend.riscv.*
import venusbackend.riscv.insts.InstructionNotFoundError
import venusbackend.riscv.insts.dsl.types.Instruction
import venusbackend.riscv.insts.dsl.getImmWarning
import venusbackend.riscv.insts.dsl.relocators.Relocator
import venusbackend.simulator.SimulatorError
/* ktlint-enable no-wildcard-imports */

/**
 * This singleton implements a simple two-pass assembler to transform files into programs.
 */
object Assembler {
    /**
     * Assembles the given code into an unlinked Program.
     *
     * @param text the code to assemble.
     * @return an unlinked program.
     * @see venus.linker.Linker
     * @see venus.simulator.Simulator
     */
    fun assemble(text: String, name: String = "anonymous"): AssemblerOutput {
        InitInstructions() // This is due to how some method of compilation handle all of the code.
        var (passOneProg, talInstructions, passOneErrors, warnings) = AssemblerPassOne(text.replace("\r", ""), name).run()

        /* This will force pc to be word aligned. Removed it because I guess you could custom it.
        if (passOneProg.insts.size > 0) {
            val l = passOneProg.insts[0].length
            if (MemorySegments.TEXT_BEGIN % l != 0) {
                /*This will align the pc so we do not have invalid stuffs.*/
                MemorySegments.setTextBegin(MemorySegments.TEXT_BEGIN - (MemorySegments.TEXT_BEGIN % l))
            }
        }*/

        if (passOneErrors.isNotEmpty()) {
            return AssemblerOutput(passOneProg, passOneErrors, ArrayList<AssemblerWarning>())
        }
        var passTwoOutput = AssemblerPassTwo(passOneProg, talInstructions).run()
        if (passTwoOutput.prog.textSize + MemorySegments.TEXT_BEGIN > MemorySegments.STATIC_BEGIN) {
            try {
                MemorySegments.setTextBegin(MemorySegments.STATIC_BEGIN - passOneProg.textSize)
                Renderer.updateText()
                val pone = AssemblerPassOne(text).run()
                passOneProg = pone.prog
                passOneErrors = pone.errors
                talInstructions = pone.talInstructions
                if (passOneErrors.isNotEmpty()) {
                    return AssemblerOutput(passOneProg, passOneErrors, ArrayList<AssemblerWarning>())
                }
                passTwoOutput = AssemblerPassTwo(passOneProg, talInstructions).run()
            } catch (e: SimulatorError) {
                throw SimulatorError("Could not change the text size so could not fit the program because the static is too low and the text would be below zero!")
            }
        }
        if (warnings.isNotEmpty()) {
            val arr = passTwoOutput.warnings.toMutableList()
            arr.addAll(warnings)
            passTwoOutput = AssemblerOutput(passTwoOutput.prog, passTwoOutput.errors, arr)
        }
        return passTwoOutput
    }
}

data class DebugInfo(val lineNo: Int, val line: String, val address: Int)
data class DebugInstruction(val debug: DebugInfo, val LineTokens: List<String>)
data class PassOneOutput(
    val prog: Program,
    val talInstructions: List<DebugInstruction>,
    val errors: List<AssemblerError>,
    val warnings: List<AssemblerWarning>
)
data class AssemblerOutput(val prog: Program, val errors: List<AssemblerError>, val warnings: List<AssemblerWarning>)

/**
 * Pass #1 of our two pass assembler.
 *
 * It parses labels, expands pseudo-instructions and follows assembler directives.
 * It populations [talInstructions], which is then used by [AssemblerPassTwo] in order to actually assemble the code.
 */
val p1warnings = ArrayList<AssemblerWarning>()
internal class AssemblerPassOne(private val text: String, name: String = "anonymous") {
    /** The program we are currently assembling */
    private val prog = Program(name)
    /** The text offset where the next instruction will be written */
    private var currentTextOffset = 0 // MemorySegments.TEXT_BEGIN
    /** The data offset where more data will be written */
    private var currentDataOffset = MemorySegments.STATIC_BEGIN
    /** The allows user to set custom memory segments until the assembler has used an offset. */
    private var allow_custom_memory_segments = true
    /** Whether or not we are currently in the text segment */
    private var inTextSegment = true
    /** TAL Instructions which will be added to the program */
    private val talInstructions = ArrayList<DebugInstruction>()
    /** The current line number (for user-friendly errors) */
    private var currentLineNumber = 0
    /** List of all errors encountered */
    private val errors = ArrayList<AssemblerError>()
    private val warnings = ArrayList<AssemblerWarning>()
    /** Preprocessor defines */
    private val defines = HashMap<String, String>()

    fun run(): PassOneOutput {
        doPassOne()
        return PassOneOutput(prog, talInstructions, errors, warnings)
    }

    private fun doPassOne() {
        for (line in text.lines()) {
            try {
                currentLineNumber++

                val offset = getOffset()

                var pline = line
                defines.forEach { (token: String, value: String) ->
                    val splitline = pline.split(Regex("\\s")).toMutableList()
                    val tokens = ArrayList<String>()
                    var diff = false
                    for (v in splitline) {
                        if (v == token) {
                            tokens.add(value)
                            diff = true
                        } else {
                            tokens.add(v)
                        }
                    }
                    if (diff) {
                        pline = tokens.joinToString(" ")
                    }
                }

                preprocess(line)

                val (labels, args) = Lexer.lexLine(pline)
                for (label in labels) {
                    allow_custom_memory_segments = false
                    val oldOffset = prog.addLabel(label, offset)
                    if (oldOffset != null) {
                        throw AssemblerError("label $label defined twice")
                    }
                }

                if (args.isEmpty() || args[0].isEmpty()) continue // empty line

                if (isAssemblerDirective(args[0])) {
                    parseAssemblerDirective(args[0], args.drop(1), pline)
                } else {
                    allow_custom_memory_segments = false
                    val expandedInsts = replacePseudoInstructions(args)
                    for (inst in expandedInsts) {
                        val dbg = DebugInfo(currentLineNumber, line, currentTextOffset)
                        val instsize = try {
                            Instruction[getInstruction(inst)].format.length
                        } catch (e: AssemblerError) {
                            4
                        }
                        talInstructions.add(DebugInstruction(dbg, inst))
                        currentTextOffset += instsize
                    }
                }
                for (p1warning in p1warnings) {
                    p1warning.line = currentLineNumber
                }
                warnings.addAll(p1warnings)
                p1warnings.clear()
            } catch (e: AssemblerError) {
                errors.add(AssemblerError(currentLineNumber, e))
            }
        }
    }

    private fun preprocess(line: String) {
        val DIRECTIVE_DEFINE = "define"
        val DIRECTIVE_UNDEF = "undef"
        var pline = line.trim()
        if (pline.startsWith("#")) {
            pline = pline.removePrefix("#").trim()
            if (pline.startsWith(DIRECTIVE_DEFINE)) {
                pline = pline.removePrefix(DIRECTIVE_DEFINE).trim()
                val tokens = pline.split(" ").toMutableList()
                defines[tokens.removeAt(0)] = tokens.joinToString(" ")
            }
            if (pline.startsWith(DIRECTIVE_UNDEF)) {
                pline = pline.removePrefix(DIRECTIVE_UNDEF).trim()
                val tokens = pline.split(" ")
                checkArgsLength(tokens, 1)
                defines.remove(tokens[0])
            }
        }
    }

    /** Gets the current offset (either text or data) depending on where we are writing */
    fun getOffset() = if (inTextSegment) currentTextOffset else currentDataOffset

    /**
     * Determines if the given token is an assembler directive
     *
     * @param cmd the token to check
     * @return true if the token is an assembler directive
     * @see parseAssemblerDirective
     */
    private fun isAssemblerDirective(cmd: String) = cmd.startsWith(".")

    /**
     * Replaces any pseudoinstructions which occur in our program.
     *
     * @param tokens a list of strings corresponding to the space delimited line
     * @return the corresponding TAL instructions (possibly unchanged)
     */
    private fun replacePseudoInstructions(tokens: LineTokens): List<LineTokens> {
        try {
            val cmd = getInstruction(tokens)
            // This is meant to allow for cmds with periods since the pseudodispatcher does not allow for special chars.
            val cleanedCMD = cmd.replace(".", "")
            val pw = PseudoDispatcher.valueOf(cleanedCMD).pw
            return pw(tokens, this)
        } catch (t: Throwable) {
            /* TODO: don't use throwable here */
            /* not a pseudoinstruction, or expansion failure */
            val linetokens = parsePossibleMachineCode(tokens)
            return linetokens
        }
    }

    private fun parsePossibleMachineCode(tokens: LineTokens): List<LineTokens> {
        val c = getInstruction(tokens)
        if (c in listOf("beq", "bge", "bgeu", "blt", "bltu", "bne")) {
            try {
                val loc = getOffset() + userStringToInt(tokens[3])
                prog.addLabel(venusInternalLabels + loc.toString(), loc)
//                warnings.add(AssemblerWarning("Interpreting label as immediate"))
//                return listOf(listOf(tokens[0], tokens[1], tokens[2], "L" + loc.toString()))
            } catch (e: Throwable) {
                //
            }
        } else if (c == "jal") {
            try {
                val loc = getOffset() + userStringToInt(tokens[2])
                prog.addLabel(venusInternalLabels + loc.toString(), loc)
//                warnings.add(AssemblerWarning("Interpreting label as immediate"))
//                return listOf(listOf(tokens[0], tokens[1], "L" + loc.toString()))
            } catch (e: Throwable) {
                //
            }
        } else {
            try {
                var cmd = userStringToInt(c)
                try {
                    val decoded = Instruction[MachineCode(cmd)].disasm(MachineCode(cmd))
                    val lex = Lexer.lexLine(decoded).second.toMutableList()
                    if (lex[0] == "jal") {
                        val loc = getOffset() + lex[2].toInt()
                        prog.addLabel("L" + loc.toString(), loc)
                        lex[2] = "L" + loc.toString()
                    }
                    if (lex[0] in listOf("beq", "bge", "bgeu", "blt", "bltu", "bne")) {
                        val loc = getOffset() + lex[3].toInt()
                        prog.addLabel("L" + loc.toString(), loc)
                        lex[3] = "L" + loc.toString()
                    }
                    val t = listOf(lex)
                    return t
                } catch (e: SimulatorError) {
                    errors.add(AssemblerError(currentLineNumber, e))
                }
            } catch (e: NumberFormatException) {
                if (c.startsWith("0x") || c.startsWith("0b") || c.matches(Regex("\\d+"))) {
                    errors.add(AssemblerError(currentLineNumber, e))
                }
            }
        }
        return listOf(tokens)
    }

    /**
     * Changes the assembler state in response to directives
     *
     * @param directive the assembler directive, starting with a "."
     * @param args any arguments following the directive
     * @param line the original line (which is needed for some directives)
     */
    private fun parseAssemblerDirective(directive: String, args: LineTokens, line: String) {
        when (directive) {
            ".data" -> inTextSegment = false
            ".text" -> {
                inTextSegment = true
            }

            ".register_size" -> {
                if (!allow_custom_memory_segments) {
                    throw AssemblerError("""You can only set the register size address BEFORE any labels or
                        |instructions have been processed""".trimMargin())
                }
                try {
                    checkArgsLength(args, 1)
                } catch (e: AssemblerError) {
                    throw AssemblerError("$directive takes in zero or one argument(s) to specify encoding!")
                }
                val instwidth = userStringToInt(args[0])
                if (!listOf(16, 32, 64, 128).contains(instwidth)) {
                    throw AssemblerError("Unknown instruction size!")
                }
                Renderer.displayWarning("Will set width to $instwidth!")
            }

            ".data_start" -> {
                if (!allow_custom_memory_segments) {
                    throw AssemblerError("""You can only set the data start address BEFORE any labels or
                        |instructions have been processed""".trimMargin())
                }
                checkArgsLength(args, 1)
                val location = userStringToInt(args[0])
                MemorySegments.STATIC_BEGIN = location
            }

            ".byte" -> {
                for (arg in args) {
                    val byte = userStringToInt(arg)
                    if (byte !in -127..255) {
                        throw AssemblerError("invalid byte $byte too big")
                    }
                    prog.addToData(byte.toByte())
                    currentDataOffset++
                }
            }

            ".string", ".asciiz" -> {
                checkArgsLength(args, 1)
                val ascii: String = try {
                    val str = args[0]
                    if (str.length < 2 || str[0] != str[str.length - 1] || str[0] != '"') {
                        throw IllegalArgumentException()
                    }
                    unescapeString(str.drop(1).dropLast(1))
                } catch (e: Throwable) {
                    throw AssemblerError("couldn't parse ${args[0]} as a string")
                }
                for (c in ascii) {
                    if (c.toInt() !in 0..127) {
                        throw AssemblerError("unexpected non-ascii character: $c")
                    }
                    prog.addToData(c.toByte())
                    currentDataOffset++
                }

                /* Add NUL terminator */
                prog.addToData(0)
                currentDataOffset++
            }

            ".word" -> {
                for (arg in args) {
                    try {
                        val word = userStringToInt(arg)
                        prog.addToData(word.toByte())
                        prog.addToData((word shr 8).toByte())
                        prog.addToData((word shr 16).toByte())
                        prog.addToData((word shr 24).toByte())
                    } catch (e: NumberFormatException) {
                        /* arg is not a number; interpret as label */
                        prog.addDataRelocation(
                                prog.symbolPart(arg),
                                prog.labelOffsetPart(arg),
                                currentDataOffset - MemorySegments.STATIC_BEGIN)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                    }
                    currentDataOffset += 4
                }
            }

            ".globl", "global" -> {
                args.forEach(prog::makeLabelGlobal)
            }

            ".import" -> {
                checkArgsLength(args, 1)
                var filepath = args[0]
                if (filepath.matches(Regex("\".*\"|'.*'"))) {
                    filepath = filepath.slice(1..(filepath.length - 2))
                }
                prog.addImport(filepath)
            }

            ".space" -> {
                checkArgsLength(args, 1)
                try {
                    val reps = userStringToInt(args[0])
                    for (c in 1..reps) {
                        prog.addToData(0)
                    }
                    currentDataOffset += reps
                } catch (e: NumberFormatException) {
                    throw AssemblerError("${args[0]} not a valid argument")
                }
            }

            ".align" -> {
                checkArgsLength(args, 1)
                val pow2 = userStringToInt(args[0])
                if (pow2 < 0 || pow2 > 8) {
                    throw AssemblerError(".align argument must be between 0 and 8, inclusive")
                }
                val mask = (1 shl pow2) - 1 // Sets pow2 rightmost bits to 1
                /* Add padding until data offset aligns with given power of 2 */
                while ((currentDataOffset and mask) != 0) {
                    prog.addToData(0)
                    currentDataOffset++
                }
            }

            ".equiv", ".equ", ".set" -> {
                checkArgsLength(args, 2)
                val oldDefn = prog.addEqu(args[0], args[1])
                if (directive == ".equiv" && oldDefn != null) {
                    throw AssemblerError("attempt to redefine ${args[0]}")
                }
            }

            ".float" -> {
                for (arg in args) {
                    try {
                        val float = userStringToFloat(arg)
                        val bits = float.toRawBits()
                        prog.addToData(bits.toByte())
                        prog.addToData((bits shr 8).toByte())
                        prog.addToData((bits shr 16).toByte())
                        prog.addToData((bits shr 24).toByte())
                    } catch (e: NumberFormatException) {
                        /* arg is not a number; interpret as label */
                        prog.addDataRelocation(arg, currentDataOffset - MemorySegments.STATIC_BEGIN)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                    }
                    currentDataOffset += 4
                }
            }

            ".double" -> {
                for (arg in args) {
                    try {
                        val double = userStringToDouble(arg)
                        val bits = double.toRawBits()
                        prog.addToData(bits.toByte())
                        prog.addToData((bits shr 8).toByte())
                        prog.addToData((bits shr 16).toByte())
                        prog.addToData((bits shr 24).toByte())
                        prog.addToData((bits shr 32).toByte())
                        prog.addToData((bits shr 40).toByte())
                        prog.addToData((bits shr 48).toByte())
                        prog.addToData((bits shr 56).toByte())
                    } catch (e: NumberFormatException) {
                        /* arg is not a number; interpret as label */
                        prog.addDataRelocation(arg, currentDataOffset - MemorySegments.STATIC_BEGIN)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                        prog.addToData(0)
                    }
                    currentDataOffset += 8
                }
            }

            else -> throw AssemblerError("unknown assembler directive $directive")
        }
    }

    fun addRelocation(relocator: Relocator, offset: Int, label: String) =
            prog.addRelocation(
                    relocator, prog.symbolPart(label),
                    prog.labelOffsetPart(label), offset)
}

/**
 * Pass #2 of our two pass assembler.
 *
 * It writes TAL instructions to the program, and also adds debug info to the program.
 * @see addInstruction
 * @see venus.riscv.Program.addDebugInfo
 */

internal class AssemblerPassTwo(val prog: Program, val talInstructions: List<DebugInstruction>) {
    private val errors = ArrayList<AssemblerError>()
    private val warnings = ArrayList<AssemblerWarning>()
    fun run(): AssemblerOutput {
        resolveEquivs(prog)
        for ((dbg, inst) in talInstructions) {
            try {
                addInstruction(inst, dbg)
                prog.addDebugInfo(dbg)
                if (getImmWarning != "") {
                    val (lineNumber, _) = dbg
                    warnings.add(AssemblerWarning(lineNumber, AssemblerWarning(getImmWarning)))
                    getImmWarning = ""
                }
            } catch (e: AssemblerError) {
                val (lineNumber, _) = dbg
                if (e.errorType is InstructionNotFoundError) {
                    val cmd = getInstruction(inst)
                    // This is meant to allow for cmds with periods since the pseudodispatcher does not allow for special chars.
                    val cleanedCMD = cmd.replace(".", "")
                    val pw = try {
                        PseudoDispatcher.valueOf(cleanedCMD).pw
                    } catch (_: Throwable) {
                        errors.add(AssemblerError(lineNumber, e))
                        continue
                    }
                    try {
                        pw(inst, AssemblerPassOne(""))
                        errors.add(AssemblerError(lineNumber, e))
                    } catch (pe: Throwable) {
                        errors.add(AssemblerError(lineNumber, pe))
                    }
                } else {
                    errors.add(AssemblerError(lineNumber, e))
                }
            }
        }
        return AssemblerOutput(prog, errors, warnings)
    }

    /**
     * Adds machine code corresponding to our instruction to the program.
     *
     * @param tokens a list of strings corresponding to the space delimited line
     */
    private fun addInstruction(tokens: LineTokens, dbg: DebugInfo) {
        if (tokens.isEmpty() || tokens[0].isEmpty()) return
        val cmd = getInstruction(tokens)
        val inst = Instruction[cmd]
        val mcode = inst.format.fill()
        inst.parser(prog, mcode, tokens.drop(1), dbg)
        prog.add(mcode)
    }

    /** Resolve all labels in PROG defined by .equiv, .equ, or .set and add
     *  these to PROG as ordinary labels.  Checks for duplicate or
     *  conflicting definition. */
    private fun resolveEquivs(prog: Program) {
        val conflicts = prog.labels.keys.intersect(prog.equivs.keys)
        if (conflicts.isNotEmpty()) {
            throw AssemblerError("conflicting definitions for $conflicts")
        }
        val processing = HashSet<String>()
        for (equiv in prog.equivs.keys) {
            if (equiv !in prog.labels.keys) {
                prog.labels[equiv] = findDefn(equiv, prog, processing)
            }
        }
    }
    /** Return the ultimate definition of SYM, an .equ-defined symbol, in
     *  PROG, assuming that if SYM is in ACTIVE, it is part of a
     *  circular chain of definitions. */
    private fun findDefn(sym: String, prog: Program, active: HashSet<String>): Int {
        // FIXME: Global symbols not defined in this program.
        if (sym in active) {
            throw AssemblerError("circularity in definition of $sym")
        }
        val value = prog.equivs[sym]!!
        if (isNumeral(value)) {
            return userStringToInt(value)
        } else if (value in prog.labels.keys) {
            return prog.labels[value]!!
        } else if (value in prog.equivs.keys) {
            active.add(sym)
            val result = findDefn(value, prog, active)
            active.remove(sym)
            return result
        } else {
            throw AssemblerError("undefined symbol: $value")
        }
    }
}

/**
 * Gets the instruction from a line of code
 *
 * @param tokens the tokens from the current line
 * @return the instruction (aka the first argument, in lowercase)
 */
private fun getInstruction(tokens: LineTokens) = tokens[0].toLowerCase()