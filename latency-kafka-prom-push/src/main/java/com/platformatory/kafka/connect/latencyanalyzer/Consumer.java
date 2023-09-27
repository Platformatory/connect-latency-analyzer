package com.platformatory.kafka.connect.latencyanalyzer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Consumer {
    private static final Gauge kafkaConnectPipelineE2ELatency = Gauge.build()
            .name("kafka_connect_pipeline_e2e_latency_seconds")
            .help("End to end latency of a connect pipeline")
            .labelNames("connect_pipeline_id")
            .register();
    private static final Gauge kafkaConnectPipelineSourceProduceLatency = Gauge.build()
            .name("kafka_connect_pipeline_source_produce_latency_seconds")
            .help("Source produce latency of a connect pipeline. Time between source and broker log append time")
            .labelNames("connect_pipeline_id")
            .register();
    private static final Gauge kafkaConnectPipelineSinkConsumeLatency = Gauge.build()
            .name("kafka_connect_pipeline_sink_consume_latency_seconds")
            .help("Sink consume latency of a connect pipeline. Time between consume by sink connector and the broker log append time. Time spent in the kafka broker before consumption.")
            .labelNames("connect_pipeline_id")
            .register();

    private static final Gauge kafkaConnectPipelineSinkFlushLatency = Gauge.build()
            .name("kafka_connect_pipeline_sink_flush_latency_seconds")
            .help("Sink flush latency of a connect pipeline. Time taken by the sink connector for processing before committing.")
            .labelNames("connect_pipeline_id")
            .register();
 
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);


    public static void main(String[] args) {

        Properties props = getProps();

        KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(System.getenv("KAFKA_TOPIC")));

        PushGateway pg = new PushGateway(System.getenv("PUSH_GATEWAY_ENDPOINT"));

        while (true) {
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<String, GenericRecord> record : records) {
                GenericRecord message = record.value();
		
		String correlationIdField = "CORRELATION_ID";
                String connectPipelineIdField = "CONNECT_PIPELINE_ID";
                String e2eLatencyField = "E2E_LATENCY";
		String sourceProduceLatencyField = "SOURCE_PRODUCE_LATENCY";
		String sinkConsumeLatencyField = "SINK_CONSUME_LATENCY";
		String sinkFlushLatencyField = "SINK_FLUSH_LATENCY";

                String correlationId = message.get(correlationIdField).toString();
                String connectPipelineId = message.get(connectPipelineIdField).toString();
                double e2eLatencySeconds = ((Long) message.get(e2eLatencyField)).doubleValue()/1000;
 		double sourceProduceLatencySeconds = ((Long) message.get(sourceProduceLatencyField)).doubleValue()/1000;
 		double sinkConsumeLatencySeconds = ((Long) message.get(sinkConsumeLatencyField)).doubleValue()/1000;
 		double sinkFlushLatencySeconds = ((Long) message.get(sinkFlushLatencyField)).doubleValue()/1000;

                logger.info("Offset = {}, Key = {}, TS = {}, CORRELATION_ID = {}, CONNECT_PIPELINE_ID = {}, E2E_LATENCY = {}",
                        record.offset(), record.key(),
                        message.get("TS"),
                        correlationId,
                        connectPipelineId,
                        e2eLatencySeconds);

		kafkaConnectPipelineE2ELatency.labels(connectPipelineId).set(e2eLatencySeconds);
            kafkaConnectPipelineSourceProduceLatency.labels(connectPipelineId).set(sourceProduceLatencySeconds);
	    kafkaConnectPipelineSinkConsumeLatency.labels(connectPipelineId).set(sinkConsumeLatencySeconds);
	    kafkaConnectPipelineSinkFlushLatency.labels(connectPipelineId).set(sinkFlushLatencySeconds);
	    
	    }

            // Pushing metrics to PushGateway after each poll
            try {
                pg.push(CollectorRegistry.defaultRegistry, "kafka_consumer_job");
            } catch (IOException e) {
                logger.error("Could not push metrics to PushGateway", e);
            }
        }
    }

    private static Properties getProps() {
        Properties props = new Properties();

        props.put("bootstrap.servers", System.getenv("BOOTSTRAP_SERVERS"));
        props.put("group.id", System.getenv("CONSUMER_GROUP_ID"));
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", KafkaAvroDeserializer.class.getName());
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, System.getenv("SCHEMA_REGISTRY_URL"));
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config", System.getenv("KAFKA_SASL_JAAS_CONFIG"));
        props.put(KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        props.put(KafkaAvroDeserializerConfig.USER_INFO_CONFIG, System.getenv("SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }
}
