package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.security.ApiSecretsResolver
import com.example.data.model.AiProviderType
import com.example.data.model.ProviderDiagnostic
import com.example.data.model.ProviderSettings
import com.example.data.remote.GeminiLiveVoiceEngine
import com.example.ui.components.GlassCard
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
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

@Composable
fun IntegrationsSettingsScreen(
    state: AetherDashboardState,
    userEmail: String?,
    userId: String,
    onUpdateSettings: ((ProviderSettings) -> ProviderSettings) -> Unit,
    onTestProvider: (String) -> Unit,
    onPingMt5: () -> Unit,
    onOpenRiskMatrix: () -> Unit,
    onSignOut: () -> Unit
) {
    val s = state.providerSettings
    var fmpKey by remember(s.fmpApiKey) { mutableStateOf(s.fmpApiKey) }
    var fredKey by remember(s.fredApiKey) { mutableStateOf(s.fredApiKey) }
    var twelveDataKey by remember(s.twelveDataApiKey) { mutableStateOf(s.twelveDataApiKey) }
    var finnhubKey by remember(s.finnhubApiKey) { mutableStateOf(s.finnhubApiKey) }

    var geminiLiveVoice by remember(s.geminiLiveVoice) { mutableStateOf(s.geminiLiveVoice) }
    var elevenLabsKey by remember(s.elevenLabsApiKey) { mutableStateOf(s.elevenLabsApiKey) }
    var elevenLabsVoiceId by remember(s.elevenLabsVoiceId) { mutableStateOf(s.elevenLabsVoiceId) }
    var elevenLabsModelId by remember(s.elevenLabsModelId) { mutableStateOf(s.elevenLabsModelId) }
    var deepgramKey by remember(s.deepgramApiKey) { mutableStateOf(s.deepgramApiKey) }
    var deepgramModel by remember(s.deepgramModel) { mutableStateOf(s.deepgramModel) }

    var deepSeekKey by remember(s.deepSeekApiKey) { mutableStateOf(s.deepSeekApiKey) }
    var deepSeekModel by remember(s.deepSeekModel) { mutableStateOf(s.deepSeekModel) }
    var openAiKey by remember(s.openAiApiKey) { mutableStateOf(s.openAiApiKey) }
    var openAiModel by remember(s.openAiModel) { mutableStateOf(s.openAiModel) }
    var openAiUrl by remember(s.openAiBaseUrl) { mutableStateOf(s.openAiBaseUrl) }
    var anthropicKey by remember(s.anthropicApiKey) { mutableStateOf(s.anthropicApiKey) }
    var anthropicModel by remember(s.anthropicModel) { mutableStateOf(s.anthropicModel) }

    var mt5Url by remember(s.mt5BridgeUrl) { mutableStateOf(s.mt5BridgeUrl) }
    var mt5Token by remember(s.mt5BridgeToken) { mutableStateOf(s.mt5BridgeToken) }
    var derivAppId by remember(s.derivAppId) { mutableStateOf(s.derivAppId) }

    fun saveAllSettings() {
        onUpdateSettings {
            it.copy(
                fmpApiKey = fmpKey.trim(),
                fredApiKey = fredKey.trim(),
                twelveDataApiKey = twelveDataKey.trim(),
                finnhubApiKey = finnhubKey.trim(),
                geminiLiveVoice = geminiLiveVoice.trim().ifBlank { "FENRIR" },
                elevenLabsApiKey = elevenLabsKey.trim(),
                elevenLabsVoiceId = elevenLabsVoiceId.trim().ifBlank { "JBFqnCBsd6RMkjVDRZzb" },
                elevenLabsModelId = elevenLabsModelId.trim().ifBlank { "eleven_multilingual_v2" },
                deepgramApiKey = deepgramKey.trim(),
                deepgramModel = deepgramModel.trim().ifBlank { "nova-3" },
                deepSeekApiKey = deepSeekKey.trim(),
                deepSeekModel = deepSeekModel.trim().ifBlank { "deepseek-chat" },
                openAiApiKey = openAiKey.trim(),
                openAiModel = openAiModel.trim().ifBlank { "gpt-4o" },
                openAiBaseUrl = openAiUrl.trim().ifBlank { "https://api.openai.com/v1" },
                anthropicApiKey = anthropicKey.trim(),
                anthropicModel = anthropicModel.trim().ifBlank { "claude-3-7-sonnet-20250219" },
                mt5BridgeUrl = mt5Url.trim(),
                mt5BridgeToken = mt5Token.trim(),
                derivAppId = derivAppId.trim()
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    "INTEGRATIONS & API KEY VAULT",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MetallicGoldBright
                )
                Text(
                    "Configure FMP, FRED, Twelve Data, Finnhub, DeepSeek, Firebase AI Logic, Risk Matrix, and MT5 bridge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
            }
        }

        // Google Identity & Cloud Persistence
        item {
            GlassCard(borderColor = LiveEmerald.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudDone, contentDescription = "Cloud Synced Account", tint = LiveEmerald)
                        Column {
                            Text("AUTHENTICATED COMMANDER", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                            Text(userEmail ?: "Google Account ($userId)", style = MaterialTheme.typography.titleSmall, color = IvoryText)
                            Text("UID: ${userId.take(16)}...", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, CrimsonAlert),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Sign Out", color = CrimsonAlert)
                    }
                }
            }
        }

        // Risk Guard Governance Shortcut
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = "Risk Defence Matrix", tint = MetallicGoldBright)
                        Column {
                            Text(
                                "RISK DEFENCE MATRIX · LEVEL ${state.constitution.permissionLevel}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MetallicGoldBright
                            )
                            Text(
                                "Max DD ${state.constitution.maxDrawdownPct}% · Kill Switch ${if (state.constitution.emergencyKillSwitch) "ENGAGED" else "ARMED"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.constitution.emergencyKillSwitch) CrimsonAlert else ChampagneSecondary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenRiskMatrix,
                        border = BorderStroke(1.dp, MetallicGold),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_risk_matrix_button")
                    ) {
                        Text("Configure", color = MetallicGoldBright)
                    }
                }
            }
        }

        // Market Data & Macro API Keys (FMP, FRED, Twelve Data, Finnhub)
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.4f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Key, contentDescription = "Market Data API Vault", tint = IceTelemetry)
                    Text("MARKET DATA & MACRO API KEYS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Set keys in the AI Studio Secrets panel or store runtime overrides in DataStore below. Keys auto-upgrade Yahoo/Kraken feeds with institutional macro series, earnings calendars & multi-vendor quotes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                ApiKeyWithTestRow(
                    label = "Financial Modeling Prep (FMP_API_KEY)",
                    value = fmpKey,
                    onValueChange = { fmpKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigFmp(),
                    isTesting = state.testingProviderId == "FMP",
                    diagnostic = state.providerDiagnostics["FMP"],
                    inputTag = "fmp_key_input",
                    testTag = "test_fmp_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("FMP")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                ApiKeyWithTestRow(
                    label = "St. Louis FRED Macro (FRED_API_KEY)",
                    value = fredKey,
                    onValueChange = { fredKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigFred(),
                    isTesting = state.testingProviderId == "FRED",
                    diagnostic = state.providerDiagnostics["FRED"],
                    inputTag = "fred_key_input",
                    testTag = "test_fred_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("FRED")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                ApiKeyWithTestRow(
                    label = "Twelve Data Market API (TWELVE_DATA_API_KEY)",
                    value = twelveDataKey,
                    onValueChange = { twelveDataKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigTwelveData(),
                    isTesting = state.testingProviderId == "TWELVE_DATA",
                    diagnostic = state.providerDiagnostics["TWELVE_DATA"],
                    inputTag = "twelve_data_key_input",
                    testTag = "test_twelve_data_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("TWELVE_DATA")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                ApiKeyWithTestRow(
                    label = "Finnhub Quotes & Calendar (FINNHUB_API_KEY)",
                    value = finnhubKey,
                    onValueChange = { finnhubKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigFinnhub(),
                    isTesting = state.testingProviderId == "FINNHUB",
                    diagnostic = state.providerDiagnostics["FINNHUB"],
                    inputTag = "finnhub_key_input",
                    testTag = "test_finnhub_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("FINNHUB")
                    }
                )
            }
        }

        // Voice Conversations & Audio APIs (Gemini 3.8 Live + ElevenLabs + Deepgram)
        item {
            GlassCard(borderColor = LiveEmerald.copy(alpha = 0.4f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = "Voice Conversations & Audio APIs", tint = LiveEmerald)
                    Text("VOICE CONVERSATIONS & AUDIO APIS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Gemini 3.8 Live (gemini-3.8-live) runs natively via Firebase AI Logic + App Check with zero client API key. Add ElevenLabs and Deepgram keys below or in AI Studio Secrets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Gemini 3.8 Live Voice Persona", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GeminiLiveVoiceEngine.AVAILABLE_VOICES.forEach { v ->
                        val selected = geminiLiveVoice.equals(v, ignoreCase = true)
                        OutlinedButton(
                            onClick = {
                                geminiLiveVoice = v
                                onUpdateSettings { it.copy(geminiLiveVoice = v) }
                            },
                            border = BorderStroke(1.dp, if (selected) MetallicGold else ObsidianBorder),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(v, style = MaterialTheme.typography.labelSmall, color = if (selected) MetallicGoldBright else ChampagneSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                ApiKeyWithTestRow(
                    label = "ElevenLabs API Key (ELEVENLABS_API_KEY)",
                    value = elevenLabsKey,
                    onValueChange = { elevenLabsKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigElevenLabs(),
                    isTesting = state.testingProviderId == "ELEVENLABS",
                    diagnostic = state.providerDiagnostics["ELEVENLABS"],
                    inputTag = "elevenlabs_key_input",
                    testTag = "test_elevenlabs_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("ELEVENLABS")
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = elevenLabsVoiceId,
                        onValueChange = { elevenLabsVoiceId = it },
                        label = { Text("ElevenLabs Voice ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = elevenLabsModelId,
                        onValueChange = { elevenLabsModelId = it },
                        label = { Text("TTS Model") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                ApiKeyWithTestRow(
                    label = "Deepgram API Key (DEEPGRAM_API_KEY)",
                    value = deepgramKey,
                    onValueChange = { deepgramKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigDeepgram(),
                    isTesting = state.testingProviderId == "DEEPGRAM",
                    diagnostic = state.providerDiagnostics["DEEPGRAM"],
                    inputTag = "deepgram_key_input",
                    testTag = "test_deepgram_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("DEEPGRAM")
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = deepgramModel,
                    onValueChange = { deepgramModel = it },
                    label = { Text("Deepgram STT Model (e.g. nova-3, nova-2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // AI Provider Selector
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("SELECT ACTIVE INTELLIGENCE ENGINE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(8.dp))
                AiProviderType.entries.forEach { provider ->
                    val selected = s.activeProvider == provider
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MetallicGold.copy(alpha = 0.14f) else ObsidianVoid.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, if (selected) MetallicGold else ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onUpdateSettings { it.copy(activeProvider = provider) } }
                            .testTag("select_provider_${provider.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = provider.displayName,
                                tint = if (selected) MetallicGoldBright else SlateMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(provider.displayName, style = MaterialTheme.typography.labelLarge, color = IvoryText)
                                Text(
                                    if (provider.requiresUserKey) "Requires API key below or AI Studio Secrets"
                                    else "Provisioned natively via Firebase AI Logic + App Check",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (provider.requiresUserKey) SimulatedAmber else LiveEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // DeepSeek & External LLM Provider Keys
        item {
            GlassCard {
                Text("DEEPSEEK & EXTERNAL AI PROVIDERS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyWithTestRow(
                    label = "DeepSeek API Key (DEEPSEEK_API_KEY)",
                    value = deepSeekKey,
                    onValueChange = { deepSeekKey = it },
                    hasBuildConfigSecret = ApiSecretsResolver.hasBuildConfigDeepSeek(),
                    isTesting = state.testingProviderId == "DEEPSEEK",
                    diagnostic = state.providerDiagnostics["DEEPSEEK"],
                    inputTag = "deepseek_key_input",
                    testTag = "test_deepseek_button",
                    onTest = {
                        saveAllSettings()
                        onTestProvider("DEEPSEEK")
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = deepSeekModel,
                    onValueChange = { deepSeekModel = it },
                    label = { Text("DeepSeek Model") },
                    placeholder = { Text("deepseek-chat or deepseek-reasoner") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { deepSeekModel = "deepseek-chat" },
                        border = BorderStroke(1.dp, if (deepSeekModel == "deepseek-chat") MetallicGold else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("deepseek-chat", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { deepSeekModel = "deepseek-reasoner" },
                        border = BorderStroke(1.dp, if (deepSeekModel == "deepseek-reasoner") MetallicGold else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("deepseek-reasoner", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    label = { Text("OpenAI-Compatible API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = openAiModel,
                        onValueChange = { openAiModel = it },
                        label = { Text("Model") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = openAiUrl,
                        onValueChange = { openAiUrl = it },
                        label = { Text("Base URL") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = anthropicKey,
                    onValueChange = { anthropicKey = it },
                    label = { Text("Anthropic API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = anthropicModel,
                    onValueChange = { anthropicModel = it },
                    label = { Text("Claude Model") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // MT5 EA Bridge & Deriv Config
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Hub, contentDescription = "Broker Bridges", tint = MetallicGoldBright)
                    Text("MT5 EA BRIDGE & DERIV CONFIG", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Connect your MetaTrader 5 HTTP/EA bridge for live spread, equity & Risk-Guard-gated order execution. Without a bridge URL, Paper Sandbox runs isolated locally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mt5Url,
                    onValueChange = { mt5Url = it },
                    label = { Text("MT5 Bridge HTTPS URL") },
                    placeholder = { Text("https://your-mt5-bridge.example.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mt5_url_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = mt5Token,
                    onValueChange = { mt5Token = it },
                    label = { Text("MT5 Bearer Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = derivAppId,
                    onValueChange = { derivAppId = it },
                    label = { Text("Deriv Synthetic App ID (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { saveAllSettings() },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_integrations_button")
                    ) {
                        Text("Save Configuration", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            saveAllSettings()
                            onPingMt5()
                        },
                        enabled = !state.isPingingBroker,
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (state.isPingingBroker) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Ping MT5")
                        }
                    }
                }
                state.brokerHealth?.let { report ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${report.statusLabel}: ${report.errorDetails ?: "Latency ${report.latencyMs}ms"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (report.connected) LiveEmerald else CrimsonAlert
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyWithTestRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hasBuildConfigSecret: Boolean,
    isTesting: Boolean,
    diagnostic: ProviderDiagnostic?,
    inputTag: String,
    testTag: String,
    onTest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (hasBuildConfigSecret) {
            Text(
                text = "BuildConfig Secret Loaded (Override below if needed)",
                style = MaterialTheme.typography.labelSmall,
                color = LiveEmerald
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(if (hasBuildConfigSecret) "Using BuildConfig secret" else "Paste API key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(inputTag)
            )
            OutlinedButton(
                onClick = onTest,
                enabled = !isTesting,
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.7f)),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MetallicGoldBright)
                } else {
                    Text("Test", color = MetallicGoldBright, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        diagnostic?.let { diag ->
            Text(
                text = "${diag.statusLabel}${if (diag.latencyMs > 0) " (${diag.latencyMs}ms)" else ""}: ${diag.details}",
                style = MaterialTheme.typography.labelSmall,
                color = if (diag.connected) LiveEmerald else CrimsonAlert
            )
        }
    }
}
