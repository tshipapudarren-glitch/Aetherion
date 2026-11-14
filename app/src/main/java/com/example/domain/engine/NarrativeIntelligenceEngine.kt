package com.example.domain.engine

import com.example.data.model.MarketQuote
import com.example.data.model.NewsHeadline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class NarrativeLifecycleStage(val label: String) {
    EMERGING("EMERGING"),
    ACCELERATING("ACCELERATING"),
    PEAK_CONSENSUS("PEAK CONSENSUS"),
    CONTESTED("CONTESTED / SPLIT"),
    DECAYING("DECAYING")
}

data class NarrativeArticleItem(
    val id: String,
    val title: String,
    val source: String,
    val sourceCategory: String,
    val region: String,
    val publishedAt: String,
    val publishedEpochMs: Long,
    val link: String,
    val summary: String,
    val sentimentScore: Double,
    val stanceLabel: String,
    val matchedEntities: List<String>
)

data class NewsSourceComparison(
    val sourceName: String,
    val category: String,
    val articleCount: Int,
    val shareOfVoicePct: Int,
    val avgSentiment: Double,
    val deltaVsConsensus: Double,
    val framingLabel: String,
    val leadHeadline: String
)

data class ConflictingReportPair(
    val id: String,
    val conflictAxis: String,
    val sourceA: String,
    val headlineA: String,
    val sentimentA: Double,
    val sourceB: String,
    val headlineB: String,
    val sentimentB: Double,
    val divergenceScorePct: Int,
    val implication: String
)

data class NarrativeTimelineEvent(
    val id: String,
    val timestampMs: Long,
    val timeLabel: String,
    val source: String,
    val headline: String,
    val markerType: String,
    val sentimentScore: Double,
    val deltaLabel: String
)

data class InstrumentRelevanceItem(
    val symbol: String,
    val displayName: String,
    val assetClass: String,
    val relevanceScore: Int,
    val betaDirection: String,
    val livePrice: Double,
    val liveChangePct: Double,
    val realizedVolPct: Double,
    val isLive: Boolean
)

data class NarrativeDecayMetrics(
    val hoursSincePeak: Double,
    val halfLifeHours: Double,
    val decayProgressPct: Int,
    val statusLabel: String,
    val projectedDecay48h: List<Double>
)

data class MarketReactionComparison(
    val symbol: String,
    val displayName: String,
    val priceChangePct: Double,
    val realizedVolPct: Double,
    val alignmentScorePct: Int,
    val regimeVerdict: String,
    val normalizedPriceSeries: List<Double>,
    val normalizedAttentionSeries: List<Double>,
    val isLive: Boolean
)

data class NarrativeCluster(
    val id: String,
    val topicTitle: String,
    val categoryTag: String,
    val summaryThesis: String,
    val keywords: List<String>,
    val articleCount: Int,
    val distinctSourcesCount: Int,
    val attentionScore: Int,
    val importanceScore: Int,
    val escalationRiskPct: Int,
    val growthRatePct: Int,
    val lifecycleStage: NarrativeLifecycleStage,
    val netSentimentScore: Double,
    val sentimentLabel: String,
    val growthSeries: List<Double>,
    val sourceComparisons: List<NewsSourceComparison>,
    val conflictingReports: List<ConflictingReportPair>,
    val conflictIntensityPct: Int,
    val timelineEvents: List<NarrativeTimelineEvent>,
    val instrumentImpacts: List<InstrumentRelevanceItem>,
    val decayTelemetry: NarrativeDecayMetrics,
    val marketReactions: List<MarketReactionComparison>,
    val articles: List<NarrativeArticleItem>
)

data class NarrativeIntelligenceSnapshot(
    val clusters: List<NarrativeCluster> = emptyList(),
    val globalAttentionIndex: Int = 0,
    val totalArticlesAnalyzed: Int = 0,
    val totalSourcesCount: Int = 0,
    val totalConflictsDetected: Int = 0,
    val fastestGrowingNarrative: String = "Awaiting live feeds",
    val fastestDecayingNarrative: String = "-",
    val updatedAtMs: Long = 0L
)

object NarrativeIntelligenceEngine {

    private data class TopicTaxonomySpec(
        val id: String,
        val title: String,
        val categoryTag: String,
        val summaryThesis: String,
        val triggerTerms: List<String>,
        val baseHalfLifeHours: Double,
        val baseInstrumentWeights: Map<String, Pair<Int, String>>
    )

