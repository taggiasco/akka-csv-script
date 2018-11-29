package ch.taggiasco.streams.csv


case class DiffResult(
  totalLines:         Int,
  okLinesCount:       Int,
  koLinesCount:       Int,
  missingLinesCount:  Int,
  lineWithNoKeyCount: Int,
  keysDiffByCols:     Map[String, Set[String]],
  koLines:            Set[String],
  missingLines:       Set[String]
) {
  
  override def toString: String = {
    s"""Total number of lines : $totalLines
Number of lines OK : $okLinesCount
Number of lines KO : $koLinesCount
Number of missing lines : $missingLinesCount
Differences by columns :
${keysDiffByCols.map(s => " - " + s._1 + " : " + s._2.size).toList.sorted.mkString("\n")}
IDs with differences by columns :
${keysDiffByCols.map(s => " - " + s._1 + " : " + s._2.mkString(", ")).toList.sorted.mkString("\n")}
IDs of lines that are KO:
  ${koLines.toList.sorted.mkString(", ")}${if(koLines.isEmpty){"** none **"}else{""}}
IDs of lines that are missing:
  ${missingLines.toList.sorted.mkString(", ")}${if(missingLines.isEmpty){"** none **"}else{""}}
"""
  }
  
  def addLine: DiffResult = this.copy(totalLines = totalLines+1)
  
  def newLineOK: DiffResult = this.copy(okLinesCount = okLinesCount+1)
  
  def newLineKO: DiffResult = this.copy(koLinesCount = koLinesCount+1)
  
  def newMissingLine: DiffResult = this.copy(missingLinesCount = missingLinesCount+1)
  
  def newLineWithNoKey: DiffResult = this.copy(lineWithNoKeyCount = lineWithNoKeyCount+1)
  
  
  def addKoLine(id: String): DiffResult = this.copy(koLines = koLines + id)
  
  def addMissingLine(id: String): DiffResult = this.copy(missingLines = missingLines + id)
  
  
  def addDiffByColForKey(columnName: String, key: String): DiffResult = {
    val keys = keysDiffByCols.get(columnName).getOrElse(Set.empty[String]) + key
    val nameKeys = keysDiffByCols + (columnName -> keys)
    this.copy(/*diffByCols = diffs, */keysDiffByCols = nameKeys)
  }
  
}



object DiffResult {
  
  def apply(): DiffResult = {
    DiffResult(0, 0, 0, 0, 0, Map.empty[String, Set[String]], Set.empty[String], Set.empty[String])
  }
  
}
