package io.openems.edge.bridge.iec104;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.CauseOfTransmission;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.InformationElement;
import org.openmuc.j60870.InformationObject;
import org.openmuc.j60870.ie.IeQualifierOfInterrogation;
import org.openmuc.j60870.ie.IeShortFloat;
import org.openmuc.j60870.ie.IeSingleCommand;
import org.openmuc.j60870.ie.IeSinglePointWithQuality;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.iec104.api.AbstractIec104Bridge;
import io.openems.edge.bridge.iec104.api.Iec104WriteRequest;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * IEC104/TCP Bridge implementation using j60870 library.
 *
 * <p>
 * Connects to a Remote Terminal Unit (RTU) via IEC 60870-5-104 protocol over
 * TCP.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Automatic connection management with reconnection
 * <li>Receives telemetry (M_ME_NC_1 short float) and tele-signals (M_SP_NA_1)
 * <li>Sends remote commands (C_SC_NA_1) and set-point commands (C_SE_NC_1) for
 * AGC
 * <li>Periodic general interrogation for point synchronization
 * </ul>
 */
@Designate(ocd = ConfigTcp.class, factory = true)
@Component(//
		name = "Bridge.Iec104.Tcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class BridgeIec104TcpImpl extends AbstractIec104Bridge
		implements BridgeIec104Tcp, EventHandler, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(BridgeIec104TcpImpl.class);

	/**
	 * j60870 Connection object.
	 */
	private Connection connection = null;

	/**
	 * Scheduled executor for reconnection and periodic interrogation.
	 */
	private ScheduledExecutorService scheduler = null;

	// Configuration
	private String ip;
	private int port;
	private int asduCommonAddress;
	private int t0;
	private int t1;
	private int t2;
	private int t3;
	private boolean usePeriodicInterrogation;
	private int interrogationInterval;

	// State
	private volatile boolean connectRequested = false;
	private long lastInterrogationTime = 0;

	public BridgeIec104TcpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeIec104.ChannelId.values(), //
				StartStoppable.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, ConfigTcp config) {
		super.activate(context, new Config(//
				config.id(), //
				config.alias(), //
				config.enabled(), //
				config.logVerbosity() //
		));
		this.applyConfig(config);
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "IEC104-Bridge-" + this.id());
			t.setDaemon(true);
			return t;
		});
		this.connectRequested = true;
		this.connect();
	}

	@Modified
	private void modified(ComponentContext context, ConfigTcp config) {
		super.modified(context, new Config(//
				config.id(), //
				config.alias(), //
				config.enabled(), //
				config.logVerbosity() //
		));
		this.applyConfig(config);
		// Reconnect on config change
		this.disconnect();
		this.connectRequested = true;
		this.connect();
	}

	private void applyConfig(ConfigTcp config) {
		this.ip = config.ip();
		this.port = config.port();
		this.asduCommonAddress = config.asduCommonAddress();
		this.t0 = config.t0();
		this.t1 = config.t1();
		this.t2 = config.t2();
		this.t3 = config.t3();
		this.usePeriodicInterrogation = config.usePeriodicInterrogation();
		this.interrogationInterval = config.interrogationInterval();
	}

	/**
	 * Establish TCP connection to RTU using j60870.
	 */
	private synchronized void connect() {
		if (!this.connectRequested || this.connection != null) {
			return;
		}

		try {
			this.logInfo(this.log, "Connecting to RTU at " + this.ip + ":" + this.port + " ...");

			ClientConnectionBuilder builder = new ClientConnectionBuilder(this.ip) //
					.setPort(this.port) //
					.setConnectionTimeout(this.t0 * 1000) // t0 in ms
					.setMaxTimeNoAckReceived(this.t1 * 1000) // t1 in ms
					.setMaxTimeNoAckSent(this.t2 * 1000) // t2 in ms
					.setMaxIdleTime(this.t3 * 1000) // t3 in ms
					.setCommonAddressFieldLength(2) // 2-byte common address
					.setIoaFieldLength(3) // 3-byte IOA
					.setConnectionEventListener(new Iec104ConnectionListener());

			this.connection = builder.build();
			this.connection.startDataTransfer();

			this._setChannelIsConnected(true);
			this._setCommunicationError(false);
			this.logInfo(this.log, "Connected to RTU successfully");

			// Send initial general interrogation
			this.sendGeneralInterrogation();

		} catch (IOException e) {
			this.connection = null;
			this._setChannelIsConnected(false);
			this._setCommunicationError(true);
			this._setConnectFailedCount(this.getConnectFailedCount() + 1);
			this.logError(this.log, "Failed to connect to RTU: " + e.getMessage());

			// Schedule reconnect after 5 seconds
			if (this.connectRequested && this.scheduler != null) {
				this.scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * Disconnect from RTU.
	 */
	private synchronized void disconnect() {
		this.connectRequested = false;
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (Exception e) {
				this.logWarn(this.log, "Error closing connection: " + e.getMessage());
			}
			this.connection = null;
		}
		this._setChannelIsConnected(false);
		this.logInfo(this.log, "Disconnected from RTU");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.disconnect();
		if (this.scheduler != null) {
			this.scheduler.shutdownNow();
			this.scheduler = null;
		}
		super.deactivate();
	}

	/**
	 * Send general interrogation (C_IC_NA_1, Type 100).
	 */
	private void sendGeneralInterrogation() {
		if (this.connection == null) {
			return;
		}

		try {
			this.logDebug(this.log, "Sending general interrogation (C_IC_NA_1)");

			InformationObject io = new InformationObject(0, // IOA 0 for general interrogation
					new IeQualifierOfInterrogation(IeQualifierOfInterrogation.STATION));

			ASdu asdu = new ASdu(ASduType.C_IC_NA_1, //
					false, // not sequence of elements
					CauseOfTransmission.ACTIVATION, //
					false, // not test frame
					false, // not negative confirm
					0, // originator address
					this.asduCommonAddress, // common address
					io);

			this.connection.send(asdu);
			this.lastInterrogationTime = System.currentTimeMillis();
			this._setSentAsduCount(this.getSentAsduCount() + 1);

		} catch (Exception e) {
			this.logError(this.log, "Failed to send general interrogation: " + e.getMessage());
		}
	}

	@Override
	protected void executeReadCycle() {
		// Periodic general interrogation
		if (this.usePeriodicInterrogation && this.connection != null) {
			long now = System.currentTimeMillis();
			if (now - this.lastInterrogationTime > this.interrogationInterval * 1000L) {
				this.sendGeneralInterrogation();
			}
		}
	}

	@Override
	protected void executeWriteCycle() {
		// Write requests are sent immediately via sendWriteRequest()
		// No periodic write tasks needed
	}

	@Override
	public void sendWriteRequest(Iec104WriteRequest request) throws OpenemsException {
		if (this.connection == null) {
			throw new OpenemsException("IEC104 not connected to RTU");
		}

		this.logDebug(this.log, "Sending write request: IOA=" + request.informationObjectAddress() //
				+ ", type=" + request.type() //
				+ ", value=" + request.value() //
				+ ", SBO=" + request.selectBeforeExecute());

		try {
			switch (request.type()) {
			case SINGLE_COMMAND:
				this.sendSingleCommand(request);
				break;
			case SETPOINT_SHORT_FLOAT:
				this.sendSetPointFloat(request);
				break;
			case SETPOINT_NORMALIZED:
			case SETPOINT_SCALED:
			case DOUBLE_COMMAND:
				throw new OpenemsException("ASDU type " + request.type() + " not yet implemented");
			}
			this._setSentAsduCount(this.getSentAsduCount() + 1);
		} catch (Exception e) {
			throw new OpenemsException("Failed to send write request: " + e.getMessage());
		}
	}

	/**
	 * Send single command (C_SC_NA_1, Type 45) for on/off control.
	 */
	private void sendSingleCommand(Iec104WriteRequest request) throws IOException {
		boolean on = request.value() > 0;
		InformationObject io = new InformationObject(request.informationObjectAddress(), //
				new IeSingleCommand(on, //
						request.selectBeforeExecute(), // SBO=select before execute
						0, // QUU - not used
						false)); // not test

		ASdu asdu = new ASdu(ASduType.C_SC_NA_1, //
				false, //
				CauseOfTransmission.ACTIVATION, //
				false, //
				false, //
				0, //
				this.asduCommonAddress, //
				io);

		this.connection.send(asdu);
	}

	/**
	 * Send set-point command with short float (C_SE_NC_1, Type 50) for AGC power.
	 */
	private void sendSetPointFloat(Iec104WriteRequest request) throws IOException {
		InformationObject io = new InformationObject(request.informationObjectAddress(), //
				new IeShortFloat((float) request.value()));

		ASdu asdu = new ASdu(ASduType.C_SE_NC_1, //
				false, //
				CauseOfTransmission.ACTIVATION, //
				false, //
				false, //
				0, //
				this.asduCommonAddress, //
				io);

		this.connection.send(asdu);
	}

	/**
	 * Handle received ASDU from j60870 ConnectionEventListener.
	 */
	private void onAsduReceived(ASdu asdu) {
		this._setReceivedAsduCount(this.getReceivedAsduCount() + 1);
		this._setLastTelegramReceived(System.currentTimeMillis());

		ASduType type = asdu.getTypeIdentification();
		CauseOfTransmission cot = asdu.getCauseOfTransmission();

		this.logTrace(this.log, "Received ASDU: type=" + type + ", COT=" + cot);

		// Skip confirmation ASDUs (ACT_CON, ACT_TERM)
		if (cot == CauseOfTransmission.ACTIVATION_CON || cot == CauseOfTransmission.ACTIVATION_TERMINATION) {
			return;
		}

		InformationObject[] objects = asdu.getInformationObjects();
		if (objects == null) {
			return;
		}

		for (InformationObject io : objects) {
			int ioa = io.getInformationObjectAddress();
			this.parseInformationObject(ioa, type, io);
		}
	}

	/**
	 * Parse a single InformationObject and update channels accordingly.
	 */
	private void parseInformationObject(int ioa, ASduType type, InformationObject io) {
		InformationElement[] elements = io.getInformationElements();
		if (elements == null || elements.length == 0) {
			return;
		}

		switch (type) {
		case M_SP_NA_1: // Single-point information (tele-signal)
			if (elements[0] instanceof IeSinglePointWithQuality) {
				IeSinglePointWithQuality sp = (IeSinglePointWithQuality) elements[0];
				this.onReceiveAsdu(ioa, sp.isOn(), true);
			}
			break;

		case M_ME_NC_1: // Measured value, short float (telemetry)
		case M_ME_TF_1: // Measured value, short float with time tag
			if (elements[0] instanceof IeShortFloat) {
				IeShortFloat sf = (IeShortFloat) elements[0];
				this.onReceiveAsdu(ioa, sf.getValue(), false);
			}
			break;

		case M_DP_NA_1: // Double-point information
			// TODO: Implement double-point parsing when needed
			this.logTrace(this.log, "Double-point information received, IOA=" + ioa + " (not yet mapped)");
			break;

		case M_ME_NA_1: // Measured value, normalized value
		case M_ME_NB_1: // Measured value, scaled value
			// TODO: Implement normalized/scaled value parsing when needed
			this.logTrace(this.log, "Analog value type " + type + " received, IOA=" + ioa + " (not yet mapped)");
			break;

		default:
			this.logTrace(this.log, "Unhandled ASDU type: " + type + ", IOA=" + ioa);
			break;
		}
	}

	/**
	 * j60870 ConnectionEventListener implementation.
	 */
	private class Iec104ConnectionListener implements ConnectionEventListener {

		@Override
		public void newASdu(Connection connection, ASdu asdu) {
			try {
				onAsduReceived(asdu);
			} catch (Exception e) {
				log.error("Error processing received ASDU: " + e.getMessage(), e);
			}
		}

		@Override
		public void connectionClosed(Connection connection, IOException cause) {
			log.warn("Connection closed: " + cause.getMessage());
			BridgeIec104TcpImpl.this.connection = null;
			_setChannelIsConnected(false);
			_setCommunicationError(true);

			// Schedule reconnect
			if (connectRequested && scheduler != null) {
				scheduler.schedule(BridgeIec104TcpImpl.this::connect, 5, TimeUnit.SECONDS);
			}
		}

		@Override
		public void dataTransferStateChanged(Connection connection, boolean stopped) {
			log.info("Data transfer state changed: stopped=" + stopped);
			if (!stopped) {
				_setChannelIsConnected(true);
				_setCommunicationError(false);
				// Resend general interrogation after reconnection
				sendGeneralInterrogation();
			}
		}
	}

	private int getConnectFailedCount() {
		var channel = this.channel(ChannelId.CONNECT_FAILED_COUNT);
		var value = channel.value().get();
		return value != null ? value : 0;
	}

	private void _setConnectFailedCount(int value) {
		this.channel(ChannelId.CONNECT_FAILED_COUNT).setNextValue(value);
	}
}
