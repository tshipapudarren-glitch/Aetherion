package com.example

import com.example.data.local.LocalStrategyEntity
import com.example.data.local.PaperOrderEntity
import com.example.data.model.CandleBar
import com.example.data.model.MarketQuote
import com.example.data.model.MarketUniverse
import com.example.data.model.StrategyStage
import com.example.domain.engine.MarketStructureEngine
import com.example.domain.engine.QuantCodeTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStructureAndQuantLabTest {

    @Test
    fun marketStructureEngine_scansAll15SetupsAndComputesAccountAndDepth() {
        val now = 1_760_000_000_000L
        val inst = MarketUniverse.instruments.first { it.symbol == "EURUSD" }
        val candles = (0 until 45).map { i ->
            val close = 1.0800 + i * 0.0004
            val open = close - 0.0002
            CandleBar(now - (45 - i) * 3_600_000L, open, close + 0.0006, open - 0.0004, close, 5000.0 + i * 25.0)
        }
        val quote = MarketQuote(
            instrument = inst,
            price = candles.last().close,
            previousClose = candles.first().close,
            changePct = 1.25,
            high24h = candles.maxOf { it.high },
            low24h = candles.minOf { it.low },
            realizedVolPct = 14.2,
            candles = candles,
            sourceName = "Yahoo Finance",
            latencyMs = 80L,
            fetchedAtMs = now,
            isLive = true
        )
        val orders = listOf(
            PaperOrderEntity(
                id = "t1",
                strategyId = "terminal_ticket",
                strategyName = "Macro Terminal Ticket",
                symbol = "EURUSD",
                side = "BUY",
                entryPrice = 1.0800,
                stopLoss = 1.0750,
                takeProfit = 1.1050,
                exposurePct = 2.0,
                status = "OPEN"
            ),
            PaperOrderEntity(
                id = "t2",
                strategyId = "terminal_ticket",
                strategyName = "Closed Winner",
                symbol = "EURUSD",
                side = "BUY",
                entryPrice = 1.0700,
                stopLoss = 1.0660,
                takeProfit = 1.0800,
                exposurePct = 2.0,
                status = "CLOSED",
                pnlPct = 12.5
            )
        )

        val snap = MarketStructureEngine.analyze(quote, listOf(quote), orders, now)
        assertEquals(15, snap.detectedSetups.size)
        assertEquals((1..15).toList(), snap.detectedSetups.map { it.number })
        assertEquals(5, snap.spreadDepth.size)
        assertEquals(100, snap.bullishSentimentPct + snap.bearishSentimentPct)
        assertTrue(snap.accountSummary.equityUsd > 100_000.0)
        assertEquals(1, snap.accountSummary.openPositionsCount)
        assertEquals(1, snap.accountSummary.closedTradesCount)
    }

    @Test
    fun quantCodeLab_generatesPineV6Mql5EaAndPassesSafetyAudit() {
        val strat = LocalStrategyEntity(
            id = "s-test",
            name = "Aether Gold Impulse",
            assetClass = "Precious Metals",
            symbol = "XAUUSD",
            timeframe = "1H",
            stage = StrategyStage.PAPER_TRADING.name,
            sharpeRatio = 1.85,
            profitFactor = 1.72,
            maxDrawdownPct = 4.1,
            winRatePct = 58.0,
            sampleSize = 120,
            genomeSummary = "EMA(10/34) + ATR 1.5x/3.0x",
            creatorAgent = "GENOME",
            fastPeriod = 10,
            slowPeriod = 34,
            atrStopMult = 1.5,
            atrTargetMult = 3.0
        )

        val pine = MarketStructureEngine.generateQuantTemplate(strat, QuantCodeTarget.PINE_V6)
        assertTrue(pine.contains("//@version=6"))
        assertTrue(pine.contains("barstate.isconfirmed"))
        val pineAudit = MarketStructureEngine.auditQuantSource(pine)
        assertTrue(pineAudit.all { it.startsWith("PASS") })

        val ea = MarketStructureEngine.generateQuantTemplate(strat, QuantCodeTarget.MQL5_EA)
        assertTrue(ea.contains("#include <Trade/Trade.mqh>"))
        assertTrue(ea.contains("SYMBOL_SPREAD"))
        val eaAudit = MarketStructureEngine.auditQuantSource(ea)
        assertTrue(eaAudit.all { it.startsWith("PASS") })
    }
}
