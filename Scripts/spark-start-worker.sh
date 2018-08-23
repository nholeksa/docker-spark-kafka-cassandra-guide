#!/bin/bash

${SPARK_HOME}/sbin/start-slave.sh spark://spark-master:7077 --memory 1G --cores 1
