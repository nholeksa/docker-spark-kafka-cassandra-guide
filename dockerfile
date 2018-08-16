FROM java:openjdk-8
LABEL maintainer="Nathaniel"



#install spark 2.3.0
RUN wget https://archive.apache.org/dist/spark/spark-2.3.0/spark-2.3.0-bin-hadoop2.7.tgz \
 && tar xvf spark-2.3.0-bin-hadoop2.7.tgz \
 && mv spark-2.3.0-bin-hadoop2.7 /spark \
 && rm spark-2.3.0-bin-hadoop2.7.tgz \
 && chmod 777 /spark

#Copy bash script and jar
COPY spark-start.sh /spark-start.sh
COPY CassandraSparkKafka.jar /CassandraSparkKafka.jar



#Environment Variables and/or ports to expose??  
ENV SPARK_HOME /spark

ENV SPARK_MASTER_OPTS="-Dspark.driver.port=7001 -Dspark.fileserver.port=7002 -Dspark.broadcast.port=7003 -Dspark.replClassServer.port=7004 -Dspark.blockManager.port=7005 -Dspark.executor.port=7006 -Dspark.ui.port=4040 -Dspark.broadcast.factory=org.apache.spark.broadcast.HttpBroadcastFactory"
ENV SPARK_WORKER_OPTS="$SPARK_MASTER_OPTS  -Dspark.shuffle.service.enabled=true"

ENV SPARK_MASTER_PORT 7077
ENV SPARK_MASTER_WEBUI_PORT 8080
ENV SPARK_WORKER_PORT 8888
ENV SPARK_WORKER_WEBUI_PORT 8081

### Spark ports
# 4040: spark ui
# 6066: spark cluster port
# 7001: spark driver
# 7002: spark fileserver
# 7003: spark broadcast
# 7004: spark replClassServer
# 7005: spark blockManager
# 7006: spark executor
# 7077: spark master
# 8080: spark master ui
# 8081: spark worker ui
# 8888: spark worker

EXPOSE 4040 7001 7002 7003 7004 7005 7006 7077 8080 8081 8888
EXPOSE 6066
#default command? NONE!

