package io.openems.edge.energy.golden;

import io.openems.shared.golden.GoldenBaseline;
import io.openems.shared.golden.GoldenCategory;

/**
 * Golden baselines for the Energy {@link Simulator} golden regression
 * ({@link GoldenEnergySimulatorTest}).
 *
 * <p>
 * Credibility chain: each baseline records the golden version commit and a
 * review note. Regenerate with {@link GoldenEnergyBaselineGeneratorTest}, review
 * manually, and commit code + baseline together.
 */
public final class GoldenEnergyBaselines {

	/** The golden version that produced these baselines. */
	public static final String GOLDEN_COMMIT = "bf2930e80389";

	private GoldenEnergyBaselines() {
	}

	public static final GoldenBaseline REPLAY_STANDARD_12H_DAY = new GoldenBaseline(//
			"replay-standard-12h-day", //
			GoldenCategory.REPLAY, //
			"Representative 12h day (48 periods), varying prices and production, fixed schedule replay. "
					+ "Detects systematic offsets.", //
			GOLDEN_COMMIT, //
			"PENDING-BOOTSTRAP: placeholder, to be replaced by reviewed generator output", //
			"{}");

	public static final GoldenBaseline BOUNDARY_SOC_ZERO = new GoldenBaseline(//
			"boundary-soc-zero", //
			GoldenCategory.BOUNDARY, //
			"SOC exactly at lower limit (0 Wh) and grid buy soft limit exactly at maxBuyPower. "
					+ "ESS discharge must clamp to zero.", //
			GOLDEN_COMMIT, //
			"PENDING-BOOTSTRAP: placeholder, to be replaced by reviewed generator output", //
			"{}");

	public static final GoldenBaseline EXTREME_PRICE_SWING_DAY = new GoldenBaseline(//
			"extreme-price-swing-day", //
			GoldenCategory.EXTREME, //
			"Extreme spot price volatility incl. deeply negative prices; full genetic engine "
					+ "(population 20, 10 generations, seeded RNG).", //
			GOLDEN_COMMIT, //
			"PENDING-BOOTSTRAP: placeholder, to be replaced by reviewed generator output", //
			"{}");

	public static final GoldenBaseline INJECTION_SHORT_SCHEDULE = new GoldenBaseline(//
			"injection-short-schedule", //
			GoldenCategory.INJECTION, //
			"Schedule shorter than period list; remaining periods fall back to mode index 0 gracefully.", //
			GOLDEN_COMMIT, //
			"PENDING-BOOTSTRAP: placeholder, to be replaced by reviewed generator output", //
			"{}");

	public static final GoldenBaseline INJECTION_EMPTY_SCHEDULE = new GoldenBaseline(//
			"injection-empty-schedule", //
			GoldenCategory.INJECTION, //
			"Empty schedule input degrades to empty result without throwing.", //
			GOLDEN_COMMIT, //
			"PENDING-BOOTSTRAP: placeholder, to be replaced by reviewed generator output", //
			"{}");
}
