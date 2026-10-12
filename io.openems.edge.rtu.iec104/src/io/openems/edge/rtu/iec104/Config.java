package io.openems.edge.rtu.iec104;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi Configuration for IEC104 RTU device.
 *
 * <p>
 * All IOA (Information Object Address) values must match the RTU point list.
 * Leave as 0 to disable a point.
 */
@ObjectClassDefinition(//
		name = "RTU IEC104", //
		description = "Remote Terminal Unit via IEC 60870-5-104 protocol")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "rtu0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IEC104 Bridge-ID", description = "ID of the IEC104 bridge component to use.")
	String bridge_id() default "iec104bridge0";

	// ===== Telemetry IOA addresses =====

	@AttributeDefinition(name = "Active Power IOA", description = "Information Object Address for Active Power (W). 0 = disabled.")
	int ioaActivePower() default 101;

	@AttributeDefinition(name = "Reactive Power IOA", description = "Information Object Address for Reactive Power (var). 0 = disabled.")
	int ioaReactivePower() default 102;

	@AttributeDefinition(name = "Voltage L1 IOA", description = "Information Object Address for L1 Voltage (V). 0 = disabled.")
	int ioaVoltageL1() default 103;

	@AttributeDefinition(name = "Voltage L2 IOA", description = "Information Object Address for L2 Voltage (V). 0 = disabled.")
	int ioaVoltageL2() default 104;

	@AttributeDefinition(name = "Voltage L3 IOA", description = "Information Object Address for L3 Voltage (V). 0 = disabled.")
	int ioaVoltageL3() default 105;

	@AttributeDefinition(name = "Current L1 IOA", description = "Information Object Address for L1 Current (mA). 0 = disabled.")
	int ioaCurrentL1() default 106;

	@AttributeDefinition(name = "Current L2 IOA", description = "Information Object Address for L2 Current (mA). 0 = disabled.")
	int ioaCurrentL2() default 107;

	@AttributeDefinition(name = "Current L3 IOA", description = "Information Object Address for L3 Current (mA). 0 = disabled.")
	int ioaCurrentL3() default 108;

	@AttributeDefinition(name = "Frequency IOA", description = "Information Object Address for Grid Frequency (Hz). 0 = disabled.")
	int ioaFrequency() default 109;

	@AttributeDefinition(name = "Battery SOC IOA", description = "Information Object Address for Battery SOC (%). 0 = disabled.")
	int ioaBatterySoc() default 110;

	// ===== Tele-signal IOA addresses =====

	@AttributeDefinition(name = "Grid Connected IOA", description = "IOA for Grid Connected status (single point). 0 = disabled.")
	int ioaGridConnected() default 201;

	@AttributeDefinition(name = "Fault State IOA", description = "IOA for Fault State (single point). 0 = disabled.")
	int ioaFaultState() default 202;

	@AttributeDefinition(name = "Remote Control IOA", description = "IOA for Remote Control Enabled (single point). 0 = disabled.")
	int ioaRemoteControl() default 203;

	// ===== Control IOA addresses =====

	@AttributeDefinition(name = "AGC Active Power Setpoint IOA", description = "IOA for AGC Active Power Setpoint command (C_SE_NC_1). 0 = disabled.")
	int ioaAgcActivePowerSetpoint() default 301;

	@AttributeDefinition(name = "Grid Connection Command IOA", description = "IOA for Grid Connect/Disconnect command (C_SC_NA_1). 0 = disabled.")
	int ioaGridConnectionCommand() default 302;

	// ===== Scaling =====

	@AttributeDefinition(name = "Power Scale Factor", description = "Scale factor for power values. Default 1.0 (value = raw * scale + offset).")
	double powerScaleFactor() default 1.0;

	@AttributeDefinition(name = "Power Offset", description = "Offset for power values. Default 0.")
	double powerOffset() default 0.0;

	String webconsole_configurationFactory_nameHint() default "RTU IEC104 [{id}]";
}
