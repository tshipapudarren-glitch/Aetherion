package com.example.domain.engine

import com.example.data.local.LocalMemoryEntity
import com.example.data.local.LocalStrategyEntity
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketQuote
import com.example.data.model.MemoryTier
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class KnowledgeDomainCategory(
    val tier: MemoryTier,
    val badge: String,
    val description: String
) {
    PLAYBOOK_RULE(
        tier = MemoryTier.MARKET_MEMORY,
        badge = "REGIME PLAYBOOK",
        description = "Macro conditions, liquidity regime & session playbooks"
    ),
    STRATEGY_RULE(
        tier = MemoryTier.STRATEGY_MEMORY,
        badge = "GENOME RULE",
        description = "Entry, exit, EMA/ATR & structural edge specifications"
    ),
    COUNCIL_DECISION(
        tier = MemoryTier.DECISION_MEMORY,
        badge = "COUNCIL MANDATE",
        description = "Sovereign Council rulings & event narrative theses"
    ),
    TRADE_AUTOPSY(
        tier = MemoryTier.EXECUTION_MEMORY,
        badge = "POST-TRADE LESSON",
        description = "Closed execution diagnostics & slippage findings"
    ),
    RISK_HEURISTIC(
        tier = MemoryTier.INTELLIGENCE_MEMORY,
        badge = "RISK HEURISTIC",
        description = "Institutional capital preservation & Arena guardrails"
    );

    companion object {
        fun fromTierName(tierName: String): KnowledgeDomainCategory {
            val tier = runCatching { MemoryTier.valueOf(tierName) }.getOrDefault(MemoryTier.INTELLIGENCE_MEMORY)
            return entries.firstOrNull { it.tier == tier } ?: RISK_HEURISTIC
        }
    }
}

data class KnowledgeCitationItem(
    val memoryId: String,
    val tier: MemoryTier,
    val categoryBadge: String,
    val title: String,
    val content: String,
    val symbol: String,
    val agentName: String,
    val confidenceScore: Double,
    val relevanceScorePct: Int,
    val matchReason: String
)

data class ArenaContenderRanking(
    val rank: Int,
    val strategy: LocalStrategyEntity,
    val baseQuantScore: Double,
    val knowledgeBuffPts: Double,
    val compositeScore: Double,
    val liveBacktestReport: BacktestReport?,
    val matchedKnowledge: List<KnowledgeCitationItem>,
    val headToHeadEdgeSummary: String
)

data class ArenaMatchupResult(
    val symbol: String,
    val challengerAName: String,
    val challengerBName: String,
    val winnerName: String,
    val winnerScorePts: Double,
    val runnerUpScorePts: Double,
    val winMarginPts: Double,
    val regimeContext: String,
    val appliedKnowledgeCount: Int,
    val appliedCitations: List<KnowledgeCitationItem>
)

data class KnowledgeArenaSnapshot(
    val totalKnowledgeEntries: Int = 0,
    val linkedArenaSymbolsCount: Int = 0,
    val avgConfidencePct: Int = 0,
    val categoryCounts: Map<MemoryTier, Int> = emptyMap(),
    val linkedMemoryIds: Set<String> = emptySet(),
    val activeSymbolCitations: List<KnowledgeCitationItem> = emptyList(),
    val contenderRankings: List<ArenaContenderRanking> = emptyList(),
    val featuredMatchup: ArenaMatchupResult? = null
)

object KnowledgeArenaEngine {

    fun starterPlaybooks(): List<LocalMemoryEntity> = listOf(
        LocalMemoryEntity(
            id = "know_xau_london_sweep",
            tier = MemoryTier.MARKET_MEMORY.name,
            title = "XAUUSD London-NY Liquidity Sweep Playbook",
            content = "During elevated central-bank or real-yield uncertainty, Gold (XAUUSD) frequently sweeps Asian range highs/lows before establishing the true impulse leg. Prefer ATR(14) stops >= 1.5x and confirm EMA momentum alignment.",
            symbol = "XAUUSD",
            agentName = "GOLDEN EYE",
            confidenceScore = 0.91
        ),
        LocalMemoryEntity(
            id = "know_btc_vol_expansion",
            tier = MemoryTier.STRATEGY_MEMORY.name,
            title = "BTCUSD Volatility Expansion Asymmetric Filter",
            content = "Crypto breakout strategies achieve superior Profit Factor when fast/slow EMA spread widens during realized volatility expansion while avoiding whipsaw chop under thin weekend liquidity.",
            symbol = "BTCUSD",
            agentName = "CRYPTO SCOUT",
            confidenceScore = 0.87
        ),
        LocalMemoryEntity(
            id = "know_eur_session_eq",
            tier = MemoryTier.STRATEGY_MEMORY.name,
            title = "EURUSD Mean-Reversion vs Trend Regime Gate",
            content = "EURUSD outperforms with wider EMA separation (15/45+) and disciplined 1.4x ATR stops when DXY spread remains range-bound outside high-impact CPI/FOMC windows.",
            symbol = "EURUSD",
            agentName = "FX HUNTER",
            confidenceScore = 0.84
        ),
        LocalMemoryEntity(
            id = "know_global_risk_guard",
            tier = MemoryTier.INTELLIGENCE_MEMORY.name,
            title = "Global Drawdown & Arena Graduation Rule",
            content = "Only promote contenders above 6.5% max drawdown if Out-of-Sample Sharpe exceeds 1.10 and spread-stress Stress Test survives >= -5%. Halt expansion when tail-risk score breaches 75/100.",
            symbol = "GLOBAL",
            agentName = "RISK GUARD",
            confidenceScore = 0.95
        )
    )

