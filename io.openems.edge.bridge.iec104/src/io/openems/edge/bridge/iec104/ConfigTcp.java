package io.openems.edge.bridge.iec104;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.bridge.iec104.api.LogVerbosity;

/**
 * OSGi Configuration for IEC104/TCP Bridge.
 */
@ObjectClassDefinition(//
		name = "Bridge IEC104/TCP", //
		description = "Connects to a RTU via IEC 60870-5-104 protocol over TCP")
@interface ConfigTcp {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "iec104bridge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the RTU (Remote Terminal Unit).")
	String ip();

	@AttributeDefinition(name = "Port", description = "The TCP port of the RTU. Standard IEC104 port is 2404.")
	int port() default 2404;

	@AttributeDefinition(name = "ASDU Common Address", description = "Common Address of ASDU (CA). Usually 1.")
	int asduCommonAddress() default 1;

	@AttributeDefinition(name = "T0 - Connection Establishment Timeout", description = "Timeout in seconds for connection establishment (standard: 30s).")
	int t0() default 30;

	@AttributeDefinition(name = "T1 - Send or Confirm Timeout", description = "Timeout in seconds for sending APDU or receiving acknowledgement (standard: 15s).")
	int t1() default 15;

	@AttributeDefinition(name = "T2 - Acknowledgment Delay", description = "Delay in seconds before sending acknowledgement (standard: 10s, must be < T1).")
	int t2() default 10;

	@AttributeDefinition(name = "T3 - Idle Timeout", description = "Timeout in seconds for sending test frames when idle (standard: 20s, must be > T1).")
	int t3() default 20;

	@AttributeDefinition(name = "Use Periodic Interrogation", description = "Send general interrogation (C_IC_NA_1) periodically to sync all points.")
	boolean usePeriodicInterrogation() default true;

	@AttributeDefinition(name = "Interrogation Interval (seconds)", description = "Interval in seconds for periodic general interrogation.")
	int interrogationInterval() default 60;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity level.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "Bridge IEC104/TCP [{id}]";
}
