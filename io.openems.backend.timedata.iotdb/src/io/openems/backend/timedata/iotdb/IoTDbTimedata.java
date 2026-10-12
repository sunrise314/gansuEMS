package io.openems.backend.timedata.iotdb;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.iotdb.isession.pool.SessionDataSetWrapper;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.pool.SessionPool;
import org.apache.iotdb.tsfile.enums.TSDataType;
import org.apache.iotdb.tsfile.write.record.Tablet;
import org.apache.iotdb.tsfile.write.schema.IMeasurementSchema;
import org.apache.iotdb.tsfile.write.schema.MeasurementSchema;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

/**
 * Apache IoTDB Timedata implementation.
 *
 * <p>
 * Stores OpenEMS channel data in Apache IoTDB with the storage model:
 *
 * <pre>
 * root.ems.{edgeId}.{channelName}
 * </pre>
 *
 * <p>
 * Uses SessionPool for connection management and Tablet for batch inserts.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.IoTDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_ONLINE //
})
public class IoTDbTimedata extends AbstractOpenemsBackendComponent implements Timedata, EventHandler, DebugLoggable {

	private final Logger log = LoggerFactory.getLogger(IoTDbTimedata.class);

	@Reference
	protected volatile Metadata metadata;

	private Config config;
	private IoTDbConnector connector = null;

	// Cache of channel data types to avoid repeated type detection
	private final Map<String, TSDataType> channelTypeCache = new HashMap<>();

	private static final DateTimeFormatter SQL_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public IoTDbTimedata() {
		super("Timedata.IoTDB");
	}

	@Activate
	private void activate(Config config) {
		this.config = config;

		this.logInfo(this.log, "Activate IoTDB Timedata [" //
				+ "host=" + config.host() + ":" + config.port() + ";" //
				+ "rootPrefix=" + config.rootPrefix() + ";" //
				+ "poolSize=" + config.maxSize() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.connector = new IoTDbConnector(config);
		try {
			this.connector.init();
		} catch (IoTDBConnectionException e) {
			this.logError(this.log, "Failed to connect to IoTDB: " + e.getMessage());
		}
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate IoTDB Timedata");
		if (this.connector != null) {
			this.connector.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		// TODO: Handle edge online/offline events if needed
	}

	// ===== Write methods =====

	@Override
	public void write(String edgeId, TimestampedDataNotification notification) {
		if (this.config.isReadOnly() || this.connector == null) {
			return;
		}

		try {
			this.writeData(edgeId, notification);
		} catch (Exception e) {
			this.logError(this.log, "Error writing timestamped data: " + e.getMessage());
		}
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification notification) {
		if (this.config.isReadOnly() || this.connector == null) {
			return;
		}

		try {
			this.writeData(edgeId, notification);
		} catch (Exception e) {
			this.logError(this.log, "Error writing aggregated data: " + e.getMessage());
		}
	}

	@Override
	public void write(String edgeId, ResendDataNotification notification) {
		if (this.config.isReadOnly() || this.connector == null) {
			return;
		}

		try {
			this.writeData(edgeId, notification);
		} catch (Exception e) {
			this.logError(this.log, "Error writing resend data: " + e.getMessage());
		}
	}

	/**
	 * Actually write data to IoTDB using Tablet batch insert.
	 */
	private void writeData(String edgeId, TimestampedDataNotification notification)
			throws IoTDBConnectionException, StatementExecutionException {
		var data = notification.getData();
		var dataEntries = data.rowMap().entrySet();
		if (dataEntries.isEmpty()) {
			return;
		}

		// Group by timestamp for batch insert
		for (var dataEntry : dataEntries) {
			long timestamp = dataEntry.getKey();
			var channelValues = dataEntry.getValue();

			if (channelValues.isEmpty()) {
				continue;
			}

			String deviceId = this.buildDeviceId(edgeId);

			// Build tablet for this timestamp
			var schemaList = new java.util.ArrayList<IMeasurementSchema>();
			var measurementNames = new java.util.ArrayList<String>();

			for (var channelEntry : channelValues.entrySet()) {
				String channel = channelEntry.getKey();
				if (isBlacklisted(channel)) {
					continue;
				}

				TSDataType dataType = inferDataType(channelEntry.getValue());
				if (dataType != null) {
					String measurementName = channel.replace('/', '_').replace('.', '_');
					schemaList.add(new MeasurementSchema(measurementName, dataType));
					measurementNames.add(measurementName);
				}
			}

			if (schemaList.isEmpty()) {
				continue;
			}

			Tablet tablet = new Tablet(deviceId, schemaList, 1);
			int rowIndex = 0;
			tablet.addTimestamp(rowIndex, timestamp);

			int col = 0;
			for (var channelEntry : channelValues.entrySet()) {
				String channel = channelEntry.getKey();
				if (isBlacklisted(channel)) {
					continue;
				}

				TSDataType dataType = inferDataType(channelEntry.getValue());
				if (dataType == null) {
					continue;
				}

				String measurementName = measurementNames.get(col);
				Object value = channelEntry.getValue();

				switch (dataType) {
				case INT32:
					tablet.addValue(measurementName, rowIndex, ((Number) value).intValue());
					break;
				case INT64:
					tablet.addValue(measurementName, rowIndex, ((Number) value).longValue());
					break;
				case FLOAT:
					tablet.addValue(measurementName, rowIndex, ((Number) value).floatValue());
					break;
				case DOUBLE:
					tablet.addValue(measurementName, rowIndex, ((Number) value).doubleValue());
					break;
				case BOOLEAN:
					tablet.addValue(measurementName, rowIndex, (Boolean) value);
					break;
				case TEXT:
					tablet.addValue(measurementName, rowIndex, value.toString());
					break;
				default:
					break;
				}
				col++;
			}

			tablet.rowSize = 1;
			this.connector.getSessionPool().insertTablet(tablet);
		}
	}

	// ===== Query methods =====

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {

		if (this.connector == null) {
			return null;
		}

		// Build IoTDB SQL query
		StringBuilder sql = new StringBuilder("SELECT ");

		// Build column list
		boolean first = true;
		for (ChannelAddress channel : channels) {
			if (!first) {
				sql.append(", ");
			}
			String path = this.connector.buildPath(edgeId, channel.toString());
			sql.append(path);
			first = false;
		}

		// Build time range
		long startTs = fromDate.toInstant().toEpochMilli();
		long endTs = toDate.toInstant().toEpochMilli();

		sql.append(" FROM ").append(this.config.rootPrefix()).append(".").append(sanitizePath(edgeId))
				.append(" WHERE time >= ").append(startTs).append(" AND time <= ").append(endTs);

		// Add aggregation by resolution
		if (resolution != null) {
			String interval = buildInterval(resolution);
			sql.append(" GROUP BY ([")
					.append(startTs).append(", ")
					.append(endTs).append("), ")
					.append(interval).append(")");
		}

		this.logDebug(this.log, "IoTDB query: " + sql);

		try (SessionDataSetWrapper wrapper = this.connector.executeQuery(sql.toString())) {
			return parseQueryResult(wrapper, channels);
		} catch (Exception e) {
			this.logError(this.log, "Query failed: " + e.getMessage());
			return null;
		}
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {

		// Energy = last value - first value
		// Simplified: query last value in range
		SortedMap<ChannelAddress, JsonElement> result = new TreeMap<>();

		if (this.connector == null) {
			return result;
		}

		// TODO: Implement proper energy calculation
		// For now, return empty map
		return result;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {

		// TODO: Implement energy per period query
		return null;
	}

	// ===== Helper methods =====

	/**
	 * Build the device ID for IoTDB path.
	 */
	private String buildDeviceId(String edgeId) {
		return config.rootPrefix() + "." + sanitizePath(edgeId);
	}

	/**
	 * Sanitize a path component for IoTDB (replace special chars).
	 */
	private String sanitizePath(String path) {
		return path.replace('/', '_').replace('.', '_').replace('-', '_');
	}

	/**
	 * Infer IoTDB data type from a JsonElement value.
	 */
	private TSDataType inferDataType(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			// Use DOUBLE for all numbers to be safe
			return TSDataType.DOUBLE;
		} else if (value instanceof Boolean) {
			return TSDataType.BOOLEAN;
		} else if (value instanceof String) {
			return TSDataType.TEXT;
		}
		return null;
	}

	/**
	 * Check if a channel is blacklisted.
	 */
	private boolean isBlacklisted(String channel) {
		for (String blacklisted : config.blacklistedChannels()) {
			if (channel.equals(blacklisted)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Build IoTDB GROUP BY interval string from Resolution.
	 */
	private String buildInterval(Resolution resolution) {
		long value = resolution.getValue();
		switch (resolution.getUnit()) {
		case MILLIS:
			return value + "ms";
		case SECONDS:
			return value + "s";
		case MINUTES:
			return value + "m";
		case HOURS:
			return value + "h";
		case DAYS:
			return value + "d";
		default:
			return value + "m";
		}
	}

	/**
	 * Parse IoTDB query result into OpenEMS format.
	 */
	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> parseQueryResult(
			SessionDataSetWrapper wrapper, Set<ChannelAddress> channels) {

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();

		try {
			var dataIterator = wrapper.iterator();
			var columnNames = wrapper.getColumnNames();

			while (dataIterator.next()) {
				// Get timestamp
				long timestamp = dataIterator.getLong("Time");
				ZonedDateTime zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp),
						java.time.ZoneId.systemDefault());

				SortedMap<ChannelAddress, JsonElement> row = new TreeMap<>();

				for (ChannelAddress channel : channels) {
					String colName = channel.toString().replace('/', '_').replace('.', '_');
					// Find matching column
					for (String actualCol : columnNames) {
						if (actualCol.endsWith("." + colName)) {
							if (!dataIterator.isNull(actualCol)) {
								double value = dataIterator.getDouble(actualCol);
								row.put(channel, new JsonPrimitive(value));
							}
							break;
						}
					}
				}

				if (!row.isEmpty()) {
					result.put(zdt, row);
				}
			}
		} catch (Exception e) {
			this.logError(this.log, "Error parsing query result: " + e.getMessage());
		}

		return result;
	}

	@Override
	public String id() {
		return config != null ? config.id() : "Timedata.IoTDB";
	}
}
