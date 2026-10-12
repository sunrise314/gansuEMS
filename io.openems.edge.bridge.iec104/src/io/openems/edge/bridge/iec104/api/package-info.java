/**
 * IEC104 Bridge API package.
 *
 * <p>
 * Contains the abstract base classes and interfaces for building IEC104-based
 * device drivers on top of the OpenEMS Edge framework.
 *
 * <p>
 * Usage:
 * <ol>
 * <li>Extend {@link io.openems.edge.bridge.iec104.api.AbstractOpenemsIec104Component}
 * <li>Build an {@link io.openems.edge.bridge.iec104.api.Iec104Protocol} with
 * {@link io.openems.edge.bridge.iec104.api.TeleValue} and
 * {@link io.openems.edge.bridge.iec104.api.TeleSignal} mappings
 * <li>Send write commands via
 * {@link io.openems.edge.bridge.iec104.api.Iec104WriteRequest}
 * </ol>
 */
package io.openems.edge.bridge.iec104.api;
