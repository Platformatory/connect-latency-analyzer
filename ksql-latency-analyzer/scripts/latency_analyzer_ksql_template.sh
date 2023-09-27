#!/bin/bash

# Input string containing comma-separated values
input_string="$1"

# Set the IFS to a comma to specify the delimiter
IFS=','

# Read the values into an array
read -ra values <<< "$input_string"

text=$(cat <<EOF
SET 'auto.offset.reset'='earliest';
EOF
)

for ((i=0; i<=${#values[@]} - 1; i++)); do
    arg="${values[i]}"
    text="$text

CREATE STREAM IF NOT EXISTS "$arg"_input_stream (
    correlation_id STRING,
    connect_pipeline_id STRING,
    timestamp_type STRING,
    timestamp BIGINT
) WITH (
    KAFKA_TOPIC = '$arg',
    VALUE_FORMAT = 'AVRO'
);
"
    if [ "$i" == 0 ]; then
        text="$text
CREATE STREAM IF NOT EXISTS connect_latency_stream WITH (kafka_topic='connect_latency_stream', value_format='AVRO', partitions=6) AS 
SELECT * FROM "$arg"_input_stream;"
    else
        text="$text
INSERT INTO connect_latency_stream SELECT * FROM "$arg"_input_stream;"
    fi
done

text="$text

CREATE TABLE IF NOT EXISTS connect_latency_table WITH (kafka_topic='connect_latency_table', key_format = 'AVRO', partitions=6) AS 
SELECT STRUCT(correlation_id := CORRELATION_ID, connect_pipeline_id := CONNECT_PIPELINE_ID) AS table_key, 
LATEST_BY_OFFSET(CORRELATION_ID) AS CORRELATION_ID, 
LATEST_BY_OFFSET(CONNECT_PIPELINE_ID) AS CONNECT_PIPELINE_ID, 
AS_MAP(COLLECT_LIST(TIMESTAMP_TYPE), COLLECT_lIST(TIMESTAMP)) AS timestamp_data
FROM connect_latency_stream
GROUP BY STRUCT(correlation_id := CORRELATION_ID, connect_pipeline_id := CONNECT_PIPELINE_ID);

CREATE TABLE IF NOT EXISTS final_latency_table with (kafka_topic='final_latency_data', key_format='AVRO', value_format='AVRO', partitions=6) AS SELECT
table_key,
UNIX_TIMESTAMP() as ts,
correlation_id,
connect_pipeline_id,
(timestamp_data['sink'] - timestamp_data['source']) as E2E_LATENCY,
(timestamp_data['broker'] - timestamp_data['source']) AS SOURCE_PRODUCE_LATENCY, 
(timestamp_data['consume'] - timestamp_data['broker']) AS SINK_CONSUME_LATENCY, 
(timestamp_data['sink'] - timestamp_data['consume']) AS SINK_FLUSH_LATENCY 
FROM connect_latency_table WHERE ARRAY_CONTAINS(MAP_KEYS(timestamp_data), 'sink');
"

echo "$text" > /home/appuser/queries.sql

# Execute "/usr/bin/docker/run"
exec /usr/bin/docker/run
