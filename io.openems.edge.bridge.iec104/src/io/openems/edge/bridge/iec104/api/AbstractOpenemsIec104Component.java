package io.openems.edge.bridge.iec104.api;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.iec104.BridgeIec104;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Abstract base class for OpenEMS components that use an IEC104 bridge.
 *
 * <p>
 * Device drivers (RTU, IED, etc.) should extend this class and provide an
 * {@link Iec104Protocol} that defines their point mapping.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * public class MyRtuDevice extends AbstractOpenemsIec104Component implements MyNature {
 *
 *   &#64;Activate
 *   protected void activate(ComponentContext context, Config config) {
 *     super.activate(context, config.id(), config.alias(), config.enabled(), config.bridgeId());
 *     // build protocol
 *     Iec104Protocol protocol = new Iec104Protocol()
 *         .addTeleValue(new TeleValue(100, "Channel/ActivePower", OpenemsType.INTEGER))
 *         .addTeleSignal(new TeleSignal(200, "Channel/GridState"));
 *     this.setModbusProtocol(protocol);
 *   }
 * }
 * </pre>
 */
public abstract class AbstractOpenemsIec104Component extends AbstractOpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsIec104Component.class);

	/**
	 * Reference to the IEC104 bridge - injected by OSGI.
	 */
	protected BridgeIec104 bridge = null;

	/**
	 * The bridge ID to use.
	 */
	private String bridgeId;

	/**
	 * The IEC104 protocol (point mappings).
	 */
	private Iec104Protocol protocol = null;

	protected AbstractOpenemsIec104Component(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * OSGi activate method.
	 *
	 * @param context   the ComponentContext
	 * @param id        component ID
	 * @param alias     human-readable alias
	 * @param enabled   enabled state
	 * @param bridgeId  the ID of the IEC104 bridge to use
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String bridgeId) {
		super.activate(context, id, alias, enabled);
		this.bridgeId = bridgeId;
	}

	/**
	 * OSGi modified method.
	 */
	protected void modified(ComponentContext context, String id, String alias, boolean enabled, String bridgeId) {
		super.modified(context, id, alias, enabled);
		this.bridgeId = bridgeId;
		// Re-attach to new bridge if changed
		if (this.bridge != null && this.protocol != null) {
			this.bridge.removeProtocol(this.id());
		}
	}

	@Override
	protected void deactivate() {
		if (this.bridge != null && this.protocol != null) {
			this.bridge.removeProtocol(this.id());
		}
		super.deactivate();
	}

	/**
	 * Set the IEC104 protocol (point mappings) for this component.
	 *
	 * <p>
	 * Call this during activate() after building the protocol.
	 *
	 * @param protocol the IEC104 protocol
	 */
	protected void setIec104Protocol(Iec104Protocol protocol) {
		this.protocol = protocol;
		// Set callback to route channel updates back to this component
		protocol.setCallback(this::updateChannelValue);
		if (this.bridge != null) {
			this.bridge.addProtocol(this.id(), protocol);
		}
	}

	/**
	 * Send a write request via the IEC104 bridge.
	 *
	 * @param request the write request
	 * @throws OpenemsException on communication error
	 */
	protected void sendWriteRequest(Iec104WriteRequest request) throws OpenemsException {
		if (this.bridge == null) {
			throw new OpenemsException("IEC104 Bridge is not available");
		}
		this.bridge.sendWriteRequest(request);
	}

	/**
	 * Update a channel value from received IEC104 telemetry.
	 *
	 * <p>
	 * Called by the bridge when data arrives for this component's mapping.
	 *
	 * @param channelAddress the channel address (e.g. "_sum/ActivePower")
	 * @param value          the received value
	 */
	public void updateChannelValue(String channelAddress, Object value) {
		try {
			String[] parts = channelAddress.split("/");
			if (parts.length == 2) {
				String componentId = parts[0];
				String channelId = parts[1];
				// Only update if it's our own channel
				if (componentId.equals(this.id())) {
					Channel<?> channel = this.channel(channelId);
					if (channel != null) {
						channel.setNextValue(value);
					}
				}
			}
		} catch (Exception e) {
			this.logError(this.log, "Error updating channel [" + channelAddress + "]: " + e.getMessage());
		}
	}

	/**
	 * Get the bridge ID.
	 *
	 * @return the bridge ID
	 */
	public String getBridgeId() {
		return this.bridgeId;
	}
}
