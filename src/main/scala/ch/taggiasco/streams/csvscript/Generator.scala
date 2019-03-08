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
import akka.event.Logging
import akka.stream.Attributes
import akka.event.LogSource




object Generator {
  
  import Utilities._
  
  
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
      implicit val adapter = Logging(system, "logger")
      
      // if no arguments, we print help
      if (args.length == 0) {
        system.terminate()
        throw StopException
      }
      // get config from arguments if possible
      val config = try {
        Config(args.toList)
      } catch {
        case e: Exception => {
          system.terminate()
          throw e
        }
      }
      adapter.log(Attributes.LogLevels.Info, config.toString)
      
      
      // reading the template
      val template   = config.loadScriptTemplate()
//      val preScript  = config.loadPreScript()
//      val postScript = config.loadPostScript()
      
      
      // create an infinite iterator of ints
      val numbers: Source[Int, _] = Source.fromIterator(
        () => Iterator.from(1)
      )
      
      
      def load(config: Config, name: String): Source[ByteString, Future[IOResult]] = {
        val path = Paths.get(config.folder.map(_+"/").getOrElse("") + name)
        val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
        source.when(config.log)(s => {
          s.log("logger").withAttributes(
            Attributes.logLevels(
              onElement = Attributes.LogLevels.Off,
              onFinish  = Attributes.LogLevels.Info,
              onFailure = Attributes.LogLevels.Error
            )
          )
        })
      }
      
      
      def scannerFlow(config: Config): Flow[ByteString, List[ByteString], NotUsed] = {
        if(config.csvComma) {
          CsvParsing.lineScanner(CsvParsing.Comma)
        } else {
          CsvParsing.lineScanner(CsvParsing.SemiColon)
        }
      }
        
      
      
      def transformData(config: Config, data: String): String = {
        if(config.singleQuoteEscape) {
          data.replaceAll("'", "''")
        } else {
          data
        }
      }
      
      
      def isUnlimitedOrBefore(config: Config, lineNumber: Int): Boolean = {
        config.scriptLimit match {
          case Some(n) if config.csvHasHeaders && n >= lineNumber - 1 =>
            true
          case Some(n) if !config.csvHasHeaders && n >= lineNumber =>
            true
          case Some(n) =>
            false
          case None =>
            true
        }
      }
      
      
      def buildKey(n: Int): String = s"COLUMN_$n"
      
      
      def transformerFlow(config: Config, template: String): Flow[(Map[String, String], Int), ByteString, NotUsed] = {
        Flow[(Map[String, String], Int)].map(element => {
          val (datas, lineNumber) = element
          if(isUnlimitedOrBefore(config, lineNumber)) {
            val row = datas.keys.toList.map(_.substring(7).toInt).sorted.foldLeft("")((acc, key) => {
              acc match {
                case "" =>
                  datas.getOrElse(buildKey(key), config.valueForRow)
                case _ =>
                  acc + ";" + datas.getOrElse(buildKey(key), config.valueForRow)
              }
            })
            val res = datas.zipWithIndex.foldLeft(template)((acc, elem) => {
              val (data, position) = elem
              val transData = transformData(config, data._2)
              acc.replaceAll(s"%${data._1}%", transData)
            }).replaceAll("%ROW%", row)
            if(config.csvHasHeaders && lineNumber == 1) {
              if(config.csvNoHeaderLine) {
                ByteString("")
              } else {
                ByteString(
                  "/* sample based on headers\n" +
                  res.replaceAll("/\\*", "/ \\*").replaceAll("\\*/", "\\* /") +
                  "\n*/" +
                  "\n\n"
                )
              }
            } else {
              ByteString(res + "\n\n")
            }
          } else {
            ByteString("")
          }
        })
      }
      def getFileSink(config: Config) = {
        FileIO.toPath(
          config.getOutputFile().toPath,
          options = Set(APPEND) // CREATE, WRITE, TRUNCATE_EXISTING)
        )
      }
      
      
      def mappingFlow(config: Config): Flow[List[ByteString], Map[String, String], NotUsed] = {
        val headers = (1 to 100).map(n => buildKey(n)).toList
        CsvToMap.withHeadersAsStrings(config.csvCharset, headers:_*)
      }
      
      
      def isNotEmpty(elements: List[ByteString]): Boolean = {
        !elements.filter(elem => !elem.isEmpty).isEmpty
      }
      
      
      def parseToFile(config: Config, source: Source[ByteString, Future[IOResult]], template: String)(implicit materializer: ActorMaterializer):
        Future[IOResult] = {
        val sink = getFileSink(config)
        val src =
          source
            .via(scannerFlow(config))
            .filter(isNotEmpty)
            .via(mappingFlow(config))
            .zipWith(numbers)((row, lineNumber) => (row, lineNumber))
            .via(transformerFlow(config, template))
            .runWith(sink)
        src
      }
      
      
      val f = parseToFile(config, load(config, config.csvFile), template)
      
      f.onComplete {
        case Success(result) => {
          // add pre and post scripts
          config.appendPostScript()
          adapter.log(Attributes.LogLevels.Info, "Script generation done!")
          system.terminate()
        }
        case Failure(e) =>
          adapter.error(e, s"Failure: ${e.getMessage}")
          system.terminate()
      }
      
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
