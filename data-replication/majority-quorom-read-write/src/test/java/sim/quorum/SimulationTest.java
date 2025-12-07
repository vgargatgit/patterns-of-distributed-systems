package sim.quorum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class SimulationTest {
    @Test
    void simulationRunsWithoutExceptionsWhenNoDrops() {
        // Args: nodeCount, dropProb, seed
        assertDoesNotThrow(() -> Simulation.main(new String[]{"3", "0.0", "7"}));
    }
}
