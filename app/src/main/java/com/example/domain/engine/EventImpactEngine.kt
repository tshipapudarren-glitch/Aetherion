package com.example.domain.engine

import com.example.data.local.PaperOrderEntity
import com.example.data.model.AssetClass
import com.example.data.model.EconomicCalendarEvent
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketQuote
import com.example.data.model.NewsHeadline
import kotlin.math.abs
import kotlin.math.roundToInt

enum class EventDomainCategory(val label: String) {
    ECONOMIC_NEWS("Economic News"),
    CENTRAL_BANKS("Central Bank Decisions"),
    GEOPOLITICAL("Geopolitical Events")
}

data class ClassifiedMacroEvent(
    val id: String,
    val title: String,
    val category: EventDomainCategory,
    val source: String,
    val publishedLabel: String,
    val shockPotentialScore: Int,
    val affectedInstruments: List<String>,
    val positioningInteraction: String
)

data class MarketPositioningRow(
    val symbol: String,
    val displayName: String,
    val assetClass: AssetClass,
    val price: Double,
    val changePct: Double,
    val rsi14: Double,
    val stretchVsMa50Pct: Double,
    val postureLabel: String, // Crowded Long, Oversold Short, Balanced, Extended Short
    val netOpenPaperExposurePct: Double,
    val eventVulnerabilityScore: Int
)

data class EventImpactIntelligenceSnapshot(
    val categorizedEvents: List<ClassifiedMacroEvent> = emptyList(),
    val positioningRows: List<MarketPositioningRow> = emptyList(),
    val centralBankCount: Int = 0,
    val economicNewsCount: Int = 0,
    val geopoliticalCount: Int = 0,
    val calendarSurprisesCount: Int = 0,
    val overallPositioningRegime: String = "Balanced Positioning",
    val positioningRiskSummary: String = "Awaiting live market quotes and news wires.",
    val updatedAtMs: Long = 0L
)

object EventImpactEngine {

    fun buildSnapshot(
        headlines: List<NewsHeadline>,
        conditions: MarketConditionsSnapshot,
        quotes: List<MarketQuote>,
        paperOrders: List<PaperOrderEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): EventImpactIntelligenceSnapshot {
        val liveQuotes = quotes.filter { it.isLive }
        val positioningRows = liveQuotes.map { q ->
            val overlays = SetupArchitectEngine.computeOverlays(q.candles)
            val rsi = overlays.latestRsi14 ?: 50.0
            val ma50 = overlays.latestMa50 ?: q.price
            val stretchPct = if (ma50 > 0.0) ((q.price - ma50) / ma50) * 100.0 else 0.0
            val openExposure = paperOrders
                .filter { it.status == "OPEN" && it.symbol == q.instrument.symbol }
                .sumOf { it.exposurePct }

            val posture = when {
                rsi >= 68.0 || stretchPct >= 2.8 -> "Crowded Long"
                rsi <= 32.0 || stretchPct <= -2.8 -> "Oversold / Short Heavy"
                q.realizedVolPct >= 30.0 -> "High-Vol Two-Way"
                else -> "Balanced"
            }

            val vulnScore = (
                abs(rsi - 50.0) * 1.45 +
                    abs(stretchPct) * 6.5 +
                    q.realizedVolPct * 0.55 +
                    openExposure * 2.0
                ).roundToInt().coerceIn(10, 99)

            MarketPositioningRow(
                symbol = q.instrument.symbol,
                displayName = q.instrument.displayName,
                assetClass = q.instrument.assetClass,
                price = q.price,
                changePct = q.changePct,
                rsi14 = rsi,
                stretchVsMa50Pct = stretchPct,
                postureLabel = posture,
                netOpenPaperExposurePct = openExposure,
                eventVulnerabilityScore = vulnScore
            )
        }.sortedByDescending { it.eventVulnerabilityScore }

        val crowdedCount = positioningRows.count { it.postureLabel.startsWith("Crowded") || it.postureLabel.startsWith("Oversold") }
        val avgVuln = positioningRows.map { it.eventVulnerabilityScore }.ifEmpty { listOf(40) }.average().roundToInt()

        val calendarClassified = conditions.economicEvents.take(10).mapIndexed { idx, ev ->
            classifyCalendarEvent(idx, ev, positioningRows)
        }
        val headlineClassified = headlines.take(24).mapIndexed { idx, h ->
            classifyHeadline(idx, h, positioningRows)
        }

        val combined = (calendarClassified + headlineClassified)
            .sortedByDescending { it.shockPotentialScore }

        val cbCount = combined.count { it.category == EventDomainCategory.CENTRAL_BANKS }
        val ecoCount = combined.count { it.category == EventDomainCategory.ECONOMIC_NEWS }
        val geoCount = combined.count { it.category == EventDomainCategory.GEOPOLITICAL }

        val overallPosture = when {
            crowdedCount >= 4 -> "Stretched / Squeeze-Prone ($crowdedCount extended)"
            avgVuln >= 65 -> "Elevated Event Sensitivity ($avgVuln/100)"
            positioningRows.isEmpty() -> "Disconnected"
            else -> "Balanced Liquidity Posture ($avgVuln/100)"
        }

        val summary = buildString {
            conditions.fedFundsRate?.let { append("Fed Funds ${String.format("%.2f%%", it)} · ") }
            conditions.us10yYield?.let { append("US10Y ${String.format("%.2f%%", it)} (${conditions.yieldCurveStatus}) · ") }
            val topVuln = positioningRows.firstOrNull()
            if (topVuln != null) {
                append("Peak Sensitivity: ${topVuln.symbol} (${topVuln.postureLabel}, ${topVuln.eventVulnerabilityScore}/100).")
            } else {
                append("Sync live markets to compute cross-asset positioning sensitivity.")
            }
        }

        return EventImpactIntelligenceSnapshot(
            categorizedEvents = combined,
            positioningRows = positioningRows,
            centralBankCount = cbCount,
            economicNewsCount = ecoCount,
            geopoliticalCount = geoCount,
            calendarSurprisesCount = conditions.economicEvents.count { it.actual.isNotBlank() && it.actual != "—" },
            overallPositioningRegime = overallPosture,
            positioningRiskSummary = summary,
            updatedAtMs = nowMs
        )
    }

