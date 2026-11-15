package com.example

import com.example.data.local.LocalStrategyEntity
import com.example.data.model.AssetClass
import com.example.data.model.CandleBar
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketInstrument
import com.example.data.model.MarketQuote
import com.example.data.model.MemoryTier
import com.example.data.model.StrategyStage
import com.example.domain.engine.KnowledgeArenaEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeArenaEngineTest {

    private fun buildSampleCandles(count: Int, startPrice: Double = 2650.0): List<CandleBar> {
        var price = startPrice
        val bars = ArrayList<CandleBar>(count)
        for (i in 0 until count) {
            val delta = if (i % 4 == 0) -6.0 else 4.5
            val open = price
            val close = (price + delta).coerceAtLeast(100.0)
            val high = maxOf(open, close) + 5.0
            val low = minOf(open, close) - 5.0
            bars.add(
                CandleBar(
                    timestampMs = 1_700_000_000_000L + i * 3_600_000L,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 1200.0 + i * 10.0
                )
            )
            price = close
        }
        return bars
    }

    @Test
    fun starterPlaybooksContainExpectedInstitutionalScopes() {
        val playbooks = KnowledgeArenaEngine.starterPlaybooks()
        assertEquals(4, playbooks.size)
        assertTrue(playbooks.any { it.symbol == "XAUUSD" })
        assertTrue(playbooks.any { it.symbol == "GLOBAL" })
        assertTrue(playbooks.all { it.confidenceScore in 0.5..1.0 })
    }

    @Test
    fun rankKnowledgeCitationsPrioritizesMatchingSymbolAndKeywords() {
        val playbooks = KnowledgeArenaEngine.starterPlaybooks()
        val citations = KnowledgeArenaEngine.rankKnowledgeCitations(
            memories = playbooks,
            targetSymbol = "XAUUSD",
            strategyName = "Aether Gold Momentum",
            genomeSummary = "EMA 12/34 ATR 1.5x SL",
            regimeName = "RISK_ON"
        )
        assertTrue(citations.isNotEmpty())
        assertEquals("XAUUSD", citations.first().symbol)
        assertTrue(citations.first().relevanceScorePct >= citations.last().relevanceScorePct)
    }

    @Test
    fun buildSnapshotConnectsKnowledgeBuffsToArenaContenders() {
        val playbooks = KnowledgeArenaEngine.starterPlaybooks()
        val xauQuote = MarketQuote(
            instrument = MarketInstrument("XAUUSD", "Gold", AssetClass.PRECIOUS_METALS, "Global", "GC=F"),
            price = 2720.0,
            previousClose = 2700.0,
            changePct = 0.74,
            high24h = 2730.0,
            low24h = 2690.0,
            realizedVolPct = 14.5,
            candles = buildSampleCandles(80),
            sourceName = "TestFeed",
            latencyMs = 25L,
            fetchedAtMs = System.currentTimeMillis(),
            isLive = true
        )
        val strategies = listOf(
            LocalStrategyEntity(
                id = "s1",
                name = "Aether Gold Momentum v1",
                assetClass = "Precious Metals",
                symbol = "XAUUSD",
                timeframe = "1H",
                stage = StrategyStage.PAPER_TRADING.name,
                sharpeRatio = 1.85,
                profitFactor = 1.65,
                maxDrawdownPct = 3.8,
                winRatePct = 58.0,
                sampleSize = 60,
                genomeSummary = "EMA(12/34) + ATR(14) momentum sweep",
                creatorAgent = "GOLDEN EYE",
                fastPeriod = 12,
                slowPeriod = 34,
                atrStopMult = 1.6,
                atrTargetMult = 2.8
            ),
            LocalStrategyEntity(
                id = "s2",
                name = "FX Session Reversion",
                assetClass = "Forex",
                symbol = "EURUSD",
                timeframe = "1H",
                stage = StrategyStage.BACKTESTED.name,
                sharpeRatio = 1.12,
                profitFactor = 1.24,
                maxDrawdownPct = 4.2,
                winRatePct = 51.0,
                sampleSize = 40,
                genomeSummary = "EMA(15/45)",
                creatorAgent = "FX HUNTER",
                fastPeriod = 15,
                slowPeriod = 45,
                atrStopMult = 1.4,
                atrTargetMult = 2.2
            )
        )

        val snap = KnowledgeArenaEngine.buildSnapshot(
            strategies = strategies,
            memories = playbooks,
            quotes = listOf(xauQuote),
            selectedSymbol = "XAUUSD",
            marketConditions = MarketConditionsSnapshot(overallRegime = "EXPANSION", volatilityRegime = "NORMAL")
        )

        assertEquals(4, snap.totalKnowledgeEntries)
        assertEquals(2, snap.contenderRankings.size)
        assertTrue(snap.linkedMemoryIds.isNotEmpty())
        assertTrue(snap.contenderRankings.first().knowledgeBuffPts > 0.0)
        assertNotNull(snap.featuredMatchup)
        assertEquals("XAUUSD", snap.featuredMatchup?.symbol)
        assertTrue((snap.categoryCounts[MemoryTier.STRATEGY_MEMORY] ?: 0) >= 1)
    }
}
