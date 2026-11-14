package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.security.ApiSecretsResolver
import com.example.data.local.RiskConstitutionEntity
import com.example.data.model.AgentDivision
import com.example.data.model.AutonomousCycleState
import com.example.data.model.IntelligenceLayer
import com.example.data.model.ProviderSettings
import com.example.data.model.StrategyStage
import com.example.data.model.VipModuleId
import com.example.data.remote.GeminiLiveVoiceEngine
import com.example.ui.components.ActiveSymbolStrip
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.TelemetryStatTile
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.IceTelemetry
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Module 11: Commander Voice Intelligence (Gemini 3.8 Live + Deepgram + ElevenLabs)
@Composable
fun CommanderVoiceScreen(
    state: AetherDashboardState,
    onStartLiveConversation: () -> Unit,
    onStopLiveConversation: () -> Unit,
    onSendLiveText: (String) -> Unit,
    onClearLiveTranscripts: () -> Unit,
    onStartDeepgramCapture: () -> Unit,
    onStopDeepgramAndExecute: (Boolean) -> Unit,
    onSpeakElevenLabs: (String) -> Unit,
    onStopElevenLabs: () -> Unit,
    onFetchElevenLabsVoices: () -> Unit,
    onUpdateSettings: ((ProviderSettings) -> ProviderSettings) -> Unit,
    onSubmitInstruction: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var commandText by remember { mutableStateOf("") }
    var liveTextPrompt by remember { mutableStateOf("") }
    var isDeviceListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var useElevenLabsOutput by remember { mutableStateOf(true) }
    var deviceTtsEnabled by remember { mutableStateOf(false) }
    var pendingMicAction by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                ttsReady = true
            }
        }
        instance
    }

    val speechRecognizerAvailable = remember(context) {
        SpeechRecognizer.isRecognitionAvailable(context)
    }
    val speechRecognizer = remember(context, speechRecognizerAvailable) {
        if (speechRecognizerAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(speechRecognizer, tts) {
        onDispose {
            runCatching { speechRecognizer?.destroy() }
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    val hasElevenLabsKey = ApiSecretsResolver.resolveElevenLabsKey(state.providerSettings).isNotBlank()
    val hasDeepgramKey = ApiSecretsResolver.resolveDeepgramKey(state.providerSettings).isNotBlank()
    val elevenLabsActive = useElevenLabsOutput && hasElevenLabsKey

    LaunchedEffect(state.voiceCommandState) {
        val success = state.voiceCommandState as? AiTaskState.Success
        if (!elevenLabsActive && deviceTtsEnabled && ttsReady && success != null) {
            tts.speak(success.data.take(400), TextToSpeech.QUEUE_FLUSH, null, "aether_cmd_reply")
        }
    }

    fun startSpeechCapture() {
        val sr = speechRecognizer
        if (sr == null) {
            voiceError = "Device SpeechRecognizer service unavailable. Use Gemini 3.8 Live, Deepgram, or typed commands."
            return
        }
        voiceError = null
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isDeviceListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isDeviceListening = false }
            override fun onError(error: Int) {
                isDeviceListening = false
                voiceError = "Device speech code $error. Verify microphone audio or use Deepgram / Gemini Live."
            }
            override fun onResults(results: Bundle?) {
                isDeviceListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    commandText = spoken
                    onSubmitInstruction(spoken, elevenLabsActive)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) commandText = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        runCatching {
            sr.startListening(intent)
        }.onFailure {
            isDeviceListening = false
            voiceError = it.localizedMessage ?: "Unable to start microphone recognition."
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        val action = pendingMicAction
        pendingMicAction = null
        if (granted) {
            when (action) {
                "LIVE" -> onStartLiveConversation()
                "DEEPGRAM" -> onStartDeepgramCapture()
                else -> startSpeechCapture()
            }
        } else {
            voiceError = "Microphone permission denied. Typed executive commands remain operational."
        }
    }

    val quickPrompts = listOf(
        "Brief me on top volatility movers and tail risk",
        "Summarize open exposure vs deterministic risk limits",
        "Which market has the clearest capital flow momentum?",
        "Engage Emergency Kill Switch"
    )

    val live = state.geminiLive

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.COMMANDER_VOICE_INTELLIGENCE, onBack) }

        // 1. REAL-TIME GEMINI 3.8 LIVE BIDIRECTIONAL VOICE CONVERSATIONS
        item {
            val liveBorder = when {
                live.isLiveActive -> LiveEmerald
                live.isConnecting -> IceTelemetry
                !live.errorMessage.isNullOrBlank() -> CrimsonAlert
                else -> MetallicGold.copy(alpha = 0.6f)
            }
            GlassCard(borderColor = liveBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "GEMINI 3.8 LIVE · VOICE CONVERSATIONS",
                            style = MaterialTheme.typography.titleSmall,
                            color = MetallicGoldBright
                        )
                        Text(
                            "Model: ${live.activeModel} · ${live.statusLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (live.isLiveActive) LiveEmerald else ChampagneSecondary
                        )
                    }
                    TelemetryStatTile(
                        label = "VOICE",
                        value = state.providerSettings.geminiLiveVoice,
                        accent = if (live.isLiveActive) LiveEmerald else IceTelemetry
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Real-time full-duplex audio stream via Firebase AI Logic + App Check. Supports natural interruptions & live tools (market quotes, Risk Guard status, symbol switching, and emergency kill switch).",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GeminiLiveVoiceEngine.AVAILABLE_VOICES.forEach { voice ->
                        val selected = state.providerSettings.geminiLiveVoice.equals(voice, ignoreCase = true)
                        OutlinedButton(
                            onClick = { onUpdateSettings { it.copy(geminiLiveVoice = voice) } },
                            enabled = !live.isLiveActive && !live.isConnecting,
                            border = BorderStroke(1.dp, if (selected) MetallicGold else ObsidianBorder),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                voice,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MetallicGoldBright else ChampagneSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (live.isLiveActive || live.isConnecting) {
                            onStopLiveConversation()
                        } else if (permissionGranted) {
                            onStartLiveConversation()
                        } else {
                            pendingMicAction = "LIVE"
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (live.isLiveActive || live.isConnecting) CrimsonAlert else LiveEmerald,
                        contentColor = if (live.isLiveActive || live.isConnecting) IvoryText else ObsidianVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("gemini_live_conversation_button")
                ) {
                    when {
                        live.isConnecting -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IvoryText, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Connecting Live API (Tap to Cancel)", fontWeight = FontWeight.Bold)
                        }
                        live.isLiveActive -> {
                            Icon(Icons.Filled.CallEnd, contentDescription = "End Live Conversation", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("End Gemini 3.8 Live Conversation", fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(Icons.Filled.Call, contentDescription = "Start Live Conversation", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Start Real-Time Voice Conversation", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!live.lastToolEvent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(live.lastToolEvent, style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                }

                if (!live.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LIVE API ERROR: ${live.errorMessage}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                }

                if (live.isLiveActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = liveTextPrompt,
                            onValueChange = { liveTextPrompt = it },
                            label = { Text("Inject Text Mid-Conversation") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val msg = liveTextPrompt
                                liveTextPrompt = ""
                                onSendLiveText(msg)
                            },
                            enabled = liveTextPrompt.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Send", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (live.transcriptTurns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LIVE CONVERSATION TRANSCRIPT", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
                        OutlinedButton(
                            onClick = onClearLiveTranscripts,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    live.transcriptTurns.takeLast(8).forEach { turn ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianVoid.copy(alpha = 0.8f),
                            border = BorderStroke(
                                1.dp,
                                if (turn.speaker.contains("GEMINI")) LiveEmerald.copy(alpha = 0.45f) else ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    turn.speaker,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (turn.speaker.contains("GEMINI")) LiveEmerald else MetallicGoldBright
                                )
                                Text(turn.text, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                            }
                        }
                    }
                }
            }
        }

        // 2. DEEPGRAM NOVA STT + ELEVENLABS NEURAL VOICE DESK
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DEEPGRAM STT & ELEVENLABS NEURAL AUDIO", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                        Text(
                            "Deepgram (${state.providerSettings.deepgramModel}): ${if (hasDeepgramKey) "KEY ACTIVE" else "KEY REQUIRED"} · ElevenLabs: ${if (hasElevenLabsKey) "KEY ACTIVE" else "KEY REQUIRED"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasDeepgramKey || hasElevenLabsKey) LiveEmerald else SimulatedAmber
                        )
                    }
                    if (state.isElevenLabsSpeaking) {
                        OutlinedButton(
                            onClick = onStopElevenLabs,
                            border = BorderStroke(1.dp, CrimsonAlert),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop ElevenLabs Audio", tint = CrimsonAlert, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Stop", color = CrimsonAlert)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (state.isDeepgramRecording) {
                                onStopDeepgramAndExecute(elevenLabsActive)
                            } else if (permissionGranted) {
                                onStartDeepgramCapture()
                            } else {
                                pendingMicAction = "DEEPGRAM"
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = !state.isDeepgramTranscribing && !live.isLiveActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isDeepgramRecording) CrimsonAlert else IceTelemetry,
                            contentColor = if (state.isDeepgramRecording) IvoryText else ObsidianVoid
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("deepgram_record_button")
                    ) {
                        when {
                            state.isDeepgramTranscribing -> {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                            }
                            state.isDeepgramRecording -> {
                                Icon(Icons.Filled.Stop, contentDescription = "Stop & Transcribe with Deepgram", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Stop & Send (Deepgram)", fontWeight = FontWeight.Bold)
                            }
                            else -> {
                                Icon(Icons.Filled.Mic, contentDescription = "Record for Deepgram STT", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Deepgram Mic", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onFetchElevenLabsVoices,
                        enabled = !state.isFetchingVoices && hasElevenLabsKey,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (state.isFetchingVoices) "..." else "Sync Voices", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (!state.deepgramStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        state.deepgramStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.deepgramStatusMessage.contains("DISCONNECTED") || state.deepgramStatusMessage.contains("failed", ignoreCase = true)) CrimsonAlert else IceTelemetry
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("ELEVENLABS VOICE PERSONA", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.elevenLabsVoices.take(8).forEach { voice ->
                        val selected = state.providerSettings.elevenLabsVoiceId == voice.voiceId
                        OutlinedButton(
                            onClick = { onUpdateSettings { it.copy(elevenLabsVoiceId = voice.voiceId) } },
                            border = BorderStroke(1.dp, if (selected) MetallicGold else ObsidianBorder),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                voice.name.take(18),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MetallicGoldBright else ChampagneSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Auto-Speak AI Replies via ElevenLabs",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasElevenLabsKey) IvoryText else SlateMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = useElevenLabsOutput,
                        onCheckedChange = { useElevenLabsOutput = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = MetallicGold)
                    )
                }
                if (!state.elevenLabsStatusMessage.isNullOrBlank()) {
                    Text(
                        state.elevenLabsStatusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.elevenLabsStatusMessage.contains("DISCONNECTED") || state.elevenLabsStatusMessage.contains("failed", ignoreCase = true)) CrimsonAlert else LiveEmerald
                    )
                }
            }
        }

        // 3. EXECUTIVE COMMAND BRIDGE (TYPED + DEVICE SPEECH RECOGNIZER + TTS)
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EXECUTIVE COMMAND BRIDGE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                        Text(
                            if (speechRecognizerAvailable) "Device SpeechRecognizer + Typed AI Routing" else "Typed Executive Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (deviceTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle device TTS voice readout",
                            tint = if (deviceTtsEnabled) MetallicGoldBright else SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Switch(
                            checked = deviceTtsEnabled,
                            onCheckedChange = { deviceTtsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = MetallicGold),
                            modifier = Modifier.testTag("tts_readout_switch")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (isDeviceListening) {
                            runCatching { speechRecognizer?.stopListening() }
                            isDeviceListening = false
                        } else if (permissionGranted) {
                            startSpeechCapture()
                        } else {
                            pendingMicAction = "DEVICE"
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    border = BorderStroke(1.dp, if (isDeviceListening) CrimsonAlert else ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("voice_capture_button")
                ) {
                    Icon(
                        imageVector = when {
                            isDeviceListening -> Icons.Filled.GraphicEq
                            permissionGranted -> Icons.Filled.Mic
                            else -> Icons.Filled.MicOff
                        },
                        contentDescription = "Device voice microphone capture",
                        tint = if (isDeviceListening) CrimsonAlert else MetallicGoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (isDeviceListening) "Stop Device Listening" else "Device Speech Recognition",
                        color = if (isDeviceListening) CrimsonAlert else IvoryText
                    )
                }

                if (!voiceError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(voiceError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = SimulatedAmber)
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("Direct Command to AETHER PRIME") },
                    placeholder = { Text("e.g. Assess XAUUSD tail risk & open orders") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_command_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val toSend = commandText
                        commandText = ""
                        onSubmitInstruction(toSend, elevenLabsActive)
                    },
                    enabled = commandText.isNotBlank() && state.voiceCommandState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_voice_command_button")
                ) {
                    if (state.voiceCommandState is AiTaskState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                    } else {
                        Text("Transmit Executive Command", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            GlassCard {
                Text("QUICK DETERMINISTIC & BRIEFING COMMANDS", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(8.dp))
                quickPrompts.forEach { preset ->
                    OutlinedButton(
                        onClick = { onSubmitInstruction(preset, elevenLabsActive) },
                        border = BorderStroke(1.dp, if (preset.contains("Kill Switch")) CrimsonAlert else ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .height(48.dp)
                    ) {
                        Text(
                            text = preset,
                            color = if (preset.contains("Kill Switch")) CrimsonAlert else IvoryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (state.voiceHistory.isNotEmpty()) {
            item {
                Text("COMMANDER RESPONSE TRANSCRIPT (${state.voiceHistory.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
            }
            val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
            state.voiceHistory.forEach { entry ->
                item(key = "${entry.timestampMs}_${entry.command.hashCode()}") {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CMD: ${entry.command}", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright, modifier = Modifier.weight(1f))
                            Text(fmt.format(Date(entry.timestampMs)), style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(entry.response, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Routed via ${entry.provider}", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary, modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { onSpeakElevenLabs(entry.response) },
                                enabled = !state.isElevenLabsSpeaking,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak via ElevenLabs", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Speak", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "VOICE_COMMAND") }
    }
}

// Module 12: Autonomous Risk Defence Matrix
@Composable
fun RiskDefenceMatrixScreen(
    state: AetherDashboardState,
    onUpdateConstitution: ((RiskConstitutionEntity) -> RiskConstitutionEntity) -> Unit,
    onToggleKillSwitch: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val c = state.constitution
    val permissionTitles = mapOf(
        1 to "Level 1 · Observer Only (No Execution)",
        2 to "Level 2 · Research & Backtest Only",
        3 to "Level 3 · Paper Sandbox Execution",
        4 to "Level 4 · Semi-Autonomous Broker Route",
        5 to "Level 5 · Full Institutional Autonomy"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.AUTONOMOUS_RISK_DEFENCE_MATRIX, onBack) }

        item {
            GlassCard(
                borderColor = if (c.emergencyKillSwitch) CrimsonAlert else LiveEmerald.copy(alpha = 0.55f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (c.emergencyKillSwitch) Icons.Filled.Warning else Icons.Filled.Security,
                        contentDescription = "Emergency Kill Switch State",
                        tint = if (c.emergencyKillSwitch) CrimsonAlert else LiveEmerald
                    )
                    Text(
                        text = if (c.emergencyKillSwitch)
                            "DETERMINISTIC KILL SWITCH ENGAGED — ALL EXECUTION BLOCKED"
                        else "RISK GUARD CONSTITUTION ACTIVE · AI OVERRIDE IMPOSSIBLE",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (c.emergencyKillSwitch) CrimsonAlert else LiveEmerald
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Hard limits are enforced inside Kotlin DeterministicRiskEngine before any paper or broker order leaves the device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onToggleKillSwitch(!c.emergencyKillSwitch) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (c.emergencyKillSwitch) LiveEmerald else CrimsonAlert,
                        contentColor = IvoryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("risk_matrix_kill_switch_button")
                ) {
                    Text(
                        text = if (c.emergencyKillSwitch) "DISENGAGE EMERGENCY KILL SWITCH" else "ENGAGE EMERGENCY KILL SWITCH",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            GlassCard {
                Text("INSTITUTIONAL HARD LIMITS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(10.dp))

                ConstitutionSlider(
                    label = "Max Account Drawdown Cap",
                    valueDisplay = String.format(Locale.US, "%.1f%%", c.maxDrawdownPct),
                    value = c.maxDrawdownPct.toFloat(),
                    range = 2f..25f,
                    onValueChangeFinished = { v -> onUpdateConstitution { it.copy(maxDrawdownPct = (v * 2).roundToInt() / 2.0) } }
                )
                ConstitutionSlider(
                    label = "Daily Loss Limit",
                    valueDisplay = String.format(Locale.US, "%.1f%%", c.dailyLossLimitPct),
                    value = c.dailyLossLimitPct.toFloat(),
                    range = 0.5f..10f,
                    onValueChangeFinished = { v -> onUpdateConstitution { it.copy(dailyLossLimitPct = (v * 2).roundToInt() / 2.0) } }
                )
                ConstitutionSlider(
                    label = "Max Total Portfolio Exposure",
                    valueDisplay = String.format(Locale.US, "%.0f%%", c.maxTotalExposurePct),
                    value = c.maxTotalExposurePct.toFloat(),
                    range = 5f..60f,
                    onValueChangeFinished = { v -> onUpdateConstitution { it.copy(maxTotalExposurePct = v.roundToInt().toDouble()) } }
                )
                ConstitutionSlider(
                    label = "Max Concurrent Open Positions",
                    valueDisplay = "${c.maxOpenPositions}",
                    value = c.maxOpenPositions.toFloat(),
                    range = 1f..20f,
                    onValueChangeFinished = { v -> onUpdateConstitution { it.copy(maxOpenPositions = v.roundToInt().coerceIn(1, 20)) } }
                )
                ConstitutionSlider(
                    label = "Max Spread Tolerance (bps)",
                    valueDisplay = "${c.maxSpreadBps.toInt()} bps",
                    value = c.maxSpreadBps.toFloat(),
                    range = 5f..50f,
                    onValueChangeFinished = { v -> onUpdateConstitution { it.copy(maxSpreadBps = v.roundToInt().toDouble()) } }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High-Impact News Blackout Guard", style = MaterialTheme.typography.titleSmall, color = IvoryText)
                        Text("Blocks entries during extreme tail-risk events (Score >= 75)", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                    }
                    Switch(
                        checked = c.newsBlackoutEnabled,
                        onCheckedChange = { enabled -> onUpdateConstitution { it.copy(newsBlackoutEnabled = enabled) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = MetallicGold),
                        modifier = Modifier.testTag("news_blackout_switch")
                    )
                }
            }
        }

        item {
            GlassCard {
                Text("AUTONOMY PERMISSION GOVERNANCE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    permissionTitles[c.permissionLevel] ?: "Level ${c.permissionLevel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = LiveEmerald
                )
                Slider(
                    value = c.permissionLevel.toFloat(),
                    onValueChange = { lvl -> onUpdateConstitution { it.copy(permissionLevel = lvl.roundToInt().coerceIn(1, 5)) } },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = MetallicGoldBright, activeTrackColor = MetallicGold),
                    modifier = Modifier.testTag("permission_level_slider")
                )
                permissionTitles.forEach { (lvl, desc) ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (c.permissionLevel == lvl) MetallicGoldBright else SlateMuted
                    )
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "RISK_MATRIX") }
    }
}

@Composable
private fun ConstitutionSlider(
    label: String,
    valueDisplay: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (Float) -> Unit
) {
    var localValue by remember(value) { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = IvoryText)
            Text(
                text = String.format(Locale.US, "%s", if (localValue == value) valueDisplay else String.format(Locale.US, "%.1f", localValue)),
                style = MaterialTheme.typography.labelLarge,
                color = MetallicGoldBright
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChangeFinished(localValue) },
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = MetallicGoldBright, activeTrackColor = MetallicGold)
        )
    }
}

// Module 13: Global Event Impact Engine
@Composable
fun GlobalEventImpactScreen(
    state: AetherDashboardState,
    onRefreshHeadlines: () -> Unit,
    onAnalyzeHeadline: (String) -> Unit,
    onBack: () -> Unit
) {
    var customHeadline by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.GLOBAL_EVENT_IMPACT_ENGINE, onBack) }

        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("AI MACRO SHOCK IMPACT ASSESSOR", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customHeadline,
                    onValueChange = { customHeadline = it },
                    label = { Text("Enter Macro Shock or Central Bank Event") },
                    placeholder = { Text("e.g. Surprise 50bps Fed rate cut or Strait supply disruption") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_event_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAnalyzeHeadline(customHeadline.ifBlank { state.headlines.firstOrNull()?.title ?: "Global central bank liquidity shift" }) },
                        enabled = state.eventImpactState !is AiTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("analyze_event_button")
                    ) {
                        if (state.eventImpactState is AiTaskState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                        } else {
                            Text("Analyze Shock Impact", fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = onRefreshHeadlines,
                        modifier = Modifier.size(48.dp).testTag("refresh_headlines_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh live headlines", tint = MetallicGoldBright)
                    }
                }

                when (val s = state.eventImpactState) {
                    is AiTaskState.Success -> {
                        val a = s.data
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianVoid.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, if (a.severityScore >= 70) CrimsonAlert else MetallicGold)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SEVERITY SCORE: ${a.severityScore}/100", style = MaterialTheme.typography.labelLarge, color = if (a.severityScore >= 70) CrimsonAlert else MetallicGoldBright)
                                    Text(a.providerUsed, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                }
                                Text("Headline: ${a.headline}", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                                Text("Affected Asset Classes: ${a.affectedClasses.joinToString(", ")}", style = MaterialTheme.typography.labelMedium, color = IceTelemetry)
                                Text("Directional Bias: ${a.directionalBias}", style = MaterialTheme.typography.bodySmall, color = IvoryText)
                                Text("Risk Action: ${a.riskMitigationAction}", style = MaterialTheme.typography.bodySmall, color = LiveEmerald)
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ANALYSIS ERROR: ${s.message}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
                }
            }
        }

        if (!state.headlineError.isNullOrBlank()) {
            item {
                GlassCard(borderColor = CrimsonAlert.copy(alpha = 0.6f)) {
                    Text("LIVE RSS FEED WARNING: ${state.headlineError}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                }
            }
        }

        item {
            Text("LIVE YAHOO FINANCE MACRO HEADLINES (${state.headlines.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
        }
        state.headlines.forEach { news ->
            item(key = news.link.ifBlank { news.title }) {
                GlassCard {
                    Text(news.title, style = MaterialTheme.typography.titleSmall, color = IvoryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${news.source} · ${news.publishedAt}", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { onAnalyzeHeadline(news.title) },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Assess", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "EVENT_ENGINE") }
    }
}

// Module 14: Autonomous Strategy Factory
@Composable
fun AutonomousStrategyFactoryScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onRunFactoryBatch: (String) -> Unit,
    onBack: () -> Unit
) {
    val stageCounts = remember(state.strategies) {
        StrategyStage.entries.associateWith { st -> state.strategies.count { it.stage == st.name } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.AUTONOMOUS_STRATEGY_FACTORY, onBack) }

        item { ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol) }

        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                Text("8-STAGE AUTONOMOUS STRATEGY FACTORY", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Synthesizes AI quantitative hypotheses using active provider (${state.providerSettings.activeProvider.displayName}), compiles EMA/ATR parameters, runs live-candle backtest + 70/30 OOS split + tail shock stress test, and gates graduation deterministically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.factoryStatusMessage, style = MaterialTheme.typography.labelMedium, color = IceTelemetry)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onRunFactoryBatch(state.selectedSymbol) },
                    enabled = !state.isFactoryRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_strategy_factory_button")
                ) {
                    if (state.isFactoryRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                    } else {
                        Text("Synthesize & Validate Strategy for ${state.selectedSymbol}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            GlassCard {
                Text("GRADUATION PIPELINE DISTRIBUTION", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(8.dp))
                StrategyStage.entries.forEach { stage ->
                    val count = stageCounts[stage] ?: 0
                    val ratio = (count.toFloat() / state.strategies.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stage.label, style = MaterialTheme.typography.labelMedium, color = IvoryText)
                            Text("$count", style = MaterialTheme.typography.labelMedium, color = MetallicGoldBright)
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (stage == StrategyStage.LIVE_ELIGIBLE) LiveEmerald else MetallicGold,
                            trackColor = ObsidianVoid,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        state.strategies.take(6).forEach { strat ->
            item(key = strat.id) {
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strat.name, style = MaterialTheme.typography.titleSmall, color = IvoryText, modifier = Modifier.weight(1f))
                        Text(strat.stage, style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                    }
                    Text(
                        "${strat.symbol} · ${strat.creatorAgent} · Sharpe ${String.format(Locale.US, "%.2f", strat.sharpeRatio)} · PF ${String.format(Locale.US, "%.2f", strat.profitFactor)} · DD ${String.format(Locale.US, "%.1f%%", strat.maxDrawdownPct)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneSecondary
                    )
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "STRATEGY_FACTORY") }
    }
}

// Module 15: AETHERION OVERMIND
@Composable
fun OvermindScreen(
    state: AetherDashboardState,
    onToggleCycle: () -> Unit,
    onStepCycleOnce: () -> Unit,
    onBack: () -> Unit
) {
    val cycle = state.autonomousCycle
    var selectedDivision by remember { mutableStateOf<AgentDivision?>(null) }
    val filteredAgents = remember(state.agents, selectedDivision) {
        if (selectedDivision == null) state.agents else state.agents.filter { it.division == selectedDivision }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.AETHERION_OVERMIND, onBack) }

        item {
            GlassCard(borderColor = if (cycle.isRunning) LiveEmerald else MetallicGold.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("12-STEP AUTONOMOUS OPERATION CYCLE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                        Text(
                            if (cycle.isRunning) "RUNNING CONTINUOUSLY · Cycles: ${cycle.completedCycles}" else "STANDBY · Cycles Completed: ${cycle.completedCycles}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cycle.isRunning) LiveEmerald else ChampagneSecondary
                        )
                    }
                    TelemetryStatTile("STEP", "${cycle.currentStepIndex + 1}/12", MetallicGoldBright)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(cycle.lastStepSummary, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleCycle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cycle.isRunning) CrimsonContainer else MetallicGold,
                            contentColor = if (cycle.isRunning) IvoryText else ObsidianVoid
                        ),
                        border = if (cycle.isRunning) BorderStroke(1.dp, CrimsonAlert) else null,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("overmind_toggle_cycle_button")
                    ) {
                        Icon(
                            imageVector = if (cycle.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Toggle autonomous cycle",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (cycle.isRunning) "Pause Cycle" else "Engage Cycle", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStepCycleOnce,
                        enabled = !cycle.isRunning,
                        modifier = Modifier.height(48.dp).testTag("overmind_step_button")
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Step cycle once", modifier = Modifier.size(18.dp))
                        Text("Step")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                AutonomousCycleState.steps.forEachIndexed { idx, stepLabel ->
                    val isCurrent = idx == cycle.currentStepIndex
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stepLabel,
                            tint = when {
                                isCurrent -> MetallicGoldBright
                                idx < cycle.currentStepIndex -> LiveEmerald
                                else -> SlateMuted
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stepLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) MetallicGoldBright else ChampagneSecondary
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Text("8 CENTRAL INTELLIGENCE LAYERS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                IntelligenceLayer.entries.forEach { layer ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${layer.code} · ${layer.label}", style = MaterialTheme.typography.labelMedium, color = MetallicGold)
                        Text(layer.responsibility, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                    }
                }
            }
        }

        item {
            Text("30 SPECIALIST AI AGENTS (${filteredAgents.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { selectedDivision = null },
                    border = BorderStroke(1.dp, if (selectedDivision == null) MetallicGold else ObsidianBorder),
                    modifier = Modifier.height(48.dp).testTag("overmind_div_all")
                ) {
                    Text(
                        "ALL (30)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedDivision == null) MetallicGoldBright else ChampagneSecondary
                    )
                }
                AgentDivision.entries.forEach { div ->
                    val isSel = selectedDivision == div
                    OutlinedButton(
                        onClick = { selectedDivision = if (isSel) null else div },
                        border = BorderStroke(1.dp, if (isSel) MetallicGold else ObsidianBorder),
                        modifier = Modifier.height(48.dp).testTag("overmind_div_${div.name.lowercase()}")
                    ) {
                        Text(
                            div.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) MetallicGoldBright else ChampagneSecondary
                        )
                    }
                }
            }
        }
        filteredAgents.forEach { agent ->
            item(key = "agent_${agent.id}") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianVoid.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${agent.id} ${agent.name} · ${agent.division.label}", style = MaterialTheme.typography.labelMedium, color = MetallicGoldBright)
                            Text("${agent.role} · Provider: ${agent.assignedProvider}", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                agent.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (agent.status) {
                                    "DISCONNECTED" -> CrimsonAlert
                                    "SANDBOX", "STANDBY" -> SimulatedAmber
                                    else -> LiveEmerald
                                }
                            )
                            Text(
                                if (agent.accuracyPct > 0.0) String.format(Locale.US, "%.1f%% acc", agent.accuracyPct) else "N/A",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Text("CROSS-MODULE EVENT BUS (${state.crossModuleEvents.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                state.crossModuleEvents.take(10).forEach { ev ->
                    Text(
                        "[${ev.sourceModule} -> ${ev.targetModule}] ${ev.summary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ev.severity == "ALERT" || ev.severity == "CRITICAL") CrimsonAlert else ChampagneSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, null) }
    }
}
