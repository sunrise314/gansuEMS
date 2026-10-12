package io.openems.shared.golden;

/**
 * Category of a golden regression dataset (see 'golden data regression'
 * concept).
 *
 * <p>
 * Each category targets a different failure mode:
 *
 * <ul>
 * <li><b>EXTREME</b>: extreme days (sandstorm, extreme spot price volatility,
 * sudden wind shutdown). Dense boundary conditions; lexicographic fitness
 * comparisons are most likely to flip sides on the boundary.
 * <li><b>BOUNDARY</b>: hand-crafted critical operating points (SOC exactly at
 * the limit, grid power exactly at the deviation band edge). Millisecond-level
 * floating-point drift shows up here first.
 * <li><b>REPLAY</b>: representative real operation days, fully replayed.
 * Detects systematic offsets (e.g. a constant 7% deviation) rather than single
 * points.
 * <li><b>INJECTION</b>: missing NWP data, communication jitter, device offline.
 * Verifies graceful degradation ('fail decently') instead of correctness.
 * </ul>
 */
public enum GoldenCategory {

	EXTREME, //
	BOUNDARY, //
	REPLAY, //
	INJECTION;
}
