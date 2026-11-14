package com.example.data.remote

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

data class LiveTranscriptTurn(
    val id: String = UUID.randomUUID().toString(),
    val speaker: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class GeminiLiveUiState(
    val statusLabel: String = "STANDBY",
    val isConnecting: Boolean = false,
    val isLiveActive: Boolean = false,
    val activeModel: String = GeminiLiveVoiceEngine.LIVE_MODEL_NAME,
    val activeVoice: String = "FENRIR",
    val transcriptTurns: List<LiveTranscriptTurn> = emptyList(),
    val lastToolEvent: String? = null,
    val errorMessage: String? = null
)

interface LiveToolBridge {
    fun buildSystemContextSummary(): String
    fun getMarketQuoteJson(symbol: String): JsonObject
    fun getRiskStatusJson(): JsonObject
    fun setEmergencyKillSwitch(engage: Boolean): JsonObject
    fun selectSymbol(symbol: String): JsonObject
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveVoiceEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeSession: LiveSession? = null
    private var conversationJob: Job? = null

    private val _state = MutableStateFlow(GeminiLiveUiState())
    val state: StateFlow<GeminiLiveUiState> = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startConversation(
        context: Context,
        voiceName: String,
        bridge: LiveToolBridge
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.update {
                it.copy(
                    statusLabel = "MIC PERMISSION REQUIRED",
                    isConnecting = false,
                    isLiveActive = false,
                    errorMessage = "Microphone permission is required for Gemini 3.8 Live voice conversations."
                )
            }
            return
        }

        if (_state.value.isConnecting || _state.value.isLiveActive) {
            return
        }

        val resolvedVoice = voiceName.trim().ifBlank { "FENRIR" }.uppercase()
        conversationJob?.cancel()
        conversationJob = engineScope.launch {
            _state.update {
                it.copy(
                    statusLabel = "CONNECTING TO GEMINI 3.8 LIVE...",
                    isConnecting = true,
                    isLiveActive = false,
                    activeVoice = resolvedVoice,
                    errorMessage = null
                )
            }
            try {
                val tools = listOf(
                    Tool.functionDeclarations(
                        listOf(
                            FunctionDeclaration(
                                name = "get_market_quote",
                                description = "Retrieve current live market price, 24h percentage change, realized volatility, and source for an AETHERION symbol like XAUUSD, BTCUSD, EURUSD, NAS100.",
                                parameters = mapOf(
                                    "symbol" to Schema.string(description = "Instrument ticker symbol, e.g. XAUUSD")
                                )
                            ),
                            FunctionDeclaration(
                                name = "get_risk_guard_status",
                                description = "Read deterministic Risk Guard constitution limits, open position count, permission level, and emergency kill switch state.",
                                parameters = emptyMap()
                            ),
                            FunctionDeclaration(
                                name = "trigger_emergency_kill_switch",
                                description = "Engage or disengage AETHERION's deterministic emergency execution kill switch.",
                                parameters = mapOf(
                                    "engage" to Schema.boolean(description = "True to engage kill switch and halt all trading; false to disarm")
                                )
                            ),
                            FunctionDeclaration(
                                name = "select_active_symbol",
                                description = "Switch the active instrument chart and telemetry focus in AETHERION.",
                                parameters = mapOf(
                                    "symbol" to Schema.string(description = "Target market symbol, e.g. XAUUSD or BTCUSD")
                                )
                            )
                        )
                    )
                )

                val systemPrompt = content {
                    text(
                        """
                        You are AETHER PRIME, the institutional quantitative commander intelligence inside AETHERION VIP Black Label.
                        Respond concisely, authoritatively, and directly in spoken voice.
                        Always respect deterministic Risk Guard limits and cite real telemetry via your tools before answering about prices or risk limits.
                        Current Workspace Context:
                        ${bridge.buildSystemContextSummary()}
                        """.trimIndent()
                    )
                }

                val liveModel = Firebase.ai.liveModel(
                    modelName = LIVE_MODEL_NAME,
                    generationConfig = liveGenerationConfig {
                        responseModality = ResponseModality.AUDIO
                        speechConfig = SpeechConfig(voice = Voice(resolvedVoice))
                        inputAudioTranscription = AudioTranscriptionConfig()
                        outputAudioTranscription = AudioTranscriptionConfig()
                    },
                    tools = tools,
                    systemInstruction = systemPrompt
                )

                val session = liveModel.connect()
                activeSession = session

                _state.update {
                    it.copy(
                        statusLabel = "LIVE CONVERSATION ACTIVE",
                        isConnecting = false,
                        isLiveActive = true,
                        errorMessage = null
                    )
                }

                val config = liveAudioConversationConfig {
                    enableInterruptions = true
                    transcriptHandler = { inputTranscription, outputTranscription ->
                        val userChunk = inputTranscription?.text.orEmpty()
                        val modelChunk = outputTranscription?.text.orEmpty()
                        if (userChunk.isNotBlank()) {
                            appendTranscript("COMMANDER", userChunk)
                        }
                        if (modelChunk.isNotBlank()) {
                            appendTranscript("GEMINI 3.8 LIVE", modelChunk)
                        }
                    }
                    goAwayHandler = { goAway ->
                        _state.update {
                            it.copy(
                                statusLabel = "SESSION EXPIRING (${goAway.timeLeft ?: "SOON"})",
                                lastToolEvent = "Server GoAway received; reconnect to continue."
                            )
                        }
                    }
                    functionCallHandler = { fnCall ->
                        handleToolCall(fnCall, bridge)
                    }
                }

                session.startAudioConversation(config)

                if (_state.value.isLiveActive && activeSession != null) {
                    _state.update {
                        it.copy(
                            statusLabel = "SESSION ENDED",
                            isConnecting = false,
                            isLiveActive = false
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Live API conversation error", e)
                val msg = e.localizedMessage ?: "Gemini 3.8 Live session failed. Verify App Check & network."
                _state.update {
                    it.copy(
                        statusLabel = "ERROR",
                        isConnecting = false,
                        isLiveActive = false,
                        errorMessage = msg
                    )
                }
                cleanupSessionOnly()
            }
        }
    }

    fun sendTextPrompt(text: String) {
        val clean = text.trim()
        val session = activeSession ?: return
        if (clean.isBlank()) return
        engineScope.launch {
            try {
                appendTranscript("COMMANDER (TEXT)", clean)
                session.send(clean)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending live text", e)
                _state.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed sending text to live session")
                }
            }
        }
    }

    fun stopConversation() {
        val job = conversationJob
        conversationJob = null
        engineScope.launch {
            cleanupSessionOnly()
            job?.cancel()
            _state.update {
                it.copy(
                    statusLabel = "STANDBY",
                    isConnecting = false,
                    isLiveActive = false
                )
            }
        }
    }

    fun clearTranscripts() {
        _state.update { it.copy(transcriptTurns = emptyList(), lastToolEvent = null, errorMessage = null) }
    }

    private suspend fun cleanupSessionOnly() {
        val s = activeSession
        activeSession = null
        if (s != null) {
            runCatching { s.stopAudioConversation() }
            runCatching { s.close() }
        }
    }

    private fun handleToolCall(fnCall: FunctionCallPart, bridge: LiveToolBridge): FunctionResponsePart {
        val payload = when (fnCall.name) {
            "get_market_quote" -> {
                val sym = fnCall.args["symbol"]?.jsonPrimitive?.contentOrNull.orEmpty()
                bridge.getMarketQuoteJson(sym)
            }
            "get_risk_guard_status" -> bridge.getRiskStatusJson()
            "trigger_emergency_kill_switch" -> {
                val engage = fnCall.args["engage"]?.jsonPrimitive?.booleanOrNull ?: true
                bridge.setEmergencyKillSwitch(engage)
            }
            "select_active_symbol" -> {
                val sym = fnCall.args["symbol"]?.jsonPrimitive?.contentOrNull.orEmpty()
                bridge.selectSymbol(sym)
            }
            else -> JsonObject(mapOf("error" to JsonPrimitive("Unknown function: ${fnCall.name}")))
        }
        _state.update {
            it.copy(lastToolEvent = "Tool executed: ${fnCall.name} -> ${payload.toString().take(90)}")
        }
        return FunctionResponsePart(fnCall.name, payload)
    }

    private fun appendTranscript(speaker: String, chunk: String) {
        val trimmed = chunk
        _state.update { current ->
            val list = current.transcriptTurns.toMutableList()
            val last = list.lastOrNull()
            if (last != null && last.speaker == speaker) {
                val joined = if (last.text.endsWith(" ") || trimmed.startsWith(" ") ||
                    trimmed.startsWith(".") || trimmed.startsWith(",") || trimmed.startsWith("?") || trimmed.startsWith("!")
                ) {
                    last.text + trimmed
                } else {
                    last.text + trimmed
                }
                list[list.lastIndex] = last.copy(text = joined.trimStart(), timestampMs = System.currentTimeMillis())
            } else {
                list.add(LiveTranscriptTurn(speaker = speaker, text = trimmed.trim()))
            }
            current.copy(transcriptTurns = list.takeLast(40))
        }
    }

    companion object {
        private const val TAG = "GeminiLiveVoiceEngine"
        const val LIVE_MODEL_NAME = "gemini-3.8-live"
        val AVAILABLE_VOICES = listOf("FENRIR", "AOEDE", "CHARON", "KORE", "PUCK")
    }
}
