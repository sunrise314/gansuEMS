package io.openems.shared.golden;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

public class GoldenCheckTest {

	private static GoldenBaseline baseline(String expectedJson) {
		return new GoldenBaseline("test-scenario", GoldenCategory.BOUNDARY, "test description", "abc123",
				"test review note", expectedJson);
	}

	@Test
	void testMatchPasses() {
		var o = new JsonObject();
		o.addProperty("a", 1);
		assertDoesNotThrow(() -> GoldenCheck.assertMatches(//
				baseline(GoldenJson.canonicalize(o)), o));
	}

	@Test
	void testKeyOrderIrrelevant() {
		var expected = new JsonObject();
		expected.addProperty("a", 1);
		expected.addProperty("b", 2);
		var actual = new JsonObject();
		actual.addProperty("b", 2);
		actual.addProperty("a", 1);
		assertDoesNotThrow(() -> GoldenCheck.assertMatches(//
				baseline(GoldenJson.canonicalize(expected)), actual));
	}

	@Test
	void testValueMismatchFails() {
		var expected = new JsonObject();
		expected.addProperty("a", 1);
		var actual = new JsonObject();
		actual.addProperty("a", 2);
		var e = assertThrows(AssertionError.class, //
				() -> GoldenCheck.assertMatches(baseline(GoldenJson.canonicalize(expected)), actual));
		assertTrue(e.getMessage().contains("GOLDEN REGRESSION MISMATCH [BOUNDARY/test-scenario]"));
		assertTrue(e.getMessage().contains("-  \"a\": 1"));
		assertTrue(e.getMessage().contains("+  \"a\": 2"));
	}

	@Test
	void testMissingKeyFails() {
		var expected = new JsonObject();
		expected.addProperty("a", 1);
		expected.addProperty("b", 2);
		var actual = new JsonObject();
		actual.addProperty("a", 1);
		var e = assertThrows(AssertionError.class, //
				() -> GoldenCheck.assertMatches(baseline(GoldenJson.canonicalize(expected)), actual));
		assertTrue(e.getMessage().contains("-  \"b\": 2"));
	}

	@Test
	void testDoubleDriftDetected() {
		var expected = new JsonObject();
		expected.addProperty("v", 0.1 + 0.2);
		var actual = new JsonObject();
		actual.addProperty("v", Math.nextUp(0.1 + 0.2));
		assertThrows(AssertionError.class, //
				() -> GoldenCheck.assertMatches(baseline(GoldenJson.canonicalize(expected)), actual));
	}
}