    fun rankKnowledgeCitations(
        memories: List<LocalMemoryEntity>,
        targetSymbol: String,
        strategyName: String = "",
        genomeSummary: String = "",
        regimeName: String = "",
        limit: Int = 5
    ): List<KnowledgeCitationItem> {
        if (memories.isEmpty()) return emptyList()
        val cleanSym = targetSymbol.trim().uppercase(Locale.US)
        val contextTokens = (strategyName + " " + genomeSummary + " " + regimeName)
            .lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()

        return memories.map { mem ->
            val memSym = mem.symbol.trim().uppercase(Locale.US)
            val memText = (mem.title + " " + mem.content).lowercase(Locale.US)

            var score = 20
            val reasons = mutableListOf<String>()

            if (memSym == cleanSym) {
                score += 42
                reasons.add("Exact symbol ($cleanSym)")
            } else if (memSym == "GLOBAL") {
                score += 24
                reasons.add("Global rule")
            }

            val tokenHits = contextTokens.count { tok -> memText.contains(tok) }
            if (tokenHits > 0) {
                score += min(22, tokenHits * 7)
                reasons.add("$tokenHits rule match(es)")
            }

            val confBonus = (mem.confidenceScore * 16.0).toInt().coerceIn(0, 16)
            score += confBonus
            val category = KnowledgeDomainCategory.fromTierName(mem.tier)
            if (category == KnowledgeDomainCategory.STRATEGY_RULE || category == KnowledgeDomainCategory.RISK_HEURISTIC) {
                score += 6
            }

            if (reasons.isEmpty()) {
                reasons.add("${category.badge} baseline")
            }

            KnowledgeCitationItem(
                memoryId = mem.id,
                tier = category.tier,
                categoryBadge = category.badge,
                title = mem.title,
                content = mem.content,
                symbol = mem.symbol,
                agentName = mem.agentName,
                confidenceScore = mem.confidenceScore,
                relevanceScorePct = score.coerceIn(10, 99),
                matchReason = reasons.joinToString(" · ")
            )
        }.sortedWith(
            compareByDescending<KnowledgeCitationItem> { it.relevanceScorePct }
                .thenByDescending { it.confidenceScore }
        ).take(limit)
    }