    private val taxonomySpecs = listOf(
        TopicTaxonomySpec(
            id = "geopolitics_energy",
            title = "Geopolitics, Maritime & Energy Shock",
            categoryTag = "GEOPOLITICS & COMMODITIES",
            summaryThesis = "Tracks armed escalation, diplomatic negotiations, strait/shipping threats, and energy supply disruption spillovers.",
            triggerTerms = listOf(
                "iran", "israel", "hormuz", "gaza", "ukraine", "russia", "missile", "airstrike", "ceasefire",
                "war", "military", "troops", "nuclear", "sanction", "oil", "crude", "brent", "wti", "opec",
                "middle east", "red sea", "strait", "shipping", "pipeline", "attack", "drone", "diplomacy"
            ),
            baseHalfLifeHours = 18.0,
            baseInstrumentWeights = mapOf(
                "WTI" to (96 to "+Beta Supply Shock"),
                "BRENT" to (95 to "+Beta Supply Shock"),
                "XAUUSD" to (91 to "+Beta Safe Haven"),
                "SPX500" to (78 to "-Beta Risk-Off"),
                "USDJPY" to (72 to "-Beta Yen Safety")
            )
        ),
        TopicTaxonomySpec(
            id = "central_banks_rates",
            title = "Central Banks, Inflation & Yields",
            categoryTag = "MONETARY & BONDS",
            summaryThesis = "Tracks Federal Reserve / ECB / BoJ rate path debates, inflation prints, sovereign yields, and liquidity tightness.",
            triggerTerms = listOf(
                "fed", "fomc", "powell", "rate", "rates", "yield", "treasury", "inflation", "cpi", "pce",
                "payroll", "jobs", "unemployment", "ecb", "boe", "boj", "cut", "hike", "hawkish", "dovish",
                "bond", "bonds", "recession", "gdp", "central bank", "monetary", "tariff"
            ),
            baseHalfLifeHours = 24.0,
            baseInstrumentWeights = mapOf(
                "US10Y" to (97 to "+Beta Rate Sensitivity"),
                "EURUSD" to (89 to "-Beta USD Strength"),
                "USDJPY" to (88 to "+Beta Yield Spread"),
                "XAUUSD" to (86 to "-Beta Real Yields"),
                "SPX500" to (82 to "-Beta Discount Rate")
            )
        ),
        TopicTaxonomySpec(
            id = "ai_tech_equities",
            title = "AI Capex, Semiconductors & Equities",
            categoryTag = "EQUITIES & AI TECH",
            summaryThesis = "Tracks hyperscaler AI infrastructure spend, chip supply chains, earnings beats/misses, and Nasdaq beta leadership.",
            triggerTerms = listOf(
                "ai", "nvidia", "nvda", "apple", "aapl", "microsoft", "google", "meta", "amazon", "tesla",
                "chip", "semiconductor", "gpu", "tech", "nasdaq", "s&p", "sp500", "earnings", "revenue",
                "guidance", "stock", "stocks", "equity", "equities", "wall street", "ipo", "buyback"
            ),
            baseHalfLifeHours = 16.0,
            baseInstrumentWeights = mapOf(
                "NVDA" to (96 to "+Beta Core Leader"),
                "NAS100" to (94 to "+Beta Index Proxy"),
                "SPX500" to (86 to "+Beta Broad Equity"),
                "AAPL" to (79 to "+Beta MegaCap Tech"),
                "US30" to (68 to "+Beta Cyclical Equity")
            )
        ),
        TopicTaxonomySpec(
            id = "crypto_digital_flow",
            title = "Crypto Institutional Flow & Policy",
            categoryTag = "DIGITAL ASSETS",
            summaryThesis = "Tracks Bitcoin/Ethereum ETF inflows, regulatory rulings, leverage liquidations, and risk-on liquidity rotation.",
            triggerTerms = listOf(
                "bitcoin", "btc", "crypto", "ethereum", "eth", "solana", "sol", "stablecoin", "etf",
                "sec", "blockchain", "defi", "token", "coinbase", "binance", "halving", "mining"
            ),
            baseHalfLifeHours = 12.0,
            baseInstrumentWeights = mapOf(
                "BTCUSD" to (98 to "+Beta Bellwether"),
                "ETHUSD" to (92 to "+Beta Smart Contract"),
                "SOLUSD" to (88 to "+Beta High Volatility"),
                "NAS100" to (70 to "+Beta Liquidity Correlated")
            )
        ),
        TopicTaxonomySpec(
            id = "global_trade_fx",
            title = "Global Trade, Tariffs & FX Rotation",
            categoryTag = "TRADE & G10 FX",
            summaryThesis = "Tracks cross-border trade barriers, Asian/European industrial output, supply chains, and currency realignment.",
            triggerTerms = listOf(
                "trade", "tariff", "tariffs", "export", "import", "china", "europe", "eu", "uk", "japan",
                "dollar", "euro", "yen", "pound", "yuan", "currency", "fx", "forex", "manufacturing",
                "stimulus", "supply chain", "port", "shipping", "airbus", "auto", "ev"
            ),
            baseHalfLifeHours = 22.0,
            baseInstrumentWeights = mapOf(
                "EURUSD" to (90 to "+Beta EUR Trade Proxy"),
                "USDJPY" to (86 to "+Beta Asia Trade"),
                "GER40" to (84 to "+Beta Export Index"),
                "GBPUSD" to (80 to "+Beta UK Exposure"),
                "AUDUSD" to (78 to "+Beta China Commodity FX")
            )
        ),
        TopicTaxonomySpec(
            id = "safe_haven_credit",
            title = "Safe-Haven Metals, Credit & Systemic Risk",
            categoryTag = "CREDIT & VOLATILITY",
            summaryThesis = "Tracks gold reserves accumulation, banking liquidity stress, sovereign fiscal deficits, and systemic volatility spikes.",
            triggerTerms = listOf(
                "gold", "silver", "precious", "safe haven", "bank", "banking", "credit", "default", "debt",
                "deficit", "crisis", "emergency", "volatility", "vix", "selloff", "crash", "liquidity",
                "shutdown", "hedge", "reserve"
            ),
            baseHalfLifeHours = 20.0,
            baseInstrumentWeights = mapOf(
                "XAUUSD" to (97 to "+Beta Monetary Shield"),
                "XAGUSD" to (88 to "+Beta Industrial+Haven"),
                "US10Y" to (85 to "-Beta Flight to Quality"),
                "SPX500" to (80 to "-Beta Stress Proxy"),
                "USDZAR" to (71 to "+Beta EM Risk Proxy")
            )
        )
    )

