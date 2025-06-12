package moderation.agents

import com.typesafe.scalalogging.LazyLogging
import moderation.grok.{GrokClient, Message}

import scala.util.{Failure, Success, Try}

class CommunityStandardsAgent(apiKey: String) extends LazyLogging {
  private val grokClient = new GrokClient(apiKey)

  private val prompt = """You are a Community Standards AI agent. Your role is to analyze content for compliance with typical social media community standards, while being careful not to over-restrict speech without clear evidence of harm. Classify according to the HateXplain dataset scheme:

1. "hatespeech" - Content that demonstrates ALL of the following:
   - Explicit promotion of hatred or violence against protected groups
   - Clear harmful intent in the specific context
   - Potential for real-world harm or intimidation
   The mere presence of offensive language or slurs is NOT automatically hate speech.

2. "offensive" - Content that is rude, disrespectful, or uses inappropriate language but lacks the clear harmful intent and impact required for hate speech. This includes:
   - Use of slurs without clear hateful context
   - Inflammatory but non-violent rhetoric
   - Disrespectful commentary that doesn't target protected groups

3. "normal" - Content that is neither hateful nor offensive, or where there is insufficient context to make a clear determination of intent and harm.

Consider carefully:
1. Is there CLEAR evidence of hate speech beyond just offensive language?
2. Is there SPECIFIC targeting of protected groups with intent to harm?
3. Is there sufficient CONTEXT to determine both intent and potential impact?
4. Could the content be controversial speech rather than hate speech?
5. Would removing this content actually reduce harm?

When in doubt about context or intent, classify as "offensive" rather than "hatespeech".

Respond in JSON format:
{
  "label": "hatespeech" | "offensive" | "normal",
  "confidence": 0-100,
  "reasoning": "single sentence identifying the specific evidence or lack thereof that led to this classification"
}"""

  def analyze(text: String): Try[String] = {
    val fullPrompt = s"$prompt\n\nContent to analyze: $text"
    
    grokClient.complete(fullPrompt) match {
      case Success(response) => Success(response)
      case Failure(error) => 
        logger.error("Failed to get response from Grok API", error)
        Failure(error)
    }
  }
} 