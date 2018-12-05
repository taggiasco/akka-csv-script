package ch.taggiasco.streams.csvscript

import scala.io.Source
import java.io.File


case class Config(
  csvFile:           String,
  scriptTemplate:    String,
  scriptOutput:      Option[String],
  singleQuoteEscape: Boolean,
  scriptLimit:       Option[Int],
  preScriptFile:     Option[String],
  postScriptFile:    Option[String]
) {
  
  def loadScriptTemplate(): String = {
    Utilities.using(Source.fromFile(scriptTemplate)) {
      source => source.getLines.mkString("\n")
    }
  }
  
  def getOutputFile(): File = {
    val s = new File(scriptOutput.getOrElse("output.script"))
    if(!s.exists()) {
      s.createNewFile()
    }
    s
  }
  
}


object Config {
  
  lazy val helper =
    """Available arguments:
  --csv              : specify the CSV file to load in order to generate the scripts
  --template         : name of the script template
  --output           : name of the output file that will contain the scripts
  --scriptLimit      : number to limit the rows treated in the script
  --prescript        : name of the pre-script file
  --postscript       : name of the post-script file
  -escapeSingleQuote : allow to espace the single quote by doubling them
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
      options.get("--template").getOrElse( throw ConfigException("CSV file is mandatory") ),
      options.get("--output"),
      options.get("-escapeSingleQuote").isDefined,
      options.get("--scriptLimit").flatMap(v => { scala.util.Try(v.toInt).toOption } ),
      options.get("--prescript"),
      options.get("--postscript")
    )
  }
}