    fun buildSnapshot(
        strategies: List<LocalStrategyEntity>,
        memories: List<LocalMemoryEntity>,
        quotes: List<MarketQuote>,
        selectedSymbol: String,
        marketConditions: MarketConditionsSnapshot
    ): KnowledgeArenaSnapshot {
        val quoteBySymbol = quotes.associateBy { it.instrument.symbol }
        val activeCitations = rankKnowledgeCitations(
            memories = memories,
            targetSymbol = selectedSymbol,
            regimeName = "${marketConditions.overallRegime} ${marketConditions.volatilityRegime}",
            limit = 4
        )

        val rawContenders = strategies.map { strat ->
            val quote = quoteBySymbol[strat.symbol]
            val backtest = if (quote != null && quote.candles.size >= strat.slowPeriod + 12) {
                QuantitativeEngines.runBacktest(
                    symbol = strat.symbol,
                    candles = quote.candles,
                    fastPeriod = strat.fastPeriod,
                    slowPeriod = strat.slowPeriod,
                    atrStopMult = strat.atrStopMult,
                    atrTargetMult = strat.atrTargetMult
                )
            } else null

            val effectiveSharpe = backtest?.takeIf { it.totalTrades >= 3 }?.sharpeRatio ?: strat.sharpeRatio
            val effectivePf = backtest?.takeIf { it.totalTrades >= 3 }?.profitFactor ?: strat.profitFactor
            val effectiveDd = backtest?.takeIf { it.totalTrades >= 3 }?.maxDrawdownPct ?: strat.maxDrawdownPct

            val baseQuant = (effectiveSharpe * 20.0 + effectivePf * 15.0 - effectiveDd * 1.8)
                .coerceIn(0.0, 92.0)

            val matched = rankKnowledgeCitations(
                memories = memories,
                targetSymbol = strat.symbol,
                strategyName = strat.name,
                genomeSummary = strat.genomeSummary,
                regimeName = marketConditions.overallRegime,
                limit = 3
            )

            // Knowledge buff up to +8.0 pts based on verified rule alignment & symbol focus
            val highRelCount = matched.count { it.relevanceScorePct >= 60 }
            val avgConf = if (matched.isNotEmpty()) matched.map { it.confidenceScore }.average() else 0.0
            val knowledgeBuff = (highRelCount * 1.8 + avgConf * 2.6).coerceIn(0.0, 8.0)
            val composite = (baseQuant + knowledgeBuff).coerceIn(0.0, 100.0)

            val activeBonus = if (strat.symbol == selectedSymbol) "Focus Asset · " else ""
            val btSummary = if (backtest != null && backtest.totalTrades > 0) {
                "${backtest.totalTrades} live bars trades (${String.format(Locale.US, "%.0f%%", backtest.winRatePct)} WR, OOS Sharpe ${String.format(Locale.US, "%.2f", backtest.oosSharpeRatio)})"
            } else {
                "Catalog Sharpe ${String.format(Locale.US, "%.2f", strat.sharpeRatio)} · PF ${String.format(Locale.US, "%.2f", strat.profitFactor)}"
            }

            Triple(
                strat,
                composite,
                ArenaContenderRanking(
                    rank = 0,
                    strategy = strat,
                    baseQuantScore = baseQuant,
                    knowledgeBuffPts = knowledgeBuff,
                    compositeScore = composite,
                    liveBacktestReport = backtest,
                    matchedKnowledge = matched,
                    headToHeadEdgeSummary = "$activeBonus$btSummary"
                )
            )
        }.sortedWith(
            compareByDescending<Triple<LocalStrategyEntity, Double, ArenaContenderRanking>> { it.third.compositeScore }
                .thenByDescending { it.first.symbol == selectedSymbol }
        )

        val ranked = rawContenders.mapIndexed { idx, (_, _, item) ->
            item.copy(rank = idx + 1)
        }

        val linkedIds = buildSet {
            activeCitations.forEach { add(it.memoryId) }
            ranked.forEach { r -> r.matchedKnowledge.forEach { add(it.memoryId) } }
        }

        val matchup = if (ranked.size >= 2) {
            val topA = ranked.firstOrNull { it.strategy.symbol == selectedSymbol } ?: ranked[0]
            val topB = ranked.firstOrNull { it.strategy.id != topA.strategy.id } ?: ranked[1]
            val winner = if (topA.compositeScore >= topB.compositeScore) topA else topB
            val runner = if (winner.strategy.id == topA.strategy.id) topB else topA
            val combinedCitations = (winner.matchedKnowledge + activeCitations)
                .distinctBy { it.memoryId }
                .take(4)

            ArenaMatchupResult(
                symbol = selectedSymbol,
                challengerAName = topA.strategy.name,
                challengerBName = topB.strategy.name,
                winnerName = winner.strategy.name,
                winnerScorePts = winner.compositeScore,
                runnerUpScorePts = runner.compositeScore,
                winMarginPts = max(0.0, winner.compositeScore - runner.compositeScore),
                regimeContext = "${marketConditions.overallRegime} · ${marketConditions.volatilityRegime}",
                appliedKnowledgeCount = combinedCitations.size,
                appliedCitations = combinedCitations
            )
        } else null

        val catCounts = MemoryTier.entries.associateWith { tier ->
            memories.count { it.tier == tier.name }
        }
        val avgConfPct = if (memories.isNotEmpty()) {
            (memories.map { it.confidenceScore }.average() * 100.0).toInt().coerceIn(0, 100)
        } else 0

        return KnowledgeArenaSnapshot(
            totalKnowledgeEntries = memories.size,
            linkedArenaSymbolsCount = memories.map { it.symbol }.distinct().size,
            avgConfidencePct = avgConfPct,
            categoryCounts = catCounts,
            linkedMemoryIds = linkedIds,
            activeSymbolCitations = activeCitations,
            contenderRankings = ranked,
            featuredMatchup = matchup
        )
    }
}
