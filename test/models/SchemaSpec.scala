package models

import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import slick.driver.SQLiteDriver.api._

class SchemaSpec extends PlaySpec with OneAppPerTest {
  "SchemaSpec" should {
    "print the database schema" in {
      println()
      PasteData.schema.createStatements.foreach(println)
      println("----")
      PasteWriteData.schema.createStatements.foreach(println)
      println("----")
      DeadPasteFiles.schema.createStatements.foreach(println)
      println()
      println("--------")
      println()
      PasteData.schema.dropStatements.foreach(println)
      println("----")
      PasteWriteData.schema.dropStatements.foreach(println)
      println("----")
      DeadPasteFiles.schema.dropStatements.foreach(println)
      println()
    }
  }
}
