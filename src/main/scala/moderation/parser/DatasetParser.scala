package moderation.parser

import play.api.libs.json._
import scalaj.http.Http
import com.typesafe.scalalogging.LazyLogging
import moderation.models._
import moderation.config.Config
import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.{Try, Success, Failure}

class DatasetParser extends LazyLogging {
  private val config = Config.datasetConfig

  def downloadDataset(): JsValue = {
    val dataFile = new File(config.path)
    
    if (dataFile.exists()) {
      //logger.info("Using cached dataset...")
      Try {
        val source = Source.fromFile(dataFile)
        val content = source.mkString
        source.close()
        Json.parse(content)
      } match {
        case Success(json) => json
        case Failure(e) => 
          logger.warn(s"Failed to read cached dataset: ${e.getMessage}. Downloading fresh copy...")
          downloadAndCache()
      }
    } else {
      //logger.info("Dataset not found. Downloading...")
      downloadAndCache()
    }
  }

  private def downloadAndCache(): JsValue = {
    Try {
      val response = Http(config.url).asString
      if (response.isError) {
        throw new RuntimeException(s"Failed to download dataset: ${response.code}")
      }
      
      new File(config.path).getParentFile.mkdirs()
      
      val writer = new PrintWriter(new File(config.path))
      writer.write(response.body)
      writer.close()
      
      Json.parse(response.body)
    } match {
      case Success(json) => json
      case Failure(e) => 
        //logger.error(s"Failed to download and cache dataset: ${e.getMessage}")
        throw e
    }
  }

  private def parseTweet(postId: String, tweetData: JsValue): Try[HateXplainEntry] = {
    Try {
      val postTokens = (tweetData \ "post_tokens").as[Seq[String]]
      val annotators = (tweetData \ "annotators").as[Seq[JsObject]].map { annotator =>
        HateXplainAnnotation(
          annotatorId = (annotator \ "annotator_id").as[Int],
          label = (annotator \ "label").as[String],
          target = (annotator \ "target").as[Seq[String]]
        )
      }

      HateXplainEntry(postId, postTokens, annotators)
    }
  }

  def parseDataset(): Seq[HateXplainEntry] = {
    parseNTweets(Int.MaxValue)
  }

  def parseNTweets(n: Int): Seq[HateXplainEntry] = {
    logger.info(s"Loading first $n tweets from dataset...")
    val dataset = downloadDataset()
    
    val entries = dataset.as[JsObject].fields
      .take(n)  // take the first n entries
      .map { case (postId, tweetData) =>
        parseTweet(postId, tweetData) match {
          case Success(entry) => Some(entry)
          case Failure(e) => 
            logger.error(s"Failed to parse tweet $postId: ${e.getMessage}")
            None
        }
      }.flatten
    
     //logger.info(s"Successfully parsed ${entries.size} entries")
    entries.toSeq
  }
} 
