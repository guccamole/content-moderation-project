// NOT USED IN THE FINAL SYSTEM
package moderation.agents

import com.typesafe.scalalogging.LazyLogging
import moderation.grok.{GrokClient, Message}
import moderation.models.{InitialClassification, HateXplainEntry}

import scala.util.{Failure, Success, Try}

class InitialClassificationAgent(apiKey: String) extends LazyLogging {
  private val grokClient = new GrokClient(apiKey)

  private val prompt = """You are an AI content classifier. Your task is to analyze text content and classify it according to the HateXplain dataset scheme:

1. "hatespeech" - Content that expresses hatred or promotes violence against protected groups
2. "offensive" - Content that is rude, disrespectful, or inappropriate but not hateful
3. "normal" - Content that is neither hateful nor offensive

Make your classification based purely on the content, without any external context or considerations.
Focus on making an objective assessment of the content's nature.

Respond in JSON format:
{
  "label": "hatespeech" | "offensive" | "normal",
  "confidence": 0-100,
  "reasoning": "brief explanation of classification"
}"""

  def classify(text: String): Try[InitialClassification] = {
    val fullPrompt = s"$prompt\n\nContent to classify: $text"
    
    grokClient.complete(fullPrompt).flatMap { response =>
      import io.circe.parser.decode
      import io.circe.generic.auto._
      
      decode[InitialClassification](response) match {
        case Right(classification) => Success(classification)
        case Left(error) => 
          logger.error(s"Failed to parse classification response: $error")
          Failure(error)
      }
    }
  }
} 