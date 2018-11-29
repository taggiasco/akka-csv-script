package ch.taggiasco.streams.csv

import java.nio.file.Paths

import akka.util.ByteString
import akka.stream.ActorMaterializer
import akka.stream.IOResult
import akka.stream.scaladsl._

import scala.concurrent.Future


object CsvDiffFile {
  
  def load(name: String): Source[ByteString, Future[IOResult]] = {
    val path = Paths.get(name)
    val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
    source
  }
  
}