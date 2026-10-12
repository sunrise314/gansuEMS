package io.openems.edge.energy.golden;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Temporary bootstrap generator: runs every golden scenario and writes its
 * canonical JSON to {@code build/golden/<id>.json}. Output is then reviewed
 * manually and copied into {@link GoldenEnergyBaselines}. Delete this class
 * after baselines are committed... no — keep it: it doubles as regeneration
 * tooling for intended baseline changes (ch12 process).
 */
public class GoldenEnergyBaselineGeneratorTest {

	private final Path outDir = Path.of("build", "golden");

	@BeforeEach
	void before() {
		GoldenEnergySimulatorTest.initializeRandomRegistryForGolden();
	}

	@Test
	void generateReplayStandardDay() throws Exception {
		write("replay-standard-12h-day", GoldenEnergySimulatorTest.runReplayStandardDay());
	}

	@Test
	void generateBoundarySocZero() throws Exception {
		write("boundary-soc-zero", GoldenEnergySimulatorTest.runBoundarySocZero());
	}

	@Test
	void generateExtremePriceSwingDay() throws Exception {
		write("extreme-price-swing-day", GoldenEnergySimulatorTest.runExtremePriceSwingDay());
	}

	@Test
	void generateInjectionShortSchedule() throws Exception {
		write("injection-short-schedule", GoldenEnergySimulatorTest.runInjectionShortSchedule());
	}

	@Test
	void generateInjectionEmptySchedule() throws Exception {
		write("injection-empty-schedule", GoldenEnergySimulatorTest.runInjectionEmptySchedule());
	}

	private void write(String id, io.openems.edge.energy.optimizer.SimulationResult result) throws Exception {
		Files.createDirectories(this.outDir);
		var json = io.openems.shared.golden.GoldenJson.canonicalize(//
				GoldenEnergySimulatorTest.goldenJson(result));
		Files.writeString(this.outDir.resolve(id + ".json"), json);
	}
}
