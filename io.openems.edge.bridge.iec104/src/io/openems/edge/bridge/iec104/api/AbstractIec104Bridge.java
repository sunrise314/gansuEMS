package io.openems.edge.bridge.iec104.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.iec104.BridgeIec104;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Abstract base class for IEC104 Bridge implementations.
 *
 * <p>
 * Manages:
 * <ul>
 * <li>Protocol registration/unregistration from device components
 * <li>Connection state tracking
 * <li>Event cycle handling (read/write cycle)
 * </ul>
 *
 * <p>
 * Concrete subclasses need to implement the actual TCP connection and protocol
 * handling.
 */
public abstract class AbstractIec104Bridge extends AbstractOpenemsComponent
		implements BridgeIec104, EventHandler, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(AbstractIec104Bridge.class);

	private LogVerbosity logVerbosity = LogVerbosity.NONE;

	/**
	 * Registered protocols by sourceId (component ID).
	 */
	protected final Map<String, Iec104Protocol> protocols = new ConcurrentHashMap<>();

	/**
	 * The configuration - set by subclasses during activate/modified.
	 */
	protected Config config;

	public record Config(//
			String id, //
			String alias, //
			boolean enabled, //
			LogVerbosity logVerbosity //
	) {
	}

	protected AbstractIec104Bridge(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * OSGi activate method - called by subclasses.
	 */
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.logVerbosity = config.logVerbosity();
	}

	/**
	 * OSGi modified method - called by subclasses.
	 */
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.logVerbosity = config.logVerbosity();
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		this.protocols.clear();
	}

	@Override
	public void addProtocol(String sourceId, Iec104Protocol protocol) {
		this.protocols.put(sourceId, protocol);
		this.logInfo(this.log, "Protocol added for [" + sourceId + "]: " //
				+ protocol.getTeleValues().size() + " tele-values, " //
				+ protocol.getTeleSignals().size() + " tele-signals");
	}

	@Override
	public void removeProtocol(String sourceId) {
		this.protocols.remove(sourceId);
		this.logInfo(this.log, "Protocol removed for [" + sourceId + "]");
	}

	@Override
	public LogVerbosity getLogVerbosity() {
		return this.logVerbosity;
	}

	/**
	 * Handle IEC104 received ASDU - called by the protocol layer when data
	 * arrives.
	 *
	 * <p>
	 * This method distributes received telemetry/tele-signals to registered
	 * protocols and updates the corresponding OpenEMS channels.
	 *
	 * @param informationObjectAddress the IOA from the received ASDU
	 * @param rawValue                 the received raw value (Float for analog,
	 *                                 Boolean for binary)
	 * @param isBinary                 true if this is a binary signal
	 */
	protected void onReceiveAsdu(int informationObjectAddress, Object rawValue, boolean isBinary) {
		this._setLastTelegramReceived(System.currentTimeMillis());
		this._setReceivedAsduCount(this.getReceivedAsduCount() + 1);

		// Distribute to all registered protocols
		for (var entry : this.protocols.entrySet()) {
			var sourceId = entry.getKey();
			var protocol = entry.getValue();

			var callback = protocol.getCallback();
			if (callback == null) {
				continue;
			}

			if (isBinary) {
				TeleSignal signal = protocol.findTeleSignal(informationObjectAddress);
				if (signal != null) {
					boolean value = (Boolean) rawValue;
					if (signal.inverted()) {
						value = !value;
					}
					callback.updateChannelValue(signal.channelAddress(), value);
					this.logDebug(this.log, String.format(
							"Updated tele-signal [%d] = %b for %s (component: %s)",
							informationObjectAddress, value, signal.channelAddress(), sourceId));
				}
			} else {
				TeleValue tele = protocol.findTeleValue(informationObjectAddress);
				if (tele != null) {
					double rawDouble = ((Number) rawValue).doubleValue();
					double transformed = tele.transform(rawDouble);
					callback.updateChannelValue(tele.channelAddress(), transformed);
					this.logDebug(this.log, String.format(
							"Updated tele-value [%d] = %.2f (raw=%.2f) for %s (component: %s)",
							informationObjectAddress, transformed, rawDouble, tele.channelAddress(), sourceId));
				}
			}
		}
	}

	/**
	 * Event handler - processes OpenEMS cycle events.
	 *
	 * <p>
	 * Subclasses should call super.handleEvent(event) and then implement their own
	 * read/write cycle logic.
	 */
	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// Read phase: poll RTU if needed
			this.executeReadCycle();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			// Write phase: send pending commands
			this.executeWriteCycle();
			break;
		}
	}

	/**
	 * Execute one read cycle - poll RTU for updated values.
	 *
	 * <p>
	 * Implemented by subclasses (TCP variant).
	 */
	protected abstract void executeReadCycle();

	/**
	 * Execute one write cycle - send pending write requests.
	 *
	 * <p>
	 * Implemented by subclasses (TCP variant).
	 */
	protected abstract void executeWriteCycle();

	/**
	 * Get the current received ASDU count.
	 */
	private long getReceivedAsduCount() {
		var channel = this.channel(ChannelId.RECEIVED_ASDU_COUNT);
		var value = channel.value().get();
		return value != null ? value : 0L;
	}
}
