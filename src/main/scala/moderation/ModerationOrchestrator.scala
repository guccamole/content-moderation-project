package moderation

import com.typesafe.scalalogging.LazyLogging
import moderation.agents._
import moderation.models._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.parser.decode

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class ModerationOrchestrator(apiKey: String) extends LazyLogging {
  private val freeSpeechAgent = new FreeSpeechAgent(apiKey)
  private val communityStandardsAgent = new CommunityStandardsAgent(apiKey)
  private val synthesisAgent = new SynthesisAgent(apiKey)
  
  private case class AgentResponse(label: String, confidence: Double, reasoning: String)
  private object AgentResponse {
    implicit val decoder: Decoder[AgentResponse] = deriveDecoder[AgentResponse]
  }
  
  private def parseAgentResponse(response: String): Try[AgentResponse] = {
    decode[AgentResponse](response) match {
      case Right(parsed) => Success(parsed)
      case Left(error) => Failure(new RuntimeException(s"Failed to parse agent response: ${error.getMessage}"))
    }
  }
  
  def moderateEntry(entry: HateXplainEntry): Try[ModerationResult] = {
    val startTime = System.currentTimeMillis()
    
    for {
      freeSpeechResponse <- freeSpeechAgent.analyze(entry.postText)
      freeSpeechDecision <- parseAgentResponse(freeSpeechResponse)
      _ = logger.info(s"Free Speech Analysis: $freeSpeechResponse")
      
      communityStandardsResponse <- communityStandardsAgent.analyze(entry.postText)
      communityStandardsDecision <- parseAgentResponse(communityStandardsResponse)
      _ = logger.info(s"Community Standards Analysis: $communityStandardsResponse")
      
      synthesisResponse <- synthesisAgent.analyze(
        entry.postText,
        freeSpeechResponse,
        communityStandardsResponse
      )
      synthesisDecision <- parseAgentResponse(synthesisResponse)
      _ = logger.info(s"Synthesis Decision: $synthesisResponse")
      
    } yield {
      val processingTime = System.currentTimeMillis() - startTime
      
      ModerationResult(
        postId = entry.postId,
        originalText = entry.postText,
        freeSpeechDecision = AgentDecision(
          label = freeSpeechDecision.label,
          confidence = freeSpeechDecision.confidence,
          reasoning = freeSpeechDecision.reasoning
        ),
        communityStandardsDecision = AgentDecision(
          label = communityStandardsDecision.label,
          confidence = communityStandardsDecision.confidence,
          reasoning = communityStandardsDecision.reasoning
        ),
        synthesisDecision = AgentDecision(
          label = synthesisDecision.label,
          confidence = synthesisDecision.confidence,
          reasoning = synthesisDecision.reasoning
        ),
        processingTimeMs = processingTime,
        humanAnnotations = entry.annotators.map(_.label),
        humanMajorityLabel = entry.majorityLabel,
        hasHumanDisagreement = entry.hasDisagreement
      )
    }
  }
  
  def batchModerate(entries: Seq[HateXplainEntry], maxEntries: Int = 10): Seq[Try[ModerationResult]] = {
    logger.info(s"Starting batch moderation of ${Math.min(entries.length, maxEntries)} entries")
    
    entries.take(maxEntries).map { entry =>
      logger.info(s"Processing entry: ${entry.postId}")
      moderateEntry(entry)
    }
  }
} 