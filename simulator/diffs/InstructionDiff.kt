package venusbackend.simulator.diffs

import venus.IRenderer
import venusbackend.simulator.Diff
import venusbackend.simulator.SimulatorState

class InstructionDiff(val idx: Int, val mc: Int, val orig: String) : Diff {
    override operator fun invoke(state: SimulatorState) {
        try {
            IRenderer.getRenderer().updateProgramListing(idx, mc, orig)
        } catch (e: Throwable) {}
    }
}