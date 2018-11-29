package ch.taggiasco.streams.csv

object DiffExecute {
  
  private def findData(
    elements: Seq[Map[String, String]],
    column:   String,
    value:    String
  ): Option[Map[String, String]] = {
    elements.find(element => {
      element.exists(kv => {
        kv._1 == column && kv._2 == value
      })
    })
  }
  
  
  private def verify(
    result: DiffResult,
    key:    String,
    source: Map[String, String],
    target: Map[String, String]
  ): DiffResult = {
    val res = source.foldLeft((result, true))((currentResult, currentElement) => {
      target.get(currentElement._1) match {
        case Some(v) if v == currentElement._2 =>
          // same data
          currentResult
        case Some(v) =>
          // different data
          (currentResult._1.addDiffByColForKey(currentElement._1, key), false)
        case None =>
          // not existing data
          (currentResult._1.addDiffByColForKey(currentElement._1, key), false)
      }
    })
    if(res._2) {
      res._1.newLineOK
    } else {
      res._1.newLineKO.addKoLine(key)
    }
  }
  
  
  private def removeDuplicateSpaces(s: String): String = s.trim().replaceAll(" +", " ")
  
  
  private def removeCarriageReturns(s: String): String = s.replaceAll("\n", " ")
  
  
  private def reformat(
    config:  CsvDiffConfig,
    value:   String
  ): String = {
    val newValue = {
      if(config.removeCarriageReturns) {
        removeCarriageReturns(value)
      } else {
        value
      }
    }
    if(config.removeDuplicateSpaces) {
      removeDuplicateSpaces(newValue)
    } else {
      newValue
    }
  }
  
  
  private def reformat(
    config:  CsvDiffConfig,
    elements: Seq[Map[String, String]]
  ): Seq[Map[String, String]] = {
    if(config.removeCarriageReturns || config.removeDuplicateSpaces) {
      elements.map(elem => {
        elem.map(nv => {
          nv._1 -> reformat(config, nv._2)
        })
      })
    } else {
      elements
    }
  }
  
  
  private def compare0(
    config:  CsvDiffConfig,
    sources: Seq[Map[String, String]],
    targets: Seq[Map[String, String]]
  ): DiffResult = {
    sources.foldLeft(DiffResult())((currentResult, source) => {
      val result = currentResult.addLine
      source.get(config.keyColumnName) match {
        case Some(value) if value.nonEmpty =>
          findData(targets, config.keyColumnName, value) match {
            case Some(target) =>
              // check the two lines
              verify(result, value, source, target)
            case None =>
              // the line is missing
              result.newMissingLine.addMissingLine(value)
          }
        case _ =>
          // if empty or not defined
          result.newLineWithNoKey
      }
    })
  }
  
  
  def compare(
    config:  CsvDiffConfig,
    sources: Seq[Map[String, String]],
    targets: Seq[Map[String, String]]
  ): DiffResult = {
    compare0(
      config,
      reformat(config, sources),
      reformat(config, targets)
    )
  }
  
}