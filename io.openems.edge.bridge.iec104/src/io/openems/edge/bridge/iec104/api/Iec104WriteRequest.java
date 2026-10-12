package io.openems.edge.bridge.iec104.api;

/**
 * A write request to send to the RTU via IEC104.
 *
 * <p>
 * Supports:
 * <ul>
 * <li>Single command (C_SC_NA_1) - for on/off control
 * <li>Set-point command (C_SE_NA_1 / C_SE_NC_1) - for AGC power set-points
 * </ul>
 */
public record Iec104WriteRequest(//
		int informationObjectAddress, //
		Type type, //
		double value, //
		boolean selectBeforeExecute //
) {

	public enum Type {
		SINGLE_COMMAND, // C_SC_NA_1
		DOUBLE_COMMAND, // C_DC_NA_1
		SETPOINT_NORMALIZED, // C_SE_NA_1
		SETPOINT_SCALED, // C_SE_NB_1
		SETPOINT_SHORT_FLOAT, // C_SE_NC_1
	}

	/**
	 * Create a set-point (AGC) write request with short float value.
	 *
	 * @param address the information object address
	 * @param value   the power set-point in Watts
	 * @return Iec104WriteRequest
	 */
	public static Iec104WriteRequest setPointFloat(int address, double value) {
		return new Iec104WriteRequest(address, Type.SETPOINT_SHORT_FLOAT, value, true);
	}

	/**
	 * Create a single command (on/off) write request.
	 *
	 * @param address the information object address
	 * @param on      true = ON, false = OFF
	 * @return Iec104WriteRequest
	 */
	public static Iec104WriteRequest singleCommand(int address, boolean on) {
		return new Iec104WriteRequest(address, Type.SINGLE_COMMAND, on ? 1 : 0, true);
	}
}