    private fun classifyCalendarEvent(
        idx: Int,
        ev: EconomicCalendarEvent,
        positioning: List<MarketPositioningRow>
    ): ClassifiedMacroEvent {
        val lower = ev.eventName.lowercase()
        val category = when {
            lower.contains("fed") || lower.contains("fomc") || lower.contains("ecb") ||
                lower.contains("boe") || lower.contains("boj") || lower.contains("rate") -> EventDomainCategory.CENTRAL_BANKS
            else -> EventDomainCategory.ECONOMIC_NEWS
        }
        val baseScore = when (ev.impact.lowercase()) {
            "high" -> 82
            "medium" -> 58
            else -> 36
        }
        val instruments = mapInstruments(lower)
        return ClassifiedMacroEvent(
            id = "cal_${idx}_${ev.eventName.hashCode()}",
            title = "${ev.country} ${ev.eventName} (Act: ${ev.actual.ifBlank { "Pending" }} / Est: ${ev.estimate.ifBlank { "—" }})",
            category = category,
            source = ev.sourceName,
            publishedLabel = ev.timeUtc.ifBlank { "Scheduled" },
            shockPotentialScore = baseScore,
            affectedInstruments = instruments,
            positioningInteraction = positioningSummaryFor(instruments, positioning)
        )
    }

    private fun classifyHeadline(
        idx: Int,
        h: NewsHeadline,
        positioning: List<MarketPositioningRow>
    ): ClassifiedMacroEvent {
        val lower = "${h.title} ${h.summary}".lowercase()
        val category = when {
            lower.contains("fed") || lower.contains("fomc") || lower.contains("powell") ||
                lower.contains("central bank") || lower.contains("ecb") || lower.contains("boj") ||
                lower.contains("boe") || lower.contains("rate cut") || lower.contains("rate hike") ||
                lower.contains("treasury") || lower.contains("yield") -> EventDomainCategory.CENTRAL_BANKS

            lower.contains("war") || lower.contains("iran") || lower.contains("israel") ||
                lower.contains("ukraine") || lower.contains("tariff") || lower.contains("sanction") ||
                lower.contains("hormuz") || lower.contains("missile") || lower.contains("ceasefire") ||
                lower.contains("geopolit") || lower.contains("opec") -> EventDomainCategory.GEOPOLITICAL

            else -> EventDomainCategory.ECONOMIC_NEWS
        }

        val instruments = mapInstruments(lower)
        val topVuln = positioning.filter { it.symbol in instruments }.maxOfOrNull { it.eventVulnerabilityScore } ?: 45
        val baseSeverity = when (category) {
            EventDomainCategory.GEOPOLITICAL -> 68
            EventDomainCategory.CENTRAL_BANKS -> 66
            EventDomainCategory.ECONOMIC_NEWS -> 54
        }
        val shockScore = ((baseSeverity * 0.65) + (topVuln * 0.35)).roundToInt().coerceIn(20, 98)

        return ClassifiedMacroEvent(
            id = "news_${idx}_${h.title.hashCode()}",
            title = h.title,
            category = category,
            source = h.source,
            publishedLabel = h.publishedAt,
            shockPotentialScore = shockScore,
            affectedInstruments = instruments,
            positioningInteraction = positioningSummaryFor(instruments, positioning)
        )
    }

    private fun mapInstruments(lowerText: String): List<String> {
        val out = mutableListOf<String>()
        if (lowerText.contains("gold") || lowerText.contains("haven")) out.add("XAUUSD")
        if (lowerText.contains("oil") || lowerText.contains("opec") || lowerText.contains("hormuz") || lowerText.contains("crude")) out.add("WTI")
        if (lowerText.contains("fed") || lowerText.contains("yield") || lowerText.contains("cpi") || lowerText.contains("inflation") || lowerText.contains("dollar")) {
            out.addAll(listOf("EURUSD", "USDJPY", "US10Y", "XAUUSD"))
        }
        if (lowerText.contains("tech") || lowerText.contains("nvidia") || lowerText.contains("stock") || lowerText.contains("nasdaq") || lowerText.contains("s&p")) {
            out.addAll(listOf("NAS100", "SPX500", "NVDA"))
        }
        if (lowerText.contains("bitcoin") || lowerText.contains("crypto") || lowerText.contains("btc")) {
            out.addAll(listOf("BTCUSD", "ETHUSD"))
        }
        return out.distinct().ifEmpty { listOf("XAUUSD", "SPX500", "EURUSD") }.take(4)
    }

    private fun positioningSummaryFor(
        instruments: List<String>,
        positioning: List<MarketPositioningRow>
    ): String {
        val matched = positioning.filter { it.symbol in instruments }
        if (matched.isEmpty()) return "Positioning telemetry offline for linked pairs."
        val lead = matched.maxByOrNull { it.eventVulnerabilityScore } ?: return "Balanced positioning."
        return "${lead.symbol}: ${lead.postureLabel} · Sensitivity ${lead.eventVulnerabilityScore}/100"
    }
}
