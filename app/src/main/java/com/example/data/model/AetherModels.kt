package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class EnvironmentMode(val badge: String) {
    LIVE("LIVE API"),
    HYBRID("LIVE + SIM"),
    SIMULATED("SIMULATED SANDBOX"),
    CONFIG_REQUIRED("ENDPOINT CONFIG")
}

enum class IntelligenceLayer(val code: String, val label: String, val responsibility: String) {
    OMNISENSE("01", "OMNISENSE", "Global market data collection"),
    CORTEX("02", "CORTEX", "Multi-AI intelligence"),
    DISCOVERY("03", "DISCOVERY", "Opportunity identification"),
    GENOME("04", "GENOME", "Strategy creation & evolution"),
    SIMULATOR("05", "SIMULATOR", "Testing & validation"),
    CAPITAL("06", "CAPITAL", "Portfolio & risk management"),
    EXECUTION("07", "EXECUTION", "Autonomous trading & MT5"),
    EVOLUTION("08", "EVOLUTION", "Performance learning & adaptation")
}

enum class VipModuleId(
    val number: Int,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val layer: IntelligenceLayer,
    val leadAgent: String,
    val environment: EnvironmentMode
) {
    MULTIVERSE_SIMULATOR(
        1, "Multiverse Market Simulator", "Multiverse",
        "Monte Carlo & jump-diffusion timeline engine seeded from live volatility",
        IntelligenceLayer.SIMULATOR, "STRESS LAB", EnvironmentMode.SIMULATED
    ),
    AI_SOVEREIGN_COUNCIL(
        2, "AI Sovereign Council", "Council",
        "Multi-agent parliament debate, dissent & consensus voting",
        IntelligenceLayer.CORTEX, "ARBITER", EnvironmentMode.LIVE
    ),
    STRATEGY_DNA_LAB(
        3, "Strategy DNA Evolution Lab", "DNA Lab",
        "Genetic algorithm genome mutation, OOS split & fitness validation",
        IntelligenceLayer.GENOME, "GENOME", EnvironmentMode.HYBRID
    ),
    GLOBAL_CAPITAL_FLOW_RADAR(
        4, "Global Capital Flow Radar", "Flow Radar",
        "Live multi-asset capital rotation & G10 currency strength matrix",
        IntelligenceLayer.DISCOVERY, "LIQUIDITY X", EnvironmentMode.LIVE
    ),
    BLACK_SWAN_PREDICTOR(
        5, "Black Swan Predictor", "Black Swan",
        "Fat-tail anomaly Z-score, kurtosis & cross-asset contagion monitor",
        IntelligenceLayer.DISCOVERY, "SENTINEL", EnvironmentMode.LIVE
    ),
    STRATEGY_CHAMPIONSHIP_ARENA(
        6, "Strategy Championship Arena", "Arena",
        "Risk-adjusted tournament ranking Sharpe, Calmar & drawdown stability",
        IntelligenceLayer.SIMULATOR, "EDGE DETECTOR", EnvironmentMode.HYBRID
    ),
    AUTONOMOUS_BROKER_INTELLIGENCE(
        7, "Autonomous Broker Intelligence", "Brokers",
        "MT5 EA Bridge & broker REST health, spread & slippage diagnostics",
        IntelligenceLayer.EXECUTION, "EXECUTOR", EnvironmentMode.CONFIG_REQUIRED
    ),
    MARKET_TIME_RECONSTRUCTION(
        8, "Market Time Reconstruction", "Time Machine",
        "Bar-by-bar historical market replay & forensic trade autopsy",
        IntelligenceLayer.EVOLUTION, "PATTERN DNA", EnvironmentMode.LIVE
    ),
    QUANTUM_CAPITAL_ALLOCATOR(
        9, "Quantum Capital Allocator", "Allocator",
        "Inverse-volatility risk parity & fractional Kelly budget allocator",
        IntelligenceLayer.CAPITAL, "CAPITAL MASTER", EnvironmentMode.HYBRID
    ),
    COLLECTIVE_INTELLIGENCE_MEMORY(
        10, "Collective Intelligence Memory", "Memory",
        "5-tier institutional memory matrix synced to Firestore & Room",
        IntelligenceLayer.EVOLUTION, "MEMORY MATRIX", EnvironmentMode.LIVE
    ),
    COMMANDER_VOICE_INTELLIGENCE(
        11, "Commander Voice Intelligence", "Voice Cmd",
        "SpeechRecognizer + AI executive command routing & TTS readout",
        IntelligenceLayer.CORTEX, "AETHER PRIME", EnvironmentMode.LIVE
    ),
    AUTONOMOUS_RISK_DEFENCE_MATRIX(
        12, "Autonomous Risk Defence Matrix", "Risk Matrix",
        "Deterministic Risk Constitution & immutable Emergency Kill Switch",
        IntelligenceLayer.CAPITAL, "RISK GUARD", EnvironmentMode.LIVE
    ),
    GLOBAL_EVENT_IMPACT_ENGINE(
        13, "Global Event Impact Engine", "Event Engine",
        "Live macro news ingestion & AI cross-asset shock mapping",
        IntelligenceLayer.OMNISENSE, "MACRO INTEL", EnvironmentMode.LIVE
    ),
    AUTONOMOUS_STRATEGY_FACTORY(
        14, "Autonomous Strategy Factory", "Factory",
        "Automated research pipeline from discovery to gated graduation",
        IntelligenceLayer.GENOME, "EVOLVER", EnvironmentMode.HYBRID
    ),
    AETHERION_OVERMIND(
        15, "AETHERION OVERMIND", "OVERMIND",
        "Central 30-agent telemetry, 12-step autonomous cycle & system audit",
        IntelligenceLayer.CORTEX, "AETHER PRIME", EnvironmentMode.LIVE
    )
}

