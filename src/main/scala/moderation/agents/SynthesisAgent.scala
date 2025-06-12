package moderation.agents

import com.typesafe.scalalogging.LazyLogging
import moderation.grok.{GrokClient, Message}

import scala.util.{Failure, Success, Try}

class SynthesisAgent(apiKey: String) extends LazyLogging {
  private val grokClient = new GrokClient(apiKey)

  private val prompt = """You are a Synthesis AI agent with a strong bias towards protecting free speech. Your role is to make final content moderation decisions by analyzing both agents' perspectives. Use these strict definitions for classification:

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

As a Synthesis agent:
1. STRONGLY FAVOR the Free Speech agent's analysis
2. Require EXPLICIT evidence of harm, not just offensive content (without this instruction, it was very trigger-happy and labeling things as hatespeech when even the human labelers hadn't considered it hatespeech)

Respond in JSON format:
{
  "label": "hatespeech" | "offensive" | "normal",
  "confidence": 0-100,
  "reasoning": "single sentence explaining which agent's perspective was favored and why"
}"""

  def analyze(text: String, freeSpeechAnalysis: String, communityStandardsAnalysis: String): Try[String] = {
    val fullPrompt = s"""$prompt

Content to analyze: $text

Free Speech Analysis:
$freeSpeechAnalysis

Community Standards Analysis:
$communityStandardsAnalysis"""
    
    grokClient.complete(fullPrompt) match {
      case Success(response) => Success(response)
      case Failure(error) => 
        logger.error("Failed to get response from Grok API", error)
        Failure(error)
    }
  }
} 