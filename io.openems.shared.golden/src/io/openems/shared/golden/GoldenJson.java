package io.openems.shared.golden;

import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Canonical JSON serialization for golden data regression.
 *
 * <p>
 * Guarantees deterministic, bit-exact comparable output:
 *
 * <ul>
 * <li>object keys are sorted lexicographically
 * <li>integral numbers are printed as long; other numbers via
 * {@link Double#toString} (shortest representation that round-trips to the same
 * double — a single ULP change in the last bit is detected)
 * <li>arrays keep their order
 * <li>multi-line indented output for line-based diffing
 * </ul>
 */
public final class GoldenJson {

	/** Largest double that is exactly representable as a long. */
	private static final double MAX_SAFE_INTEGRAL = 9.007199254740992E15;

	private GoldenJson() {
	}

	/**
	 * Serializes a {@link JsonElement} to its canonical multi-line form.
	 *
	 * @param e the {@link JsonElement}
	 * @return canonical string
	 */
	public static String canonicalize(JsonElement e) {
		return pretty(e, 0);
	}

	private static String pretty(JsonElement e, int indent) {
		final var b = new StringBuilder();
		switch (e) {
		case JsonNull n -> b.append("null");
		case JsonPrimitive p -> prettyPrimitive(b, p);
		case JsonArray a -> prettyArray(b, a, indent);
		case JsonObject o -> prettyObject(b, o, indent);
		default -> throw new IllegalArgumentException("Unknown JsonElement type: " + e.getClass());
		}
		return b.toString();
	}

	private static void prettyPrimitive(StringBuilder b, JsonPrimitive p) {
		if (p.isBoolean()) {
			b.append(p.getAsBoolean());
		} else if (p.isNumber()) {
			b.append(formatNumber(p.getAsDouble()));
		} else {
			b.append(quote(p.getAsString()));
		}
	}

	private static void prettyArray(StringBuilder b, JsonArray a, int indent) {
		if (a.isEmpty()) {
			b.append("[]");
			return;
		}
		b.append("[\n");
		for (var i = 0; i < a.size(); i++) {
			b.append(" ".repeat(indent + 2)) //
					.append(pretty(a.get(i), indent + 2));
			if (i < a.size() - 1) {
				b.append(",");
			}
			b.append("\n");
		}
		b.append(" ".repeat(indent)).append("]");
	}

	private static void prettyObject(StringBuilder b, JsonObject o, int indent) {
		if (o.isEmpty()) {
			b.append("{}");
			return;
		}
		final var sorted = new TreeMap<String, JsonElement>();
		for (var entry : o.entrySet()) {
			sorted.put(entry.getKey(), entry.getValue());
		}
		b.append("{\n");
		var it = sorted.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			b.append(" ".repeat(indent + 2)) //
					.append(quote(entry.getKey())) //
					.append(": ") //
					.append(pretty(entry.getValue(), indent + 2));
			if (it.hasNext()) {
				b.append(",");
			}
			b.append("\n");
		}
		b.append(" ".repeat(indent)).append("}");
	}

	/**
	 * Formats a double deterministically: integral values as long, otherwise the
	 * shortest round-trip representation.
	 *
	 * @param d the double
	 * @return canonical number string
	 */
	public static String formatNumber(double d) {
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return Double.toString(d); // NaN, Infinity, -Infinity compare textually
		}
		if (d == Math.rint(d) && Math.abs(d) <= MAX_SAFE_INTEGRAL) {
			return Long.toString((long) d);
		}
		return Double.toString(d);
	}

	private static String quote(String s) {
		final var b = new StringBuilder(s.length() + 2);
		b.append('"');
		for (var i = 0; i < s.length(); i++) {
			final var c = s.charAt(i);
			switch (c) {
			case '"' -> b.append("\\\"");
			case '\\' -> b.append("\\\\");
			case '\n' -> b.append("\\n");
			case '\r' -> b.append("\\r");
			case '\t' -> b.append("\\t");
			default -> {
				if (c < 0x20) {
					b.append(String.format("\\u%04x", (int) c));
				} else {
					b.append(c);
				}
			}
			}
		}
		b.append('"');
		return b.toString();
	}
}
