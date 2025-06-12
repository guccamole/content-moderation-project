# Two-Phase Content Moderation System

Hey Jerry! I built a basic content moderation system that uses multiple AI agents to make moderation decisions. Basically it has different agents look at the same content from different angles (like free speech, community standards etc) and then makes a judgement.

## Dataset & Methodology

This project uses the HateXplain dataset, which contains 20,148 tweets that were manually labeled by multiple human annotators. Each tweet was classified as either normal, offensive, or hate speech. What's really interesting about this dataset is that over half of it (10,303 tweets, or 51.14%) has disagreement among the annotators - i.e. there is substantial variation among well-intentioned humans about what constitutes hateful and/or offensive speech.

Thus, I'm throwing AI at this problem to help resolve these disputes in a more systematic way. So I only focused on disputed tweets because I thought those would be more interesting.

## How It Works

The system uses a two-phase approach:

### Phase 1: Multiple Perspectives

Two different AI agents analyze each tweet: (although in theory you may want even more agent coming from different angles, but I found performance is terrible and not systematic if you only use one, and more than two has benefits in performance so two seemed reasonable for this first version of the project)

1. **Free Speech Agent**

   - Prioritizes freedom of expression
   - Only flags content as hate speech if there's clear evidence of harm
   - Considers context heavily before making decisions
   - Treats offensive language differently from actual hate speech

2. **Community Standards Agent**
   - Evaluates content against typical social media platform guidelines
   - Looks for specific violations like harassment or discrimination
   - Requires explicit evidence of harmful intent
   - Considers potential real-world impact

### Phase 2: Synthesis

A third agent looks at both perspectives and makes the final call. It's designed to:

- Favor the Free Speech agent's analysis
- Require explicit evidence of harm, not just offensive content (without this instruction, it was very trigger-happy and lableing things as hatespeech when even the human labelers hadn't considered it hatespeech)
- contextualize
- Provide a one-line reasoning judgement

Each agent classifies content into three categories (I used the HateXplain schema):

- **Hatespeech**: Content that explicitly promotes violence or expresses clear hatred toward protected groups
- **Offensive**: Content that's rude or inappropriate but lacks clear hateful intent
- **Normal**: Everything else

## Implementation Details

The system is built in Scala (which I spent a lot of time learning about this quarter because I need to code in it for my upcoming job) and uses the Grok API for the language models. Some key technical details:

- Processing time: ~6-8 seconds per tweet
- retry logic for API calls
- JSON handling for large datasets
- Configurable batch processing
- Multi-agentic framework - easy to add or remove agents to fit usecase

## Setup & Usage

1. Requirements:

   - Scala 2.13.12
   - SBT
   - x.ai API key

2. Environment setup:

```bash
export GROK_API_KEY=your_key_here
```

3. Build:

```bash
sbt compile
```

4. Run tests:

```bash
sbt "runMain TestHateXplainApp"
```

You can modify `NumTweetsToTest` in TestHateXplainApp.scala to process more tweets at once.

## Project Structure (used AI to make it pretty)

```
src/main/scala/moderation/
├── agents/                 # AI agent implementations
│   ├── FreeSpeechAgent.scala
│   ├── CommunityStandardsAgent.scala
│   └── SynthesisAgent.scala
├── models/                 # Data models
├── parser/                 # Dataset parsing logic
└── ModerationOrchestrator.scala
```

## Results

I ran the system on about 100 disputed tweets. I found some pretty interesting stuff. It typically sided with the majority human opinion which was validating, but sometimes it'd pick a different label entirely - like marking something as "offensive" when humans were split between "normal" and "hate speech". This makes sense since we basically told it to err on the side of free speech unless something was clearly hateful. The two agents would often disagree because they had very different directives. The modes were very "trigger happy" at first and would always label anything with an offesnive word as hatespeech, but it's just not the case that simply using a bad word is necessarily hatespeech. So it took some finicking with the prompt to get it to start making what I considered "normative, reasonable" judgements. If we had more time it'd be cool to run this on way more tweets and maybe make some charts showing how often it agrees with human majority vs. doing its own thing.
