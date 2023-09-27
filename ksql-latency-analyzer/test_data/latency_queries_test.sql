CREATE STREAM connect_latency_input_stream (
    correlation_id STRING,
    connect_pipeline_id STRING,
    timestamp_type STRING,
    timestamp BIGINT
) WITH (
    KAFKA_TOPIC = 'connect_latency',
    VALUE_FORMAT = 'AVRO'
);

CREATE STREAM connect_latency_stream WITH (value_format='AVRO') AS 
SELECT * FROM connect_latency_input_stream;

CREATE TABLE connect_latency_table WITH (key_format = 'AVRO') AS 
SELECT STRUCT(correlation_id := CORRELATION_ID, connect_pipeline_id := CONNECT_PIPELINE_ID) AS table_key, 
LATEST_BY_OFFSET(CORRELATION_ID) AS CORRELATION_ID, 
LATEST_BY_OFFSET(CONNECT_PIPELINE_ID) AS CONNECT_PIPELINE_ID, 
AS_MAP(COLLECT_LIST(TIMESTAMP_TYPE), COLLECT_lIST(TIMESTAMP)) AS timestamp_data
FROM connect_latency_stream
GROUP BY STRUCT(correlation_id := CORRELATION_ID, connect_pipeline_id := CONNECT_PIPELINE_ID);

CREATE TABLE final_latency_table with (kafka_topic='final_latency_data', key_format='AVRO', value_format='AVRO', partitions=1) AS SELECT
table_key,
correlation_id,
connect_pipeline_id,
(timestamp_data['sink'] - timestamp_data['source']) as E2E_LATENCY,
(timestamp_data['broker'] - timestamp_data['source']) AS SOURCE_PRODUCE_LATENCY, 
(timestamp_data['consume'] - timestamp_data['broker']) AS SINK_CONSUME_LATENCY, 
(timestamp_data['sink'] - timestamp_data['consume']) AS SINK_FLUSH_LATENCY 
FROM connect_latency_table WHERE ARRAY_CONTAINS(MAP_KEYS(timestamp_data), 'sink');