    private val bullishRiskOnLexicon = setOf(
        "surge", "surges", "rally", "rallies", "jump", "jumps", "gain", "gains", "beat", "beats",
        "growth", "expand", "expansion", "ceasefire", "talks", "deal", "agreement", "truce", "easing",
        "reopen", "invest", "investment", "record", "optimism", "bullish", "recover", "recovery",
        "cut", "cuts", "approval", "breakthrough", "soar", "soars", "up", "higher", "strong", "boost"
    )

    private val bearishRiskOffLexicon = setOf(
        "plunge", "plunges", "drop", "drops", "fall", "falls", "slump", "slumps", "crisis", "warning",
        "warns", "threat", "threats", "escalation", "escalate", "strike", "strikes", "missile", "attack",
        "closure", "close", "halt", "disruption", "crunch", "rejects", "mock", "mocks", "fear", "fears",
        "risk", "crash", "collapse", "loss", "losses", "negative", "bearish", "inflation", "hawkish",
        "spike", "conflict", "war", "shortage", "sanction", "down", "lower", "weak", "deficit"
    )

    private val contradictionAxes = listOf(
        Triple("Diplomatic De-escalation vs Military Escalation", listOf("ceasefire", "talks", "negotiation", "truce", "peace", "deal"), listOf("rejects", "mocks", "strikes", "missile", "attack", "war", "closure", "airborne")),
        Triple("Rate Cut / Easing vs Higher-For-Longer Tightening", listOf("cut", "cuts", "dovish", "easing", "cool", "slowing"), listOf("hike", "hikes", "hawkish", "hot", "sticky", "tightening", "surge")),
        Triple("Growth Beat / Expansion vs Contraction / Crunch", listOf("beat", "rally", "record", "growth", "boost", "invest"), listOf("miss", "drop", "profit drops", "crunch", "warning", "slump", "setback")),
        Triple("Supply Relief vs Chokepoint Disruption", listOf("reopen", "resume", "easing", "reserve"), listOf("closure", "shortage", "emergency", "disruption", "halt"))
    )

