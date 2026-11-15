package com.example

import com.example.data.local.PaperOrderEntity
import com.example.data.local.RiskConstitutionEntity
import com.example.data.model.CandleBar
import com.example.data.model.EconomicCalendarEvent
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketQuote
import com.example.data.model.MarketUniverse
import com.example.data.model.NewsHeadline
import com.example.domain.engine.EventDomainCategory
import com.example.domain.engine.EventImpactEngine
import com.example.domain.engine.SetupArchitectEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupArchitectAndEventEngineTest {

    private fun buildSampleQuote(symbol: String, basePrice: Double, step: Double): MarketQuote {
        val now = 1_760_000_000_000L
        val inst = MarketUniverse.instruments.first { it.symbol == symbol }
        val candles = (0 until 60).map { idx ->
            val close = basePrice + idx * step
            val open = close - step * 0.6
            val high = maxOf(open, close) + kotlin.math.abs(basePrice) * 0.004
            val low = minOf(open, close) - kotlin.math.abs(basePrice) * 0.004
            CandleBar(now - (60 - idx) * 3_600_000L, open, high, low, close, 1200.0 + idx * 15.0)
        }
        val lastPrice = candles.last().close
        return MarketQuote(
            instrument = inst,
            price = lastPrice,
            previousClose = candles.first().close,
            changePct = ((lastPrice - candles.first().close) / candles.first().close) * 100.0,
            high24h = candles.maxOf { it.high },
            low24h = candles.minOf { it.low },
            realizedVolPct = 22.5,
            candles = candles,
            sourceName = "Yahoo Finance",
            latencyMs = 95L,
            fetchedAtMs = now,
            isLive = true
        )
    }

    @Test
    fun setupArchitect_generatesValidLongAndShortIndicatorsAndRespectsKillSwitch() {
        val bullQuote = buildSampleQuote("XAUUSD", 2600.0, 2.5)
        val longPlan = SetupArchitectEngine.architectSetup(
            quote = bullQuote,
            conditions = MarketConditionsSnapshot(overallRegime = "Risk-On Expansion", vixLevel = 15.2, us10yYield = 4.15),
            forcedDirection = "LONG",
            timeframeLabel = "4H"
        )
        assertNotNull(longPlan)
        longPlan!!
        assertEquals("LONG", longPlan.direction)
        assertEquals("4H", longPlan.timeframeLabel)
        assertTrue(longPlan.stopLoss < longPlan.entryPrice)
        assertTrue(longPlan.tp1 > longPlan.entryPrice)
        assertTrue(longPlan.tp2 > longPlan.tp1)
        assertTrue(longPlan.tp3 > longPlan.tp2)
        assertTrue(longPlan.drawnIndicators.any { it.id == "ENTRY" })
        assertTrue(longPlan.drawnIndicators.any { it.id == "SL" })
        assertTrue(longPlan.drawnIndicators.any { it.id == "TP1" })
        assertTrue(longPlan.drawnIndicators.any { it.id == "TP2" })
        assertTrue(longPlan.drawnIndicators.any { it.id == "TP3" })
        assertTrue(longPlan.riskGuardApproved)

        val shortPlan = SetupArchitectEngine.architectSetup(
            quote = bullQuote,
            constitution = RiskConstitutionEntity(emergencyKillSwitch = true),
            forcedDirection = "SHORT",
            timeframeLabel = "1H"
        )
        assertNotNull(shortPlan)
        shortPlan!!
        assertEquals("SHORT", shortPlan.direction)
        assertTrue(shortPlan.stopLoss > shortPlan.entryPrice)
        assertTrue(shortPlan.tp1 < shortPlan.entryPrice)
        assertTrue(shortPlan.tp2 < shortPlan.tp1)
        assertTrue(shortPlan.tp3 < shortPlan.tp2)
        assertFalse("Kill switch must veto trade execution on Setup Architect plan", shortPlan.riskGuardApproved)

        val refined = SetupArchitectEngine.applyAiRefinement(
            base = longPlan,
            direction = "LONG",
            entryPrice = longPlan.entryPrice,
            stopLoss = longPlan.entryPrice * 0.985,
            tp1 = longPlan.entryPrice * 1.018,
            tp2 = longPlan.entryPrice * 1.032,
            tp3 = longPlan.entryPrice * 1.055,
            confidencePct = 86,
            structuralThesis = "AI validated breakout above MA50 with liquidity sweep support.",
            eventAlignmentNote = "Aligned with dovish FOMC posture.",
            providerUsed = "Firebase AI Logic · Gemini 3.8 Flash"
        )
        assertTrue(refined.isAiRefined)
        assertEquals(86, refined.confidencePct)
        assertTrue(refined.tp3 > refined.tp2 && refined.tp2 > refined.tp1)
    }

    @Test
    fun eventImpactEngine_classifiesEconomicCentralBankGeopoliticalAndPositioning() {
        val xauQuote = buildSampleQuote("XAUUSD", 2600.0, 4.0)
        val btcQuote = buildSampleQuote("BTCUSD", 91000.0, -250.0)
        val quotes = listOf(xauQuote, btcQuote)

        val headlines = listOf(
            NewsHeadline("FOMC Powell signals cautious rate cut path as Treasury yields shift", "Reuters", "10m ago", "https://example.com/cb"),
            NewsHeadline("Strait of Hormuz missile alerts and new tariff sanctions lift energy volatility", "CNBC", "20m ago", "https://example.com/geo"),
            NewsHeadline("US Core CPI and retail sales beat consensus forecasts", "FMP Wire", "40m ago", "https://example.com/eco")
        )
        val conditions = MarketConditionsSnapshot(
            overallRegime = "Macro Catalyst Watch",
            fedFundsRate = 4.50,
            us10yYield = 4.25,
            yieldCurveStatus = "Positive (+0.32%)",
            economicEvents = listOf(
                EconomicCalendarEvent("US Non-Farm Payrolls", "US", "high", "13:30 UTC", "210K", "185K", "175K", "FMP")
            )
        )
        val orders = listOf(
            PaperOrderEntity(
                id = "ord-1",
                strategyId = "s-1",
                strategyName = "Gold Momentum",
                symbol = "XAUUSD",
                side = "BUY",
                entryPrice = 2700.0,
                stopLoss = 2660.0,
                takeProfit = 2850.0,
                exposurePct = 3.5,
                status = "OPEN"
            )
        )

        val snapshot = EventImpactEngine.buildSnapshot(headlines, conditions, quotes, orders)

        assertTrue(snapshot.centralBankCount >= 1)
        assertTrue(snapshot.geopoliticalCount >= 1)
        assertTrue(snapshot.economicNewsCount >= 2)
        assertEquals(2, snapshot.positioningRows.size)
        assertTrue(snapshot.categorizedEvents.any { it.category == EventDomainCategory.CENTRAL_BANKS })
        assertTrue(snapshot.categorizedEvents.any { it.category == EventDomainCategory.GEOPOLITICAL })
        assertTrue(snapshot.categorizedEvents.any { it.category == EventDomainCategory.ECONOMIC_NEWS })
        assertTrue(snapshot.positioningRows.first().eventVulnerabilityScore in 10..99)
    }
}
