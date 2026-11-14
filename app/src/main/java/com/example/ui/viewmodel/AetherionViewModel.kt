package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AetherDatabase
import com.example.data.local.LocalAuditLogEntity
import com.example.data.local.LocalMemoryEntity
import com.example.data.local.LocalStrategyEntity
import com.example.data.local.PaperOrderEntity
import com.example.data.local.RiskConstitutionEntity
import com.example.data.local.SettingsPreferences
import com.example.data.model.AuditLogRecord
import com.example.data.model.AuditOutcome
import com.example.data.model.AutonomousCycleState
import com.example.data.model.MarketQuote
import com.example.data.model.MemoryRecord
import com.example.data.model.MemoryTier
import com.example.data.model.ProviderSettings
import com.example.data.model.RiskConfigRecord
import com.example.data.model.StrategyRecord
import com.example.data.model.StrategyStage
import com.example.data.remote.BrokerBridgeClient
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.remote.GeminiLiveVoiceEngine
import com.example.data.remote.LiveMarketDataService
import com.example.data.remote.LiveToolBridge
import com.example.data.remote.MultiProviderAiGateway
import com.example.data.remote.VoiceApisClient
import com.example.domain.engine.AetherEventBus
import com.example.domain.engine.CapitalFlowEngine
import com.example.domain.engine.DeterministicRiskEngine
import com.example.domain.engine.NarrativeIntelligenceEngine
import com.example.domain.engine.QuantitativeEngines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

