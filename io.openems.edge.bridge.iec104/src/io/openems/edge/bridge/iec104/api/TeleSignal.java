package io.openems.edge.bridge.iec104.api;

/**
 * Tele-signal mapping: IEC104 Information Object Address -> OpenEMS Channel.
 *
 * <p>
 * Used for single point information (M_SP_NA_1) and double point information
 * (M_DP_NA_1).
 */
public record TeleSignal(//
		int informationObjectAddress, //
		String channelAddress, //
		boolean inverted //
) {

	/**
	 * Convenience constructor with inverted=false.
	 */
	public TeleSignal(int informationObjectAddress, String channelAddress) {
		this(informationObjectAddress, channelAddress, false);
	}
}
