package io.kagera.analyse

import akka.analytics.cassandra.JournalKey
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.rdd.RDD
import akka.analytics.cassandra._
import com.typesafe.config.ConfigFactory
import io.kagera.akka.persistence.TransitionFired

object Test extends App {

  val serializerConfig =
    """
      |akka {
      |  persistence {
      |    serializers = {
      |      scalapb = "io.kagera.akka.actor.ScalaPBSerializer"
      |    }
      |  }
      |}
    """.stripMargin

  val conf = new SparkConf()
    .setAppName("KageraExample")
    .setMaster("local[4]") // run locally with 4 threads
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("spark.cassandra.journal.keyspace", "akka")
    .set("spark.cassandra.journal.table", "messages")

  val sc = new SparkContext(conf).withSerializerConfig(ConfigFactory.parseString(serializerConfig))

  // expose journaled Akka Persistence events as RDD
  val rdd: RDD[(JournalKey, Any)] = sc.eventTable().cache()

  val total_transition_time = rdd
    .map(_._2)
    .flatMap {
      case e: TransitionFired => Some(e)
      case _ => None
    }
    .groupBy(_.transitionId.get)
    .map { case (id, i) =>
      id -> i.map(e => e.timeCompleted.get - e.timeStarted.get).reduce(_ + _)
    }
    .collect
    .foreach { case (tid, time) =>
      println(s"total time for $tid: $time")
    }
}
