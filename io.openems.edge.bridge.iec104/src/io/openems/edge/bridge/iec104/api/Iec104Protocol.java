package io.openems.edge.bridge.iec104.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the IEC104 point mapping protocol for a device component.
 *
 * <p>
 * This class is built by the device component to declare:
 * <ul>
 * <li>Which telemetry (analog) points to read and their channel mapping
 * <li>Which tele-signal (binary) points to read and their channel mapping
 * <li>Which points are writable (control/set-point)
 * </ul>
 */
public class Iec104Protocol {

	/**
	 * Callback interface for updating channel values from received ASDUs.
	 */
	public interface ChannelUpdateCallback {
		/**
		 * Update a channel value.
		 *
		 * @param channelAddress the channel address (e.g. "meter0/ActivePower")
		 * @param value          the received value
		 */
		void updateChannelValue(String channelAddress, Object value);
	}

	private final List<TeleValue> teleValues = new ArrayList<>();
	private final List<TeleSignal> teleSignals = new ArrayList<>();
	private final List<Integer> periodicInterrogationAddresses = new ArrayList<>();

	/**
	 * Callback to parent component for channel updates.
	 */
	private ChannelUpdateCallback callback = null;

	/**
	 * Set the callback for channel value updates.
	 *
	 * @param callback the callback
	 */
	public void setCallback(ChannelUpdateCallback callback) {
		this.callback = callback;
	}

	/**
	 * Get the callback for channel value updates.
	 *
	 * @return the callback or null if not set
	 */
	public ChannelUpdateCallback getCallback() {
		return this.callback;
	}

	/**
	 * Add a telemetry (analog value) mapping.
	 *
	 * @param teleValue the telemetry mapping
	 * @return this
	 */
	public Iec104Protocol addTeleValue(TeleValue teleValue) {
		this.teleValues.add(teleValue);
		return this;
	}

	/**
	 * Add a tele-signal (binary value) mapping.
	 *
	 * @param teleSignal the tele-signal mapping
	 * @return this
	 */
	public Iec104Protocol addTeleSignal(TeleSignal teleSignal) {
		this.teleSignals.add(teleSignal);
		return this;
	}

	/**
	 * Add an address for periodic (cyclic) interrogation.
	 *
	 * @param informationObjectAddress the IOA to poll periodically
	 * @return this
	 */
	public Iec104Protocol addPeriodicInterrogation(int informationObjectAddress) {
		this.periodicInterrogationAddresses.add(informationObjectAddress);
		return this;
	}

	/**
	 * Find a tele-value mapping by IOA.
	 *
	 * @param ioa the information object address
	 * @return TeleValue or null if not found
	 */
	public TeleValue findTeleValue(int ioa) {
		for (TeleValue tv : this.teleValues) {
			if (tv.informationObjectAddress() == ioa) {
				return tv;
			}
		}
		return null;
	}

	/**
	 * Find a tele-signal mapping by IOA.
	 *
	 * @param ioa the information object address
	 * @return TeleSignal or null if not found
	 */
	public TeleSignal findTeleSignal(int ioa) {
		for (TeleSignal ts : this.teleSignals) {
			if (ts.informationObjectAddress() == ioa) {
				return ts;
			}
		}
		return null;
	}

	public List<TeleValue> getTeleValues() {
		return this.teleValues;
	}

	public List<TeleSignal> getTeleSignals() {
		return this.teleSignals;
	}

	public List<Integer> getPeriodicInterrogationAddresses() {
		return this.periodicInterrogationAddresses;
	}
}
