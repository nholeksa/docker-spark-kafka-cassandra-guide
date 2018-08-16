import java.util.Properties
import com.datastax.spark.connector._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark._
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.Try
import scala.util.matching.Regex
import scala.collection.immutable.Map


object CassandraSparkKafka {

  var sensorHashMap : HashMap[String, Long] = new HashMap[String, Long]()
  var dosArray : HashMap[String,Boolean] = new HashMap[String,Boolean]()


  def tryToLong(s : String) = Try(s.toLong).toOption

  def tryToFloat(s : String) = Try(s.toFloat).toOption

  def debug(i : Int) = println("Sent from line" + i)

  def printMap(h : HashMap[String,Long]) = h.foreach(x => print(x._1 + ": " + x._2))

  def mergeMap[T](h1 : HashMap[String,T], h2 : HashMap[String,T]) : HashMap[String,T] = {
    h1.foreach(x => h2.put(x._1,x._2))
    h1
  }


  def sendToKafka(producer : KafkaProducer[String,String], topic : String, data : String) =
  {
    val message = new ProducerRecord[String, String](topic, data)
    producer.send(message).get.toString()
  }

  def main(args : Array[String]): Unit = {


    //start spark context with cassandra host connection
    val sparkConf : SparkConf = new SparkConf()
                                  .setAppName("Test")
                                  .set("spark.cassandra.connection.host","<cassandra.host.ip>")

   


    //start streaming context giving the conf and an interval
    val sc : StreamingContext = new StreamingContext(sparkConf, Seconds(10))

    var sensorRdd = sc.sparkContext.parallelize(sensorHashMap.toSeq)
    var dosRdd = sc.sparkContext.parallelize(dosHashMap.toSeq)


    var broadCastSensor = sc.sparkContext.broadcast(sensorHashMap)
    var broadCastDos = sc.sparkContext.broadcast(dosArray)


    spark.sparkContext.setLogLevel("WARN")

    //kafka Params 
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "<kafka.host.ip>:9092",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "group.id" -> "10"
    )

    //input topic
    val topics = Array("test")




    //kafka topics in -> out
    val outputParams = new Properties()
    outputParams.put("bootstrap.servers", "<kafka.host.ip>:9092")
    outputParams.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer")
    outputParams.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    //output topics
    val secTopicInj = "securityInj"
    val secTopicDos = "securityDOS"


    //create dstream with streaming context
    val stream = KafkaUtils.createDirectStream[String, String](
      sc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    //process the stream
    val processedStream = stream.map(record => record.value())

    

    //check for formatted strings first !!!!!!!!!!!
    //each stream has multiple rdds
     processedStream.foreachRDD(rdd => {

       val rng = new scala.util.Random(System.currentTimeMillis())

       //filter out any DOS data or Injected data
       newRdd.filter((data) => {


        //create kafka producer to send data to new kafka topics
         val producer = new KafkaProducer[String, String](outputParams)

         //split data into fields
         val fields = data.split("-")


         //check for signs of data formatting
         //if not 3 fields

         fields.size match {
           case 3 => {

             val currentTimestamp = tryToLong(fields(1))
             println(tryToLong(fields(1)) + " " + tryToFloat(fields(2)))
             val sensorData = tryToFloat(fields(2))

      s       //if illegal sensor ID then false and skip

             if (currentTimestamp.isEmpty || sensorData.isEmpty)
             {
             
               sendToKafka(producer, secTopicInj, data)
               false
             }

             //check if blacklisted sensor id
             else if (broadCastDos.value.contains(fields(0)))
             {
                false
             }
            //check for regex matches
             else
             {
               val chars   = raw"[^0-9\-.]"
               val regex   = new Regex(chars,"")
               val matches = regex.findFirstIn(data)

               if (matches.isDefined)
               {

                 sendToKafka(producer, secTopicInj, data)
                 false
               }
                //check if data out of range
               else if(fields(2).toFloat < 15 || fields(2).toFloat > 26)
               {
                 sendToKafka(producer,secTopicInj, data)
                 false
               }
               
               else if(broadCastSensor.value.contains(fields(0)))
               {
                 val timeStamp = broadCastSensor.value.get(fields(0)) match {
                   case Some(x) => x
                   case None => -1
                 }
                 val currentNum = tryToLong(fields(1)) match {
                   case Some(x) => x
                   case None => -1
                 }
                 if (currentNum > 0)
                 {
                   //check if DOS attack
                   println(currentNum)
                   println(timeStamp)
                   val difference : Long = currentNum - timeStamp
                   if (difference < 2000)
                   {          
                     sendToKafka(producer, secTopicDos, data)
                     dosArray.put(fields(0),true)
                     false
                   }
                   else
                   {           
                     sensorHashMap.put(fields(0),fields(1).toLong)
                     true
                   }

                 }
                 //current num <= 0 means some sort of data alteration or malfunction
                 else
                 {                
                   sendToKafka(producer, secTopicInj, data)
                   false
                 }

               }
               else
               {
                 if (tryToLong(fields(1)).isDefined)
                 {
                   sensorHashMap.put(fields(0), fields(1).toLong)
                   true
                 }
                 else
                 {
                   sendToKafka(producer, secTopicInj, data)
                   false
                 }
               }

             }
           }

           case _ => {
             sendToKafka(producer, secTopicInj, data)
             false
           }
         }
        //map the data with a random integer (not necessary, just fills one column) then send to kafka
        }).map(x => Tuple2(x,rng.nextInt)).saveToCassandra("test","kv", SomeColumns("data","id"))

    })


    sensorHashMap = mergeMap(sensorHashMap, broadCastSensor.value)
    dosArray = mergeMap(dosArray,broadCastDos.value)



    broadCastDos.unpersist()
    broadCastDos = sc.sparkContext.broadcast(dosArray)

    broadCastSensor.unpersist()
    broadCastSensor = sc.sparkContext.broadcast(sensorHashMap)

    processedStream.print()

    //key for spark streaming, need these two steps
    sc.start()
    sc.awaitTermination()




  }


}
