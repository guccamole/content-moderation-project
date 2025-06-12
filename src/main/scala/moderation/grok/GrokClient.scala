package moderation.grok

import io.circe.{Json, parser}
import io.circe.generic.auto._
import io.circe.syntax._
import scalaj.http.{Http, HttpResponse}
import com.typesafe.scalalogging.LazyLogging
import moderation.config.Config

import scala.util.{Failure, Success, Try}

case class Message(role: String, content: String)
case class ChatRequest(
  model: String,
  messages: List[Message],
  stream: Boolean = false,
  temperature: Double
)
case class Choice(message: Message, index: Int)
case class ChatResponse(choices: List[Choice])

class GrokClient(apiKey: String) extends LazyLogging {
  private val config = Config.grokConfig
  private val baseUrl = config.apiUrl
  private val model = config.model
  private val systemMessage = Message("system", "You are Grok, an AI trained to analyze and moderate content with a focus on balancing free speech with community safety.")

  def complete(prompt: String): Try[String] = {
    completeWithRetry(prompt, retriesLeft = config.maxRetries)
  }

  private def completeWithRetry(prompt: String, retriesLeft: Int): Try[String] = {
    val request = ChatRequest(
      model = model,
      messages = List(
        systemMessage,
        Message("user", prompt)
      ),
      stream = false,
      temperature = config.temperature
    )

    Try {
      val response: HttpResponse[String] = Http(baseUrl)
        .header("Content-Type", "application/json")
        .header("Authorization", s"Bearer $apiKey")
        .timeout(connTimeoutMs = config.timeout.toMillis.toInt, readTimeoutMs = config.timeout.toMillis.toInt)
        .postData(request.asJson.noSpaces)
        .asString

      handleResponse(response)
    }.flatten.recoverWith { case error =>
      if (retriesLeft > 0) {
        logger.warn(s"API request failed (${config.maxRetries - retriesLeft + 1}/${config.maxRetries}), retrying in ${config.retryDelay.toMillis}ms: ${error.getMessage}")
        Thread.sleep(config.retryDelay.toMillis)
        completeWithRetry(prompt, retriesLeft - 1)
      } else {
        logger.error(s"API request failed after ${config.maxRetries} retries: ${error.getMessage}")
        Failure(error)
      }
    }
  }

  private def handleResponse(response: HttpResponse[String]): Try[String] = {
    if (response.code == 200) {
      parser.parse(response.body).toTry.flatMap { json =>
        Try {
          json.as[ChatResponse].toTry.map { chatResponse =>
            chatResponse.choices.headOption.map(_.message.content)
              .getOrElse(throw new RuntimeException("No response content"))
          }.get
        }
      }
    } else {
      val errorMessage = s"API request failed with status ${response.code}: ${response.body}"
      logger.error(errorMessage)
      Failure(new RuntimeException(errorMessage))
    }
  }
} 