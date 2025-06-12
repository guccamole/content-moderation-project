package moderation

import com.typesafe.scalalogging.LazyLogging
import moderation.parser.DatasetParser
import moderation.agents.InitialClassificationAgent
import moderation.models._
import moderation.utils.DatasetUtils

import scala.util.{Success, Failure, Try}

object Phase1Pipeline extends App with LazyLogging {
  val apiKey = sys.env.getOrElse("GROK_API_KEY", {
    logger.error("GROK_API_KEY environment variable not set")
    System.exit(1)
    ""
  })

  def runInitialClassification(sampleSize: Int = 10): Unit = {
    logger.info(s"Starting Phase 1: Initial Classification of $sampleSize entries")
    
    val parser = new DatasetParser()     // Initialize + Load + Process 
    val classifier = new InitialClassificationAgent(apiKey)
    val allEntries = parser.parseDataset()
    val sampleEntries = scala.util.Random.shuffle(allEntries).take(sampleSize)

    // ogger.info(s"Processing ${sampleEntries.size} entries...")
    
    val results = sampleEntries.map { entry =>
      val startTime = System.currentTimeMillis()
      
      classifier.classify(entry.postText) match {
        case Success(classification) =>
          val processingTime = System.currentTimeMillis() - startTime
          
          val labelCounts = entry.annotators.map(_.label).groupBy(identity).view.mapValues(_.size).toMap
          val maxCount = labelCounts.values.max
          val majorityLabel = if (labelCounts.values.count(_ == maxCount) == 1) {
            Some(labelCounts.maxBy(_._2)._1)
          } else None
          
          Success(Phase1Result(
            postId = entry.postId,
            originalText = entry.postText,
            classification = classification,
            humanAnnotations = entry.annotators.map(_.label),
            humanMajorityLabel = majorityLabel,
            agreesWithMajority = majorityLabel.map(_ == classification.label),
            processingTimeMs = processingTime
          ))
          
        case Failure(error) =>
          logger.error(s"Failed to classify entry ${entry.postId}", error)
          Failure(error)
      }
    }
    
    results.foreach {
      case Success(result) =>
        logger.info("\n=== Entry Analysis ===")
        logger.info(s"Post ID: ${result.postId}")
        logger.info(s"Text: ${result.originalText}")
        logger.info(s"AI Classification: ${result.classification.label} (${result.classification.confidence}%)")
        logger.info(s"AI Reasoning: ${result.classification.reasoning}")
        logger.info(s"Human Annotations: ${result.humanAnnotations.mkString(", ")}")
        logger.info(s"Human Majority: ${result.humanMajorityLabel.getOrElse("No majority")}")
        logger.info(s"Agrees with Majority: ${result.agreesWithMajority.map(if (_) "Yes" else "No").getOrElse("N/A")}")
        logger.info(s"Processing Time: ${result.processingTimeMs}ms")
        logger.info("==================\n")
        
      case Failure(error) =>
        logger.error("Processing failed", error)
    }
    
    // Calculate statistics
    val successfulResults = results.collect { case Success(r) => r }
    val resultsWithMajority = successfulResults.filter(_.humanMajorityLabel.isDefined)
    val agreementRate = if (resultsWithMajority.nonEmpty) {
      resultsWithMajority.count(_.agreesWithMajority.getOrElse(false)).toDouble / resultsWithMajority.size
    } else 0.0
    
    logger.info("\n=== Phase 1 Summary ===")
    logger.info(s"Processed ${results.size} entries")
    logger.info(s"Entries with human majority: ${resultsWithMajority.size}")
    logger.info(s"Agreement rate with human majority: ${agreementRate * 100}%")
    logger.info("=====================")
    
    // Phase 2
    val disputedEntries = successfulResults.filter(r => 
      r.humanAnnotations.distinct.size > 1 || // Human annotators disagreed
      r.humanMajorityLabel.exists(_ != r.classification.label) // AI disagrees with human majority
    )
    
    if (disputedEntries.nonEmpty) {
      logger.info(s"\nFound ${disputedEntries.size} entries requiring Phase 2 analysis")
      DatasetUtils.createDisputedSubset(
        disputedEntries.map(r => HateXplainEntry(
          r.postId,
          r.originalText.split(" ").toSeq,
          r.humanAnnotations.zipWithIndex.map { case (label, idx) =>
            HateXplainAnnotation(idx + 1, label, Seq("None"))
          }
        )),
        "data/phase2_entries.json"
      )
    }
  }
  
  runInitialClassification() // run
} 