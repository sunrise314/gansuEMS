package io.openems.backend.ai.alert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.ai.Config;
import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.common.event.EventReader;

/**
 * Alert Agent - intelligent alarm processing.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Subscribes to OpenEMS alarm/alert events
 * <li>Alert storm deduplication and aggregation
 * <li>NER entity extraction from alarm text
 * <li>Root cause analysis suggestions
 * </ul>
 *
 * <p>
 * TODO: Integrate actual LLM/NER model calls. Currently a skeleton with event
 * subscription and storm aggregation logic.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "AI.AlertAgent", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		"io/openems/edge/ALARM_RAISED", //
		"io/openems/edge/ALARM_CLEARED" //
})
public class AlertAgent extends AbstractOpenemsBackendComponent implements EventHandler {

	private final Logger log = LoggerFactory.getLogger(AlertAgent.class);

	private Config config;

	/**
	 * Pending alerts for storm aggregation.
	 * Key: edgeId + alarmType
	 */
	private final ConcurrentHashMap<String, List<AlertEvent>> pendingAlerts = new ConcurrentHashMap<>();

	private ScheduledExecutorService scheduler;

	public AlertAgent() {
		super("AI.AlertAgent");
	}

	@Activate
	private void activate(Config config) {
		super.activate(null, config.id(), "", config.enabled());
		this.config = config;

		if (config.enabled()) {
			this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "AlertAgent-Aggregator");
				t.setDaemon(true);
				return t;
			});
			// Schedule storm aggregation flush
			this.scheduler.scheduleAtFixedRate(this::flushAlerts,
					config.alertStormWindowSeconds(),
					config.alertStormWindowSeconds(),
					TimeUnit.SECONDS);
			this.logInfo(this.log, "Alert Agent activated with storm window=" + config.alertStormWindowSeconds() + "s");
		}
	}

	@Deactivate
	private void deactivate() {
		if (this.scheduler != null) {
			this.scheduler.shutdownNow();
		}
		this.pendingAlerts.clear();
		this.logInfo(this.log, "Alert Agent deactivated");
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		var reader = new EventReader(event);
		String topic = event.getTopic();

		try {
			String edgeId = reader.getString("edgeId");
			String alarmType = reader.getString("alarmType");
			String message = reader.getString("message");
			Long timestamp = reader.getLong("timestamp");

			AlertEvent alert = new AlertEvent(edgeId, alarmType, message, timestamp != null ? timestamp : System.currentTimeMillis());

			String key = edgeId + ":" + alarmType;
			this.pendingAlerts.computeIfAbsent(key, k -> new ArrayList<>()).add(alert);

			this.logDebug(this.log, "Alert received: " + alarmType + " from " + edgeId);

		} catch (Exception e) {
			this.logError(this.log, "Error processing alert event: " + e.getMessage());
		}
	}

	/**
	 * Flush pending alerts - aggregate storms and trigger analysis.
	 */
	private void flushAlerts() {
		if (this.pendingAlerts.isEmpty()) {
			return;
		}

		this.logInfo(this.log, "Flushing " + this.pendingAlerts.size() + " alert groups");

		for (var entry : this.pendingAlerts.entrySet()) {
			String key = entry.getKey();
			List<AlertEvent> alerts = entry.getValue();

			if (alerts.size() >= config.maxAlertsPerStorm()) {
				// This is a storm - aggregate
				this.handleAlertStorm(key, alerts);
			} else if (!alerts.isEmpty()) {
				// Normal alerts - process individually
				for (AlertEvent alert : alerts) {
					this.processSingleAlert(alert);
				}
			}

			alerts.clear();
		}
	}

	/**
	 * Handle an alert storm (many similar alerts in short time).
	 */
	private void handleAlertStorm(String key, List<AlertEvent> alerts) {
		this.logWarn(this.log, "ALERT STORM detected: " + key + " (" + alerts.size() + " alerts)");

		AlertStorm storm = new AlertStorm(//
				key, //
				alerts.size(), //
				alerts.get(0).timestamp(), //
				alerts.get(alerts.size() - 1).timestamp() //
		);

		// TODO: Call LLM to summarize storm and suggest root cause
		// this.callLlmRootCauseAnalysis(storm);
	}

	/**
	 * Process a single alert.
	 */
	private void processSingleAlert(AlertEvent alert) {
		// TODO: Extract entities from alert message using NER
		// List<NerEntity> entities = this.extractEntities(alert.message());
		// TODO: Look up similar historical alerts for diagnosis
	}

	/**
	 * Internal record for an alert event.
	 */
	private record AlertEvent(//
			String edgeId, //
			String alarmType, //
			String message, //
			long timestamp //
	) {
	}

	/**
	 * Internal record for an aggregated alert storm.
	 */
	public record AlertStorm(//
			String alarmKey, //
			int alertCount, //
			long startTime, //
			long endTime //
	) {
	}
}
