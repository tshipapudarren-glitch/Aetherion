package com.example.data.remote

import android.util.Log
import com.example.core.security.ApiSecretsResolver
import com.example.data.model.AiProviderType
import com.example.data.model.ProviderSettings
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CouncilVoteResult(
    val symbol: String,
    val consensusStance: String,
    val confidencePct: Int,
    val oracleThesis: String,
    val criticObjection: String,
    val macroIntelBrief: String,
    val arbiterRuling: String,
    val recommendedStrategy: String,
    val providerUsed: String
)

data class EventImpactAnalysis(
    val headline: String,
    val severityScore: Int,
    val affectedClasses: List<String>,
    val directionalBias: String,
    val riskMitigationAction: String,
    val providerUsed: String
)

data class NarrativeAiBrief(
    val clusterId: String,
    val executiveSummary: String,
    val dominantDriver: String,
    val conflictResolutionThesis: String,
    val crossAssetTransmission: String,
    val attentionForecast: String,
    val providerUsed: String
)

data class FactoryAiProposal(
    val name: String,
    val rationale: String,
    val fastPeriod: Int,
    val slowPeriod: Int,
    val atrStopMult: Double,
    val atrTargetMult: Double,
    val providerUsed: String
)

class MultiProviderAiGateway {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(55, TimeUnit.SECONDS)
        .build()

    private val geminiFlash by lazy { Firebase.ai.generativeModel("gemini-3.8-flash") }
    private val geminiArgon by lazy { Firebase.ai.generativeModel("gemini-4-argon") }
    private val geminiLite by lazy { Firebase.ai.generativeModel("gemini-3.5-flash-lite") }

    private fun geminiModelId(provider: AiProviderType): String = when (provider) {
        AiProviderType.FIREBASE_GEMINI_ARGON -> "gemini-4-argon"
        AiProviderType.FIREBASE_GEMINI_LITE -> "gemini-3.5-flash-lite"
        else -> "gemini-3.8-flash"
    }

    private val councilSchema = Schema.obj(
        properties = mapOf(
            "consensusStance" to Schema.enumeration(listOf("BULLISH", "BEARISH", "NEUTRAL", "HALT")),
            "confidencePct" to Schema.integer(description = "0 to 100"),
            "oracleThesis" to Schema.string(description = "ORACLE forecast thesis"),
            "criticObjection" to Schema.string(description = "CRITIC counter-argument and risk flaw"),
            "macroIntelBrief" to Schema.string(description = "MACRO INTEL liquidity & central bank view"),
            "arbiterRuling" to Schema.string(description = "ARBITER resolution and next step"),
            "recommendedStrategy" to Schema.string(description = "Strategy archetype name")
        )
    )

    private val eventImpactSchema = Schema.obj(
        properties = mapOf(
            "severityScore" to Schema.integer(description = "1 to 100 tail shock score"),
            "affectedClasses" to Schema.array(Schema.string(), description = "Affected asset classes"),
            "directionalBias" to Schema.string(description = "Cross-asset market bias"),
            "riskMitigationAction" to Schema.string(description = "Deterministic risk recommendation")
        )
    )

    private val narrativeSchema = Schema.obj(
        properties = mapOf(
            "executiveSummary" to Schema.string(description = "2-sentence synthesis of how this narrative is evolving"),
            "dominantDriver" to Schema.string(description = "Core fundamental or geopolitical catalyst driving attention"),
            "conflictResolutionThesis" to Schema.string(description = "How conflicting news reports reconcile and what resolves them"),
            "crossAssetTransmission" to Schema.string(description = "Specific transmission path across linked instruments"),
            "attentionForecast" to Schema.string(description = "Whether narrative velocity is still accelerating or approaching decay/exhaustion")
        )
    )

