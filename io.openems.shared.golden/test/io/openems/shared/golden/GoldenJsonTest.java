package io.openems.shared.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

public class GoldenJsonTest {

	@Test
	void testSortsObjectKeys() {
		var o = new JsonObject();
		o.addProperty("b", 1);
		o.addProperty("a", 2);
		assertEquals("""
				{
				  "a": 2,
				  "b": 1
				}""", GoldenJson.canonicalize(o));
	}

	@Test
	void testIntegralNumbersAsLong() {
		var o = new JsonObject();
		o.addProperty("i", 42);
		o.addProperty("d", 42.0);
		o.addProperty("f", 3.14);
		o.addProperty("n", -0.0);
		assertEquals("""
				{
				  "d": 42,
				  "f": 3.14,
				  "i": 42,
				  "n": 0
				}""", GoldenJson.canonicalize(o));
	}

	@Test
	void testDoubleRoundTripPrecision() {
		var o = new JsonObject();
		o.addProperty("a", 0.1 + 0.2);
		assertEquals("0.30000000000000004", GoldenJson.canonicalize(o).replaceAll(".*: ", "").trim());
		// a single ULP change must be detectable
		var o2 = new JsonObject();
		o2.addProperty("a", Math.nextUp(0.1 + 0.2));
		assertEquals("0.3000000000000001", GoldenJson.canonicalize(o2).replaceAll(".*: ", "").trim());
	}

	@Test
	void testArraysKeepOrder() {
		var a = new com.google.gson.JsonArray();
		a.add(2);
		a.add(1);
		assertEquals("""
				[
				  2,
				  1
				]""", GoldenJson.canonicalize(a));
	}

	@Test
	void testEmptyAndNull() {
		assertEquals("{}", GoldenJson.canonicalize(new JsonObject()));
		assertEquals("[]", GoldenJson.canonicalize(new com.google.gson.JsonArray()));
		var o = new JsonObject();
		o.add("x", null);
		assertEquals("""
				{
				  "x": null
				}""", GoldenJson.canonicalize(o));
	}

	@Test
	void testStringEscaping() {
		var o = new JsonObject();
		o.addProperty("s", "a\"b\\c\n");
		assertEquals("""
				{
				  "s": "a\\"b\\\\c\\n"
				}""", GoldenJson.canonicalize(o));
	}
}
