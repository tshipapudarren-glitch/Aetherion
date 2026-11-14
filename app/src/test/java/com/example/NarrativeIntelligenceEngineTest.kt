package com.example

import com.example.data.model.CandleBar
import com.example.data.model.MarketQuote
import com.example.data.model.MarketUniverse
import com.example.data.model.NewsHeadline
import com.example.domain.engine.NarrativeIntelligenceEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeIntelligenceEngineTest {

    @Test
    fun analyzeNarratives_coversClusteringConflictsTimelinesDecayAndReactions() {
        val now = 1_760_000_000_000L
        val headlines = listOf(
            NewsHeadline(
                title = "Iran rejects ceasefire talks as missile strikes hit energy corridor near Strait of Hormuz",
                source = "Reuters / CNBC",
                publishedAt = "15m ago",
                link = "https://example.com/1",
                publishedEpochMs = now - 900_000L
            ),
            NewsHeadline(
                title = "Middle East diplomats see ceasefire truce talks reopening Gulf shipping lanes",
                source = "BBC World Geopolitics",
                publishedAt = "1h ago",
                link = "https://example.com/2",
                publishedEpochMs = now - 3_600_000L
            ),
            NewsHeadline(
                title = "WTI crude oil surges on Red Sea and Strait of Hormuz disruption risk",
                source = "Yahoo Finance Macro",
                publishedAt = "4h ago",
                link = "https://example.com/3",
                publishedEpochMs = now - 14_400_000L
            ),
            NewsHeadline(
                title = "Federal Reserve signals possible rate cut as CPI inflation cools toward target",
                source = "CNBC Finance",
                publishedAt = "2h ago",
                link = "https://example.com/4",
                publishedEpochMs = now - 7_200_000L
            ),
            NewsHeadline(
                title = "Sticky inflation warning keeps Fed hawkish and pushes Treasury yields higher",
                source = "FMP Research Wire",
                publishedAt = "6h ago",
                link = "https://example.com/5",
                publishedEpochMs = now - 21_600_000L
            )
        )

        val wtiInst = MarketUniverse.instruments.first { it.symbol == "WTI" }
        val xauInst = MarketUniverse.instruments.first { it.symbol == "XAUUSD" }
        val wtiCandles = (0 until 24).map { idx ->
            val p = 72.0 + idx * 0.25
            CandleBar(now - (24 - idx) * 3_600_000L, p, p + 0.3, p - 0.2, p + 0.1, 1000.0)
        }
        val xauCandles = (0 until 24).map { idx ->
            val p = 2650.0 + idx * 1.5
            CandleBar(now - (24 - idx) * 3_600_000L, p, p + 3.0, p - 2.0, p + 1.0, 2000.0)
        }

        val quotes = listOf(
            MarketQuote(wtiInst, 78.5, 72.0, 4.2, 79.0, 71.8, 34.0, wtiCandles, "Yahoo Finance", 120L, now, true),
            MarketQuote(xauInst, 2685.0, 2650.0, 1.32, 2690.0, 2648.0, 18.0, xauCandles, "Yahoo Finance", 110L, now, true)
        )

        val snapshot = NarrativeIntelligenceEngine.analyzeNarratives(headlines, quotes, now)

        assertTrue("Expected at least 2 narrative clusters", snapshot.clusters.size >= 2)
        assertTrue("Expected conflicting reports detected across clusters", snapshot.totalConflictsDetected >= 1)

        val geoCluster = snapshot.clusters.first { it.id == "geopolitics_energy" }
        assertTrue(geoCluster.sourceComparisons.size >= 2)
        assertTrue(geoCluster.conflictingReports.isNotEmpty())
        assertTrue(geoCluster.timelineEvents.size == 3)
        assertTrue(geoCluster.instrumentImpacts.any { it.symbol == "WTI" && it.isLive })
        assertTrue(geoCluster.decayTelemetry.projectedDecay48h.size == 9)
        assertTrue(geoCluster.marketReactions.any { it.symbol == "WTI" && it.normalizedPriceSeries.size == 8 })
    }
}