    private val factorySchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Concise institutional strategy name"),
            "rationale" to Schema.string(description = "Quantitative edge hypothesis"),
            "fastPeriod" to Schema.integer(description = "Fast EMA lookback 5..30"),
            "slowPeriod" to Schema.integer(description = "Slow EMA lookback 20..90"),
            "atrStopMult" to Schema.double(description = "ATR stop multiplier 0.8..3.5"),
            "atrTargetMult" to Schema.double(description = "ATR target multiplier 1.2..6.0")
        )
    )

    suspend fun conveneSovereignCouncil(
        symbol: String,
        marketContext: String,
        settings: ProviderSettings
    ): Result<CouncilVoteResult> {
        val prompt = """
            You are AETHERION's AI Sovereign Council (ORACLE, CRITIC, MACRO INTEL, ARBITER).
            Evaluate live market telemetry for $symbol:
            $marketContext

            Rules:
            - ORACLE provides the forward market hypothesis.
            - CRITIC MUST challenge ORACLE with concrete invalidation/liquidity risks.
            - MACRO INTEL evaluates yields, dollar strength, or macro drivers.
            - ARBITER resolves disagreement. Remember: Risk Guard holds absolute deterministic veto.
        """.trimIndent()

        return try {
            val rawJson = executeStructuredCall(prompt, councilSchema, "consensusStance (BULLISH|BEARISH|NEUTRAL|HALT), confidencePct (int), oracleThesis, criticObjection, macroIntelBrief, arbiterRuling, recommendedStrategy", settings)
            val json = JSONObject(extractJsonObject(rawJson))
            Result.success(
                CouncilVoteResult(
                    symbol = symbol,
                    consensusStance = json.optString("consensusStance", "NEUTRAL"),
                    confidencePct = json.optInt("confidencePct", 50).coerceIn(0, 100),
                    oracleThesis = json.optString("oracleThesis", "-"),
                    criticObjection = json.optString("criticObjection", "-"),
                    macroIntelBrief = json.optString("macroIntelBrief", "-"),
                    arbiterRuling = json.optString("arbiterRuling", "-"),
                    recommendedStrategy = json.optString("recommendedStrategy", "Adaptive Momentum"),
                    providerUsed = resolveProviderLabel(settings)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Council generation timed out", e)
            Result.failure(IllegalStateException("AI provider timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Council generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeEventImpact(
        headline: String,
        marketSnapshot: String,
        settings: ProviderSettings
    ): Result<EventImpactAnalysis> {
        val prompt = """
            You are AETHERION MACRO INTEL & SENTINEL.
            Analyze this financial event headline against live market context:
            Headline: "$headline"
            Live Market Snapshot: $marketSnapshot
        """.trimIndent()

        return try {
            val raw = executeStructuredCall(
                prompt,
                eventImpactSchema,
                "severityScore (int 1-100), affectedClasses (array of strings), directionalBias (string), riskMitigationAction (string)",
                settings
            )
            val json = JSONObject(extractJsonObject(raw))
            val arr = json.optJSONArray("affectedClasses")
            val classes = if (arr != null) {
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            } else emptyList()
            Result.success(
                EventImpactAnalysis(
                    headline = headline,
                    severityScore = json.optInt("severityScore", 40).coerceIn(1, 100),
                    affectedClasses = classes,
                    directionalBias = json.optString("directionalBias", "Neutral monitoring"),
                    riskMitigationAction = json.optString("riskMitigationAction", "Maintain standard Risk Guard thresholds"),
                    providerUsed = resolveProviderLabel(settings)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Event impact analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeNarrativeCluster(
        clusterId: String,
        narrativeContext: String,
        settings: ProviderSettings
    ): Result<NarrativeAiBrief> {
        val prompt = """
            You are AETHERION MACRO INTEL, ARBITER & PATTERN DNA.
            Synthesize this live market narrative cluster, conflicting news claims, and cross-asset price reactions:
            $narrativeContext

            Provide sharp institutional insight explaining:
            1. Executive summary of the narrative arc.
            2. Dominant macro/structural driver.
            3. Conflict resolution thesis (reconcile the opposing reports if any exist).
            4. Cross-asset transmission mechanism across relevant instruments.
            5. Attention forecast (expansion vs priced-in exhaustion/decay).
        """.trimIndent()

        return try {
            val raw = executeStructuredCall(
                prompt = prompt,
                geminiSchema = narrativeSchema,
                jsonKeysHint = "executiveSummary, dominantDriver, conflictResolutionThesis, crossAssetTransmission, attentionForecast",
                settings = settings
            )
            val json = JSONObject(extractJsonObject(raw))
            Result.success(
                NarrativeAiBrief(
                    clusterId = clusterId,
                    executiveSummary = json.optString("executiveSummary", "Narrative under active monitoring."),
                    dominantDriver = json.optString("dominantDriver", "Multi-source macro catalyst."),
                    conflictResolutionThesis = json.optString("conflictResolutionThesis", "Monitor next confirmed wire/data print for resolution."),
                    crossAssetTransmission = json.optString("crossAssetTransmission", "Cross-asset repricing via yields, USD & commodity risk premia."),
                    attentionForecast = json.optString("attentionForecast", "Moderate half-life decay expected barring fresh escalation."),
                    providerUsed = resolveProviderLabel(settings)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Narrative AI synthesis timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Narrative cluster analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateStrategyProposal(
        symbol: String,
        regimeSummary: String,
        settings: ProviderSettings
    ): Result<FactoryAiProposal> {
        val prompt = """
            You are AETHERION GENOME & EVOLVER.
            Design an experimental EMA/ATR quantitative strategy candidate for $symbol given current live telemetry:
            $regimeSummary
            Constrain fastPeriod in [5, 25], slowPeriod in [22, 80] (slow > fast + 5), atrStopMult in [1.0, 3.0], atrTargetMult in [1.5, 5.0].
        """.trimIndent()

        return try {
            val raw = executeStructuredCall(
                prompt,
                factorySchema,
                "name, rationale, fastPeriod (int), slowPeriod (int), atrStopMult (number), atrTargetMult (number)",
                settings
            )
            val json = JSONObject(extractJsonObject(raw))
            val fast = json.optInt("fastPeriod", 12).coerceIn(5, 25)
            val slow = json.optInt("slowPeriod", 34).coerceIn(fast + 5, 90)
            Result.success(
                FactoryAiProposal(
                    name = json.optString("name", "$symbol Adaptive Genome"),
                    rationale = json.optString("rationale", "Volatility-adjusted trend momentum"),
                    fastPeriod = fast,
                    slowPeriod = slow,
                    atrStopMult = json.optDouble("atrStopMult", 1.5).coerceIn(0.8, 3.5),
                    atrTargetMult = json.optDouble("atrTargetMult", 2.8).coerceIn(1.2, 6.0),
                    providerUsed = resolveProviderLabel(settings)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Strategy proposal failed", e)
            Result.failure(e)
        }
    }

    suspend fun queryFreeformIntelligence(
        systemRole: String,
        userPrompt: String,
        settings: ProviderSettings
    ): Result<String> {
        val fullPrompt = "$systemRole\n\n$userPrompt"
        return try {
            val responseText = when (settings.activeProvider) {
                AiProviderType.FIREBASE_GEMINI_ARGON -> {
                    withTimeout(60_000L) { geminiArgon.generateContent(fullPrompt).text }
                        ?: throw IllegalStateException("Gemini 4 Argon returned empty text.")
                }
                AiProviderType.FIREBASE_GEMINI_LITE -> {
                    withTimeout(60_000L) { geminiLite.generateContent(fullPrompt).text }
                        ?: throw IllegalStateException("Gemini 3.5 Flash-Lite returned empty text.")
                }
                AiProviderType.FIREBASE_GEMINI_FLASH -> {
                    withTimeout(60_000L) { geminiFlash.generateContent(fullPrompt).text }
                        ?: throw IllegalStateException("Gemini 3.8 Flash returned empty text.")
                }
                AiProviderType.DEEPSEEK -> callDeepSeek(fullPrompt, settings)
                AiProviderType.OPENAI_COMPATIBLE -> callOpenAiCompatible(fullPrompt, settings)
                AiProviderType.ANTHROPIC_CLAUDE -> callAnthropic(fullPrompt, settings)
            }
            Result.success(responseText.trim())
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI inference timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI freeform query failed", e)
            Result.failure(e)
        }
    }

    private suspend fun executeStructuredCall(
        prompt: String,
        geminiSchema: Schema,
        jsonKeysHint: String,
        settings: ProviderSettings
    ): String {
        val jsonPrompt = "$prompt\nRespond ONLY with valid JSON containing keys: $jsonKeysHint."
        return when (settings.activeProvider) {
            AiProviderType.DEEPSEEK -> callDeepSeek(jsonPrompt, settings)
            AiProviderType.OPENAI_COMPATIBLE -> callOpenAiCompatible(jsonPrompt, settings)
            AiProviderType.ANTHROPIC_CLAUDE -> callAnthropic(jsonPrompt, settings)
            else -> {
                val model = Firebase.ai.generativeModel(
                    modelName = geminiModelId(settings.activeProvider),
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = geminiSchema
                    }
                )
                withTimeout(60_000L) { model.generateContent(prompt).text }
                    ?: throw IllegalStateException("Empty Gemini response")
            }
        }
    }

    private suspend fun callDeepSeek(prompt: String, settings: ProviderSettings): String =
        withContext(Dispatchers.IO) {
            val apiKey = ApiSecretsResolver.resolveDeepSeekKey(settings)
            if (apiKey.isBlank()) {
                throw IllegalStateException("DISCONNECTED: Add DEEPSEEK_API_KEY in AI Studio Secrets or Integrations Settings.")
            }
            val model = settings.deepSeekModel.ifBlank { "deepseek-chat" }
            val payload = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                if (model != "deepseek-reasoner") {
                    put("temperature", 0.3)
                }
            }
            val req = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("DeepSeek HTTP ${resp.code}: ${body.take(180)}")
                }
                val content = JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                content.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
            }
        }

    private suspend fun callOpenAiCompatible(prompt: String, settings: ProviderSettings): String =
        withContext(Dispatchers.IO) {
            val apiKey = settings.openAiApiKey.trim()
            if (apiKey.isBlank()) {
                throw IllegalStateException("DISCONNECTED: OpenAI API key not configured in Provider Settings.")
            }
            val baseUrl = settings.openAiBaseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("model", settings.openAiModel.ifBlank { "gpt-4o" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.3)
            }
            val req = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("OpenAI HTTP ${resp.code}: ${body.take(180)}")
                }
                val json = JSONObject(body)
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }

    private suspend fun callAnthropic(prompt: String, settings: ProviderSettings): String =
        withContext(Dispatchers.IO) {
            val apiKey = settings.anthropicApiKey.trim()
            if (apiKey.isBlank()) {
                throw IllegalStateException("DISCONNECTED: Anthropic API key not configured in Provider Settings.")
            }
            val payload = JSONObject().apply {
                put("model", settings.anthropicModel.ifBlank { "claude-3-7-sonnet-20250219" })
                put("max_tokens", 1200)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }
            val req = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Anthropic HTTP ${resp.code}: ${body.take(180)}")
                }
                val json = JSONObject(body)
                json.getJSONArray("content").getJSONObject(0).getString("text")
            }
        }

    private fun resolveProviderLabel(settings: ProviderSettings): String = when (settings.activeProvider) {
        AiProviderType.DEEPSEEK -> "DeepSeek (${settings.deepSeekModel})"
        AiProviderType.OPENAI_COMPATIBLE -> "OpenAI (${settings.openAiModel})"
        AiProviderType.ANTHROPIC_CLAUDE -> "Anthropic (${settings.anthropicModel})"
        else -> settings.activeProvider.displayName
    }

    private fun extractJsonObject(raw: String): String {
        val cleaned = raw.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }

    companion object {
        private const val TAG = "MultiProviderAiGateway"
    }
}
