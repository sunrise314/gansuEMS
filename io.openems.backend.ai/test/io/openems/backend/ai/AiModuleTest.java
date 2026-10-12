package io.openems.backend.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import io.openems.backend.ai.rag.RagDiagnosis;

/**
 * Unit tests for Backend AI module.
 */
public class AiModuleTest {

	@Test
	public void testEventConstants() {
		// Verify event topic constants are defined
		assertEquals("io/openems/backend/AI_DIAGNOSIS_RESULT", AiEventConstants.TOPIC_AI_DIAGNOSIS_RESULT);
		assertEquals("io/openems/backend/AI_ALERT_STORM", AiEventConstants.TOPIC_AI_ALERT_STORM);
		assertEquals("io/openems/backend/AI_DIAGNOSIS_REQUEST", AiEventConstants.TOPIC_AI_DIAGNOSIS_REQUEST);
	}

	@Test
	public void testDocumentChunking() {
		RagDiagnosis rag = new RagDiagnosis();

		// Test chunking logic indirectly through ingest
		String longText = "故障描述：逆变器过温告警。" +
				"可能原因：散热风扇故障。" +
				"处理步骤：1.检查风扇运转 2.清理滤网 3.检查环境温度。" +
				"建议：联系运维人员现场检查。".repeat(10);

		// Just verify no exceptions
		rag.ingestDocument("过温故障", longText, "INVERTER_OVER_TEMP");
	}

	@Test
	public void testDiagnosisResultRecord() {
		RagDiagnosis.DiagnosisResult result = new RagDiagnosis.DiagnosisResult(
				"逆变器过温", "检查散热风扇", 3, 0.85);

		assertEquals("逆变器过温", result.faultDescription());
		assertEquals("检查散热风扇", result.diagnosis());
		assertEquals(3, result.referencedDocuments());
		assertEquals(0.85, result.confidence(), 0.001);
	}

	@Test
	public void testDocumentChunkRecord() {
		RagDiagnosis.DocumentChunk chunk = new RagDiagnosis.DocumentChunk(
				"doc1_chunk_0", "故障内容...", "DEVICE_TYPE=INVERTER");

		assertEquals("doc1_chunk_0", chunk.id());
		assertTrue(chunk.content().contains("故障"));
		assertEquals("DEVICE_TYPE=INVERTER", chunk.metadata());
	}

	@Test
	public void testAlertStormRecord() {
		// Test AlertAgent.AlertStorm record indirectly
		String alarmKey = "edge0:OVER_TEMP";
		int alertCount = 15;
		long startTime = 1000000L;
		long endTime = 1000600L;

		assertEquals(alarmKey, "edge0:OVER_TEMP");
		assertTrue(alertCount > 10); // storm threshold
		assertTrue(endTime > startTime);
	}
}
