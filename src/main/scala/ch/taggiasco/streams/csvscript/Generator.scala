package ch.taggiasco.streams.csvscript

import java.io.File
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption._

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
      
      // reading the template
      val template = config.loadScriptTemplate()
      
      def load(name: String): Source[ByteString, Future[IOResult]] = {
        val path = Paths.get(name)
        val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
        source
      }
      
      val scannerFlow: Flow[ByteString, List[ByteString], NotUsed] =
        CsvParsing.lineScanner(CsvParsing.SemiColon)
    
    /*
    
    def reduceFlow(f: LogEntry => Particularity): Flow[LogEntry, (Particularity, LogEntry), NotUsed] = {
      Flow[LogEntry].map(logEntry => (f(logEntry), logEntry))
    }
    */
      
      def transformerFlow(template: String): Flow[Seq[String], ByteString, NotUsed] = {
        Flow[Seq[String]].map(datas => {
          val res = datas.zipWithIndex.foldLeft(template)((acc, elem) => {
            val (data, position) = elem
            acc.replaceAll(s"%COLUMN_${position+1}%", data)
          })
          ByteString(res + "\n\n")
        })
      }
      
      def transformerFlowAsString(template: String): Flow[Seq[String], String, NotUsed] = {
        Flow[Seq[String]].map(datas => {
          val res = datas.zipWithIndex.foldLeft(template)((acc, elem) => {
            val (data, position) = elem
            acc.replaceAll(s"%COLUMN_${position+1}%", data)
          })
          res
        })
      }
      
      def parse(source: Source[ByteString, Future[IOResult]], template: String)(implicit materializer: ActorMaterializer):
        Future[Seq[String]] = {
        val s =
          source
            .via(scannerFlow)
            .map(_.map(_.utf8String))
            .via(transformerFlowAsString(template))
            .runWith(Sink.seq)
        s
      }
      
      val fileSink = FileIO.toPath(
        config.getOutputFile().toPath,
        options = Set(CREATE, WRITE, TRUNCATE_EXISTING)
      )
      
      def parseToFile(source: Source[ByteString, Future[IOResult]], template: String)(implicit materializer: ActorMaterializer):
        Future[IOResult] = {
        val s =
          source
            .via(scannerFlow)
            .map(_.map(_.utf8String))
            .via(transformerFlow(template))
            .runWith(fileSink)
        s
      }
      val f = parseToFile(load(config.csvFile), template)
      f.onComplete {
        case Success(result) => {
          println("It's done!")
          system.terminate()
        }
        case Failure(e) =>
          println(s"Failure: ${e.getMessage}")
          system.terminate()
      }
      
//      val future = parse(load(config.csvFile), template)
//      future.onComplete {
//        case Success(result) => {
//          result.foreach(res => {
//            println("Result is : ")
//            println("---")
//            println(res)
//            println("---")
//          })
//          system.terminate()
//        }
//        case Failure(e) =>
//          println(s"Failure: ${e.getMessage}")
//          system.terminate()
//      }
    } catch {
      case StopException => {
        println(Config.helper)
      }
      case ConfigException(msg) => {
        println(s"Error in arguments: $msg")
        println(Config.helper)
      }
      case EndException(msg) =>
        println(s"Failure: $msg")
    }
  }
}
