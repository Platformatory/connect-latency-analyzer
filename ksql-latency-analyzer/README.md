# Headless KSQL server for latency analyzer

## Built from custom Dockerfile
```docker
FROM confluentinc/ksqldb-server:0.29.0

# Copy your bash script to the container
COPY scripts/latency_analyzer_ksql_template.sh /home/appuser/latency_analyzer_ksql_template.sh

CMD ["/bin/bash", "-c", "/home/appuser/latency_analyzer_ksql_template.sh $KSQL_INPUT_TOPICS"]
```

## Pass in topics as Environment variable
**Environment variable name**: KSQL_INPUT_TOPICS.


Topics should be a list of comma separated string without spaces. For Example,
```
KSQL_INPUT_TOPICS=topic1,topic2,topic3
```

## Highlights

- Any number of input topics can be configured
- KSQL queries are built dynamically on start of the service
- A stream is created per input topic and all of them are combined into one stream before processing
- All the transient queries are backed by a pre-defined topic and not randomly generated

## Assumptions

- KSQL assumes all the input topic follow the same AVRO schema
- All the topics have the value format as AVRO
- The final topic name is not configurable at the moment


#### NOTE
Please refer to `latency_analyzer_ksql_template.sh` for the creation of KSQL queries file.


## KSQL Test Runner

KSQL Test runner validates the KSQL query for latency analyzer with a set of given input and expected output.

There are three required files for the test runner,
- `latency_queries_test.sql` - KSQL queries file
- `test_input.json` - Input JSON file
- `test_output.json` - Output JSON file

All the above files need to present inside the `test_data` folder. This folder will be loaded into the test runner docker container.

`ksqldb-test-runner` service in the docker compose file will execute the testing.