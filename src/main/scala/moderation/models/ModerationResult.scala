package moderation.models

case class AgentDecision(
  label: String,  // "hatespeech", "offensive", or "normal"
  confidence: Double,
  reasoning: String
)

case class ModerationResult(
  postId: String,
  originalText: String,
  freeSpeechDecision: AgentDecision,
  communityStandardsDecision: AgentDecision,
  synthesisDecision: AgentDecision,
  processingTimeMs: Long,
  humanAnnotations: Seq[String],  // Original HateXplain annotations
  humanMajorityLabel: String,     // Majority label from human annotators
  hasHumanDisagreement: Boolean   // Whether human annotators disagreed
) 