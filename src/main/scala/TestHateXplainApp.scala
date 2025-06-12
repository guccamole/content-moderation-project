import com.typesafe.scalalogging.LazyLogging
import moderation.ModerationOrchestrator
import moderation.models._
import moderation.parser.DatasetParser

import scala.util.{Failure, Success}

object TestHateXplainApp extends App with LazyLogging {
  val apiKey = sys.env.getOrElse("GROK_API_KEY", {
    logger.error("GROK_API_KEY environment variable not set")
    System.exit(1)
    ""
  })

  val NumTweetsToTest = 1
  logger.info(s"Starting Moderation Test with $NumTweetsToTest tweets")

  val parser = new DatasetParser()
  val testEntries = parser.parseNTweets(NumTweetsToTest)
  
  if (testEntries.isEmpty) {
    logger.error("No entries found in dataset")
    System.exit(1)
  }

  logger.info(s"Successfully loaded ${testEntries.size} tweets for testing")
  val orchestrator = new ModerationOrchestrator(apiKey)
  
  testEntries.zipWithIndex.foreach { case (entry, index) =>
    logger.info(s"\nProcessing tweet ${index + 1}/$NumTweetsToTest")
    logger.info(s"Tweet ID: ${entry.postId}")
    logger.info(s"Content: '${entry.postTokens.mkString(" ")}'")
    logger.info(s"Human annotations: ${entry.annotators.map(_.label).mkString(", ")}")
    
    orchestrator.moderateEntry(entry) match {
      case Success(result) =>
        logger.info("\nModeration Result:")
        logger.info(s"AI Classification: ${result.synthesisDecision.label}")
        logger.info(s"Confidence: ${result.synthesisDecision.confidence}%")
        logger.info(s"Reasoning: ${result.synthesisDecision.reasoning}")
        logger.info(s"Processing Time: ${result.processingTimeMs}ms")
        
      case Failure(error) =>
        logger.error(s"Failed to moderate tweet ${index + 1}", error)
    }
  }

  logger.info("\nTest completed!")
} 