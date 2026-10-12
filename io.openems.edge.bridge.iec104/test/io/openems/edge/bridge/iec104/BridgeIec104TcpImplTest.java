package io.openems.edge.bridge.iec104;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.iec104.api.Iec104WriteRequest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

/**
 * Unit tests for BridgeIec104TcpImpl.
 *
 * <p>
 * TODO: Add integration tests with a real RTU simulator or mock TCP server.
 */
public class BridgeIec104TcpImplTest {

	private ComponentTest componentTest;

	@Before
	public void setup() throws Exception {
		this.componentTest = new ComponentTest(new BridgeIec104TcpImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addConfiguration("ip", "127.0.0.1") //
				.addConfiguration("port", 2404) //
				.addConfiguration("asduCommonAddress", 1);
	}

	@Test
	public void testConfiguration() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() {
				// Verify component activates with correct config
				var bridge = (BridgeIec104TcpImpl) this.componentTest.component;
				assertEquals("iec104bridge0", bridge.id());
			}
		});
	}

	@Test
	public void testInitialConnectionState() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() {
				var bridge = (BridgeIec104TcpImpl) this.componentTest.component;
				// Initially should not be connected (no real RTU in test)
				// In a real environment, this would attempt connection
				assertFalse(bridge.isConnected().orElse(true));
			}
		});
	}

	@Test(expected = OpenemsException.class)
	public void testSendWriteRequestWhenDisconnected() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() {
				var bridge = (BridgeIec104TcpImpl) this.componentTest.component;
				try {
					// Should throw exception because not connected
					bridge.sendWriteRequest(Iec104WriteRequest.setPointFloat(100, 5000.0));
				} catch (OpenemsException e) {
					assertEquals("IEC104 not connected to RTU", e.getMessage());
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Test
	public void testWriteRequestFactory() {
		// Test set-point write request factory
		var req = Iec104WriteRequest.setPointFloat(100, 5000.0);
		assertEquals(100, req.informationObjectAddress());
		assertEquals(Iec104WriteRequest.Type.SETPOINT_SHORT_FLOAT, req.type());
		assertEquals(5000.0, req.value(), 0.001);
		assertTrue(req.selectBeforeExecute());

		// Test single command write request factory
		var onCmd = Iec104WriteRequest.singleCommand(200, true);
		assertEquals(200, onCmd.informationObjectAddress());
		assertEquals(Iec104WriteRequest.Type.SINGLE_COMMAND, onCmd.type());
		assertEquals(1.0, onCmd.value(), 0.001);

		var offCmd = Iec104WriteRequest.singleCommand(200, false);
		assertEquals(0.0, offCmd.value(), 0.001);
	}

	@Test
	public void testTeleValueTransform() {
		// Test telemetry value scaling
		var tele = new io.openems.edge.bridge.iec104.api.TeleValue(
				100, "meter0/ActivePower", io.openems.common.types.OpenemsType.INTEGER, 0.1, 0.0);

		// raw=5000 -> 5000 * 0.1 + 0 = 500.0
		assertEquals(500.0, tele.transform(5000), 0.001);

		// Default constructor (scale=1.0)
		var teleDefault = new io.openems.edge.bridge.iec104.api.TeleValue(
				101, "meter0/Voltage", io.openems.common.types.OpenemsType.INTEGER);
		assertEquals(220.0, teleDefault.transform(220.0), 0.001);
	}
}