enum class AgentDivision(val label: String) {
    EXECUTIVE("Executive Intelligence"),
    MARKET_RESEARCH("Market Research"),
    QUANTITATIVE("Quantitative Research"),
    TRADING_CAPITAL("Trading & Capital"),
    EVOLUTION("Intelligence Evolution")
}

data class AgentProfile(
    val id: Int,
    val name: String,
    val role: String,
    val division: AgentDivision,
    val assignedProvider: String,
    val accuracyPct: Double,
    val status: String = "STANDBY",
    val lastTask: String = "Monitoring event bus"
)

object AetherAgentsRegistry {
    val allAgents: List<AgentProfile> = listOf(
        AgentProfile(1, "AETHER PRIME", "Chief intelligence coordinator", AgentDivision.EXECUTIVE, "Gemini 3.8 Flash", 91.4, "ACTIVE"),
        AgentProfile(2, "STRATEGOS", "Trading strategy director", AgentDivision.EXECUTIVE, "Gemini 3.8 Flash", 88.2, "ACTIVE"),
        AgentProfile(3, "ORACLE", "Market forecasting", AgentDivision.EXECUTIVE, "Gemini 4 Argon", 79.6, "ACTIVE"),
        AgentProfile(4, "CRITIC", "Challenges AI conclusions", AgentDivision.EXECUTIVE, "Gemini 3.8 Flash", 86.9, "ACTIVE"),
        AgentProfile(5, "ARBITER", "Resolves agent conflicts", AgentDivision.EXECUTIVE, "Gemini 3.8 Flash", 90.1, "ACTIVE"),
        AgentProfile(6, "WATCHMAN", "Monitors all operations", AgentDivision.EXECUTIVE, "Deterministic", 99.4, "ACTIVE"),

        AgentProfile(7, "GOLDEN EYE", "Gold & precious metals", AgentDivision.MARKET_RESEARCH, "Gemini 3.8 Flash", 83.5, "ACTIVE"),
        AgentProfile(8, "FX HUNTER", "Forex markets", AgentDivision.MARKET_RESEARCH, "Gemini 3.8 Flash", 81.2, "ACTIVE"),
        AgentProfile(9, "CRYPTO SCOUT", "Cryptocurrency markets", AgentDivision.MARKET_RESEARCH, "Gemini 3.5 Flash-Lite", 78.4, "ACTIVE"),
        AgentProfile(10, "EQUITY MIND", "Stocks & indices", AgentDivision.MARKET_RESEARCH, "Gemini 3.8 Flash", 82.0, "ACTIVE"),
        AgentProfile(11, "SYNTHETIC CORE", "Deriv synthetic indices", AgentDivision.MARKET_RESEARCH, "Bridge Config", 0.0, "DISCONNECTED"),
        AgentProfile(12, "MACRO INTEL", "Economic & central banks", AgentDivision.MARKET_RESEARCH, "Gemini 4 Argon", 84.7, "ACTIVE"),
        AgentProfile(13, "LIQUIDITY X", "Liquidity & market structure", AgentDivision.MARKET_RESEARCH, "Quantitative", 87.3, "ACTIVE"),
        AgentProfile(14, "VOLTAGE", "Volatility & momentum", AgentDivision.MARKET_RESEARCH, "Quantitative", 85.9, "ACTIVE"),
        AgentProfile(15, "SENTINEL", "Unusual market activity", AgentDivision.MARKET_RESEARCH, "Quantitative", 89.2, "ACTIVE"),

        AgentProfile(16, "GENOME", "Creates trading strategies", AgentDivision.QUANTITATIVE, "Gemini 3.8 Flash", 84.1, "ACTIVE"),
        AgentProfile(17, "EVOLVER", "Mutates strategy candidates", AgentDivision.QUANTITATIVE, "Quantitative", 86.4, "ACTIVE"),
        AgentProfile(18, "BACKTESTER", "Historical strategy testing", AgentDivision.QUANTITATIVE, "Deterministic", 100.0, "ACTIVE"),
        AgentProfile(19, "STRESS LAB", "Adverse market simulations", AgentDivision.QUANTITATIVE, "Monte Carlo", 100.0, "ACTIVE"),
        AgentProfile(20, "PATTERN DNA", "Historical similarity", AgentDivision.QUANTITATIVE, "Quantitative", 80.8, "ACTIVE"),
        AgentProfile(21, "EDGE DETECTOR", "Statistical trading edges", AgentDivision.QUANTITATIVE, "Quantitative", 83.3, "ACTIVE"),

        AgentProfile(22, "EXECUTOR", "Sends & manages orders", AgentDivision.TRADING_CAPITAL, "MT5 / Paper", 98.7, "SANDBOX"),
        AgentProfile(23, "CAPITAL MASTER", "Allocates trading capital", AgentDivision.TRADING_CAPITAL, "Risk Parity", 95.0, "ACTIVE"),
        AgentProfile(24, "RISK GUARD", "Enforces account-level risk", AgentDivision.TRADING_CAPITAL, "Deterministic", 100.0, "ENFORCING"),
        AgentProfile(25, "HEDGE INTEL", "Evaluates hedging options", AgentDivision.TRADING_CAPITAL, "Quantitative", 84.0, "STANDBY"),
        AgentProfile(26, "PORTFOLIO X", "Manages combined exposure", AgentDivision.TRADING_CAPITAL, "Deterministic", 100.0, "ACTIVE"),
        AgentProfile(27, "RECOVERY INTEL", "Drawdown recovery analysis", AgentDivision.TRADING_CAPITAL, "Gemini 3.8 Flash", 82.6, "STANDBY"),

        AgentProfile(28, "TRADE AUTOPSY", "Investigates closed trades", AgentDivision.EVOLUTION, "Gemini 3.8 Flash", 88.9, "ACTIVE"),
        AgentProfile(29, "MEMORY MATRIX", "Maintains 5-tier memory", AgentDivision.EVOLUTION, "Room+Firestore", 99.1, "ACTIVE"),
        AgentProfile(30, "SYSTEM EVOLUTION", "Architecture optimization", AgentDivision.EVOLUTION, "Gemini 4 Argon", 85.4, "STANDBY")
    )
}

