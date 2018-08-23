#!/bin/bash

${SPARK_HOME}/sbin/start-master.sh

#can start slave aswell if you wish
#${SPARK_HOME}/sbin/start-slave.sh spark://spark-master:7077  --memory 1G

${SPARK_HOME}/bin/spark-submit --class CassandraSparkKafka --master spark://spark-master:6066 --deploy-mode cluster /CassandraSparkKafka.jar
