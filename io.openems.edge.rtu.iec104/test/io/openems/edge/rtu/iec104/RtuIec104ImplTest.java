package io.openems.edge.rtu.iec104;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.iec104.BridgeIec104;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

/**
 * Unit tests for RtuIec104Impl.
 */
public class RtuIec104ImplTest {

	private ComponentTest componentTest;
	private BridgeIec104 mockBridge;

	@Before
	public void setup() throws Exception {
		this.mockBridge = mock(BridgeIec104.class);
		when(this.mockBridge.id()).thenReturn("iec104bridge0");

		this.componentTest = new ComponentTest(new RtuIec104Impl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("bridge", this.mockBridge) //
				.addConfiguration("id", "rtu0") //
				.addConfiguration("bridge_id", "iec104bridge0") //
				.addConfiguration("ioaActivePower", 101) //
				.addConfiguration("ioaReactivePower", 102) //
				.addConfiguration("ioaGridConnected", 201) //
				.addConfiguration("ioaFaultState", 202) //
				.addConfiguration("ioaAgcActivePowerSetpoint", 301) //
				.addConfiguration("ioaGridConnectionCommand", 302);
	}

	@Test
	public void testActivate() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() {
				RtuIec104Impl rtu = (RtuIec104Impl) componentTest.component;
				assertEquals("rtu0", rtu.id());
			}
		});
	}

	@Test
	public void testProtocolRegistration() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() {
				// Verify that addProtocol was called on the bridge
				verify(mockBridge).addProtocol(org.mockito.ArgumentMatchers.eq("rtu0"),
						org.mockito.ArgumentMatchers.any());
			}
		});
	}

	@Test
	public void testSendAgcSetpoint() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() throws OpenemsException {
				RtuIec104Impl rtu = (RtuIec104Impl) componentTest.component;
				rtu.sendAgcActivePowerSetpoint(50000); // 50 kW
				// Verify bridge.sendWriteRequest was called
				verify(mockBridge).sendWriteRequest(org.mockito.ArgumentMatchers.any());
			}
		});
	}

	@Test
	public void testSendGridConnectionCommand() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() throws OpenemsException {
				RtuIec104Impl rtu = (RtuIec104Impl) componentTest.component;
				rtu.sendGridConnectionCommand(true);
				verify(mockBridge).sendWriteRequest(org.mockito.ArgumentMatchers.any());
			}
		});
	}

	@Test(expected = OpenemsException.class)
	public void testAgcNotConfigured() throws Exception {
		// Reconfigure with AGC IOA disabled
		this.componentTest.removeConfiguration("ioaAgcActivePowerSetpoint");
		this.componentTest.addConfiguration("ioaAgcActivePowerSetpoint", 0);

		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() throws OpenemsException {
				RtuIec104Impl rtu = (RtuIec104Impl) componentTest.component;
				rtu.sendAgcActivePowerSetpoint(10000);
			}
		});
	}

	@Test
	public void testChannelAccessors() throws Exception {
		this.componentTest.activate(new TestCase() {
			@Override
			protected void doctest() {
				RtuIec104Impl rtu = (RtuIec104Impl) componentTest.component;

				// Set some values
				rtu.channel(RtuIec104.ChannelId.ACTIVE_POWER).setNextValue(50000);
				rtu.channel(RtuIec104.ChannelId.GRID_CONNECTED).setNextValue(true);
				rtu.channel(RtuIec104.ChannelId.FAULT_STATE).setNextValue(false);

				// Verify accessors
				assertEquals(Integer.valueOf(50000), rtu.getActivePower().get());
				assertTrue(rtu.getGridConnected().orElse(false));
				assertFalse(rtu.getFaultState().orElse(true));
			}
		});
	}
}