enum class AssetClass(val displayName: String) {
    PRECIOUS_METALS("Precious Metals"),
    FOREX("Forex"),
    CRYPTO("Cryptocurrencies"),
    INDICES("Indices"),
    ENERGY("Energy"),
    STOCKS("Equities"),
    BONDS("Bonds & Yields"),
    SYNTHETIC("Synthetic Indices")
}

data class MarketInstrument(
    val symbol: String,
    val displayName: String,
    val assetClass: AssetClass,
    val region: String,
    val yahooTicker: String,
    val twelveDataSymbol: String = "",
    val fmpSymbol: String = "",
    val finnhubSymbol: String = "",
    val isConfigurableBridgeOnly: Boolean = false
)

object MarketUniverse {
    val instruments: List<MarketInstrument> = listOf(
        MarketInstrument("XAUUSD", "Gold Spot / Futures", AssetClass.PRECIOUS_METALS, "Global", "GC=F", "XAU/USD", "XAUUSD", "OANDA:XAU_USD"),
        MarketInstrument("XAGUSD", "Silver Futures", AssetClass.PRECIOUS_METALS, "Global", "SI=F", "XAG/USD", "XAGUSD", "OANDA:XAG_USD"),
        MarketInstrument("EURUSD", "EUR / USD", AssetClass.FOREX, "Europe", "EURUSD=X", "EUR/USD", "EURUSD", "OANDA:EUR_USD"),
        MarketInstrument("GBPUSD", "GBP / USD", AssetClass.FOREX, "UK", "GBPUSD=X", "GBP/USD", "GBPUSD", "OANDA:GBP_USD"),
        MarketInstrument("USDJPY", "USD / JPY", AssetClass.FOREX, "Asia", "USDJPY=X", "USD/JPY", "USDJPY", "OANDA:USD_JPY"),
        MarketInstrument("AUDUSD", "AUD / USD", AssetClass.FOREX, "APAC", "AUDUSD=X", "AUD/USD", "AUDUSD", "OANDA:AUD_USD"),
        MarketInstrument("USDZAR", "USD / ZAR", AssetClass.FOREX, "Africa", "USDZAR=X", "USD/ZAR", "USDZAR", "OANDA:USD_ZAR"),
        MarketInstrument("BTCUSD", "Bitcoin / USD", AssetClass.CRYPTO, "Global", "BTC-USD", "BTC/USD", "BTCUSD", "BINANCE:BTCUSDT"),
        MarketInstrument("ETHUSD", "Ethereum / USD", AssetClass.CRYPTO, "Global", "ETH-USD", "ETH/USD", "ETHUSD", "BINANCE:ETHUSDT"),
        MarketInstrument("SOLUSD", "Solana / USD", AssetClass.CRYPTO, "Global", "SOL-USD", "SOL/USD", "SOLUSD", "BINANCE:SOLUSDT"),
        MarketInstrument("NAS100", "Nasdaq 100", AssetClass.INDICES, "North America", "^NDX", "QQQ", "^NDX", "QQQ"),
        MarketInstrument("SPX500", "S&P 500", AssetClass.INDICES, "North America", "^GSPC", "SPY", "^GSPC", "SPY"),
        MarketInstrument("US30", "Dow Jones 30", AssetClass.INDICES, "North America", "^DJI", "DIA", "^DJI", "DIA"),
        MarketInstrument("GER40", "DAX 40", AssetClass.INDICES, "Europe", "^GDAXI", "DAX", "^GDAXI", "EWG"),
        MarketInstrument("UK100", "FTSE 100", AssetClass.INDICES, "UK", "^FTSE", "EWU", "^FTSE", "EWU"),
        MarketInstrument("WTI", "WTI Crude Oil", AssetClass.ENERGY, "Global", "CL=F", "WTI/USD", "CLUSD", "USO"),
        MarketInstrument("BRENT", "Brent Crude", AssetClass.ENERGY, "Europe", "BZ=F", "BCO/USD", "BZUSD", "BNO"),
        MarketInstrument("NVDA", "NVIDIA Corp", AssetClass.STOCKS, "North America", "NVDA", "NVDA", "NVDA", "NVDA"),
        MarketInstrument("AAPL", "Apple Inc", AssetClass.STOCKS, "North America", "AAPL", "AAPL", "AAPL", "AAPL"),
        MarketInstrument("US10Y", "US 10Y Treasury Yield", AssetClass.BONDS, "North America", "^TNX", "", "^TNX", ""),
        MarketInstrument("V75_SYNTH", "Volatility 75 (Deriv)", AssetClass.SYNTHETIC, "Synthetic", "", isConfigurableBridgeOnly = true)
    )
}

