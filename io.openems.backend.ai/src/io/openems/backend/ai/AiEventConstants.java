package io.openems.backend.ai;

/**
 * Event topics and constants for AI module integration.
 *
 * <p>
 * These event topics integrate the AI module with OpenEMS Backend's internal
 * OSGi EventAdmin event bus.
 */
public class AiEventConstants {

	/**
	 * Topic for AI diagnosis results.
	 *
	 * <p>
	 * Properties:
	 * <ul>
	 * <li>edgeId: the Edge ID
	 * <li>faultType: the fault type identifier
	 * <li>diagnosis: diagnosis text
	 * <li>confidence: confidence score (0.0 - 1.0)
	 * </ul>
	 */
	public static final String TOPIC_AI_DIAGNOSIS_RESULT = "io/openems/backend/AI_DIAGNOSIS_RESULT";

	/**
	 * Topic for alert storm notifications.
	 *
	 * <p>
	 * Properties:
	 * <ul>
	 * <li>edgeId: the Edge ID
	 * <li>alarmKey: aggregated alarm key
	 * <li>alertCount: number of alerts in storm
	 * <li>startTime: storm start time (epoch ms)
	 * </ul>
	 */
	public static final String TOPIC_AI_ALERT_STORM = "io/openems/backend/AI_ALERT_STORM";

	/**
	 * Topic for manual diagnosis trigger.
	 *
	 * <p>
	 * Properties:
	 * <ul>
	 * <li>edgeId: the Edge ID
	 * <li>faultDescription: description of the fault
	 * </ul>
	 */
	public static final String TOPIC_AI_DIAGNOSIS_REQUEST = "io/openems/backend/AI_DIAGNOSIS_REQUEST";

	private AiEventConstants() {
		// prevent instantiation
	}
}
