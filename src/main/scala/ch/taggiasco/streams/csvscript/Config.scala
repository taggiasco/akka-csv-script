package ch.taggiasco.streams.csvscript

import scala.io.Source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.io.FileWriter


case class Config(
  csvFile:           String,
  csvHasHeaders:     Boolean,
  csvNoHeaderLine:   Boolean,
  scriptTemplate:    String,
  scriptOutput:      Option[String],
  singleQuoteEscape: Boolean,
  scriptLimit:       Option[Int],
  preScriptFile:     Option[String],
  postScriptFile:    Option[String]
) {
  
  private val dateFormater = new SimpleDateFormat("dd.MM.yyyy hh:mm")
  
  private def loadFromFile(file: String): String = {
    Utilities.using(Source.fromFile(file)) {
      source => source.getLines.mkString("\n")
    }
  }
  
  def loadScriptTemplate(): String = loadFromFile(scriptTemplate)
  
  def loadPreScript(): String = preScriptFile.map(loadFromFile).getOrElse("")
  
  def loadPostScript(): String = postScriptFile.map(loadFromFile).getOrElse("")
  
  def appendPostScript() {
    val content = loadPostScript()
    if (content.nonEmpty) {
      val f = getOutputFile()
      Utilities.using(new FileWriter(f))(fw => {
        fw.append(content)
      })
    }
  }
  
  def getOutputFile(): File = {
    val date = dateFormater.format(Calendar.getInstance().getTime())
    val f = new File(scriptOutput.getOrElse(s"output_$date.script"))
    if(!f.exists()) {
      f.createNewFile()
    }
    val content = loadPreScript()
    if(content.nonEmpty) {
      Utilities.using(new FileWriter(f))(fw => {
        fw.append(content)
      })
    }
    f
  }
  
}


object Config {
  
  lazy val helper =
    """Available arguments:
  --csv                 : specify the CSV file to load in order to generate the scripts
  -csv-has-header       : indicate that the CSV file contains a header line
  -csv-no-header-line   : indicate that the CSV header line should be put as an example
  --template            : name of the script template
  --output              : name of the output file that will contain the scripts
  --script-limit        : number to limit the rows treated in the script
  --pre-script          : name of the pre-script file
  --post-script         : name of the post-script file
  -escape-single-quote  : allow to espace the single quote by doubling them
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
  
  
  def apply(args: List[String]): Config = {
    val options = getOptions(Map(), args)
    Config(
      options.get("--csv").getOrElse( throw ConfigException("CSV file is mandatory") ),
      options.get("-csv-has-header").isDefined,
      options.get("-csv-no-header-line").isDefined,
      options.get("--template").getOrElse( throw ConfigException("CSV file is mandatory") ),
      options.get("--output"),
      options.get("-escape-single-quote").isDefined,
      options.get("--script-limit").flatMap(v => { scala.util.Try(v.toInt).toOption } ),
      options.get("--pre-script"),
      options.get("--post-script")
    )
  }
}
