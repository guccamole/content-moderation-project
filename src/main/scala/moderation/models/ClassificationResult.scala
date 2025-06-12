package moderation.models

case class InitialClassification(
  label: String,
  confidence: Double,
  reasoning: String
)

case class Phase1Result(
  postId: String,
  originalText: String,
  classification: InitialClassification,
  humanAnnotations: Seq[String],
  humanMajorityLabel: Option[String],  // None if no majority exists
  agreesWithMajority: Option[Boolean], // None if no majority exists
  processingTimeMs: Long
) 