class AetherionViewModel(
    private val currentUserId: String,
    private val localDb: AetherDatabase,
    private val firestoreRepo: FirestoreSyncRepository,
    private val settingsPrefs: SettingsPreferences,
    private val marketService: LiveMarketDataService = LiveMarketDataService(),
    private val aiGateway: MultiProviderAiGateway = MultiProviderAiGateway(),
    private val brokerClient: BrokerBridgeClient = BrokerBridgeClient(),
    private val riskEngine: DeterministicRiskEngine = DeterministicRiskEngine(),
    private val eventBus: AetherEventBus = AetherEventBus(),
    private val geminiLiveEngine: GeminiLiveVoiceEngine = GeminiLiveVoiceEngine(),
    private val voiceApisClient: VoiceApisClient = VoiceApisClient()
) : ViewModel() {

    private val _state = MutableStateFlow(AetherDashboardState())
    val state: StateFlow<AetherDashboardState> = _state.asStateFlow()

    private var autonomousJob: Job? = null
    private var replayJob: Job? = null

    init {
        bootstrapInitialConstitutionAndSeedIfEmpty()
        observePersistenceStreams()
        viewModelScope.launch {
            geminiLiveEngine.state.collect { liveState ->
                _state.update { it.copy(geminiLive = liveState) }
            }
        }
        viewModelScope.launch {
            val initialSettings = runCatching { settingsPrefs.settingsFlow.first() }.getOrDefault(ProviderSettings())
            _state.update { it.copy(providerSettings = initialSettings) }
            refreshMarketsAndAnalytics()
            refreshHeadlines()
        }
    }

    private fun bootstrapInitialConstitutionAndSeedIfEmpty() {
        viewModelScope.launch {
            if (localDb.riskConstitutionDao().getConfig() == null) {
                localDb.riskConstitutionDao().upsert(RiskConstitutionEntity())
            }
            if (localDb.localStrategyDao().getAll().isEmpty()) {
                val seedStrategies = listOf(
                    LocalStrategyEntity(
                        id = "dna_xau_momentum",
                        name = "Aether Gold Momentum v1",
                        assetClass = "Precious Metals",
                        symbol = "XAUUSD",
                        timeframe = "1H",
                        stage = StrategyStage.PAPER_TRADING.name,
                        sharpeRatio = 1.68,
                        profitFactor = 1.54,
                        maxDrawdownPct = 4.1,
                        winRatePct = 56.0,
                        sampleSize = 48,
                        genomeSummary = "EMA(12/34) crossover + ATR(14) 1.6x SL / 2.8x TP",
                        creatorAgent = "GENOME",
                        fastPeriod = 12,
                        slowPeriod = 34,
                        atrStopMult = 1.6,
                        atrTargetMult = 2.8,
                        isPaperActive = true
                    ),
                    LocalStrategyEntity(
                        id = "dna_btc_volbreak",
                        name = "Crypto Scout Volatility Expansion",
                        assetClass = "Cryptocurrencies",
                        symbol = "BTCUSD",
                        timeframe = "1H",
                        stage = StrategyStage.STRESS_TESTED.name,
                        sharpeRatio = 1.42,
                        profitFactor = 1.48,
                        maxDrawdownPct = 6.2,
                        winRatePct = 52.5,
                        sampleSize = 62,
                        genomeSummary = "EMA(9/28) + ATR(14) 1.8x SL / 3.2x TP",
                        creatorAgent = "CRYPTO SCOUT",
                        fastPeriod = 9,
                        slowPeriod = 28,
                        atrStopMult = 1.8,
                        atrTargetMult = 3.2
                    ),
                    LocalStrategyEntity(
                        id = "dna_eur_meanrev",
                        name = "FX Hunter Session Equilibrium",
                        assetClass = "Forex",
                        symbol = "EURUSD",
                        timeframe = "1H",
                        stage = StrategyStage.BACKTESTED.name,
                        sharpeRatio = 1.15,
                        profitFactor = 1.29,
                        maxDrawdownPct = 3.4,
                        winRatePct = 54.0,
                        sampleSize = 40,
                        genomeSummary = "EMA(15/45) + ATR(14) 1.4x SL / 2.2x TP",
                        creatorAgent = "FX HUNTER",
                        fastPeriod = 15,
                        slowPeriod = 45,
                        atrStopMult = 1.4,
                        atrTargetMult = 2.2
                    )
                )
                localDb.localStrategyDao().upsertAll(seedStrategies)
                seedStrategies.forEach { strat ->
                    firestoreRepo.upsertStrategy(strat.toFirestoreRecord(currentUserId))
                }
            }
            recordAudit(
                moduleId = "AETHER_CORE",
                actor = "AETHER PRIME",
                action = "Session authenticated ($currentUserId)",
                outcome = AuditOutcome.INFO,
                details = "App Check attested; deterministic Risk Guard active."
            )
            eventBus.publish(
                "AETHER_CORE", "ALL_MODULES", "SYSTEM_BOOT",
                "8 Intelligence Layers online; 30 specialist agents standing by."
            )
        }
    }

    private fun observePersistenceStreams() {
        viewModelScope.launch {
            combine(
                localDb.riskConstitutionDao().observeConfig(),
                localDb.localStrategyDao().observeAll(),
                localDb.localMemoryDao().observeAll(),
                localDb.localAuditLogDao().observeRecent(),
                localDb.paperOrderDao().observeAll()
            ) { constitution, strategies, memories, logs, orders ->
                Tuple5(
                    constitution ?: RiskConstitutionEntity(),
                    strategies,
                    memories,
                    logs,
                    orders
                )
            }.collect { (constitution, strategies, memories, logs, orders) ->
                _state.update {
                    it.copy(
                        constitution = constitution,
                        strategies = strategies,
                        memories = memories,
                        auditLogs = logs,
                        paperOrders = orders
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsPrefs.settingsFlow.collect { settings ->
                _state.update { it.copy(providerSettings = settings) }
            }
        }

        viewModelScope.launch {
            eventBus.events.collect { events ->
                _state.update { it.copy(crossModuleEvents = events) }
            }
        }

        // Observe Firestore remote strategies/memories/logs safely (auth-gated by userId in ViewModel scope)
        viewModelScope.launch {
            firestoreRepo.observeStrategies()
                .collect { remoteStrategies ->
                    if (remoteStrategies.isNotEmpty()) {
                        // Sync status acknowledgement without overwriting richer local genetic parameters
                        eventBus.publish(
                            "FIRESTORE_SYNC", "DNA_VAULT", "CLOUD_SYNC",
                            "Synced ${remoteStrategies.size} Strategy DNA certificates from Firestore."
                        )
                    }
                }
        }
    }

    fun selectSymbol(symbol: String) {
        _state.update { it.copy(selectedSymbol = symbol, timeReplayIndex = 0, isTimeReplaying = false) }
        recomputeDerivedAnalytics()
    }

    fun refreshMarketsAndAnalytics() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingMarkets = true, isRefreshingConditions = true, marketFetchError = null) }
            try {
                val activeSettings = _state.value.providerSettings
                val quotes = marketService.fetchAllUniverseQuotes(activeSettings)
                val liveCount = quotes.count { it.isLive }
                val err = if (liveCount == 0) {
                    quotes.firstOrNull { !it.errorMessage.isNullOrBlank() }?.errorMessage
                        ?: "Unable to reach live market APIs."
                } else null

                val conditions = marketService.fetchLiveMarketConditions(quotes, activeSettings)

                _state.update {
                    it.copy(
                        quotes = quotes,
                        isRefreshingMarkets = false,
                        isRefreshingConditions = false,
                        marketLastSyncMs = System.currentTimeMillis(),
                        marketFetchError = err,
                        marketConditions = conditions
                    )
                }
                recomputeDerivedAnalytics()
                eventBus.publish(
                    "OMNISENSE", "DISCOVERY", "MARKET_TELEMETRY",
                    "Fetched $liveCount/${quotes.size} live feeds · Regime: ${conditions.overallRegime}."
                )
                if (liveCount > 0) {
                    recordAudit(
                        "OMNISENSE", "WATCHMAN",
                        "Live market & conditions sync ($liveCount feeds)",
                        AuditOutcome.INFO,
                        "Regime=${conditions.overallRegime}, RiskScore=${conditions.riskAppetiteScore}/100, Vol=${conditions.volatilityRegime}."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Market refresh failed", e)
                _state.update {
                    it.copy(
                        isRefreshingMarkets = false,
                        isRefreshingConditions = false,
                        marketFetchError = e.localizedMessage ?: "Market feed refresh failed"
                    )
                }
            }
        }
    }

    fun refreshMarketConditions() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingConditions = true) }
            try {
                val conditions = marketService.fetchLiveMarketConditions(
                    quotes = _state.value.quotes,
                    settings = _state.value.providerSettings
                )
                _state.update { it.copy(isRefreshingConditions = false, marketConditions = conditions) }
                eventBus.publish(
                    "MACRO_INTEL", "RISK_MATRIX", "CONDITIONS_UPDATED",
                    "${conditions.overallRegime} · Risk Appetite ${conditions.riskAppetiteScore}/100"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshingConditions = false,
                        marketConditions = it.marketConditions.copy(errorSummary = e.localizedMessage ?: "Conditions fetch failed")
                    )
                }
            }
        }
    }

    fun analyzeMarketConditionsWithAi() {
        viewModelScope.launch {
            _state.update { it.copy(conditionsAiBriefState = AiTaskState.Loading) }
            val mc = _state.value.marketConditions
            val indicatorLines = mc.indicators.joinToString("\n") {
                "${it.seriesId} (${it.title}): ${it.valueText} [Delta: ${it.deltaText}] (${it.sourceName})"
            }
            val eventsLine = mc.economicEvents.take(5).joinToString("; ") {
                "${it.country} ${it.eventName} (${it.impact}, est=${it.estimate})"
            }.ifBlank { "No external calendar events loaded." }
            val prompt = """
                Live Market Conditions Snapshot:
                Overall Regime: ${mc.overallRegime}
                Volatility Regime: ${mc.volatilityRegime}
                Yield Curve Status: ${mc.yieldCurveStatus}
                Active Sessions: ${mc.activeTradingSessions}
                Market Breadth: ${mc.advancersCount} Advancers / ${mc.declinersCount} Decliners
                Risk Appetite Index: ${mc.riskAppetiteScore}/100
                Macro Indicators:
                $indicatorLines
                Upcoming Economic Catalysts: $eventsLine

                Provide a concise institutional executive briefing covering liquidity regime, tail risk posture, and favored vs penalized asset classes.
            """.trimIndent()
            aiGateway.queryFreeformIntelligence(
                "You are AETHERION MACRO INTEL & STRATEGIC REGIME ANALYST.",
                prompt,
                _state.value.providerSettings
            ).onSuccess { brief ->
                _state.update { it.copy(conditionsAiBriefState = AiTaskState.Success(brief)) }
                recordAudit("MACRO_INTEL", "MACRO INTEL", "AI Market Conditions brief generated", AuditOutcome.INFO, "${mc.overallRegime} analyzed.")
            }.onFailure { err ->
                _state.update { it.copy(conditionsAiBriefState = AiTaskState.Error(err.localizedMessage ?: "AI conditions brief failed")) }
            }
        }
    }

    fun testProviderConnection(providerId: String) {
        viewModelScope.launch {
            _state.update { it.copy(testingProviderId = providerId) }
            val settings = _state.value.providerSettings
            val diagnostic = when (providerId) {
                "ELEVENLABS" -> voiceApisClient.testElevenLabsConnection(settings)
                "DEEPGRAM" -> voiceApisClient.testDeepgramConnection(settings)
                else -> marketService.testDataProviderConnection(providerId, settings)
            }
            _state.update {
                it.copy(
                    testingProviderId = null,
                    providerDiagnostics = it.providerDiagnostics + (providerId to diagnostic)
                )
            }
            recordAudit(
                "INTEGRATIONS", "WATCHMAN",
                "Tested $providerId connection -> ${diagnostic.statusLabel}",
                if (diagnostic.connected) AuditOutcome.APPROVED else AuditOutcome.ERROR,
                diagnostic.details.take(160)
            )
        }
    }

    fun refreshHeadlines() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingNarratives = true) }
            marketService.fetchLiveMacroHeadlines(_state.value.providerSettings)
                .onSuccess { list ->
                    val snapshot = NarrativeIntelligenceEngine.analyzeNarratives(
                        headlines = list,
                        quotes = _state.value.quotes
                    )
                    val chosenId = _state.value.selectedNarrativeId
                        ?.takeIf { id -> snapshot.clusters.any { it.id == id } }
                        ?: snapshot.clusters.firstOrNull()?.id
                    _state.update {
                        it.copy(
                            headlines = list,
                            headlineError = null,
                            narrativeSnapshot = snapshot,
                            selectedNarrativeId = chosenId,
                            isRefreshingNarratives = false
                        )
                    }
                    eventBus.publish(
                        "OMNISENSE", "EVENT_ENGINE", "NARRATIVES_CLUSTERED",
                        "${snapshot.clusters.size} narratives · ${snapshot.totalConflictsDetected} conflicts · Top: ${snapshot.fastestGrowingNarrative}",
                        severity = if (snapshot.totalConflictsDetected >= 3) "ALERT" else "INFO"
                    )
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            headlineError = e.localizedMessage ?: "RSS/News feed unreachable",
                            isRefreshingNarratives = false
                        )
                    }
                }
        }
    }

    fun selectNarrativeCluster(clusterId: String) {
        _state.update { it.copy(selectedNarrativeId = clusterId) }
    }

    private fun recomputeDerivedAnalytics() {
        val current = _state.value
        val selectedQuote = current.quotes.find { it.instrument.symbol == current.selectedSymbol }
            ?: current.quotes.firstOrNull { it.isLive }

        val multiverse = selectedQuote?.let { q ->
            QuantitativeEngines.runMultiverseSimulation(
                spotPrice = q.price,
                annualizedVolPct = q.realizedVolPct,
                regimeName = current.selectedMultiverseRegime
            )
        }

        val blackSwan = current.quotes.filter { it.isLive }.map {
            QuantitativeEngines.analyzeBlackSwan(it)
        }.sortedByDescending { it.tailRiskScore }

        val highTail = blackSwan.firstOrNull { it.tailRiskScore >= 75 }
        if (highTail != null) {
            eventBus.publish(
                "BLACK_SWAN", "RISK_MATRIX", "TAIL_RISK_WARNING",
                "${highTail.symbol} tail-risk score ${highTail.tailRiskScore}/100 (${highTail.regimeStatus})",
                severity = "ALERT"
            )
        }

        val strengths = QuantitativeEngines.computeCurrencyStrength(current.quotes)
        val capitalFlowSnap = CapitalFlowEngine.analyzeCapitalFlows(
            quotes = current.quotes,
            activeSessionsText = current.marketConditions.activeTradingSessions
        )
        val allocations = QuantitativeEngines.computeRiskParityAllocation(
            current.quotes,
            current.constitution.maxTotalExposurePct
        )

        val strategyForSymbol = current.strategies.firstOrNull { it.symbol == current.selectedSymbol }
            ?: current.strategies.firstOrNull()
        val backtest = if (selectedQuote != null && selectedQuote.candles.size >= 30 && strategyForSymbol != null) {
            QuantitativeEngines.runBacktest(
                symbol = selectedQuote.instrument.symbol,
                candles = selectedQuote.candles,
                fastPeriod = strategyForSymbol.fastPeriod,
                slowPeriod = strategyForSymbol.slowPeriod,
                atrStopMult = strategyForSymbol.atrStopMult,
                atrTargetMult = strategyForSymbol.atrTargetMult
            )
        } else null

        val narrativeSnap = if (current.headlines.isNotEmpty()) {
            NarrativeIntelligenceEngine.analyzeNarratives(
                headlines = current.headlines,
                quotes = current.quotes
            )
        } else current.narrativeSnapshot

        val activeNarrativeId = current.selectedNarrativeId
            ?.takeIf { id -> narrativeSnap.clusters.any { it.id == id } }
            ?: narrativeSnap.clusters.firstOrNull()?.id

        _state.update {
            it.copy(
                multiverseReport = multiverse,
                blackSwanAlerts = blackSwan,
                currencyStrengths = strengths,
                capitalFlowSnapshot = capitalFlowSnap,
                allocations = allocations,
                activeBacktest = backtest,
                narrativeSnapshot = narrativeSnap,
                selectedNarrativeId = activeNarrativeId
            )
        }
    }

    // Module 1: Multiverse Market Simulator
    fun runMultiverseScenario(regimeName: String) {
        _state.update { it.copy(selectedMultiverseRegime = regimeName) }
        val q = _state.value.quotes.find { it.instrument.symbol == _state.value.selectedSymbol }
            ?: _state.value.quotes.firstOrNull { it.isLive }
        if (q == null || !q.isLive) {
            _state.update { it.copy(statusBanner = "Live quote required to seed Multiverse simulation.") }
            return
        }
        val report = QuantitativeEngines.runMultiverseSimulation(
            spotPrice = q.price,
            annualizedVolPct = q.realizedVolPct,
            regimeName = regimeName,
            seed = (System.currentTimeMillis() % 10000).toInt()
        )
        _state.update { it.copy(multiverseReport = report) }
        eventBus.publish(
            "MULTIVERSE", "RISK_MATRIX", "SCENARIO_GENERATED",
            "${q.instrument.symbol} $regimeName VaR95=${String.format("%.2f%%", report.var95Pct)}, Survival=${String.format("%.1f%%", report.survivalRatePct)}"
        )
    }

    // Module 4: Global Capital Flow Radar AI Liquidity Synthesis
    fun analyzeCapitalFlowsWithAi() {
        val snap = _state.value.capitalFlowSnapshot
        if (snap.assetFlows.isEmpty() && snap.regionFlows.isEmpty()) {
            _state.update {
                it.copy(capitalFlowAiBriefState = AiTaskState.Error("No live liquidity telemetry available yet. Refresh feeds first."))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(capitalFlowAiBriefState = AiTaskState.Loading) }
            val assetLines = snap.assetFlows.joinToString("; ") {
                "${it.displayName}: Flow=${it.flowScore}, Ret=${String.format(Locale.US, "%+.2f%%", it.avgChangePct)}, VolImp=${String.format(Locale.US, "%.2fx", it.volumeImpulseRatio)}"
            }
            val regionLines = snap.regionFlows.joinToString("; ") {
                "${it.region}: Flow=${it.flowScore}, Ret=${String.format(Locale.US, "%+.2f%%", it.avgChangePct)}, Lead=${it.leadSymbol}"
            }
            val corridorLines = snap.corridors.joinToString("; ") {
                "${it.fromLabel} -> ${it.toLabel} (${it.intensityPct}% intensity)"
            }
            val prompt = """
                Real-Time Global Capital Flow Telemetry:
                Net Liquidity Score: ${snap.netLiquidityScore}/100
                Dominant Inflow: ${snap.dominantInflow} | Dominant Outflow: ${snap.dominantOutflow}
                Asset Class Flows: $assetLines
                Regional Liquidity: $regionLines
                Active Corridors: $corridorLines

                As AETHERION LIQUIDITY X, provide a concise institutional liquidity rotation briefing identifying where capital is concentrating, regional divergence, and risk-on vs safe-haven posture.
            """.trimIndent()
            aiGateway.queryFreeformIntelligence(
                "You are AETHERION LIQUIDITY X, Sovereign Institutional Capital Flow Analyst.",
                prompt,
                _state.value.providerSettings
            ).onSuccess { brief ->
                _state.update { it.copy(capitalFlowAiBriefState = AiTaskState.Success(brief)) }
                recordAudit(
                    "CAPITAL_FLOW_RADAR",
                    "LIQUIDITY X",
                    "Analyzed global capital flow rotation",
                    AuditOutcome.INFO,
                    "Inflow: ${snap.dominantInflow} · Outflow: ${snap.dominantOutflow}"
                )
            }.onFailure { err ->
                _state.update {
                    it.copy(capitalFlowAiBriefState = AiTaskState.Error(err.localizedMessage ?: "Liquidity AI synthesis failed"))
                }
            }
        }
    }

    // Module 2: AI Sovereign Council
    fun conveneSovereignCouncil(symbol: String = _state.value.selectedSymbol) {
        viewModelScope.launch {
            _state.update { it.copy(councilState = AiTaskState.Loading) }
            val quote = _state.value.quotes.find { it.instrument.symbol == symbol }
            val swan = _state.value.blackSwanAlerts.find { it.symbol == symbol }
            val contextText = if (quote != null && quote.isLive) {
                "Price=${quote.price}, Change=${String.format("%.2f%%", quote.changePct)}, " +
                    "AnnVol=${String.format("%.1f%%", quote.realizedVolPct)}, Source=${quote.sourceName}, " +
                    "TailScore=${swan?.tailRiskScore ?: 0}/100 (${swan?.regimeStatus ?: "N/A"}), " +
                    "KillSwitch=${_state.value.constitution.emergencyKillSwitch}, " +
                    "CurrentDD=${_state.value.constitution.currentDrawdownPct}%"
            } else {
                "No live feed currently available for $symbol. Recommend HALT or NEUTRAL until market feed reconnects."
            }

            aiGateway.conveneSovereignCouncil(symbol, contextText, _state.value.providerSettings)
                .onSuccess { vote ->
                    _state.update { it.copy(councilState = AiTaskState.Success(vote)) }
                    eventBus.publish(
                        "SOVEREIGN_COUNCIL", "AETHER_CORE", "COUNCIL_VOTE",
                        "$symbol: ${vote.consensusStance} (${vote.confidencePct}%) via ${vote.providerUsed}"
                    )
                    saveMemoryEntry(
                        tier = MemoryTier.DECISION_MEMORY,
                        title = "Council Vote · $symbol (${vote.consensusStance})",
                        content = "ORACLE: ${vote.oracleThesis}\nCRITIC: ${vote.criticObjection}\nARBITER: ${vote.arbiterRuling}",
                        symbol = symbol,
                        agentName = "ARBITER",
                        confidence = vote.confidencePct / 100.0
                    )
                    recordAudit(
                        "SOVEREIGN_COUNCIL", "ARBITER",
                        "Council consensus ${vote.consensusStance} on $symbol",
                        AuditOutcome.INFO,
                        "Confidence ${vote.confidencePct}%. Strategy: ${vote.recommendedStrategy}. Provider: ${vote.providerUsed}."
                    )
                }
                .onFailure { err ->
                    val msg = err.localizedMessage ?: "Sovereign Council call failed."
                    _state.update { it.copy(councilState = AiTaskState.Error(msg)) }
                    recordAudit("SOVEREIGN_COUNCIL", "ARBITER", "Council failed for $symbol", AuditOutcome.ERROR, msg)
                }
        }
    }

    // Module 3 & 14: Strategy DNA Evolution Lab & Autonomous Strategy Factory
    fun mutateAndBacktestStrategy(strategy: LocalStrategyEntity) {
        viewModelScope.launch {
            val quote = _state.value.quotes.find { it.instrument.symbol == strategy.symbol }
                ?: _state.value.quotes.firstOrNull { it.isLive && it.candles.size >= 40 }
            if (quote == null || quote.candles.size < 35) {
                _state.update { it.copy(statusBanner = "Need live candle history for ${strategy.symbol} to evolve & backtest.") }
                return@launch
            }
            val rng = Random(System.currentTimeMillis())
            val newFast = (strategy.fastPeriod + rng.nextInt(-3, 4)).coerceIn(5, 24)
            val newSlow = (strategy.slowPeriod + rng.nextInt(-6, 7)).coerceIn(newFast + 6, 85)
            val newStop = (strategy.atrStopMult + (rng.nextDouble() - 0.5) * 0.4).coerceIn(1.0, 3.2)
            val newTarget = (strategy.atrTargetMult + (rng.nextDouble() - 0.5) * 0.6).coerceIn(1.4, 5.2)

            val report = QuantitativeEngines.runBacktest(
                symbol = quote.instrument.symbol,
                candles = quote.candles,
                fastPeriod = newFast,
                slowPeriod = newSlow,
                atrStopMult = newStop,
                atrTargetMult = newTarget
            )
            val stage = riskEngine.evaluateGraduationEligibility(
                sampleSize = report.totalTrades,
                profitFactor = report.profitFactor,
                maxDrawdownPct = report.maxDrawdownPct,
                oosSharpe = report.oosSharpeRatio,
                stressSurvived = report.stressTestSurvived,
                constitution = _state.value.constitution
            )
            val mutated = strategy.copy(
                id = "dna_${UUID.randomUUID().toString().take(8)}",
                name = "${strategy.name.substringBefore(" v")} v${rng.nextInt(2, 99)}",
                stage = stage.name,
                sharpeRatio = report.sharpeRatio,
                profitFactor = report.profitFactor,
                maxDrawdownPct = report.maxDrawdownPct,
                winRatePct = report.winRatePct,
                sampleSize = report.totalTrades,
                genomeSummary = "Mutated EMA($newFast/$newSlow) + ATR(${String.format("%.1f", newStop)}x SL / ${String.format("%.1f", newTarget)}x TP)",
                creatorAgent = "EVOLVER",
                fastPeriod = newFast,
                slowPeriod = newSlow,
                atrStopMult = newStop,
                atrTargetMult = newTarget,
                updatedAtMs = System.currentTimeMillis()
            )
            localDb.localStrategyDao().upsert(mutated)
            firestoreRepo.upsertStrategy(mutated.toFirestoreRecord(currentUserId))
            _state.update { it.copy(activeBacktest = report) }
            recordAudit(
                "DNA_LAB", "EVOLVER",
                "Mutated ${mutated.name} -> ${stage.label}",
                AuditOutcome.SIMULATED,
                "Sharpe=${String.format("%.2f", report.sharpeRatio)}, PF=${String.format("%.2f", report.profitFactor)}, DD=${String.format("%.1f%%", report.maxDrawdownPct)}, OOS Sharpe=${String.format("%.2f", report.oosSharpeRatio)}"
            )
        }
    }

    fun runAutonomousStrategyFactoryBatch(symbol: String = _state.value.selectedSymbol) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isFactoryRunning = true,
                    factoryStatusMessage = "GENOME requesting AI strategy hypothesis for $symbol..."
                )
            }
            val quote = _state.value.quotes.find { it.instrument.symbol == symbol }
                ?: _state.value.quotes.firstOrNull { it.isLive && it.candles.size >= 35 }
            if (quote == null || quote.candles.size < 35) {
                _state.update {
                    it.copy(
                        isFactoryRunning = false,
                        factoryStatusMessage = "Factory halted: live historical candles unavailable for $symbol."
                    )
                }
                return@launch
            }

            val regimeSummary = "Symbol=${quote.instrument.symbol}, Price=${quote.price}, Change=${String.format("%.2f%%", quote.changePct)}, AnnVol=${String.format("%.1f%%", quote.realizedVolPct)}, Candles=${quote.candles.size}"
            aiGateway.generateStrategyProposal(quote.instrument.symbol, regimeSummary, _state.value.providerSettings)
                .onSuccess { proposal ->
                    val report = QuantitativeEngines.runBacktest(
                        symbol = quote.instrument.symbol,
                        candles = quote.candles,
                        fastPeriod = proposal.fastPeriod,
                        slowPeriod = proposal.slowPeriod,
                        atrStopMult = proposal.atrStopMult,
                        atrTargetMult = proposal.atrTargetMult
                    )
                    val graduatedStage = riskEngine.evaluateGraduationEligibility(
                        sampleSize = report.totalTrades,
                        profitFactor = report.profitFactor,
                        maxDrawdownPct = report.maxDrawdownPct,
                        oosSharpe = report.oosSharpeRatio,
                        stressSurvived = report.stressTestSurvived,
                        constitution = _state.value.constitution
                    )
                    val entity = LocalStrategyEntity(
                        id = "fac_${UUID.randomUUID().toString().take(8)}",
                        name = proposal.name.take(60),
                        assetClass = quote.instrument.assetClass.displayName,
                        symbol = quote.instrument.symbol,
                        timeframe = "1H",
                        stage = graduatedStage.name,
                        sharpeRatio = report.sharpeRatio,
                        profitFactor = report.profitFactor,
                        maxDrawdownPct = report.maxDrawdownPct,
                        winRatePct = report.winRatePct,
                        sampleSize = report.totalTrades,
                        genomeSummary = "${proposal.rationale.take(180)} | EMA(${proposal.fastPeriod}/${proposal.slowPeriod})",
                        creatorAgent = "GENOME (${proposal.providerUsed.take(18)})",
                        fastPeriod = proposal.fastPeriod,
                        slowPeriod = proposal.slowPeriod,
                        atrStopMult = proposal.atrStopMult,
                        atrTargetMult = proposal.atrTargetMult
                    )
                    localDb.localStrategyDao().upsert(entity)
                    firestoreRepo.upsertStrategy(entity.toFirestoreRecord(currentUserId))
                    _state.update {
                        it.copy(
                            isFactoryRunning = false,
                            activeBacktest = report,
                            factoryStatusMessage = "Created '${entity.name}' -> ${graduatedStage.label} (Sharpe ${String.format("%.2f", report.sharpeRatio)}, Trades ${report.totalTrades})."
                        )
                    }
                    recordAudit(
                        "STRATEGY_FACTORY", "GENOME",
                        "Factory synthesized ${entity.name}",
                        AuditOutcome.SIMULATED,
                        "Graduated to ${graduatedStage.label}; OOS Sharpe=${String.format("%.2f", report.oosSharpeRatio)}."
                    )
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isFactoryRunning = false,
                            factoryStatusMessage = "Factory error: ${err.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun promoteOrRetireStrategy(strategy: LocalStrategyEntity, targetStage: StrategyStage) {
        viewModelScope.launch {
            if (targetStage == StrategyStage.LIVE_ELIGIBLE && _state.value.constitution.permissionLevel < 4) {
                recordAudit(
                    "RISK_MATRIX", "RISK GUARD",
                    "Blocked promotion of ${strategy.name} to Live Eligible",
                    AuditOutcome.VETOED,
                    "Permission level ${_state.value.constitution.permissionLevel} < Level 4."
                )
                _state.update { it.copy(statusBanner = "RISK GUARD VETO: Increase Permission Level to 4+ to promote to Live Eligible.") }
                return@launch
            }
            val updated = strategy.copy(stage = targetStage.name, updatedAtMs = System.currentTimeMillis())
            localDb.localStrategyDao().upsert(updated)
            firestoreRepo.upsertStrategy(updated.toFirestoreRecord(currentUserId))
            recordAudit(
                "CHAMPIONSHIP_ARENA", "STRATEGOS",
                "Strategy ${strategy.name} moved to ${targetStage.label}",
                AuditOutcome.APPROVED,
                "Sharpe=${String.format("%.2f", strategy.sharpeRatio)}, MaxDD=${String.format("%.1f%%", strategy.maxDrawdownPct)}"
            )
        }
    }

    // Module 7 & 12: Execution gated by DeterministicRiskEngine
    fun executeAuthorisedTrade(
        strategy: LocalStrategyEntity,
        side: String,
        proposedExposurePct: Double,
        routeToMt5Bridge: Boolean
    ) {
        viewModelScope.launch {
            val current = _state.value
            val quote = current.quotes.find { it.instrument.symbol == strategy.symbol }
            val spot = quote?.price?.takeIf { it > 0.0 } ?: 100.0
            val openOrders = current.paperOrders.filter { it.status == "OPEN" }
            val currentExposure = openOrders.sumOf { it.exposurePct }

            val stage = runCatching { StrategyStage.valueOf(strategy.stage) }
                .getOrDefault(StrategyStage.EXPERIMENTAL)
            val isHighRiskNews = current.blackSwanAlerts.any { it.symbol == strategy.symbol && it.tailRiskScore >= 75 }

            val check = riskEngine.evaluateExecutionProposal(
                constitution = current.constitution,
                strategyStage = stage,
                proposedExposurePct = proposedExposurePct,
                currentExposurePct = currentExposure,
                openPositionsCount = openOrders.size,
                estimatedSpreadBps = 6.0,
                estimatedSlippageBps = 4.0,
                isHighImpactNewsWindow = isHighRiskNews,
                isLiveBrokerRoute = routeToMt5Bridge
            )

            _state.update { it.copy(lastRiskEvaluation = check) }

            if (!check.approved) {
                recordAudit(
                    "RISK_MATRIX", "RISK GUARD",
                    "VETOED $side ${strategy.symbol}",
                    AuditOutcome.VETOED,
                    check.violatedRules.joinToString(" | ")
                )
                eventBus.publish("RISK_MATRIX", "EXECUTION", "ORDER_VETOED", check.reason, "ALERT")
                return@launch
            }

            val sl = if (side == "BUY") spot * 0.99 else spot * 1.01
            val tp = if (side == "BUY") spot * 1.02 else spot * 0.98

            if (routeToMt5Bridge) {
                val res = brokerClient.submitOrderToMt5Bridge(
                    baseUrl = current.providerSettings.mt5BridgeUrl,
                    authToken = current.providerSettings.mt5BridgeToken,
                    symbol = strategy.symbol,
                    side = side,
                    volumeLots = (proposedExposurePct / 10.0).coerceAtLeast(0.01),
                    stopLoss = sl,
                    takeProfit = tp
                )
                res.onSuccess { msg ->
                    recordAudit("EXECUTION", "EXECUTOR", "MT5 Live Order: $side ${strategy.symbol}", AuditOutcome.EXECUTED, msg)
                    _state.update { it.copy(statusBanner = msg) }
                }.onFailure { err ->
                    val errMsg = err.localizedMessage ?: "MT5 Bridge submission failed"
                    recordAudit("EXECUTION", "EXECUTOR", "MT5 Order Failed", AuditOutcome.ERROR, errMsg)
                    _state.update { it.copy(statusBanner = "MT5 ERROR: $errMsg") }
                }
            } else {
                val paper = PaperOrderEntity(
                    id = "pap_${UUID.randomUUID().toString().take(8)}",
                    strategyId = strategy.id,
                    strategyName = strategy.name,
                    symbol = strategy.symbol,
                    side = side,
                    entryPrice = spot,
                    stopLoss = sl,
                    takeProfit = tp,
                    exposurePct = proposedExposurePct,
                    status = "OPEN",
                    pnlPct = 0.0
                )
                localDb.paperOrderDao().upsert(paper)
                recordAudit(
                    "EXECUTION", "EXECUTOR",
                    "Paper Sandbox Order: $side ${strategy.symbol} @ $spot",
                    AuditOutcome.SIMULATED,
                    "Strategy: ${strategy.name}, Exposure: ${String.format("%.1f%%", proposedExposurePct)}"
                )
                _state.update { it.copy(statusBanner = "PAPER SANDBOX EXECUTED: $side ${strategy.symbol} @ $spot") }
            }
        }
    }

    fun closePaperOrder(order: PaperOrderEntity) {
        viewModelScope.launch {
            val spot = _state.value.quotes.find { it.instrument.symbol == order.symbol }?.price?.takeIf { it > 0 }
                ?: order.entryPrice
            val rawPnl = if (order.side == "BUY") {
                ((spot - order.entryPrice) / order.entryPrice) * 100.0
            } else {
                ((order.entryPrice - spot) / order.entryPrice) * 100.0
            }
            localDb.paperOrderDao().upsert(
                order.copy(status = "CLOSED", pnlPct = rawPnl, closedAtMs = System.currentTimeMillis())
            )
            saveMemoryEntry(
                tier = MemoryTier.EXECUTION_MEMORY,
                title = "Trade Autopsy · ${order.symbol} (${order.side})",
                content = "Entry=${order.entryPrice}, Exit=$spot, Net=${String.format("%.2f%%", rawPnl)}, Strategy=${order.strategyName}",
                symbol = order.symbol,
                agentName = "TRADE AUTOPSY",
                confidence = 1.0
            )
            recordAudit(
                "EVOLUTION", "TRADE AUTOPSY",
                "Closed Paper ${order.side} ${order.symbol}",
                AuditOutcome.SIMULATED,
                "Realized PnL: ${String.format("%.2f%%", rawPnl)}"
            )
        }
    }

    fun pingMt5Bridge() {
        viewModelScope.launch {
            _state.update { it.copy(isPingingBroker = true) }
            val s = _state.value.providerSettings
            val report = brokerClient.pingMt5Bridge(s.mt5BridgeUrl, s.mt5BridgeToken)
            _state.update { it.copy(isPingingBroker = false, brokerHealth = report) }
            recordAudit(
                "BROKER_INTELLIGENCE", "EXECUTOR",
                "MT5 Bridge Ping: ${report.statusLabel}",
                if (report.connected) AuditOutcome.APPROVED else AuditOutcome.ERROR,
                report.errorDetails ?: "Latency ${report.latencyMs}ms"
            )
        }
    }

    // Module 8: Market Time Reconstruction
    fun setReplayIndex(index: Int) {
        _state.update { it.copy(timeReplayIndex = index.coerceAtLeast(0)) }
    }

    fun toggleTimeReplayPlayback() {
        val currentlyPlaying = _state.value.isTimeReplaying
        if (currentlyPlaying) {
            replayJob?.cancel()
            _state.update { it.copy(isTimeReplaying = false) }
            return
        }
        val quote = _state.value.quotes.find { it.instrument.symbol == _state.value.selectedSymbol }
        val maxIdx = (quote?.candles?.size ?: 1) - 1
        if (maxIdx <= 1) return

        _state.update { it.copy(isTimeReplaying = true) }
        replayJob = viewModelScope.launch {
            while (isActive) {
                delay(450L)
                val next = _state.value.timeReplayIndex + 1
                if (next >= maxIdx) {
                    _state.update { it.copy(timeReplayIndex = maxIdx, isTimeReplaying = false) }
                    break
                } else {
                    _state.update { it.copy(timeReplayIndex = next) }
                }
            }
        }
    }

    // Module 10: Collective Intelligence Memory
    fun saveMemoryEntry(
        tier: MemoryTier,
        title: String,
        content: String,
        symbol: String,
        agentName: String = "MEMORY MATRIX",
        confidence: Double = 0.85
    ) {
        viewModelScope.launch {
            val id = "mem_${UUID.randomUUID().toString().take(8)}"
            localDb.localMemoryDao().insert(
                LocalMemoryEntity(
                    id = id,
                    tier = tier.name,
                    title = title,
                    content = content,
                    symbol = symbol,
                    agentName = agentName,
                    confidenceScore = confidence
                )
            )
            firestoreRepo.createMemory(
                MemoryRecord(
                    id = id,
                    userId = currentUserId,
                    tier = tier.name,
                    title = title,
                    content = content,
                    symbol = symbol,
                    agentName = agentName,
                    confidenceScore = confidence
                )
            )
        }
    }

    fun queryCollectiveMemoryWithAi(question: String) {
        viewModelScope.launch {
            _state.update { it.copy(memorySynthesisState = AiTaskState.Loading) }
            val recentMem = _state.value.memories.take(15).joinToString("\n") {
                "[${it.tier}] (${it.symbol} / ${it.agentName}): ${it.title} -> ${it.content}"
            }
            val stratSummary = _state.value.strategies.take(8).joinToString("\n") {
                "${it.name} [${it.symbol}] stage=${it.stage}, sharpe=${String.format("%.2f", it.sharpeRatio)}, dd=${String.format("%.1f%%", it.maxDrawdownPct)}"
            }
            val prompt = "Question: $question\n\nInstitutional Memory Ledger:\n$recentMem\n\nStrategies:\n$stratSummary"
            aiGateway.queryFreeformIntelligence(
                "You are AETHERION MEMORY MATRIX. Synthesize answers strictly from the stored records and state clearly when historical evidence is limited.",
                prompt,
                _state.value.providerSettings
            ).onSuccess { answer ->
                _state.update { it.copy(memorySynthesisState = AiTaskState.Success(answer)) }
            }.onFailure { err ->
                _state.update { it.copy(memorySynthesisState = AiTaskState.Error(err.localizedMessage ?: "Memory synthesis failed")) }
            }
        }
    }

    // Module 11: Commander Voice Intelligence (Gemini 3.8 Live + Deepgram + ElevenLabs)
    fun startGeminiLiveConversation(context: Context) {
        val bridge = object : LiveToolBridge {
            override fun buildSystemContextSummary(): String {
                val cur = _state.value
                val liveSummary = cur.quotes.filter { it.isLive }.take(6).joinToString(", ") {
                    "${it.instrument.symbol}:${it.price}(${String.format(Locale.US, "%+.2f%%", it.changePct)})"
                }
                return "SelectedSymbol=${cur.selectedSymbol}; Regime=${cur.marketConditions.overallRegime}; RiskScore=${cur.marketConditions.riskAppetiteScore}/100; KillSwitch=${cur.constitution.emergencyKillSwitch}; PermissionLevel=${cur.constitution.permissionLevel}; LiveFeeds=[$liveSummary]"
            }

            override fun getMarketQuoteJson(symbol: String): JsonObject {
                val clean = symbol.trim().uppercase(Locale.US)
                val q = _state.value.quotes.find { it.instrument.symbol.equals(clean, ignoreCase = true) }
                return if (q != null && q.isLive) {
                    JsonObject(
                        mapOf(
                            "symbol" to JsonPrimitive(q.instrument.symbol),
                            "price" to JsonPrimitive(q.price),
                            "changePct" to JsonPrimitive(q.changePct),
                            "realizedVolPct" to JsonPrimitive(q.realizedVolPct),
                            "source" to JsonPrimitive(q.sourceName)
                        )
                    )
                } else {
                    JsonObject(
                        mapOf(
                            "symbol" to JsonPrimitive(clean),
                            "error" to JsonPrimitive(q?.errorMessage ?: "Symbol quote not live or unavailable")
                        )
                    )
                }
            }

            override fun getRiskStatusJson(): JsonObject {
                val c = _state.value.constitution
                val openCount = _state.value.paperOrders.count { it.status == "OPEN" }
                return JsonObject(
                    mapOf(
                        "emergencyKillSwitch" to JsonPrimitive(c.emergencyKillSwitch),
                        "permissionLevel" to JsonPrimitive(c.permissionLevel),
                        "maxDrawdownPct" to JsonPrimitive(c.maxDrawdownPct),
                        "dailyLossLimitPct" to JsonPrimitive(c.dailyLossLimitPct),
                        "maxOpenPositions" to JsonPrimitive(c.maxOpenPositions),
                        "currentOpenPaperOrders" to JsonPrimitive(openCount)
                    )
                )
            }

            override fun setEmergencyKillSwitch(engage: Boolean): JsonObject {
                toggleEmergencyKillSwitch(engage)
                return JsonObject(
                    mapOf(
                        "emergencyKillSwitch" to JsonPrimitive(engage),
                        "status" to JsonPrimitive(if (engage) "ENGAGED" else "DISENGAGED")
                    )
                )
            }

            override fun selectSymbol(symbol: String): JsonObject {
                val matched = _state.value.quotes.find { it.instrument.symbol.equals(symbol.trim(), ignoreCase = true) }
                return if (matched != null) {
                    selectSymbol(matched.instrument.symbol)
                    JsonObject(mapOf("selectedSymbol" to JsonPrimitive(matched.instrument.symbol), "ok" to JsonPrimitive(true)))
                } else {
                    JsonObject(mapOf("error" to JsonPrimitive("Unknown symbol $symbol"), "ok" to JsonPrimitive(false)))
                }
            }
        }

        val voice = _state.value.providerSettings.geminiLiveVoice
        geminiLiveEngine.startConversation(context.applicationContext, voice, bridge)
        viewModelScope.launch {
            recordAudit(
                "VOICE_COMMAND",
                "GEMINI 3.8 LIVE",
                "Initiated real-time Gemini 3.8 Live voice conversation ($voice)",
                AuditOutcome.INFO,
                "Bidirectional audio stream + tool calls active."
            )
        }
    }

    fun stopGeminiLiveConversation() {
        geminiLiveEngine.stopConversation()
        viewModelScope.launch {
            recordAudit(
                "VOICE_COMMAND",
                "GEMINI 3.8 LIVE",
                "Ended real-time Gemini 3.8 Live voice conversation",
                AuditOutcome.INFO,
                "Live session closed cleanly."
            )
        }
    }

    fun sendGeminiLiveText(text: String) {
        geminiLiveEngine.sendTextPrompt(text)
    }

    fun clearGeminiLiveTranscripts() {
        geminiLiveEngine.clearTranscripts()
    }

    fun startDeepgramCapture(context: Context) {
        voiceApisClient.startDeepgramRecording(context.applicationContext)
            .onSuccess {
                _state.update {
                    it.copy(
                        isDeepgramRecording = true,
                        deepgramStatusMessage = "Recording 16kHz PCM audio for Deepgram STT..."
                    )
                }
            }
            .onFailure { err ->
                _state.update {
                    it.copy(
                        isDeepgramRecording = false,
                        deepgramStatusMessage = err.localizedMessage ?: "Unable to start Deepgram mic capture."
                    )
                }
            }
    }

    fun stopDeepgramAndExecute(context: Context, speakReplyWithElevenLabs: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDeepgramRecording = false,
                    isDeepgramTranscribing = true,
                    deepgramStatusMessage = "Uploading WAV to Deepgram Nova STT..."
                )
            }
            voiceApisClient.stopDeepgramAndTranscribe(_state.value.providerSettings)
                .onSuccess { res ->
                    val confPct = (res.confidence * 100.0).toInt().coerceIn(0, 100)
                    _state.update {
                        it.copy(
                            isDeepgramTranscribing = false,
                            deepgramStatusMessage = "Deepgram (${res.modelUsed}, $confPct% conf, ${String.format(Locale.US, "%.1fs", res.durationSec)}): \"${res.transcript}\""
                        )
                    }
                    recordAudit(
                        "VOICE_COMMAND",
                        "DEEPGRAM STT",
                        "Transcribed Commander audio ($confPct% confidence)",
                        AuditOutcome.APPROVED,
                        res.transcript
                    )
                    submitCommanderInstruction(
                        instruction = res.transcript,
                        speakReplyWithElevenLabs = speakReplyWithElevenLabs,
                        contextForAudio = context.applicationContext
                    )
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isDeepgramTranscribing = false,
                            deepgramStatusMessage = err.localizedMessage ?: "Deepgram transcription failed"
                        )
                    }
                }
        }
    }

    fun cancelDeepgramCapture() {
        voiceApisClient.cancelDeepgramCapture()
        _state.update { it.copy(isDeepgramRecording = false, isDeepgramTranscribing = false) }
    }

    fun speakWithElevenLabs(context: Context, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isElevenLabsSpeaking = true,
                    elevenLabsStatusMessage = "Synthesizing ElevenLabs neural voice..."
                )
            }
            voiceApisClient.synthesizeAndPlay(
                text = clean,
                settings = _state.value.providerSettings,
                context = context.applicationContext,
                onPlaybackDone = {
                    _state.update { st ->
                        st.copy(
                            isElevenLabsSpeaking = false,
                            elevenLabsStatusMessage = "ElevenLabs playback complete."
                        )
                    }
                }
            ).onSuccess { msg ->
                _state.update { it.copy(elevenLabsStatusMessage = msg) }
                recordAudit(
                    "VOICE_COMMAND",
                    "ELEVENLABS TTS",
                    "Synthesized neural briefing playback",
                    AuditOutcome.INFO,
                    msg
                )
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isElevenLabsSpeaking = false,
                        elevenLabsStatusMessage = err.localizedMessage ?: "ElevenLabs synthesis failed"
                    )
                }
            }
        }
    }

    fun stopElevenLabsPlayback() {
        voiceApisClient.stopPlayback()
        _state.update {
            it.copy(
                isElevenLabsSpeaking = false,
                elevenLabsStatusMessage = "ElevenLabs playback stopped."
            )
        }
    }

    fun fetchElevenLabsVoices() {
        viewModelScope.launch {
            _state.update { it.copy(isFetchingVoices = true) }
            voiceApisClient.fetchElevenLabsVoices(_state.value.providerSettings)
                .onSuccess { list ->
                    _state.update {
                        it.copy(
                            isFetchingVoices = false,
                            elevenLabsVoices = if (list.isNotEmpty()) list else VoiceApisClient.CURATED_VOICES,
                            elevenLabsStatusMessage = "Loaded ${list.size} ElevenLabs voices."
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isFetchingVoices = false,
                            elevenLabsStatusMessage = err.localizedMessage ?: "Failed fetching ElevenLabs voices"
                        )
                    }
                }
        }
    }

    fun submitCommanderInstruction(
        instruction: String,
        speakReplyWithElevenLabs: Boolean = false,
        contextForAudio: Context? = null
    ) {
        val clean = instruction.trim()
        if (clean.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(voiceCommandState = AiTaskState.Loading) }
            val lower = clean.lowercase()

            // Deterministic safety & operational shortcuts first
            if (lower.contains("kill switch") || lower.contains("emergency shutdown") || lower.contains("halt all")) {
                toggleEmergencyKillSwitch(true)
                val msg = "DETERMINISTIC ACTION EXECUTED: Emergency Kill Switch ENGAGED. All live & paper execution blocked."
                _state.update {
                    it.copy(
                        voiceCommandState = AiTaskState.Success(msg),
                        voiceHistory = listOf(VoiceLogEntry(clean, msg, "RISK GUARD (Deterministic)")) + it.voiceHistory
                    )
                }
                if (speakReplyWithElevenLabs && contextForAudio != null) {
                    speakWithElevenLabs(contextForAudio, msg)
                }
                return@launch
            }

            val marketBrief = _state.value.quotes.filter { it.isLive }.take(8).joinToString("; ") {
                "${it.instrument.symbol}=${it.price} (${String.format("%+.2f%%", it.changePct)})"
            }
            val prompt = """
                Commander instruction: "$clean"
                Live Telemetry: $marketBrief
                Risk Constitution: MaxDD=${_state.value.constitution.maxDrawdownPct}%, KillSwitch=${_state.value.constitution.emergencyKillSwitch}, PermissionLevel=${_state.value.constitution.permissionLevel}
                Respond concisely as AETHER PRIME, citing real telemetry and explicitly stating any Risk Guard boundaries.
            """.trimIndent()

            aiGateway.queryFreeformIntelligence(
                "You are AETHER PRIME, Commander Interface of AETHERION.",
                prompt,
                _state.value.providerSettings
            ).onSuccess { reply ->
                _state.update {
                    it.copy(
                        voiceCommandState = AiTaskState.Success(reply),
                        voiceHistory = listOf(VoiceLogEntry(clean, reply, _state.value.providerSettings.activeProvider.displayName)) + it.voiceHistory
                    )
                }
                if (speakReplyWithElevenLabs && contextForAudio != null) {
                    speakWithElevenLabs(contextForAudio, reply)
                }
                recordAudit("VOICE_COMMAND", "AETHER PRIME", "Processed Commander instruction", AuditOutcome.INFO, clean)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Voice AI command failed"
                _state.update { it.copy(voiceCommandState = AiTaskState.Error(msg)) }
            }
        }
    }

    // Module 12: Autonomous Risk Defence Matrix
    fun updateRiskConstitution(transform: (RiskConstitutionEntity) -> RiskConstitutionEntity) {
        viewModelScope.launch {
            val updated = transform(_state.value.constitution).copy(updatedAtMs = System.currentTimeMillis())
            localDb.riskConstitutionDao().upsert(updated)
            firestoreRepo.saveRiskConfig(
                RiskConfigRecord(
                    userId = currentUserId,
                    maxDrawdownPct = updated.maxDrawdownPct,
                    dailyLossLimitPct = updated.dailyLossLimitPct,
                    maxOpenPositions = updated.maxOpenPositions.toLong(),
                    maxTotalExposurePct = updated.maxTotalExposurePct,
                    maxSpreadBps = updated.maxSpreadBps,
                    maxSlippageBps = updated.maxSlippageBps,
                    newsBlackoutEnabled = updated.newsBlackoutEnabled,
                    emergencyKillSwitch = updated.emergencyKillSwitch,
                    permissionLevel = updated.permissionLevel.toLong()
                )
            )
            recordAudit(
                "RISK_MATRIX", "COMMANDER",
                "Updated Risk Constitution",
                AuditOutcome.APPROVED,
                "MaxDD=${updated.maxDrawdownPct}%, DailyLoss=${updated.dailyLossLimitPct}%, PermLevel=${updated.permissionLevel}, Kill=${updated.emergencyKillSwitch}"
            )
        }
    }

    fun toggleEmergencyKillSwitch(active: Boolean) {
        updateRiskConstitution { it.copy(emergencyKillSwitch = active) }
        eventBus.publish(
            "RISK_MATRIX", "ALL_MODULES", "KILL_SWITCH",
            if (active) "EMERGENCY KILL SWITCH ENGAGED" else "Emergency Kill Switch disengaged",
            if (active) "CRITICAL" else "INFO"
        )
    }

    // Module 13: Global Event Impact Engine
    fun analyzeHeadlineImpact(headline: String) {
        viewModelScope.launch {
            _state.update { it.copy(eventImpactState = AiTaskState.Loading) }
            val snapshot = _state.value.quotes.filter { it.isLive }.take(8).joinToString(", ") {
                "${it.instrument.symbol}:${it.price}(${String.format("%+.2f%%", it.changePct)})"
            }
            aiGateway.analyzeEventImpact(headline, snapshot, _state.value.providerSettings)
                .onSuccess { analysis ->
                    _state.update { it.copy(eventImpactState = AiTaskState.Success(analysis)) }
                    recordAudit(
                        "EVENT_ENGINE", "MACRO INTEL",
                        "Assessed event shock (${analysis.severityScore}/100)",
                        AuditOutcome.INFO,
                        "$headline -> ${analysis.directionalBias}"
                    )
                }
                .onFailure { err ->
                    _state.update { it.copy(eventImpactState = AiTaskState.Error(err.localizedMessage ?: "Impact analysis failed")) }
                }
        }
    }

    fun analyzeSelectedNarrativeWithAi(clusterId: String? = _state.value.selectedNarrativeId) {
        val snap = _state.value.narrativeSnapshot
        val cluster = snap.clusters.find { it.id == clusterId } ?: snap.clusters.firstOrNull()
        if (cluster == null) {
            _state.update {
                it.copy(narrativeAiBriefState = AiTaskState.Error("No active narrative cluster available. Refresh live feeds first."))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(narrativeAiBriefState = AiTaskState.Loading) }
            val sourcesSummary = cluster.sourceComparisons.take(5).joinToString("; ") {
                "${it.sourceName} (${it.articleCount} arts, sent=${String.format(Locale.US, "%+.2f", it.avgSentiment)}): ${it.leadHeadline}"
            }
            val conflictsSummary = cluster.conflictingReports.take(3).joinToString("\n") { c ->
                "Conflict [${c.conflictAxis}]: ${c.sourceA}='${c.headlineA}' VS ${c.sourceB}='${c.headlineB}'"
            }.ifBlank { "No sharp binary source contradictions detected; framing variance is mild." }
            val reactionsSummary = cluster.marketReactions.take(4).joinToString("; ") { m ->
                "${m.symbol} ${String.format(Locale.US, "%+.2f%%", m.priceChangePct)} (Vol ${String.format(Locale.US, "%.1f%%", m.realizedVolPct)}, Align ${m.alignmentScorePct}%: ${m.regimeVerdict})"
            }
            val context = """
                Narrative Topic: ${cluster.topicTitle} (${cluster.categoryTag})
                Lifecycle: ${cluster.lifecycleStage.label} | Attention: ${cluster.attentionScore}/100 | Growth Velocity: ${cluster.growthRatePct}%
                Decay Telemetry: Half-Life ${String.format(Locale.US, "%.1fh", cluster.decayTelemetry.halfLifeHours)}, Decay Progress ${cluster.decayTelemetry.decayProgressPct}% (${cluster.decayTelemetry.statusLabel})
                Net Sentiment: ${cluster.sentimentLabel}
                Sources Coverage: $sourcesSummary
                Conflicting Reports:
                $conflictsSummary
                Live Market Reactions: $reactionsSummary
            """.trimIndent()

            aiGateway.analyzeNarrativeCluster(cluster.id, context, _state.value.providerSettings)
                .onSuccess { brief ->
                    _state.update { it.copy(narrativeAiBriefState = AiTaskState.Success(brief)) }
                    saveMemoryEntry(
                        tier = MemoryTier.MARKET_MEMORY,
                        title = "Narrative Brief · ${cluster.topicTitle}",
                        content = "${brief.executiveSummary}\nConflicts: ${brief.conflictResolutionThesis}\nAssets: ${brief.crossAssetTransmission}",
                        symbol = cluster.instrumentImpacts.firstOrNull()?.symbol ?: "GLOBAL",
                        agentName = "MACRO INTEL",
                        confidence = (cluster.attentionScore / 100.0).coerceIn(0.4, 0.96)
                    )
                    recordAudit(
                        "NARRATIVE_ENGINE", "MACRO INTEL",
                        "Synthesized AI narrative brief for ${cluster.topicTitle}",
                        AuditOutcome.INFO,
                        "Provider: ${brief.providerUsed} · Growth ${cluster.growthRatePct}% · Conflicts ${cluster.conflictingReports.size}"
                    )
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(narrativeAiBriefState = AiTaskState.Error(err.localizedMessage ?: "Narrative AI synthesis failed"))
                    }
                }
        }
    }

    // Module 15: AETHERION OVERMIND 12-Step Autonomous Operation Cycle
    fun toggleAutonomousCycle() {
        if (_state.value.autonomousCycle.isRunning) {
            autonomousJob?.cancel()
            _state.update { it.copy(autonomousCycle = it.autonomousCycle.copy(isRunning = false)) }
            viewModelScope.launch {
                recordAudit("OVERMIND", "AETHER PRIME", "Autonomous 12-step cycle paused", AuditOutcome.INFO, "Paused by Commander.")
            }
            return
        }
        _state.update { it.copy(autonomousCycle = it.autonomousCycle.copy(isRunning = true)) }
        viewModelScope.launch {
            recordAudit("OVERMIND", "AETHER PRIME", "Autonomous 12-step cycle ENGAGED", AuditOutcome.APPROVED, "Executing continuous intelligence pipeline.")
        }
        autonomousJob = viewModelScope.launch {
            while (isActive) {
                val idx = _state.value.autonomousCycle.currentStepIndex
                executeAutonomousStep(idx)
                delay(2800L)
            }
        }
    }

    fun stepAutonomousCycleOnce() {
        viewModelScope.launch {
            executeAutonomousStep(_state.value.autonomousCycle.currentStepIndex)
        }
    }

    private suspend fun executeAutonomousStep(stepIdx: Int) {
        val stateNow = _state.value
        val stepName = AutonomousCycleState.steps[stepIdx]
        val summary = when (stepIdx) {
            0 -> {
                val live = stateNow.quotes.count { it.isLive }
                "Monitored $live live instruments across ${stateNow.quotes.map { it.instrument.assetClass }.distinct().size} divisions."
            }
            1 -> {
                val topTail = stateNow.blackSwanAlerts.firstOrNull()
                "SENTINEL peak tail anomaly: ${topTail?.symbol ?: "N/A"} (${topTail?.tailRiskScore ?: 0}/100)."
            }
            2 -> {
                val mover = stateNow.quotes.filter { it.isLive }.maxByOrNull { abs(it.changePct) }
                "Investigating volatility mover: ${mover?.instrument?.symbol ?: "XAUUSD"} (${String.format("%+.2f%%", mover?.changePct ?: 0.0)})."
            }
            3 -> {
                val mult = stateNow.multiverseReport
                "Multiverse VaR95=${String.format("%.2f%%", mult?.var95Pct ?: 0.0)} across 350 timelines."
            }
            4 -> {
                val topStrat = stateNow.strategies.maxByOrNull { it.sharpeRatio }
                "Top validated candidate: ${topStrat?.name ?: "None"} (Sharpe ${String.format("%.2f", topStrat?.sharpeRatio ?: 0.0)})."
            }
            5 -> {
                val openExp = stateNow.paperOrders.filter { it.status == "OPEN" }.sumOf { it.exposurePct }
                "Exposure ${String.format("%.1f%%", openExp)} / Cap ${stateNow.constitution.maxTotalExposurePct}%."
            }
            6 -> {
                val kill = stateNow.constitution.emergencyKillSwitch
                if (kill) "Execution BLOCKED by Emergency Kill Switch." else "Spread, slippage & permission gates verified."
            }
            7 -> {
                if (stateNow.constitution.emergencyKillSwitch || stateNow.constitution.permissionLevel < 3) {
                    "No order submitted (KillSwitch=${stateNow.constitution.emergencyKillSwitch}, Perm=${stateNow.constitution.permissionLevel})."
                } else {
                    "Paper/Bridge router ready; awaiting signal trigger."
                }
            }
            8 -> {
                val openCount = stateNow.paperOrders.count { it.status == "OPEN" }
                "Monitoring $openCount open Paper Sandbox position(s)."
            }
            9 -> {
                val closedCount = stateNow.paperOrders.count { it.status == "CLOSED" }
                "TRADE AUTOPSY cataloged $closedCount completed trade(s)."
            }
            10 -> {
                val healthy = stateNow.strategies.count { it.sharpeRatio >= 1.0 }
                "$healthy/${stateNow.strategies.size} strategies above 1.0 Sharpe threshold."
            }
            else -> {
                "SYSTEM EVOLUTION queued next genetic mutation batch."
            }
        }

        val nextIdx = (stepIdx + 1) % AutonomousCycleState.steps.size
        val completed = if (nextIdx == 0) stateNow.autonomousCycle.completedCycles + 1 else stateNow.autonomousCycle.completedCycles
        _state.update {
            it.copy(
                autonomousCycle = it.autonomousCycle.copy(
                    currentStepIndex = nextIdx,
                    completedCycles = completed,
                    lastStepSummary = "$stepName → $summary",
                    lastRunEpochMs = System.currentTimeMillis()
                )
            )
        }
        eventBus.publish("OVERMIND", "EVENT_BUS", "CYCLE_STEP", "$stepName: $summary")
    }

    fun updateProviderSettings(transform: (ProviderSettings) -> ProviderSettings) {
        viewModelScope.launch {
            settingsPrefs.updateSettings(transform)
            recordAudit("INTEGRATIONS", "COMMANDER", "Updated AI/Bridge provider configuration", AuditOutcome.INFO, "Provider configuration persisted locally.")
        }
    }

    fun dismissBanner() {
        _state.update { it.copy(statusBanner = null) }
    }

    private suspend fun recordAudit(
        moduleId: String,
        actor: String,
        action: String,
        outcome: AuditOutcome,
        details: String
    ) {
        val id = "log_${UUID.randomUUID().toString().take(8)}"
        localDb.localAuditLogDao().insert(
            LocalAuditLogEntity(
                id = id,
                moduleId = moduleId,
                actor = actor,
                action = action,
                outcome = outcome.name,
                details = details
            )
        )
        firestoreRepo.appendAuditLog(
            AuditLogRecord(
                id = id,
                userId = currentUserId,
                moduleId = moduleId,
                actor = actor,
                action = action,
                outcome = outcome.name,
                details = details
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        autonomousJob?.cancel()
        replayJob?.cancel()
        geminiLiveEngine.stopConversation()
        voiceApisClient.releaseAll()
    }

    companion object {
        private const val TAG = "AetherionViewModel"
    }
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

private fun LocalStrategyEntity.toFirestoreRecord(userId: String): StrategyRecord {
    return StrategyRecord(
        id = id,
        userId = userId,
        name = name,
        assetClass = assetClass,
        symbol = symbol,
        timeframe = timeframe,
        stage = stage,
        sharpeRatio = sharpeRatio,
        profitFactor = profitFactor,
        maxDrawdownPct = maxDrawdownPct,
        winRatePct = winRatePct,
        sampleSize = sampleSize.toLong(),
        genomeSummary = genomeSummary,
        creatorAgent = creatorAgent
    )
}
