package com.example.ui.viewmodel

import com.example.data.local.LocalAuditLogEntity
import com.example.data.local.LocalMemoryEntity
import com.example.data.local.LocalStrategyEntity
import com.example.data.local.PaperOrderEntity
import com.example.data.local.RiskConstitutionEntity
import com.example.data.model.AgentProfile
import com.example.data.model.AetherAgentsRegistry
import com.example.data.model.AutonomousCycleState
import com.example.data.model.CrossModuleEvent
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketQuote
import com.example.data.model.NewsHeadline
import com.example.data.model.ProviderDiagnostic
import com.example.data.model.ProviderSettings
import com.example.data.model.RiskCheckResult
import com.example.data.remote.BrokerHealthReport
import com.example.data.remote.CouncilVoteResult
import com.example.data.remote.ElevenLabsVoiceProfile
import com.example.data.remote.EventImpactAnalysis
import com.example.data.remote.GeminiLiveUiState
import com.example.data.remote.VoiceApisClient
import com.example.domain.engine.AllocationSlice
import com.example.domain.engine.BacktestReport
import com.example.domain.engine.BlackSwanTelemetry
import com.example.domain.engine.CurrencyStrengthItem
import com.example.domain.engine.MultiverseScenarioReport

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data object Loading : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

data class VoiceLogEntry(
    val command: String,
    val response: String,
    val provider: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AetherDashboardState(
    val quotes: List<MarketQuote> = emptyList(),
    val isRefreshingMarkets: Boolean = false,
    val marketLastSyncMs: Long = 0L,
    val marketFetchError: String? = null,
    val selectedSymbol: String = "XAUUSD",
    val marketConditions: MarketConditionsSnapshot = MarketConditionsSnapshot(),
    val isRefreshingConditions: Boolean = false,
    val conditionsAiBriefState: AiTaskState<String> = AiTaskState.Idle,
    val providerDiagnostics: Map<String, ProviderDiagnostic> = emptyMap(),
    val testingProviderId: String? = null,
    val constitution: RiskConstitutionEntity = RiskConstitutionEntity(),
    val strategies: List<LocalStrategyEntity> = emptyList(),
    val memories: List<LocalMemoryEntity> = emptyList(),
    val auditLogs: List<LocalAuditLogEntity> = emptyList(),
    val paperOrders: List<PaperOrderEntity> = emptyList(),
    val agents: List<AgentProfile> = AetherAgentsRegistry.allAgents,
    val crossModuleEvents: List<CrossModuleEvent> = emptyList(),
    val providerSettings: ProviderSettings = ProviderSettings(),
    val autonomousCycle: AutonomousCycleState = AutonomousCycleState(),

    // Module-specific states
    val multiverseReport: MultiverseScenarioReport? = null,
    val selectedMultiverseRegime: String = "Base Live Drift",
    val councilState: AiTaskState<CouncilVoteResult> = AiTaskState.Idle,
    val activeBacktest: BacktestReport? = null,
    val timeReplayIndex: Int = 0,
    val isTimeReplaying: Boolean = false,
    val blackSwanAlerts: List<BlackSwanTelemetry> = emptyList(),
    val currencyStrengths: List<CurrencyStrengthItem> = emptyList(),
    val allocations: List<AllocationSlice> = emptyList(),
    val brokerHealth: BrokerHealthReport? = null,
    val isPingingBroker: Boolean = false,
    val lastRiskEvaluation: RiskCheckResult? = null,
    val headlines: List<NewsHeadline> = emptyList(),
    val headlineError: String? = null,
    val eventImpactState: AiTaskState<EventImpactAnalysis> = AiTaskState.Idle,
    val factoryStatusMessage: String = "Ready to discover & evolve quantitative candidates.",
    val isFactoryRunning: Boolean = false,
    val memorySynthesisState: AiTaskState<String> = AiTaskState.Idle,

    // Voice Conversations (Gemini 3.8 Live + Deepgram + ElevenLabs)
    val voiceCommandState: AiTaskState<String> = AiTaskState.Idle,
    val voiceHistory: List<VoiceLogEntry> = emptyList(),
    val geminiLive: GeminiLiveUiState = GeminiLiveUiState(),
    val isDeepgramRecording: Boolean = false,
    val isDeepgramTranscribing: Boolean = false,
    val deepgramStatusMessage: String? = null,
    val isElevenLabsSpeaking: Boolean = false,
    val elevenLabsStatusMessage: String? = null,
    val elevenLabsVoices: List<ElevenLabsVoiceProfile> = VoiceApisClient.CURATED_VOICES,
    val isFetchingVoices: Boolean = false,

    val statusBanner: String? = null
)