data class CandleBar(
    val timestampMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class MarketQuote(
    val instrument: MarketInstrument,
    val price: Double,
    val previousClose: Double,
    val changePct: Double,
    val high24h: Double,
    val low24h: Double,
    val realizedVolPct: Double,
    val candles: List<CandleBar>,
    val sourceName: String,
    val latencyMs: Long,
    val fetchedAtMs: Long,
    val isLive: Boolean,
    val errorMessage: String? = null
)

enum class StrategyStage(val label: String, val order: Int) {
    EXPERIMENTAL("Experimental", 0),
    BACKTESTED("Backtested", 1),
    OOS_VALIDATED("OOS Validated", 2),
    STRESS_TESTED("Stress Tested", 3),
    PAPER_TRADING("Paper Trading", 4),
    LIVE_ELIGIBLE("Live Eligible", 5),
    RETIRED("Retired", 6)
}

@IgnoreExtraProperties
data class StrategyRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val assetClass: String = "",
    val symbol: String = "",
    val timeframe: String = "1H",
    val stage: String = StrategyStage.EXPERIMENTAL.name,
    val sharpeRatio: Double = 0.0,
    val profitFactor: Double = 0.0,
    val maxDrawdownPct: Double = 0.0,
    val winRatePct: Double = 0.0,
    val sampleSize: Long = 0L,
    val genomeSummary: String = "",
    val creatorAgent: String = "GENOME",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class MemoryTier(val label: String) {
    MARKET_MEMORY("Market Memory"),
    STRATEGY_MEMORY("Strategy Memory"),
    DECISION_MEMORY("Decision Memory"),
    EXECUTION_MEMORY("Execution Memory"),
    INTELLIGENCE_MEMORY("Intelligence Memory")
}

@IgnoreExtraProperties
data class MemoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tier: String = MemoryTier.MARKET_MEMORY.name,
    val title: String = "",
    val content: String = "",
    val symbol: String = "GLOBAL",
    val agentName: String = "MEMORY MATRIX",
    val confidenceScore: Double = 0.85,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AuditOutcome {
    APPROVED, VETOED, EXECUTED, ERROR, INFO, SIMULATED
}

@IgnoreExtraProperties
data class AuditLogRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val moduleId: String = "",
    val actor: String = "",
    val action: String = "",
    val outcome: String = AuditOutcome.INFO.name,
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RiskConfigRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val maxDrawdownPct: Double = 8.0,
    val dailyLossLimitPct: Double = 2.5,
    val maxOpenPositions: Long = 6L,
    val maxTotalExposurePct: Double = 25.0,
    val maxSpreadBps: Double = 18.0,
    val maxSlippageBps: Double = 12.0,
    val newsBlackoutEnabled: Boolean = true,
    val emergencyKillSwitch: Boolean = false,
    val permissionLevel: Long = 3L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AiProviderType(val displayName: String, val requiresUserKey: Boolean) {
    FIREBASE_GEMINI_FLASH("Firebase AI Logic · Gemini 3.8 Flash", false),
    FIREBASE_GEMINI_ARGON("Firebase AI Logic · Gemini 4 Argon", false),
    FIREBASE_GEMINI_LITE("Firebase AI Logic · Gemini 3.5 Flash-Lite", false),
    DEEPSEEK("DeepSeek R1 / Chat REST", true),
    OPENAI_COMPATIBLE("OpenAI / Compatible REST", true),
    ANTHROPIC_CLAUDE("Anthropic Claude REST", true)
}

data class ProviderSettings(
    val activeProvider: AiProviderType = AiProviderType.FIREBASE_GEMINI_FLASH,
    val deepSeekApiKey: String = "",
    val deepSeekModel: String = "deepseek-chat",
    val openAiApiKey: String = "",
    val openAiModel: String = "gpt-4o",
    val openAiBaseUrl: String = "https://api.openai.com/v1",
    val anthropicApiKey: String = "",
    val anthropicModel: String = "claude-3-7-sonnet-20250219",
    val fmpApiKey: String = "",
    val fredApiKey: String = "",
    val twelveDataApiKey: String = "",
    val finnhubApiKey: String = "",
    val geminiLiveVoice: String = "FENRIR",
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "JBFqnCBsd6RMkjVDRZzb",
    val elevenLabsModelId: String = "eleven_multilingual_v2",
    val deepgramApiKey: String = "",
    val deepgramModel: String = "nova-3",
    val mt5BridgeUrl: String = "",
    val mt5BridgeToken: String = "",
    val derivAppId: String = ""
)

data class CrossModuleEvent(
    val id: String,
    val sourceModule: String,
    val targetModule: String,
    val eventType: String,
    val summary: String,
    val severity: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class RiskCheckResult(
    val approved: Boolean,
    val reason: String,
    val violatedRules: List<String> = emptyList()
)

data class AutonomousCycleState(
    val isRunning: Boolean = false,
    val currentStepIndex: Int = 0,
    val completedCycles: Int = 0,
    val lastStepSummary: String = "Awaiting activation by Commander",
    val lastRunEpochMs: Long = 0L
) {
    companion object {
        val steps = listOf(
            "01 Monitor Supported Markets",
            "02 Identify Unusual Behaviour",
            "03 Investigate Opportunities",
            "04 Generate Competing Scenarios",
            "05 Match Validated Strategies",
            "06 Calculate Risk & Exposure",
            "07 Validate Execution Conditions",
            "08 Execute Authorised Trades",
            "09 Monitor Open Positions",
            "10 Investigate Completed Trades",
            "11 Evaluate Strategy Health",
            "12 Commission New Experiments"
        )
    }
}

data class NewsHeadline(
    val title: String,
    val source: String,
    val publishedAt: String,
    val link: String
)

data class MacroIndicator(
    val seriesId: String,
    val title: String,
    val valueText: String,
    val numericValue: Double?,
    val deltaText: String,
    val observationDate: String,
    val sourceName: String,
    val isLive: Boolean
)

data class EconomicCalendarEvent(
    val eventName: String,
    val country: String,
    val impact: String,
    val timeUtc: String,
    val actual: String,
    val estimate: String,
    val previous: String,
    val sourceName: String
)

data class ProviderDiagnostic(
    val providerName: String,
    val connected: Boolean,
    val statusLabel: String,
    val latencyMs: Long,
    val details: String
)

data class MarketConditionsSnapshot(
    val overallRegime: String = "INITIALIZING TELEMETRY",
    val volatilityRegime: String = "MEASURING",
    val yieldCurveStatus: String = "UNVERIFIED",
    val activeTradingSessions: String = "Evaluating UTC clock",
    val advancersCount: Int = 0,
    val declinersCount: Int = 0,
    val averageRealizedVolPct: Double = 0.0,
    val riskAppetiteScore: Int = 50,
    val vixLevel: Double? = null,
    val us10yYield: Double? = null,
    val yieldSpread10y2y: Double? = null,
    val fedFundsRate: Double? = null,
    val indicators: List<MacroIndicator> = emptyList(),
    val economicEvents: List<EconomicCalendarEvent> = emptyList(),
    val updatedAtMs: Long = 0L,
    val errorSummary: String? = null
)
