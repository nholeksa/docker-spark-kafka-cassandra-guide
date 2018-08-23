import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringDeserializer
import com.datastax.spark.connector._
import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector.streaming._
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import java.util.Properties
import java.util.Base64

import javax.mail.{Authenticator, PasswordAuthentication}
import javax.mail.internet.MimeMessage
import org.apache.commons.mail._


object sparkSecurity
{

  def sendMail(msg : String) =
  {
    val email = new SimpleEmail
    email.setHostName("smtp.googlemail.com")
    email.setSmtpPort(465)
    email.setAuthenticator(new DefaultAuthenticator("<address>@gmail.com", "<password>"))
    email.setSSL(true)
    email.setFrom("<address>@gmail.com")
    email.setSubject("Attack Attempt")
    email.setMsg("The following data was sent to the network " + msg)
    email.addTo("<address>@gmail.com")

    email.send()
  }

  def main (args : Array[String]) : Unit =
  {
    //start streaming context with cassandra info to shark tank



    val sparkConf : SparkConf = new SparkConf()
      .setAppName("Test")
      .set("spark.cassandra.connection.host","<shark.tank.ip>")                //change to shark tank ip

    val sc : StreamingContext = new StreamingContext(sparkConf, Seconds(10))

    //kafka Params
    //establish kafka connection
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "<kafka.ip>:9092",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "group.id" -> "10"
    )

    val topicDos = Array("securityInj")
    val topicInj = Array("securityDOS")




    //create dstream
    val streamDOS = KafkaUtils.createDirectStream[String, String](
      sc,
      PreferConsistent,
      Subscribe[String, String](topicDos, kafkaParams)
    )

    val streamInj = KafkaUtils.createDirectStream[String, String] (
      sc,
      PreferConsistent,
      Subscribe[String, String](topicInj, kafkaParams)
    )


    val processedStreamDos = streamDOS.map(record => record.value())
    val processedStreamInj = streamInj.map(record => record.value())


    processedStreamInj.foreachRDD(rdd => {
      rdd.foreach((data : String) => {

        //send email with data
        sendMail(data)



        //encode data and save to Cassandra shark tank
        val encryptedData = Base64.getEncoder.encode(data.getBytes())

        //change keyspace name etc...
        rdd.map(x => Tuple1(x)).saveToCassandra("test","kv", SomeColumns("data"))
      })
    })

    processedStreamDos.foreachRDD(rdd => {
      rdd.foreach((data: String) => {

        val id = data.split("-")(0)

        sendMail(data)

      })
    })


    //check HashMap occurences
    /*processedStreamDos = streamDOS.foreachRDD(rdd => {
      rdd.foreach((data : String) => {

      }
    })*/


    sc.start()
    sc.awaitTermination()







  }

}

