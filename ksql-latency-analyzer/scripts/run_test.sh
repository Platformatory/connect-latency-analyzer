#!/bin/bash

ksql-test-runner -s test_data/latency_queries_test.sql -i test_data/test_input.json -o test_data/test_output.json | grep Test