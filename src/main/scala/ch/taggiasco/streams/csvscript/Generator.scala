package ch.taggiasco.streams.csv

import java.nio.file.Paths
import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.ActorSystem
import akka.util.ByteString
import akka.stream.ActorMaterializer
import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}

import scala.util.{Try, Failure, Success}
import scala.concurrent.Future

import com.typesafe.config.ConfigFactory


object Generator {
  
  private case class EndException(msg: String) extends Exception(msg)
  
  
  private def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B = {
    try {
        f(resource)
    } finally {
        resource.close()
    }
  }
  
  
  private def endApp(msg: String)(implicit system: ActorSystem): Unit = {
    system.terminate()
    throw EndException(msg)
  }
  
  
  def main(args: Array[String]) {
    try {
      // actor system and implicit materializer
      implicit val system = ActorSystem("system")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = materializer.executionContext
      implicit val configuration = ConfigFactory.load()
      val columnPrefix = "column_"
      
      
      val x = using(io.Source.fromFile("example.txt")) { source => source.getLines }
      
      
      val arglist: Array[String] = Array("--csv", "test.csv", "--output", "test.sql", "--cols", "4");
      def isSwitch(s: String) = s.startsWith("--")
      def getOptions(acc: Map[String, String], args: List[String]) : Map[String, String] = {
        args match {
          case Nil => acc
          case key :: value :: tail if isSwitch(key) => getOptions(acc ++ Map(key -> value), args.tail)
          case option :: tail => println("Unknown option "+option); System.exit(1); Map()
        }
      }
      val options = getOptions(Map(), arglist.toList)
      println(options)
      
      
      val config = CsvDiffConfig(args.head, columnPrefix)
      
      val columns = (1 to config.columns map { n => columnPrefix+n }).toSeq
      
      val originGraph = CsvDiffParser.parse(CsvDiffFile.load(config.originFilename), columns)
      val targetGraph = CsvDiffParser.parse(CsvDiffFile.load(config.targetFilename), columns)
      
      val futures = List(originGraph, targetGraph)
      Future.sequence(futures).onComplete {
        case Success(results) =>
          val originResults = results(0)
          val targetResults = results(1)
          val result = DiffExecute.compare(config, originResults, targetResults)
          println("Results:")
          println("--------")
          println(result)
          system.terminate()
        case Failure(e) =>
          println(s"Failure: ${e.getMessage}")
          system.terminate()
      }
    } catch {
      case EndException(msg) =>
        println(s"Failure: $msg")
    }
  }
}
