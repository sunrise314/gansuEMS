package io.openems.edge.rtu.iec104;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * RTU (Remote Terminal Unit) device via IEC 60870-5-104 protocol.
 *
 * <p>
 * This interface defines the standard channels for a grid-tied RTU that
 * provides:
 * <ul>
 * <li>Telemetry: active/reactive power, voltage, current, frequency, SOC
 * <li>Tele-signals: grid connection status, fault status, remote/local mode
 * <li>Control: AGC active power set-point, grid connection command
 * </ul>
 */
public interface RtuIec104 extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// ===== Telemetry (Read-only) =====

		/**
		 * Active Power in [W]. Positive = injection to grid, negative = consumption.
		 */
		ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		/**
		 * Reactive Power in [var].
		 */
		REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //

		/**
		 * Grid voltage L1-N in [V].
		 */
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //

		/**
		 * Grid voltage L2-N in [V].
		 */
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //

		/**
		 * Grid voltage L3-N in [V].
		 */
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //

		/**
		 * Current L1 in [mA].
		 */
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		/**
		 * Current L2 in [mA].
		 */
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		/**
		 * Current L3 in [mA].
		 */
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		/**
		 * Grid frequency in [Hz].
		 */
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //

		/**
		 * Battery State of Charge in [%].
		 */
		BATTERY_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //

		// ===== Tele-signals (Read-only) =====

		/**
		 * True if the plant is connected to the grid.
		 */
		GRID_CONNECTED(Doc.of(Level.INFO) //
				.text("Plant is connected to grid")), //

		/**
		 * True if there is an active fault.
		 */
		FAULT_STATE(Doc.of(Level.WARNING) //
				.text("Active fault detected")), //

		/**
		 * True if the RTU is in remote control mode.
		 */
		REMOTE_CONTROL_ENABLED(Doc.of(Level.INFO) //
				.text("Remote control mode active")), //

		// ===== Control (Read/Write) =====

		/**
		 * AGC active power set-point in [W]. Set via write channel.
		 */
		AGC_ACTIVE_POWER_SETPOINT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		/**
		 * Command to connect/disconnect from grid.
		 */
		GRID_CONNECTION_COMMAND(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// ===== Convenience accessor methods =====

	/**
	 * Gets the Active Power in [W].
	 *
	 * @return the Channel value
	 */
	public default Value<Integer> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER).value();
	}

	/**
	 * Gets the Reactive Power in [var].
	 *
	 * @return the Channel value
	 */
	public default Value<Integer> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER).value();
	}

	/**
	 * Gets the Grid Connected state.
	 *
	 * @return true if connected to grid
	 */
	public default Value<Boolean> getGridConnected() {
		return this.channel(ChannelId.GRID_CONNECTED).value();
	}

	/**
	 * Gets the Fault State.
	 *
	 * @return true if fault is active
	 */
	public default Value<Boolean> getFaultState() {
		return this.channel(ChannelId.FAULT_STATE).value();
	}

	/**
	 * Gets the AGC Active Power Set-point write channel.
	 *
	 * @return the FloatWriteChannel for AGC set-point
	 */
	public default FloatWriteChannel getAacActivePowerSetpointChannel() {
		return this.channel(ChannelId.AGC_ACTIVE_POWER_SETPOINT);
	}

	/**
	 * Gets the Grid Connection Command write channel.
	 *
	 * @return the BooleanWriteChannel for grid connection
	 */
	public default BooleanWriteChannel getGridConnectionCommandChannel() {
		return this.channel(ChannelId.GRID_CONNECTION_COMMAND);
	}
}
