package moderation.agents

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import moderation.models.{Tweet, AgentResponse}

class TestAgent extends ModerationAgent {
  val name = "Test Agent"
  val systemPrompt = "You are a test agent."
}

class ModerationAgentSpec extends AnyFlatSpec with Matchers {
  "ModerationAgent" should "handle API errors gracefully" in {
    val agent = new TestAgent()
    val tweet = Tweet("test_id", "test tweet", "normal")
    
    val response = agent.analyzeTweet(tweet)
    response.label should (be("neutral") or be("possibly illegal") or be("illegal"))
    response.reasoning should not be empty
  }
  
  "FreeSpeechAgent" should "have correct name and prompt" in {
    val agent = new FreeSpeechAgent()
    agent.name should be("Free Speech Agent")
    agent.systemPrompt should include("defending speech")
  }
  
  "CommunityStandardsAgent" should "have correct name and prompt" in {
    val agent = new CommunityStandardsAgent()
    agent.name should be("Community Standards Agent")
    agent.systemPrompt should include("community expectations")
  }
  
  "SynthesisAgent" should "handle empty agent responses" in {
    val agents = Seq(new TestAgent())
    val synthesisAgent = new SynthesisAgent(agents)
    val tweet = Tweet("test_id", "test tweet", "normal")
    
    val result = synthesisAgent.synthesize(tweet, Map.empty[String, AgentResponse])
    result.finalLabel should (be("neutral") or be("possibly illegal") or be("illegal"))
    result.reasoning should not be empty
  }
} 