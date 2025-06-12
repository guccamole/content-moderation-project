package moderation.utils

import com.typesafe.scalalogging.LazyLogging
import moderation.models._
import play.api.libs.json._
import java.io.{File, PrintWriter}

object DatasetUtils extends LazyLogging {
  def createDisputedSubset(entries: Seq[HateXplainEntry], outputPath: String): Unit = {
    logger.info(s"${entries.count(_.hasDisagreement)}") // dbug

    val disputedEntries = entries.filter(_.hasDisagreement)
    logger.info(s"${disputedEntries.size}") // dbug

    implicit val annotationWrites = Json.writes[HateXplainAnnotation]
    implicit val entryWrites = Json.writes[HateXplainEntry]

    val json = Json.obj(
      "metadata" -> Json.obj(
        "total_entries" -> disputedEntries.size,
        "creation_date" -> java.time.LocalDateTime.now().toString,
        "description" -> "Subset of HateXplain dataset containing only entries with annotator disagreement"
      ),
      "entries" -> disputedEntries
    )
    new File(outputPath).getParentFile.mkdirs()
    val writer = new PrintWriter(new File(outputPath))
    writer.write(Json.prettyPrint(json))
    writer.close()
    logger.info(s"${outputPath}") // dbug
  }

  def loadSubset(path: String): Seq[HateXplainEntry] = {
    logger.info(s"${path}") // dbug

    implicit val annotationReads = Json.reads[HateXplainAnnotation]
    implicit val entryReads = Json.reads[HateXplainEntry]

    val json = Json.parse(scala.io.Source.fromFile(path).mkString)
    val entries = (json \ "entries").as[Seq[HateXplainEntry]]

    logger.info(s"${entries.size}") // dbug
    entries
  }
}
