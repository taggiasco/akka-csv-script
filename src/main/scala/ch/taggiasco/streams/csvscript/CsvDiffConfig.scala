package ch.taggiasco.streams.csv

import com.typesafe.config.Config


case class CsvDiffConfig(
  name:                  String,
  originFilename:        String,
  targetFilename:        String,
  columns:               Int,
  keyColumn:             Int,
  keyColumnName:         String,
  removeDuplicateSpaces: Boolean,
  removeCarriageReturns: Boolean
) {
  require(keyColumn <= columns)
}


object CsvDiffConfig {
  def apply(name: String, columnPrefix: String)(implicit config: Config): CsvDiffConfig = {
    val conf = config.getConfig("csv-diff").getConfig(name)
    CsvDiffConfig(
      name,
      conf.getString("originFilename"),
      conf.getString("targetFilename"),
      conf.getInt("columns"),
      conf.getInt("keyColumn"),
      columnPrefix + conf.getInt("keyColumn"),
      conf.getBoolean("removeDuplicateSpaces"),
      conf.getBoolean("removeCarriageReturns")
    )
  }
}
