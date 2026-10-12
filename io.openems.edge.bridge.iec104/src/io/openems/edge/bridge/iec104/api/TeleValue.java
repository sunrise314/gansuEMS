package io.openems.edge.bridge.iec104.api;

import io.openems.common.types.OpenemsType;

/**
 * Telemetry value mapping: IEC104 Information Object Address -> OpenEMS
 * Channel.
 *
 * <p>
 * Used for analog measured values (M_ME_NA_1, M_ME_NB_1, M_ME_NC_1, etc.).
 */
public record TeleValue(//
		int informationObjectAddress, //
		String channelAddress, //
		OpenemsType openemsType, //
		double scaleFactor, //
		double offset //
) {

	/**
	 * Convenience constructor with default scale=1.0 and offset=0.0.
	 */
	public TeleValue(int informationObjectAddress, String channelAddress, OpenemsType openemsType) {
		this(informationObjectAddress, channelAddress, openemsType, 1.0, 0.0);
	}

	/**
	 * Apply scale and offset to a raw value.
	 *
	 * @param rawValue the raw value from IEC104
	 * @return transformed value
	 */
	public double transform(double rawValue) {
		return rawValue * this.scaleFactor + this.offset;
	}
}
