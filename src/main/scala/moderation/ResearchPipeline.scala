package moderation

import com.typesafe.scalalogging.LazyLogging
import moderation.parser.DatasetParser
import moderation.models._

import scala.util.{Success, Failure, Try}

object ResearchPipeline extends App with LazyLogging {
  val apiKey = sys.env.getOrElse("GROK_API_KEY", {
    logger.error("GROK_API_KEY environment variable not set")
    System.exit(1)
    ""
  })

  def runDisputedTweetsAnalysis(sampleSize: Int = 10): Unit = {
    logger.info(s"Starting analysis of $sampleSize disputed tweets")
    
    val parser = new DatasetParser()
    val orchestrator = new ModerationOrchestrator(apiKey)
    val allEntries = parser.parseDataset()
    val disputedEntries = allEntries.filter(_.hasDisagreement)
    
    // logger.info(s"Found ${disputedEntries.size} disputed entries in dataset")
    
    val sampleEntries = scala.util.Random.shuffle(disputedEntries).take(sampleSize)
    val results = orchestrator.batchModerate(sampleEntries)
    
    results.zip(sampleEntries).foreach {
      case (Success(result), entry) =>
        logger.info("\n=== Entry Analysis ===")
        logger.info(s"Post ID: ${result.postId}")
        logger.info(s"Text: ${result.originalText}")
        logger.info(s"Human Annotations: ${result.humanAnnotations.mkString(", ")}")
        logger.info(s"Human Majority: ${result.humanMajorityLabel}")
        logger.info("\nAgent Decisions:")
        logger.info(s"Free Speech: ${result.freeSpeechDecision.label} (${result.freeSpeechDecision.confidence}%)")
        logger.info(s"Community Standards: ${result.communityStandardsDecision.label} (${result.communityStandardsDecision.confidence}%)")
        logger.info(s"Synthesis: ${result.synthesisDecision.label} (${result.synthesisDecision.confidence}%)")
        logger.info(s"Processing Time: ${result.processingTimeMs}ms")
        logger.info("==================\n")
      
      case (Failure(error), entry) =>
        logger.error(s"Failed to process entry ${entry.postId}", error)
    }
    
    val successfulResults = results.collect { case Success(r) => r }
    val agreementRate = successfulResults.count(r => r.synthesisDecision.label == r.humanMajorityLabel).toDouble / successfulResults.size
    
    logger.info("\n=== Analysis Summary ===")
    logger.info(s"Processed ${results.size} disputed entries")
    logger.info(s"Agreement rate with human majority: ${agreementRate * 100}%")
    logger.info("=====================")
  }
  
  runDisputedTweetsAnalysis()
} 