package io.openems.backend.timedata.iotdb;

import java.util.ArrayList;
import java.util.List;

import org.apache.iotdb.isession.pool.SessionDataSetWrapper;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.pool.SessionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IoTDB Connector - manages connection pool and provides low-level operations.
 */
public class IoTDbConnector {

	private final Logger log = LoggerFactory.getLogger(IoTDbConnector.class);

	private final Config config;
	private SessionPool sessionPool = null;

	public IoTDbConnector(Config config) {
		this.config = config;
	}

	/**
	 * Initialize the IoTDB connection pool.
	 *
	 * @throws IoTDBConnectionException on connection failure
	 */
	public void init() throws IoTDBConnectionException {
		List<String> nodeUrls = new ArrayList<>();
		nodeUrls.add(config.host() + ":" + config.port());

		this.sessionPool = new SessionPool.Builder() //
				.nodeUrls(nodeUrls) //
				.user(config.username()) //
				.password(config.password()) //
				.maxSize(config.maxSize()) //
				.build();

		log.info("IoTDB connected to " + config.host() + ":" + config.port());
	}

	/**
	 * Close the connection pool.
	 */
	public void deactivate() {
		if (this.sessionPool != null) {
			try {
				this.sessionPool.close();
			} catch (Exception e) {
				log.warn("Error closing IoTDB session pool: " + e.getMessage());
			}
			this.sessionPool = null;
		}
	}

	/**
	 * Execute a non-query SQL statement (DDL/DML).
	 *
	 * @param sql the SQL statement
	 * @throws StatementExecutionException on SQL error
	 * @throws IoTDBConnectionException     on connection error
	 */
	public void executeNonQuery(String sql) throws StatementExecutionException, IoTDBConnectionException {
		this.sessionPool.executeNonQueryStatement(sql);
	}

	/**
	 * Execute a query SQL statement.
	 *
	 * @param sql the query SQL
	 * @return SessionDataSetWrapper result set
	 * @throws StatementExecutionException on SQL error
	 * @throws IoTDBConnectionException     on connection error
	 */
	public SessionDataSetWrapper executeQuery(String sql) throws StatementExecutionException, IoTDBConnectionException {
		return this.sessionPool.executeQueryStatement(sql);
	}

	/**
	 * Get the SessionPool instance.
	 *
	 * @return SessionPool
	 */
	public SessionPool getSessionPool() {
		return this.sessionPool;
	}

	/**
	 * Build the full IoTDB path for a channel.
	 *
	 * @param edgeId       the Edge ID
	 * @param channelAddress the channel address (e.g. "_sum/ActivePower")
	 * @return full path like "root.ems.edge0._sum_ActivePower"
	 */
	public String buildPath(String edgeId, String channelAddress) {
		// Convert channel address to IoTDB path format
		// Replace "/" and other special chars with "_"
		String safeChannel = channelAddress.replace('/', '_').replace('.', '_');
		String safeEdge = edgeId.replace('/', '_').replace('.', '_');
		return config.rootPrefix() + "." + safeEdge + "." + safeChannel;
	}
}
