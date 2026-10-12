package io.openems.backend.ai;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi Configuration for AI Agent module.
 */
@ObjectClassDefinition(//
		name = "Backend AI Agent", //
		description = "AI extension: Alert Agent + RAG Fault Diagnosis")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "aiAgent0";

	@AttributeDefinition(name = "Is enabled?", description = "Is this AI module enabled?")
	boolean enabled() default false;

	// ===== Alert Agent settings =====

	@AttributeDefinition(name = "Alert Storm Window (seconds)", description = "Time window for alert storm aggregation in seconds. Default: 60.")
	int alertStormWindowSeconds() default 60;

	@AttributeDefinition(name = "Max Alerts per Storm", description = "Maximum number of raw alerts to aggregate into one storm. Default: 50.")
	int maxAlertsPerStorm() default 50;

	// ===== RAG settings =====

	@AttributeDefinition(name = "Vector Store Type", description = "Vector store type: IN_MEMORY, REDIS, MILVUS, CHROMA. Default: IN_MEMORY.")
	String vectorStoreType() default "IN_MEMORY";

	@AttributeDefinition(name = "Embedding Model", description = "Embedding model name. Default: bge-small-zh-v1.5.")
	String embeddingModel() default "bge-small-zh-v1.5";

	@AttributeDefinition(name = "Top-K Retrieval", description = "Number of documents to retrieve. Default: 5.")
	int topKRetrieval() default 5;

	@AttributeDefinition(name = "Chunk Size", description = "Document chunk size in tokens. Default: 512.")
	int chunkSize() default 512;

	@AttributeDefinition(name = "Chunk Overlap", description = "Overlap between chunks in tokens. Default: 50.")
	int chunkOverlap() default 50;

	String webconsole_configurationFactory_nameHint() default "Backend AI Agent [{id}]";
}
