package moderation.config

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

case class GrokConfig(
  apiUrl: String,
  model: String,
  timeout: Duration,
  maxRetries: Int,
  retryDelay: Duration,
  temperature: Double
)

case class DatasetConfig(
  url: String,
  path: String
)

case class OutputConfig(
  path: String,
  phase1Path: String,
  phase2Path: String,
  disputedPath: String
)

case class AgentConfig(
  name: String,
  description: String
)

case class AgentsConfig(
  initialClassifier: AgentConfig,
  freeSpeech: AgentConfig,
  communityStandards: AgentConfig,
  synthesis: AgentConfig
)

case class ModerationConfig(
  grok: GrokConfig,
  dataset: DatasetConfig,
  output: OutputConfig,
  agents: AgentsConfig
)

object Config {
  private val config = ConfigFactory.load()
  private val moderationConfig = config.getConfig("moderation")
  private val agentConfigSection = config.getConfig("agents")
  
  val grokConfig = GrokConfig(
    apiUrl = moderationConfig.getConfig("grok").getConfig("api").getString("url"),
    model = moderationConfig.getConfig("grok").getConfig("api").getString("model"),
    timeout = moderationConfig.getConfig("grok").getDuration("timeout").toMillis.millis,
    maxRetries = moderationConfig.getConfig("grok").getInt("max-retries"),
    retryDelay = moderationConfig.getConfig("grok").getDuration("retry-delay").toMillis.millis,
    temperature = moderationConfig.getConfig("grok").getDouble("temperature")
  )
  
  val datasetConfig = DatasetConfig(
    url = moderationConfig.getConfig("dataset").getString("url"),
    path = moderationConfig.getConfig("dataset").getString("path")
  )
  
  val outputConfig = OutputConfig(
    path = moderationConfig.getConfig("output").getString("path"),
    phase1Path = moderationConfig.getConfig("output").getString("phase1"),
    phase2Path = moderationConfig.getConfig("output").getString("phase2"),
    disputedPath = moderationConfig.getConfig("output").getString("disputed")
  )
  
  val agents = AgentsConfig(
    initialClassifier = AgentConfig(
      name = agentConfigSection.getConfig("initial-classifier").getString("name"),
      description = agentConfigSection.getConfig("initial-classifier").getString("description")
    ),
    freeSpeech = AgentConfig(
      name = agentConfigSection.getConfig("free-speech").getString("name"),
      description = agentConfigSection.getConfig("free-speech").getString("description")
    ),
    communityStandards = AgentConfig(
      name = agentConfigSection.getConfig("community-standards").getString("name"),
      description = agentConfigSection.getConfig("community-standards").getString("description")
    ),
    synthesis = AgentConfig(
      name = agentConfigSection.getConfig("synthesis").getString("name"),
      description = agentConfigSection.getConfig("synthesis").getString("description")
    )
  )
  
  val moderationAppConfig = ModerationConfig(grokConfig, datasetConfig, outputConfig, agents)
} 