    fun analyzeNarratives(
        headlines: List<NewsHeadline>,
        quotes: List<MarketQuote>,
        nowMs: Long = System.currentTimeMillis()
    ): NarrativeIntelligenceSnapshot {
        if (headlines.isEmpty()) {
            return NarrativeIntelligenceSnapshot(updatedAtMs = nowMs)
        }

        // Convert headlines into enriched articles with sentiment & matched entities
        val articles = headlines.mapIndexed { idx, h ->
            val effectiveTs = if (h.publishedEpochMs > 0L) h.publishedEpochMs else (nowMs - idx * 900_000L)
            val fullText = "${h.title} ${h.summary}".lowercase(Locale.US)
            val sentiment = computeSentimentScore(fullText)
            val entities = extractMatchedEntities(fullText)
            NarrativeArticleItem(
                id = "art_${idx}_${h.title.hashCode()}",
                title = h.title,
                source = h.source.ifBlank { "Global Wire" },
                sourceCategory = h.sourceCategory.ifBlank { "Wire" },
                region = h.region.ifBlank { "Global" },
                publishedAt = h.publishedAt,
                publishedEpochMs = effectiveTs,
                link = h.link,
                summary = h.summary,
                sentimentScore = sentiment,
                stanceLabel = classifyStance(sentiment),
                matchedEntities = entities
            )
        }

        // Topic clustering by highest taxonomy term match score
        val grouped = mutableMapOf<TopicTaxonomySpec, MutableList<NarrativeArticleItem>>()
        val fallbackSpec = TopicTaxonomySpec(
            id = "global_macro_pulse",
            title = "Cross-Market Corporate & Macro Pulse",
            categoryTag = "GENERAL MACRO",
            summaryThesis = "Captures emerging cross-sector catalysts, M&A flows, and idiosyncratic market movers before dedicated cluster formation.",
            triggerTerms = emptyList(),
            baseHalfLifeHours = 14.0,
            baseInstrumentWeights = mapOf(
                "SPX500" to (80 to "+Beta Benchmark"),
                "NAS100" to (74 to "+Beta Growth Benchmark"),
                "XAUUSD" to (68 to "+Beta Hedge"),
                "EURUSD" to (66 to "+Beta G10 FX")
            )
        )

        articles.forEach { article ->
            val combined = "${article.title} ${article.summary}".lowercase(Locale.US)
            var bestSpec: TopicTaxonomySpec? = null
            var bestScore = 0
            for (spec in taxonomySpecs) {
                val hits = spec.triggerTerms.count { term -> containsWholeOrSub(combined, term) }
                if (hits > bestScore) {
                    bestScore = hits
                    bestSpec = spec
                }
            }
            val target = bestSpec ?: fallbackSpec
            grouped.getOrPut(target) { mutableListOf() }.add(article)
        }

        val totalArticleCount = articles.size.coerceAtLeast(1)
        val clusters = grouped.entries
            .filter { it.value.isNotEmpty() }
            .map { (spec, rawList) ->
                buildCluster(spec, rawList.sortedByDescending { it.publishedEpochMs }, quotes, totalArticleCount, nowMs)
            }
            .sortedByDescending { it.attentionScore }

        val totalConflicts = clusters.sumOf { it.conflictingReports.size }
        val allSources = articles.map { it.source }.distinct().size
        val globalAttention = if (clusters.isNotEmpty()) {
            (clusters.take(3).map { it.attentionScore }.average()).roundToInt().coerceIn(5, 100)
        } else 0
        val fastestGrowing = clusters.maxByOrNull { it.growthRatePct }?.topicTitle ?: "None"
        val fastestDecaying = clusters.maxByOrNull { it.decayTelemetry.decayProgressPct }?.topicTitle ?: "None"

        return NarrativeIntelligenceSnapshot(
            clusters = clusters,
            globalAttentionIndex = globalAttention,
            totalArticlesAnalyzed = articles.size,
            totalSourcesCount = allSources,
            totalConflictsDetected = totalConflicts,
            fastestGrowingNarrative = fastestGrowing,
            fastestDecayingNarrative = fastestDecaying,
            updatedAtMs = nowMs
        )
    }

