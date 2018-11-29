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


object CsvDiffParser {
  
  private val scannerFlow: Flow[ByteString, List[ByteString], NotUsed] = CsvParsing.lineScanner(CsvParsing.SemiColon)
    
  private def asMapFlow(columns: Seq[String]): Flow[List[ByteString], Map[String, String], NotUsed] = {
      CsvToMap.withHeadersAsStrings(StandardCharsets.UTF_8, columns:_*)
  }
  
  def parse(source: Source[ByteString, Future[IOResult]], columns: Seq[String])(implicit materializer: ActorMaterializer): Future[Seq[Map[String, String]]] = {
    source.via(scannerFlow).via(asMapFlow(columns)).runWith(Sink.seq)
  }
  
}
