package io.openems.backend.timedata.iotdb;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi Configuration for IoTDB Timedata.
 */
@ObjectClassDefinition(//
		name = "Timedata.IoTDB", //
		description = "Configures the Apache IoTDB timedata provider")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timedata0";

	@AttributeDefinition(name = "Host", description = "IoTDB server host/IP address.")
	String host() default "127.0.0.1";

	@AttributeDefinition(name = "Port", description = "IoTDB RPC port. Default 6667.")
	int port() default 6667;

	@AttributeDefinition(name = "Username", description = "IoTDB username.")
	String username() default "root";

	@AttributeDefinition(name = "Password", description = "IoTDB password.")
	String password() default "root";

	@AttributeDefinition(name = "Root Prefix", description = "Root path prefix for time series. Default: root.ems")
	String rootPrefix() default "root.ems";

	@AttributeDefinition(name = "Session Pool Size", description = "Maximum number of sessions in the pool.")
	int maxSize() default 8;

	@AttributeDefinition(name = "Read-Only mode", description = "Activates read-only mode. No data will be written.")
	boolean isReadOnly() default false;

	@AttributeDefinition(name = "Blacklisted Channels", description = "List of channel addresses that should not be written to IoTDB. e.g. \"_sum/State\"")
	String[] blacklistedChannels() default {};

	String webconsole_configurationFactory_nameHint() default "Timedata IoTDB";
}