    private fun buildCluster(
        spec: TopicTaxonomySpec,
        clusterArticles: List<NarrativeArticleItem>,
        quotes: List<MarketQuote>,
        totalArticles: Int,
        nowMs: Long
    ): NarrativeCluster {
        val sources = clusterArticles.groupBy { it.source }
        val distinctSources = sources.size
        val avgSentiment = clusterArticles.map { it.sentimentScore }.average()
        val absIntensity = clusterArticles.map { abs(it.sentimentScore) }.average()

        // Bucket article timestamps into 8 intervals (oldest -> newest)
        val oldestTs = clusterArticles.minOf { it.publishedEpochMs }
        val newestTs = clusterArticles.maxOf { it.publishedEpochMs }
        val spanMs = max(newestTs - oldestTs, 4 * 3600_000L)
        val bucketCounts = DoubleArray(8)
        clusterArticles.forEach { art ->
            val ratio = ((art.publishedEpochMs - oldestTs).toDouble() / spanMs.toDouble()).coerceIn(0.0, 0.9999)
            val b = (ratio * 8).toInt().coerceIn(0, 7)
            bucketCounts[b] += 1.0
        }
        // Smooth growth series & normalize 0..100 for charts
        val maxBucket = bucketCounts.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val growthSeries = bucketCounts.map { ((it / maxBucket) * 100.0).coerceIn(8.0, 100.0) }

        val earlierMentions = bucketCounts.slice(0..3).sum()
        val recentMentions = bucketCounts.slice(4..7).sum()
        val growthRatePct = when {
            earlierMentions == 0.0 && recentMentions > 0.0 -> (recentMentions * 45).roundToInt().coerceIn(25, 220)
            earlierMentions > 0.0 -> (((recentMentions - earlierMentions) / earlierMentions) * 100.0).roundToInt().coerceIn(-85, 240)
            else -> 0
        }

        // Source Comparison
        val sourceComparisons = sources.entries.map { (srcName, list) ->
            val srcSent = list.map { it.sentimentScore }.average()
            val delta = srcSent - avgSentiment
            val framing = when {
                delta >= 0.18 -> "More Optimistic (+)"
                delta <= -0.18 -> "More Alarmist (-)"
                abs(srcSent) < 0.12 -> "Measured / Neutral"
                else -> "Aligned w/ Consensus"
            }
            NewsSourceComparison(
                sourceName = srcName,
                category = list.first().sourceCategory,
                articleCount = list.size,
                shareOfVoicePct = ((list.size.toDouble() / clusterArticles.size) * 100.0).roundToInt(),
                avgSentiment = srcSent,
                deltaVsConsensus = delta,
                framingLabel = framing,
                leadHeadline = list.first().title
            )
        }.sortedByDescending { it.articleCount }

        // Conflicting Reports Detection
        val conflicts = detectConflictingReports(clusterArticles)
        val conflictIntensity = if (conflicts.isEmpty()) {
            (computeSentimentDispersion(clusterArticles) * 85.0).roundToInt().coerceIn(0, 48)
        } else {
            (45 + conflicts.size * 18 + (conflicts.maxOf { it.divergenceScorePct } * 0.25)).roundToInt().coerceIn(45, 99)
        }

        // Attention, Importance & Escalation Risk
        val voiceShare = clusterArticles.size.toDouble() / totalArticles.toDouble()
        val recencyBonus = if ((nowMs - newestTs) < 3 * 3600_000L) 14 else 4
        val attentionScore = (22.0 + voiceShare * 95.0 + distinctSources * 7.5 + absIntensity * 16.0 + recencyBonus)
            .roundToInt().coerceIn(12, 99)
        val importanceScore = (30.0 + distinctSources * 9.0 + absIntensity * 25.0 + (conflictIntensity * 0.2))
            .roundToInt().coerceIn(18, 99)
        val bearishWeight = clusterArticles.count { it.sentimentScore < -0.1 }.toDouble() / clusterArticles.size
        val escalationRisk = if (spec.id == "geopolitics_energy" || spec.id == "safe_haven_credit") {
            (25.0 + bearishWeight * 55.0 + conflictIntensity * 0.25).roundToInt().coerceIn(15, 98)
        } else {
            (18.0 + absIntensity * 40.0 + conflictIntensity * 0.22).roundToInt().coerceIn(10, 92)
        }

        // Lifecycle Stage & Decay Metrics
        val peakBucketIdx = bucketCounts.indices.maxByOrNull { bucketCounts[it] } ?: 7
        val hoursSincePeak = (((7 - peakBucketIdx).toDouble() / 7.0) * (spanMs / 3_600_000.0)) +
            max(0.0, (nowMs - newestTs) / 3_600_000.0)
        val halfLifeHours = (spec.baseHalfLifeHours * (1.0 + conflictIntensity / 220.0)).coerceIn(6.0, 48.0)
        val decayFraction = 1.0 - exp(-ln(2.0) * (hoursSincePeak / halfLifeHours))
        val decayProgressPct = (decayFraction * 100.0).roundToInt().coerceIn(4, 96)
        val decayStatus = when {
            decayProgressPct < 20 -> "FRESH CATALYST (Peak Discovery)"
            decayProgressPct < 45 -> "ACTIVE PROPAGATION"
            decayProgressPct < 70 -> "MATURE / PARTIALLY PRICED IN"
            else -> "DECAYING / ATTENTION FADING"
        }
        val currentLevel = (100.0 * (1.0 - decayFraction)).coerceIn(8.0, 100.0)
        val projected48h = (0..8).map { step ->
            val h = step * 6.0
            (currentLevel * exp(-ln(2.0) * (h / halfLifeHours))).coerceIn(4.0, 100.0)
        }
        val decayTelemetry = NarrativeDecayMetrics(
            hoursSincePeak = hoursSincePeak,
            halfLifeHours = halfLifeHours,
            decayProgressPct = decayProgressPct,
            statusLabel = decayStatus,
            projectedDecay48h = projected48h
        )

        val lifecycleStage = when {
            conflictIntensity >= 65 -> NarrativeLifecycleStage.CONTESTED
            growthRatePct >= 40 && decayProgressPct < 40 -> NarrativeLifecycleStage.ACCELERATING
            clusterArticles.size <= 3 && decayProgressPct < 35 -> NarrativeLifecycleStage.EMERGING
            decayProgressPct >= 58 || growthRatePct <= -30 -> NarrativeLifecycleStage.DECAYING
            else -> NarrativeLifecycleStage.PEAK_CONSENSUS
        }

        // Chronological Event Timeline
        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.US)
        val timelineAsc = clusterArticles.sortedBy { it.publishedEpochMs }
        var prevSent = 0.0
        val timelineEvents = timelineAsc.mapIndexed { idx, art ->
            val delta = art.sentimentScore - prevSent
            prevSent = art.sentimentScore
            val marker = when {
                idx == 0 -> "CATALYST"
                abs(delta) >= 0.45 -> "DIVERGENCE"
                art.sentimentScore <= -0.35 -> "ESCALATION"
                idx == timelineAsc.lastIndex && decayProgressPct >= 55 -> "COOLING"
                else -> "UPDATE"
            }
            val deltaStr = String.format(Locale.US, "%+.2f", delta)
            NarrativeTimelineEvent(
                id = "tl_${art.id}",
                timestampMs = art.publishedEpochMs,
                timeLabel = fmt.format(Date(art.publishedEpochMs)),
                source = art.source,
                headline = art.title,
                markerType = marker,
                sentimentScore = art.sentimentScore,
                deltaLabel = "Sent ${String.format(Locale.US, "%+.2f", art.sentimentScore)} (Δ $deltaStr)"
            )
        }.reversed() // Newest milestone first for clean mobile scanning

