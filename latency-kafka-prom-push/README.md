
# KafkaAvroMetricPush

A Java Program that consumes Avro-serialized data from Confluent cloud kafka topic and pushes key metrics to Prometheus' PushGateway.


## Environment Variables

To run this project, you will need to add the following environment variables

Prometheus Pushgateway Configs

`METRIC_NAME`

`METRIC_HELP`

`LABEL_NAME_1` `LABEL_NAME_2`

Topic name to consume

`KAFKA_TOPIC`

Pushgateway address

`PUSH_GATEWAY_ENDPOINT`

`CORRELATION_ID_FIELD` `CONNECT_PIPELINE_ID_FIELD` `E2E_LATENCY_FIELD`

Kafka Server Configs

`BOOTSTRAP_SERVERS`

`GROUP_ID`

`SCHEMA_REGISTRY_URL`

`SECURITY_PROTOCOL`

`SASL_MECHANISM`

`KAFKA_USERNAME` `KAFKA_PASSWORD`

`BASIC_AUTH_CREDENTIALS_SOURCE`

`USER_INFO_CONFIG`

`SPECIFIC_AVRO_READER_CONFIG`

`AUTO_OFFSET_RESET_CONFIG`
