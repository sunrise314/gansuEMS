package io.openems.edge.rtu.iec104;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.iec104.BridgeIec104;
import io.openems.edge.bridge.iec104.api.AbstractOpenemsIec104Component;
import io.openems.edge.bridge.iec104.api.Iec104Protocol;
import io.openems.edge.bridge.iec104.api.Iec104WriteRequest;
import io.openems.edge.bridge.iec104.api.TeleSignal;
import io.openems.edge.bridge.iec104.api.TeleValue;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * RTU (Remote Terminal Unit) device implementation via IEC 60870-5-104.
 *
 * <p>
 * This component connects to an RTU through an IEC104 bridge and provides:
 * <ul>
 * <li>Telemetry reading: active/reactive power, voltage, current, frequency,
 * SOC
 * <li>Tele-signal reading: grid status, fault status, remote control mode
 * <li>Control commands: AGC power set-point, grid connection command
 * </ul>
 *
 * <p>
 * Point mappings are configured via OSGi metatype configuration.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Rtu.Iec104", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class RtuIec104Impl extends AbstractOpenemsIec104Component implements RtuIec104, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(RtuIec104Impl.class);

	private Config config;

	public RtuIec104Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				RtuIec104.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.bridge_id());
		this.config = config;
		this.buildProtocol();
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled(), config.bridge_id());
		this.config = config;
		this.buildProtocol();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Build the IEC104 protocol point mappings from configuration.
	 */
	private void buildProtocol() {
		Iec104Protocol protocol = new Iec104Protocol();

		// Telemetry (analog values)
		addTeleValueIfEnabled(protocol, config.ioaActivePower(), ChannelId.ACTIVE_POWER, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaReactivePower(), ChannelId.REACTIVE_POWER, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaVoltageL1(), ChannelId.VOLTAGE_L1, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaVoltageL2(), ChannelId.VOLTAGE_L2, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaVoltageL3(), ChannelId.VOLTAGE_L3, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaCurrentL1(), ChannelId.CURRENT_L1, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaCurrentL2(), ChannelId.CURRENT_L2, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaCurrentL3(), ChannelId.CURRENT_L3, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaFrequency(), ChannelId.FREQUENCY, OpenemsType.INTEGER);
		addTeleValueIfEnabled(protocol, config.ioaBatterySoc(), ChannelId.BATTERY_SOC, OpenemsType.INTEGER);

		// Tele-signals (binary values)
		addTeleSignalIfEnabled(protocol, config.ioaGridConnected(), ChannelId.GRID_CONNECTED);
		addTeleSignalIfEnabled(protocol, config.ioaFaultState(), ChannelId.FAULT_STATE);
		addTeleSignalIfEnabled(protocol, config.ioaRemoteControl(), ChannelId.REMOTE_CONTROL_ENABLED);

		this.setIec104Protocol(protocol);
		this.logInfo(this.log, "IEC104 protocol configured: " //
				+ "bridge=" + config.bridge_id() + ", " //
				+ "AGC IOA=" + config.ioaAgcActivePowerSetpoint());
	}

	/**
	 * Add a telemetry mapping if the IOA is enabled (> 0).
	 */
	private void addTeleValueIfEnabled(Iec104Protocol protocol, int ioa, ChannelId channelId,
			OpenemsType type) {
		if (ioa > 0) {
			String channelAddress = this.id() + "/" + channelId.name();
			double scale = config.powerScaleFactor();
			double offset = config.powerOffset();
			protocol.addTeleValue(new TeleValue(ioa, channelAddress, type, scale, offset));
		}
	}

	/**
	 * Add a tele-signal mapping if the IOA is enabled (> 0).
	 */
	private void addTeleSignalIfEnabled(Iec104Protocol protocol, int ioa, ChannelId channelId) {
		if (ioa > 0) {
			String channelAddress = this.id() + "/" + channelId.name();
			protocol.addTeleSignal(new TeleSignal(ioa, channelAddress, false));
		}
	}

	// ===== Control command handlers =====

	/**
	 * Handle AGC Active Power Set-point write requests.
	 *
	 * <p>
	 * This method is called when the channel value is updated.
	 */
	// TODO: Implement channel write callback for AGC set-point
	// For now, provide a public method to send AGC commands

	/**
	 * Send AGC active power set-point command to RTU.
	 *
	 * @param powerWatts the active power set-point in Watts
	 * @throws OpenemsException on communication error
	 */
	public void sendAgcActivePowerSetpoint(int powerWatts) throws OpenemsException {
		if (config.ioaAgcActivePowerSetpoint() <= 0) {
			throw new OpenemsException("AGC Active Power Setpoint IOA is not configured");
		}

		Iec104WriteRequest request = Iec104WriteRequest.setPointFloat(
				config.ioaAgcActivePowerSetpoint(), powerWatts);

		this.sendWriteRequest(request);
		this.logInfo(this.log, "AGC set-point sent: " + powerWatts + " W");
	}

	/**
	 * Send grid connection/disconnection command to RTU.
	 *
	 * @param connect true = connect to grid, false = disconnect
	 * @throws OpenemsException on communication error
	 */
	public void sendGridConnectionCommand(boolean connect) throws OpenemsException {
		if (config.ioaGridConnectionCommand() <= 0) {
			throw new OpenemsException("Grid Connection Command IOA is not configured");
		}

		Iec104WriteRequest request = Iec104WriteRequest.singleCommand(
				config.ioaGridConnectionCommand(), connect);

		this.sendWriteRequest(request);
		this.logInfo(this.log, "Grid connection command sent: " + (connect ? "CONNECT" : "DISCONNECT"));
	}
}
