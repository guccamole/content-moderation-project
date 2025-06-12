package moderation.models

case class HateXplainAnnotation(
  annotatorId: Int,
  label: String,
  target: Seq[String]
)

case class HateXplainEntry(
  postId: String,
  postTokens: Seq[String],
  annotators: Seq[HateXplainAnnotation]
) {
  def postText: String = postTokens.mkString(" ")
  
  def majorityLabel: String = {
    val labelCounts = annotators.map(_.label).groupBy(identity).view.mapValues(_.size).toMap
    labelCounts.maxBy(_._2)._1
  }
  
  def hasDisagreement: Boolean = {
    annotators.map(_.label).distinct.size > 1
  }
}

case class HateXplainData(entries: Seq[HateXplainEntry]) 