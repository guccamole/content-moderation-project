//Basic initial sanity tests 

package moderation.parser

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._
import scala.util.Success

class DatasetParserSpec extends AnyFlatSpec with Matchers {
  "DatasetParser" should "parse a tweet correctly" in {
    val parser = new DatasetParser()
    val tweetJson = Json.parse("""
      {
        "post_tokens": ["hello", "world"],
        "annotators": [
          {"label": "normal", "annotator_id": 1, "target": ["None"]},
          {"label": "normal", "annotator_id": 2, "target": ["None"]},
          {"label": "offensive", "annotator_id": 3, "target": ["None"]}
        ]
      }
    """)
    
    val result = parser.parseTweet("test_id", tweetJson)
    result shouldBe a[Success[_]]
    
    val tweet = result.get
    tweet.postId should be("test_id")
    tweet.text should be("hello world")
    tweet.majorityLabel should be("normal")
  }
  
  it should "handle empty post_tokens" in {
    val parser = new DatasetParser()
    val tweetJson = Json.parse("""
      {
        "post_tokens": [],
        "annotators": [
          {"label": "normal", "annotator_id": 1, "target": ["None"]}
        ]
      }
    """)
    
    val result = parser.parseTweet("test_id", tweetJson)
    result shouldBe a[Success[_]]
    
    val tweet = result.get
    tweet.text should be("")
  }
  
  it should "handle missing fields gracefully" in {
    val parser = new DatasetParser()
    val tweetJson = Json.parse("""
      {
        "post_tokens": ["hello"]
      }
    """)
    
    val result = parser.parseTweet("test_id", tweetJson)
    result.isFailure should be(true)
  }
} 