        // Instrument Relevance Mapping using live universe quotes
        val quoteBySymbol = quotes.associateBy { it.instrument.symbol }
        val mentionedScores = mutableMapOf<String, Int>()
        clusterArticles.forEach { art ->
            art.matchedEntities.forEach { sym ->
                mentionedScores[sym] = (mentionedScores[sym] ?: 0) + 14
            }
        }
        val candidateSymbols = (spec.baseInstrumentWeights.keys + mentionedScores.keys).distinct()
        val instrumentImpacts = candidateSymbols.mapNotNull { sym ->
            val q = quoteBySymbol[sym] ?: return@mapNotNull null
            val baseSpec = spec.baseInstrumentWeights[sym]
            val baseRel = baseSpec?.first ?: 52
            val entityBoost = mentionedScores[sym] ?: 0
            val volBoost = (q.realizedVolPct * 0.15).roundToInt()
            val finalRel = (baseRel * 0.72 + entityBoost + volBoost).roundToInt().coerceIn(35, 99)
            InstrumentRelevanceItem(
                symbol = sym,
                displayName = q.instrument.displayName,
                assetClass = q.instrument.assetClass.displayName,
                relevanceScore = finalRel,
                betaDirection = baseSpec?.second ?: "Cross-Asset Spillover",
                livePrice = q.price,
                liveChangePct = q.changePct,
                realizedVolPct = q.realizedVolPct,
                isLive = q.isLive
            )
        }.sortedByDescending { it.relevanceScore }.take(6)

        // Market Reaction Comparisons (comparing narrative attention curve vs actual hourly candle paths)
        val marketReactions = instrumentImpacts.take(4).map { inst ->
            val q = quoteBySymbol[inst.symbol]
            buildMarketReactionComparison(inst, q, growthSeries, avgSentiment)
        }

        val sentimentLabel = when {
            conflictIntensity >= 65 -> "CONTESTED (${conflicts.size} Split Reports)"
            avgSentiment <= -0.22 -> "BEARISH / RISK-OFF (${String.format(Locale.US, "%+.2f", avgSentiment)})"
            avgSentiment >= 0.22 -> "BULLISH / RISK-ON (${String.format(Locale.US, "%+.2f", avgSentiment)})"
            else -> "NEUTRAL / BALANCED (${String.format(Locale.US, "%+.2f", avgSentiment)})"
        }

        val extractedKeywords = clusterArticles
            .flatMap { it.matchedEntities + extractTopKeywords(it.title, spec.triggerTerms) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)

