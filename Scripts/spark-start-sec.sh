#!/bin/bash

${SPARK_HOME}/sbin/start-master.sh
${SPARK_HOME}/sbin/start-slave.sh spark://spark-security-master:7077 --memory 1G

${SPARK_HOME}/bin/spark-submit --class sparkSecurity --master spark://spark-security-master:6066 --deploy-mode cluster /CassandraSparkKafka.jar
