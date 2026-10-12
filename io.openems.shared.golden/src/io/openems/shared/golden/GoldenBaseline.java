package io.openems.shared.golden;

/**
 * A golden baseline: the recorded "known correct" output of one golden dataset
 * scenario.
 *
 * <p>
 * A baseline without a credibility chain makes regression pointless — the
 * golden version must be traceable to "which person, based on which evidence,
 * accepted it" ({@link #goldenCommit}, {@link #reviewNote}).
 *
 * @param id           unique scenario id, e.g. "replay-standard-12h-day"
 * @param category     the {@link GoldenCategory}
 * @param description  what this scenario targets / which failure mode it
 *                     covers
 * @param goldenCommit the commit hash of the golden version that produced the
 *                     baseline
 * @param reviewNote   human review record: why this output was accepted as
 *                     correct
 * @param expectedJson canonical expected output in
 *                     {@link GoldenJson#canonicalize} format
 */
public record GoldenBaseline(//
		String id, //
		GoldenCategory category, //
		String description, //
		String goldenCommit, //
		String reviewNote, //
		String expectedJson) {
}