        return NarrativeCluster(
            id = spec.id,
            topicTitle = spec.title,
            categoryTag = spec.categoryTag,
            summaryThesis = spec.summaryThesis,
            keywords = extractedKeywords,
            articleCount = clusterArticles.size,
            distinctSourcesCount = distinctSources,
            attentionScore = attentionScore,
            importanceScore = importanceScore,
            escalationRiskPct = escalationRisk,
            growthRatePct = growthRatePct,
            lifecycleStage = lifecycleStage,
            netSentimentScore = avgSentiment,
            sentimentLabel = sentimentLabel,
            growthSeries = growthSeries,
            sourceComparisons = sourceComparisons,
            conflictingReports = conflicts,
            conflictIntensityPct = conflictIntensity,
            timelineEvents = timelineEvents,
            instrumentImpacts = instrumentImpacts,
            decayTelemetry = decayTelemetry,
            marketReactions = marketReactions,
            articles = clusterArticles
        )
    }

    private fun detectConflictingReports(articles: List<NarrativeArticleItem>): List<ConflictingReportPair> {
        if (articles.size < 2) return emptyList()
        val pairs = mutableListOf<ConflictingReportPair>()
        val usedIndices = mutableSetOf<Int>()

        for (i in articles.indices) {
            if (i in usedIndices) continue
            val a = articles[i]
            val lowerA = "${a.title} ${a.summary}".lowercase(Locale.US)
            var bestJ = -1
            var bestScore = 0
            var bestAxis = "Sentiment Polarity Divergence"

            for (j in (i + 1) until articles.size) {
                if (j in usedIndices) continue
                val b = articles[j]
                val lowerB = "${b.title} ${b.summary}".lowercase(Locale.US)
                val sentimentDiff = abs(a.sentimentScore - b.sentimentScore)
                val oppositeSigns = (a.sentimentScore > 0.08 && b.sentimentScore < -0.08) ||
                    (a.sentimentScore < -0.08 && b.sentimentScore > 0.08)

                var axisMatchScore = 0
                var matchedAxisName = "Editorial Framing Divergence"
                for ((axisTitle, side1Terms, side2Terms) in contradictionAxes) {
                    val aHas1 = side1Terms.any { containsWholeOrSub(lowerA, it) }
                    val bHas2 = side2Terms.any { containsWholeOrSub(lowerB, it) }
                    val aHas2 = side2Terms.any { containsWholeOrSub(lowerA, it) }
                    val bHas1 = side1Terms.any { containsWholeOrSub(lowerB, it) }
                    if ((aHas1 && bHas2) || (aHas2 && bHas1)) {
                        axisMatchScore = 45
                        matchedAxisName = axisTitle
                        break
                    }
                }

                val sameSourcePenalty = if (a.source == b.source) -18 else 12
                val totalScore = (sentimentDiff * 55.0).roundToInt() +
                    (if (oppositeSigns) 28 else 0) +
                    axisMatchScore +
                    sameSourcePenalty

                if ((oppositeSigns || axisMatchScore > 0 || sentimentDiff >= 0.45) && totalScore > bestScore && totalScore >= 46) {
                    bestScore = totalScore.coerceIn(48, 98)
                    bestJ = j
                    bestAxis = matchedAxisName
                }
            }

            if (bestJ != -1) {
                val b = articles[bestJ]
                usedIndices.add(i)
                usedIndices.add(bestJ)
                val implication = when {
                    bestAxis.contains("Military") -> "Headline whip-saw elevates tail risk across WTI, Gold & SPX intraday liquidity."
                    bestAxis.contains("Rate Cut") -> "Monetary policy disagreement widens Treasury yield volatility & G10 FX spreads."
                    bestAxis.contains("Growth") -> "Divergent corporate outlook drives sector rotation & factor dispersion."
                    else -> "Conflicting wire claims signal unpriced two-way repricing risk."
                }
                pairs.add(
                    ConflictingReportPair(
                        id = "conf_${a.id}_${b.id}",
                        conflictAxis = bestAxis,
                        sourceA = a.source,
                        headlineA = a.title,
                        sentimentA = a.sentimentScore,
                        sourceB = b.source,
                        headlineB = b.title,
                        sentimentB = b.sentimentScore,
                        divergenceScorePct = bestScore,
                        implication = implication
                    )
                )
            }
            if (pairs.size >= 4) break
        }
        return pairs
    }

    private fun buildMarketReactionComparison(
        instrument: InstrumentRelevanceItem,
        quote: MarketQuote?,
        attentionSeries: List<Double>,
        avgSentiment: Double
    ): MarketReactionComparison {
        val candles = quote?.candles.orEmpty()
        if (candles.size < 8 || quote == null || !quote.isLive) {
            return MarketReactionComparison(
                symbol = instrument.symbol,
                displayName = instrument.displayName,
                priceChangePct = instrument.liveChangePct,
                realizedVolPct = instrument.realizedVolPct,
                alignmentScorePct = 0,
                regimeVerdict = if (quote?.isLive == true) "Spot Only (No Candle Window)" else "Feed Disconnected",
                normalizedPriceSeries = emptyList(),
                normalizedAttentionSeries = attentionSeries,
                isLive = quote?.isLive == true
            )
        }

        // Sample 8 points from last 32 hourly candles (approx. 1.5-2 day reaction window matching news)
        val tail = candles.takeLast(min(32, candles.size))
        val baseClose = tail.first().close.takeIf { it > 0.0 } ?: 1.0
        val sampledReturns = (0..7).map { step ->
            val idx = ((step.toDouble() / 7.0) * (tail.size - 1)).roundToInt().coerceIn(0, tail.lastIndex)
            ((tail[idx].close - baseClose) / baseClose) * 100.0
        }

        val minR = sampledReturns.minOrNull() ?: 0.0
        val maxR = sampledReturns.maxOrNull() ?: 0.0
        val spanR = (maxR - minR).takeIf { it > 1e-5 } ?: 1.0
        val normPrice = sampledReturns.map { (((it - minR) / spanR) * 100.0).coerceIn(4.0, 100.0) }

        val corr = pearsonCorrelation(attentionSeries, normPrice)
        val alignmentPct = (abs(corr) * 100.0).roundToInt().coerceIn(5, 98)

        val expectedRiskOnMove = avgSentiment >= 0.0
        val actualPositive = quote.changePct >= 0.0
        val verdict = when {
            alignmentPct >= 62 && corr > 0.0 -> "DIRECT ATTENTION AMPLIFICATION (+r)"
            alignmentPct >= 62 && corr < 0.0 -> "INVERSE HEDGE / SAFE-HAVEN FLOW (-r)"
            expectedRiskOnMove != actualPositive -> "NARRATIVE-PRICE DIVERGENCE"
            else -> "LOW BETA / IDIOSYNCRATIC CHOP"
        }

        return MarketReactionComparison(
            symbol = instrument.symbol,
            displayName = instrument.displayName,
            priceChangePct = quote.changePct,
            realizedVolPct = quote.realizedVolPct,
            alignmentScorePct = alignmentPct,
            regimeVerdict = verdict,
            normalizedPriceSeries = normPrice,
            normalizedAttentionSeries = attentionSeries,
            isLive = true
        )
    }

    private fun pearsonCorrelation(xs: List<Double>, ys: List<Double>): Double {
        if (xs.size != ys.size || xs.size < 3) return 0.0
        val meanX = xs.average()
        val meanY = ys.average()
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        val denom = sqrt(denX * denY)
        return if (denom > 1e-6) (num / denom).coerceIn(-1.0, 1.0) else 0.0
    }

    private fun computeSentimentScore(text: String): Double {
        val tokens = text.split(Regex("[^a-z0-9']+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return 0.0
        var pos = 0.0
        var neg = 0.0
        tokens.forEach { t ->
            if (t in bullishRiskOnLexicon) pos += 1.0
            if (t in bearishRiskOffLexicon) neg += 1.0
        }
        val net = pos - neg
        val denom = (pos + neg + 2.0)
        return (net / denom).coerceIn(-0.95, 0.95)
    }

    private fun classifyStance(sentiment: Double): String = when {
        sentiment <= -0.22 -> "BEARISH"
        sentiment >= 0.22 -> "BULLISH"
        else -> "NEUTRAL"
    }

    private fun computeSentimentDispersion(list: List<NarrativeArticleItem>): Double {
        if (list.size < 2) return 0.0
        val mean = list.map { it.sentimentScore }.average()
        val variance = list.sumOf { (it.sentimentScore - mean) * (it.sentimentScore - mean) } / list.size
        return sqrt(variance).coerceIn(0.0, 1.0)
    }

    private fun extractMatchedEntities(lowerText: String): List<String> {
        val out = mutableListOf<String>()
        if (containsAny(lowerText, "gold", "bullion", "xau")) out.add("XAUUSD")
        if (containsAny(lowerText, "silver", "xag")) out.add("XAGUSD")
        if (containsAny(lowerText, "crude", "wti", "oil", "hormuz", "opec")) out.add("WTI")
        if (containsAny(lowerText, "brent", "opec", "hormuz")) out.add("BRENT")
        if (containsAny(lowerText, "bitcoin", "btc")) out.add("BTCUSD")
        if (containsAny(lowerText, "ethereum", "eth")) out.add("ETHUSD")
        if (containsAny(lowerText, "solana", "sol")) out.add("SOLUSD")
        if (containsAny(lowerText, "nvidia", "nvda", "chip", "gpu", "ai")) out.add("NVDA")
        if (containsAny(lowerText, "apple", "aapl", "iphone")) out.add("AAPL")
        if (containsAny(lowerText, "nasdaq", "tech", "ai", "semiconductor")) out.add("NAS100")
        if (containsAny(lowerText, "s&p", "sp500", "spy", "wall street", "stocks", "equities")) out.add("SPX500")
        if (containsAny(lowerText, "yield", "treasury", "fed", "fomc", "rates", "bond", "inflation")) out.add("US10Y")
        if (containsAny(lowerText, "euro", "ecb", "eurusd")) out.add("EURUSD")
        if (containsAny(lowerText, "yen", "boj", "japan", "usdjpy")) out.add("USDJPY")
        if (containsAny(lowerText, "pound", "uk", "boe", "britain", "gbpusd")) out.add("GBPUSD")
        if (containsAny(lowerText, "dax", "germany", "europe")) out.add("GER40")
        if (containsAny(lowerText, "ftse", "london", "uk")) out.add("UK100")
        return out.distinct()
    }

    private fun extractTopKeywords(title: String, triggerTerms: List<String>): List<String> {
        val lower = title.lowercase(Locale.US)
        return triggerTerms.filter { containsWholeOrSub(lower, it) }
            .map { it.uppercase(Locale.US) }
            .take(3)
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { containsWholeOrSub(text, it) }

    private fun containsWholeOrSub(text: String, term: String): Boolean {
        return if (term.length <= 3) {
            Regex("\\b${Regex.escape(term)}\\b").containsMatchIn(text)
        } else {
            text.contains(term)
        }
    }
}
