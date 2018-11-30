package ch.taggiasco.streams.csvscript

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
  private case object StopException extends Exception
  
  
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
      
      // if no arguments, we print help
      if (args.length == 0) {
        system.terminate()
        throw StopException
      }
      // get config from arguments if possible
      val config = try {
        Config(args.toList)
      } catch {
        case e: ConfigException => {
          system.terminate()
          throw e
        }
      }
      
      
      /*
      val columns = (1 to config.columns map { n => columnPrefix+n }).toSeq
      
      val originGraph = CsvDiffParser.parse(CsvDiffFile.load(config.originFilename), columns)
      
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
      */
    } catch {
      case StopException => {
        println(Config.helper)
      }
      case ConfigException(msg) => {
        println(s"Error in arguments")
        println(Config.helper)
      }
      case EndException(msg) =>
        println(s"Failure: $msg")
    }
  }
}
