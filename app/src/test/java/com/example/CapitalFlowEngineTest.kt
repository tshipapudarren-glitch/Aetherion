package com.example

import com.example.data.model.AssetClass
import com.example.data.model.CandleBar
import com.example.data.model.MarketQuote
import com.example.data.model.MarketUniverse
import com.example.domain.engine.CapitalFlowEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapitalFlowEngineTest {

    private fun buildQuote(
        symbol: String,
        changePct: Double,
        recentVolumeMultiplier: Double = 1.0,
        volPct: Double = 16.0
    ): MarketQuote {
        val inst = MarketUniverse.instruments.first { it.symbol == symbol }
        val now = 1_760_000_000_000L
        val basePrice = 100.0
        val candles = (0 until 24).map { idx ->
            val progress = idx / 23.0
            val p = basePrice * (1.0 + (changePct / 100.0) * progress)
            val vol = if (idx >= 21) 1000.0 * recentVolumeMultiplier else 1000.0
            CandleBar(now - (24 - idx) * 3_600_000L, p, p * 1.005, p * 0.995, p, vol)
        }
        return MarketQuote(
            instrument = inst,
            price = basePrice * (1.0 + changePct / 100.0),
            previousClose = basePrice,
            changePct = changePct,
            high24h = basePrice * 1.05,
            low24h = basePrice * 0.95,
            realizedVolPct = volPct,
            candles = candles,
            sourceName = "TestFeed",
            latencyMs = 45L,
            fetchedAtMs = now,
            isLive = true
        )
    }

    @Test
    fun analyzeCapitalFlows_aggregatesAssetClassesRegionsTimelineAndCorridors() {
        val quotes = listOf(
            buildQuote("XAUUSD", changePct = 2.4, recentVolumeMultiplier = 1.8, volPct = 18.0),
            buildQuote("BTCUSD", changePct = 3.1, recentVolumeMultiplier = 1.6, volPct = 42.0),
            buildQuote("SPX500", changePct = -1.5, recentVolumeMultiplier = 1.2, volPct = 15.0),
            buildQuote("EURUSD", changePct = 0.6, recentVolumeMultiplier = 1.1, volPct = 9.0),
            buildQuote("WTI", changePct = -2.2, recentVolumeMultiplier = 1.5, volPct = 28.0)
        )

        val snapshot = CapitalFlowEngine.analyzeCapitalFlows(
            quotes = quotes,
            activeSessionsText = "New York · London",
            nowMs = 1_760_000_000_000L
        )

        assertTrue("Expected asset class flows", snapshot.assetFlows.size >= 5)
        assertTrue("Expected CRYPTO as top positive flow", snapshot.assetFlows.first().assetClass == AssetClass.CRYPTO)
        assertTrue("Expected ENERGY as deepest negative flow", snapshot.assetFlows.last().assetClass == AssetClass.ENERGY)

        val metals = snapshot.assetFlows.first { it.assetClass == AssetClass.PRECIOUS_METALS }
        assertTrue("Expected volume impulse > 1.0 from trailing bars", metals.volumeImpulseRatio > 1.2)
        assertEquals("XAUUSD", metals.topLeaderSymbol)

        assertTrue("Expected 8 timeline buckets", snapshot.timelineBuckets.size == 8)
        assertEquals("-24h", snapshot.timelineBuckets.first().bucketLabel)
        assertEquals("LIVE", snapshot.timelineBuckets.last().bucketLabel)

        assertTrue("Expected active capital rotation corridors", snapshot.corridors.isNotEmpty())
        assertTrue("Expected North America active session flag", snapshot.regionFlows.first { it.region == "North America" }.sessionActive)
        assertTrue("Expected Europe & UK active session flag", snapshot.regionFlows.first { it.region == "Europe & UK" }.sessionActive)
    }

    @Test
    fun analyzeCapitalFlows_ignoresNonLiveQuotesGracefully() {
        val inst = MarketUniverse.instruments.first { it.symbol == "XAUUSD" }
        val disconnectedQuote = MarketQuote(
            instrument = inst,
            price = 0.0,
            previousClose = 0.0,
            changePct = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            realizedVolPct = 0.0,
            candles = emptyList(),
            sourceName = "Disconnected",
            latencyMs = 0L,
            fetchedAtMs = 0L,
            isLive = false
        )

        val snapshot = CapitalFlowEngine.analyzeCapitalFlows(listOf(disconnectedQuote))
        assertTrue(snapshot.assetFlows.isEmpty())
        assertTrue(snapshot.regionFlows.isEmpty())
        assertTrue(snapshot.corridors.isEmpty())
        assertEquals(50, snapshot.netLiquidityScore)
    }
}
