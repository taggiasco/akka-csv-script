package ch.taggiasco.streams.csvscript

import scala.io.Source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.io.FileWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.Paths
import java.nio.file.Path


case class Config(
  csvFile:           String,
  csvHasHeaders:     Boolean,
  csvNoHeaderLine:   Boolean,
  csvComma:          Boolean,
  csvCharset:        Charset,
  scriptTemplate:    String,
  scriptOutput:      Option[String],
  singleQuoteEscape: Boolean,
  scriptLimit:       Option[Int],
  preScriptFile:     Option[String],
  postScriptFile:    Option[String],
  folder:            Option[String],
  log:               Boolean
) {
  
  private val defaultValue = "*****"
  
  val valueForRow = defaultValue
  
  private val dateFormater = new SimpleDateFormat("dd.MM.yyyy_hh.mm")
  
  private def loadFromFile(file: String): String = {
    val path = folder.map(_ + "/").getOrElse("") + file
    println(path)
    Utilities.using(Source.fromFile(path)) {
      source => source.getLines.mkString("\n")
    }
  }
  
  
  def getOutputFile(apprendPreScript: Boolean = true): File = {
    val date = dateFormater.format(Calendar.getInstance().getTime())
    val f = new File(folder.map(_ + "/").getOrElse("") + scriptOutput.getOrElse(s"output_$date.script"))
    if(!f.exists()) {
      f.createNewFile()
    }
    if(apprendPreScript) {
      writePreScript()
    }
    f
  }
  
  def getOutputPath(): Path = {
    getOutputFile(false).toPath()
  }
  
  
  def loadScriptTemplate(): String = loadFromFile(scriptTemplate)
  
  private def loadPreScript(): String = preScriptFile.map(loadFromFile).getOrElse("")
  
  private def loadPostScript(): String = postScriptFile.map(loadFromFile).getOrElse("")
  
  def appendPostScript() {
    val content = loadPostScript()
    if(content.nonEmpty) {
      Files.write(getOutputPath(), (content + "\n\n").getBytes(csvCharset), StandardOpenOption.APPEND)
    }
  }
  
  private def writePreScript() {
    val content = loadPreScript()
    if(content.nonEmpty) {
      Files.write(getOutputPath(), (content + "\n\n").getBytes(csvCharset), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
    }
  }
  
}


object Config {
  
  lazy val helper =
    """
Mandatory arguments:
  --csv                 : specify the CSV file to load in order to generate the scripts
  --template            : name of the script template

Available options:
  -csv-comma            : indicate that the values in the CSV file are separated with a comma (by default, the semi-colon is used)
  -csv-has-header       : indicate that the CSV file contains a header line
  -csv-no-header-line   : indicate that the CSV header line should not be put as an example
  --csv-charset         : name of the charset encoding of the CSV file (utf-8, iso-8859-1, ...)
  --output              : name of the output file that will contain the scripts, by default, "output_{date}.script" will be used.
  --script-limit        : number to limit the rows treated in the script
  --pre-script          : name of the pre-script file
  --post-script         : name of the post-script file
  --folder              : folder that contains the template, pre- and post-script, and where the generated script will be created
  -escape-single-quote  : allow to espace the single quote by doubling them
  -help                 : force to output the different options
  -log                  : log every element of the stream

"""
  
  
  private def isArgName(s: String) = s.startsWith("--")
  
  private def isArgFlag(s: String) = s.startsWith("-")
  
  private def getOptions(acc: Map[String, String], args: List[String]) : Map[String, String] = {
    args match {
      case key :: value :: tail if isArgName(key) => {
        getOptions(acc ++ Map(key -> value), tail)
      }
      case flag :: tail if isArgFlag(flag) => {
        getOptions(acc ++ Map(flag -> "1"), tail)
      }
      case option :: tail => Map()
      case Nil => acc
    }
  }
  
  private def identifyCharset(arg: Option[String]): Charset = {
    arg.map(_.toLowerCase().replaceAll("-", "").replaceAll("_", "")) match {
      case Some(v) if v == "iso88591" => StandardCharsets.ISO_8859_1
      case Some(v) if v == "usascii"  => StandardCharsets.US_ASCII
      case Some(v) if v == "utf16"    => StandardCharsets.UTF_16
      case Some(v) if v == "utf16be"  => StandardCharsets.UTF_16BE
      case Some(v) if v == "utf16le"  => StandardCharsets.UTF_16LE
      case Some(v) if v == "utf8"     => StandardCharsets.UTF_8
      case _                          => StandardCharsets.UTF_8
    }
  }
  
  
  def apply(args: List[String]): Config = {
    val options = getOptions(Map(), args)
    val help = options.get("-help").isDefined
    if(help) {
      throw StopException
    }
    Config(
      options.get("--csv").getOrElse( throw ConfigException("CSV file is mandatory") ),
      options.get("-csv-has-header").isDefined,
      options.get("-csv-no-header-line").isDefined,
      options.get("-csv-comma").isDefined,
      identifyCharset(options.get("--csv-charset")),
      options.get("--template").getOrElse( throw ConfigException("CSV file is mandatory") ),
      options.get("--output"),
      options.get("-escape-single-quote").isDefined,
      options.get("--script-limit").flatMap(v => { scala.util.Try(v.toInt).toOption } ),
      options.get("--pre-script"),
      options.get("--post-script"),
      options.get("--folder"),
      options.get("-log").isDefined
    )
  }
}
