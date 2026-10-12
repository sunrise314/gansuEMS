package io.openems.edge.bridge.iec104;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.iec104.api.Iec104Protocol;
import io.openems.edge.bridge.iec104.api.Iec104WriteRequest;
import io.openems.edge.bridge.iec104.api.LogVerbosity;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Main interface for the IEC104 Bridge.
 *
 * <p>
 * Provides connection management to RTU devices via IEC 60870-5-104 protocol,
 * and allows registered components to read telemetry/tele-signals and send
 * write commands.
 */
@ProviderType
public interface BridgeIec104 extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Connection state to the RTU.
		 */
		CHANNEL_IS_CONNECTED(Doc.of(Level.INFO) //
				.text("IEC104 connection is established")), //

		/**
		 * Number of failed connection attempts.
		 */
		CONNECT_FAILED_COUNT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //

		/**
		 * Last received telemetry update timestamp.
		 */
		LAST_TELEGRAM_RECEIVED(Doc.of(OpenemsType.LONG) //
				.unit(Unit.MILLISECONDS)), //

		/**
		 * Number of ASDU received.
		 */
		RECEIVED_ASDU_COUNT(Doc.of(OpenemsType.LONG) //
				.unit(Unit.NONE)), //

		/**
		 * Number of ASDU sent.
		 */
		SENT_ASDU_COUNT(Doc.of(OpenemsType.LONG) //
				.unit(Unit.NONE)), //

		/**
		 * Communication error state.
		 */
		COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
				.text("IEC104 communication error")), //
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

	/**
	 * Gets the Channel for {@link ChannelId#CHANNEL_IS_CONNECTED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getChannelIsConnectedChannel() {
		return this.channel(ChannelId.CHANNEL_IS_CONNECTED);
	}

	/**
	 * Gets the connection state. See {@link ChannelId#CHANNEL_IS_CONNECTED}.
	 *
	 * @return true if connected
	 */
	public default Value<Boolean> isConnected() {
		return this.getChannelIsConnectedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHANNEL_IS_CONNECTED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChannelIsConnected(boolean value) {
		this.getChannelIsConnectedChannel().setNextValue(value);
	}

	/**
	 * Adds a Protocol with a source identifier to this IEC104 Bridge.
	 *
	 * <p>
	 * The protocol defines which information objects to read and their mapping to
	 * OpenEMS channels.
	 *
	 * @param sourceId the unique source identifier (usually component ID)
	 * @param protocol the IEC104 Protocol configuration
	 */
	public void addProtocol(String sourceId, Iec104Protocol protocol);

	/**
	 * Removes a Protocol from this IEC104 Bridge.
	 *
	 * @param sourceId the unique source identifier
	 */
	public void removeProtocol(String sourceId);

	/**
	 * Send a write request to the RTU.
	 *
	 * @param request the write request (command or set-point)
	 * @throws io.openems.common.exceptions.OpenemsException on communication error
	 */
	public void sendWriteRequest(Iec104WriteRequest request)
			throws io.openems.common.exceptions.OpenemsException;

	/**
	 * Get the current log verbosity level.
	 *
	 * @return LogVerbosity
	 */
	public LogVerbosity getLogVerbosity();
}
