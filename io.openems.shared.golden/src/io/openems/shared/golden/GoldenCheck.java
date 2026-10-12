package io.openems.shared.golden;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * Golden regression check: compares actual output against a
 * {@link GoldenBaseline} bit-exactly.
 *
 * <p>
 * On mismatch the {@link AssertionError} contains baseline metadata (credibility
 * chain) plus a line-based diff excerpt around the first divergence.
 *
 * <p>
 * Process on intended changes (never allow "seemingly harmless" exceptions):
 * review the change manually, update the baseline constant, commit change and
 * baseline together — baseline evolution keeps the same pace as code evolution.
 */
public final class GoldenCheck {

	private static final int DIFF_CONTEXT_LINES = 5;
	private static final int DIFF_MAX_DIFF_LINES = 12;

	private GoldenCheck() {
	}

	/**
	 * Asserts that the canonical form of {@code actual} matches the baseline
	 * bit-exactly.
	 *
	 * @param baseline the {@link GoldenBaseline}
	 * @param actual   the actual output as {@link JsonObject}
	 */
	public static void assertMatches(GoldenBaseline baseline, JsonObject actual) {
		final var expected = baseline.expectedJson();
		final var actualCanonical = GoldenJson.canonicalize(actual);
		if (expected.equals(actualCanonical)) {
			return;
		}
		throw new AssertionError(message(baseline, expected, actualCanonical));
	}

	/**
	 * Builds a readable mismatch message with credibility chain and diff excerpt.
	 *
	 * @param baseline the {@link GoldenBaseline}
	 * @param expected canonical expected output
	 * @param actual   canonical actual output
	 * @return the mismatch message
	 */
	public static String message(GoldenBaseline baseline, String expected, String actual) {
		final var b = new StringBuilder();
		b.append("GOLDEN REGRESSION MISMATCH [").append(baseline.category()).append("/").append(baseline.id())
				.append("]\n");
		b.append("  goldenCommit: ").append(baseline.goldenCommit()).append("\n");
		b.append("  description:  ").append(baseline.description()).append("\n");
		b.append("  reviewNote:   ").append(baseline.reviewNote()).append("\n");
		b.append(diff(expected, actual));
		b.append("\nIf this change is intended: review the change, update the baseline constant, ")
				.append("and commit code + baseline together. Never allow 'seemingly harmless' exceptions.");
		return b.toString();
	}

	/**
	 * Builds a unified line-diff excerpt around the first divergence.
	 *
	 * @param expected canonical expected output
	 * @param actual   canonical actual output
	 * @return the diff text
	 */
	public static String diff(String expected, String actual) {
		final var e = expected.split("\n", -1);
		final var a = actual.split("\n", -1);
		int first = -1;
		final int min = Math.min(e.length, a.length);
		for (var i = 0; i < min; i++) {
			if (!e[i].equals(a[i])) {
				first = i;
				break;
			}
		}
		final var b = new StringBuilder();
		if (first < 0 && e.length == a.length) {
			b.append("(outputs identical)");
			return b.toString();
		}
		if (first < 0) {
			first = min; // common prefix, difference is in length
		}
		final var from = Math.max(0, first - DIFF_CONTEXT_LINES);
		final var to = Math.min(Math.max(e.length, a.length), first + DIFF_MAX_DIFF_LINES + DIFF_CONTEXT_LINES);
		b.append("--- expected (golden)\n+++ actual\n");
		for (var i = from; i < to; i++) {
			final var inE = i < e.length;
			final var inA = i < a.length;
			if (inE && inA && e[i].equals(a[i])) {
				b.append("  ").append(e[i]).append("\n");
			} else {
				if (inE) {
					b.append("-").append(e[i]).append("\n");
				}
				if (inA) {
					b.append("+").append(a[i]).append("\n");
				}
			}
		}
		b.append("(first difference at line ").append(first + 1) //
				.append("; expected ").append(e.length).append(" lines vs actual ").append(a.length)
				.append(" lines)");
		return b.toString();
	}

	/**
	 * Returns the lines that differ between expected and actual, or an empty list
	 * if identical.
	 *
	 * @param expected canonical expected output
	 * @param actual   canonical actual output
	 * @return list of differing expected/actual line pairs
	 */
	public static List<String[]> differingLines(String expected, String actual) {
		final var result = new ArrayList<String[]>();
		final var e = expected.split("\n", -1);
		final var a = actual.split("\n", -1);
		final int max = Math.max(e.length, a.length);
		for (var i = 0; i < max; i++) {
			final var el = i < e.length ? e[i] : null;
			final var al = i < a.length ? a[i] : null;
			if (!java.util.Objects.equals(el, al)) {
				result.add(new String[] { el, al });
			}
		}
		return result;
	}
}
