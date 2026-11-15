package com.example.domain.engine

import com.example.data.local.PaperOrderEntity
import com.example.data.local.RiskConstitutionEntity
import com.example.data.model.CandleBar
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketQuote
import com.example.data.model.StrategyStage
import com.example.data.remote.EventImpactAnalysis
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SetupIndicatorBadge(
    val id: String,
    val label: String,
    val valueText: String,
    val role: String // ENTRY, SL, TP1, TP2, TP3, OVERLAY
)

data class ChartTechnicalOverlays(
    val ma50: List<Double?> = emptyList(),
    val ma200: List<Double?> = emptyList(),
    val volSma9: List<Double?> = emptyList(),
    val rsi14: List<Double?> = emptyList(),
    val rsiSma14: List<Double?> = emptyList(),
    val latestMa50: Double? = null,
    val latestMa200: Double? = null,
    val latestRsi14: Double? = null,
    val latestVolSma9: Double? = null
)

data class TradeSetupPlan(
    val symbol: String,
    val direction: String, // LONG or SHORT
    val entryPrice: Double,
    val stopLoss: Double,
    val tp1: Double,
    val tp2: Double,
    val tp3: Double,
    val riskDistance: Double,
    val riskRewardTp1: Double,
    val riskRewardTp2: Double,
    val riskRewardTp3: Double,
    val atr14: Double,
    val ma50: Double?,
    val rsi14: Double?,
    val confidencePct: Int,
    val positioningLabel: String,
    val macroAlignmentNote: String,
    val structuralRationale: String,
    val riskGuardApproved: Boolean,
    val riskGuardNote: String,
    val timeframeLabel: String = "1H",
    val isAiRefined: Boolean = false,
    val providerLabel: String = "Setup Architect Quant Engine",
    val drawnIndicators: List<SetupIndicatorBadge> = emptyList(),
    val overlays: ChartTechnicalOverlays = ChartTechnicalOverlays(),
    val createdAtMs: Long = System.currentTimeMillis()
)

object SetupArchitectEngine {

    fun computeOverlays(candles: List<CandleBar>): ChartTechnicalOverlays {
        if (candles.isEmpty()) return ChartTechnicalOverlays()
        val closes = candles.map { it.close }
        val volumes = candles.map { it.volume }
        val ma50 = computeRollingSma(closes, 50)
        val ma200 = computeRollingSma(closes, 200)
        val volSma9 = computeRollingSma(volumes, 9)
        val rsi14 = computeRollingRsi(closes, 14)
        val rsiSma14 = computeRollingSmaOnNullable(rsi14, 14)

        return ChartTechnicalOverlays(
            ma50 = ma50,
            ma200 = ma200,
            volSma9 = volSma9,
            rsi14 = rsi14,
            rsiSma14 = rsiSma14,
            latestMa50 = ma50.lastOrNull { it != null },
            latestMa200 = ma200.lastOrNull { it != null },
            latestRsi14 = rsi14.lastOrNull { it != null },
            latestVolSma9 = volSma9.lastOrNull { it != null }
        )
    }

    fun architectSetup(
        quote: MarketQuote,
        conditions: MarketConditionsSnapshot = MarketConditionsSnapshot(),
        eventAnalysis: EventImpactAnalysis? = null,
        paperOrders: List<PaperOrderEntity> = emptyList(),
        constitution: RiskConstitutionEntity = RiskConstitutionEntity(),
        forcedDirection: String? = null,
        timeframeLabel: String = "1H",
        nowMs: Long = System.currentTimeMillis()
    ): TradeSetupPlan? {
        if (!quote.isLive || quote.price <= 0.0) return null
        val candles = quote.candles
        val overlays = computeOverlays(candles)
        val entry = quote.price
        val atr14 = max(computeLatestAtr14(candles, 14), entry * 0.0025)

        val ma50 = overlays.latestMa50 ?: entry
        val rsi14 = overlays.latestRsi14 ?: 50.0
        val trendUp = entry >= ma50
        val eventBiasText = eventAnalysis?.directionalBias?.lowercase().orEmpty()
        val eventFavorsBearish = eventBiasText.contains("bearish") || eventBiasText.contains("risk-off") || eventBiasText.contains("downside")
        val eventFavorsBullish = eventBiasText.contains("bullish") || eventBiasText.contains("risk-on") || eventBiasText.contains("upside")

        val direction = when (forcedDirection?.uppercase()) {
            "LONG" -> "LONG"
            "SHORT" -> "SHORT"
            else -> {
                var bullPoints = 0
                if (trendUp) bullPoints += 2 else bullPoints -= 2
                if (rsi14 in 45.0..68.0) bullPoints += 1 else if (rsi14 > 72.0 || rsi14 < 34.0) bullPoints -= 1
                if (quote.changePct >= 0.0) bullPoints += 1 else bullPoints -= 1
                if (eventFavorsBullish) bullPoints += 1
                if (eventFavorsBearish) bullPoints -= 1
                if (bullPoints >= 0) "LONG" else "SHORT"
            }
        }

        val lookback = candles.takeLast(18)
        val swingLow = lookback.minOfOrNull { it.low } ?: (entry - atr14 * 1.5)
        val swingHigh = lookback.maxOfOrNull { it.high } ?: (entry + atr14 * 1.5)

        val sl: Double
        val riskDist: Double
        val tp1: Double
        val tp2: Double
        val tp3: Double

        if (direction == "LONG") {
            val structuralSl = min(swingLow - atr14 * 0.25, entry - atr14 * 1.35)
            riskDist = max(entry - structuralSl, atr14 * 1.1).coerceAtMost(entry * 0.12)
            sl = (entry - riskDist).coerceAtLeast(entry * 0.01)
            tp1 = entry + riskDist * 1.2
            tp2 = entry + riskDist * 2.2
            tp3 = entry + riskDist * 3.5
        } else {
            val structuralSl = max(swingHigh + atr14 * 0.25, entry + atr14 * 1.35)
            riskDist = max(structuralSl - entry, atr14 * 1.1).coerceAtMost(entry * 0.12)
            sl = entry + riskDist
            tp1 = (entry - riskDist * 1.2).coerceAtLeast(entry * 0.01)
            tp2 = (entry - riskDist * 2.2).coerceAtLeast(entry * 0.01)
            tp3 = (entry - riskDist * 3.5).coerceAtLeast(entry * 0.01)
        }

        val openForSymbol = paperOrders.filter { it.status == "OPEN" && it.symbol == quote.instrument.symbol }
        val openExposurePct = openForSymbol.sumOf { it.exposurePct }
        val positioningLabel = when {
            rsi14 >= 70.0 -> "Crowded Long Stretch (RSI ${rsi14.roundToInt()})"
            rsi14 <= 30.0 -> "Capitulation / Oversold (RSI ${rsi14.roundToInt()})"
            openExposurePct > 0.0 -> "Active Exposure ${String.format("%.1f%%", openExposurePct)} · RSI ${rsi14.roundToInt()}"
            trendUp -> "Constructive Trend Alignment (RSI ${rsi14.roundToInt()})"
            else -> "Distribution / Defensive Positioning (RSI ${rsi14.roundToInt()})"
        }

        val macroNote = buildString {
            append("Regime: ${conditions.overallRegime}")
            conditions.vixLevel?.let { append(" · VIX ${String.format("%.1f", it)}") }
            conditions.us10yYield?.let { append(" · US10Y ${String.format("%.2f%%", it)}") }
            eventAnalysis?.let { append(" · Event Severity ${it.severityScore}/100") }
        }

        val openAll = paperOrders.filter { it.status == "OPEN" }
        val riskCheck = DeterministicRiskEngine().evaluateExecutionProposal(
            constitution = constitution,
            strategyStage = StrategyStage.PAPER_TRADING,
            proposedExposurePct = 2.0,
            currentExposurePct = openAll.sumOf { it.exposurePct },
            openPositionsCount = openAll.size,
            estimatedSpreadBps = 6.0,
            estimatedSlippageBps = 4.0,
            isHighImpactNewsWindow = (eventAnalysis?.severityScore ?: 0) >= 80,
            isLiveBrokerRoute = false
        )

        var confidence = 64
        if ((direction == "LONG" && trendUp) || (direction == "SHORT" && !trendUp)) confidence += 10
        if (rsi14 in 40.0..66.0) confidence += 7
        if ((direction == "LONG" && eventFavorsBullish) || (direction == "SHORT" && eventFavorsBearish)) confidence += 6
        if (!riskCheck.approved) confidence -= 15
        confidence = confidence.coerceIn(28, 94)

        val indicators = buildIndicatorList(entry, sl, tp1, tp2, tp3, ma50, rsi14)

        return TradeSetupPlan(
            symbol = quote.instrument.symbol,
            direction = direction,
            entryPrice = entry,
            stopLoss = sl,
            tp1 = tp1,
            tp2 = tp2,
            tp3 = tp3,
            riskDistance = riskDist,
            riskRewardTp1 = 1.2,
            riskRewardTp2 = 2.2,
            riskRewardTp3 = 3.5,
            atr14 = atr14,
            ma50 = overlays.latestMa50,
            rsi14 = overlays.latestRsi14,
            confidencePct = confidence,
            positioningLabel = positioningLabel,
            macroAlignmentNote = macroNote,
            structuralRationale = "Auto-drawn from ${candles.size} live candles: Swing liquidity guard (${formatPrice(sl)}) vs 1.2R/2.2R/3.5R ATR expansion targets.",
            riskGuardApproved = riskCheck.approved,
            riskGuardNote = riskCheck.reason,
            timeframeLabel = timeframeLabel,
            isAiRefined = false,
            providerLabel = "Setup Architect Quant Engine",
            drawnIndicators = indicators,
            overlays = overlays,
            createdAtMs = nowMs
        )
    }

    fun applyAiRefinement(
        base: TradeSetupPlan,
        direction: String,
        entryPrice: Double,
        stopLoss: Double,
        tp1: Double,
        tp2: Double,
        tp3: Double,
        confidencePct: Int,
        structuralThesis: String,
        eventAlignmentNote: String,
        providerUsed: String
    ): TradeSetupPlan {
        val dir = if (direction.uppercase() == "SHORT") "SHORT" else "LONG"
        val safeEntry = if (entryPrice > 0.0) entryPrice else base.entryPrice
        val validSl = if (dir == "LONG") {
            if (stopLoss in (safeEntry * 0.5)..(safeEntry * 0.998)) stopLoss else base.stopLoss
        } else {
            if (stopLoss in (safeEntry * 1.002)..(safeEntry * 1.5)) stopLoss else base.stopLoss
        }
        val risk = max(abs(safeEntry - validSl), safeEntry * 0.002)
        val validTp1 = validateTarget(dir, safeEntry, tp1, risk * 1.2, base.tp1)
        val validTp2 = validateTarget(dir, safeEntry, tp2, risk * 2.2, base.tp2)
        val validTp3 = validateTarget(dir, safeEntry, tp3, risk * 3.5, base.tp3)

        val indicators = buildIndicatorList(safeEntry, validSl, validTp1, validTp2, validTp3, base.ma50, base.rsi14)

        return base.copy(
            direction = dir,
            entryPrice = safeEntry,
            stopLoss = validSl,
            tp1 = validTp1,
            tp2 = validTp2,
            tp3 = validTp3,
            riskDistance = risk,
            riskRewardTp1 = abs(validTp1 - safeEntry) / risk,
            riskRewardTp2 = abs(validTp2 - safeEntry) / risk,
            riskRewardTp3 = abs(validTp3 - safeEntry) / risk,
            confidencePct = confidencePct.coerceIn(15, 98),
            structuralRationale = structuralThesis,
            macroAlignmentNote = eventAlignmentNote,
            isAiRefined = true,
            providerLabel = providerUsed,
            drawnIndicators = indicators,
            createdAtMs = System.currentTimeMillis()
        )
    }

    private fun validateTarget(dir: String, entry: Double, candidate: Double, fallbackOffset: Double, baseTarget: Double): Double {
        return if (dir == "LONG") {
            if (candidate > entry * 1.0005 && candidate < entry * 2.0) candidate else max(baseTarget, entry + fallbackOffset)
        } else {
            if (candidate > 0.0 && candidate < entry * 0.9995) candidate else min(baseTarget, (entry - fallbackOffset).coerceAtLeast(entry * 0.01))
        }
    }

    private fun buildIndicatorList(
        entry: Double,
        sl: Double,
        tp1: Double,
        tp2: Double,
        tp3: Double,
        ma50: Double?,
        rsi14: Double?
    ): List<SetupIndicatorBadge> = buildList {
        add(SetupIndicatorBadge("ENTRY", "ENTRY ZONE", formatPrice(entry), "ENTRY"))
        add(SetupIndicatorBadge("SL", "STOP LOSS", formatPrice(sl), "SL"))
        add(SetupIndicatorBadge("TP1", "TP1 (1.2R)", formatPrice(tp1), "TP1"))
        add(SetupIndicatorBadge("TP2", "TP2 (2.2R)", formatPrice(tp2), "TP2"))
        add(SetupIndicatorBadge("TP3", "TP3 (3.5R)", formatPrice(tp3), "TP3"))
        if (ma50 != null) add(SetupIndicatorBadge("MA50", "MA 50", formatPrice(ma50), "OVERLAY"))
        if (rsi14 != null) add(SetupIndicatorBadge("RSI14", "RSI 14", String.format("%.1f", rsi14), "OVERLAY"))
    }

    fun formatPrice(price: Double): String = when {
        price >= 1000.0 -> String.format("%.2f", price)
        price >= 10.0 -> String.format("%.3f", price)
        else -> String.format("%.4f", price)
    }

    private fun computeRollingSma(values: List<Double>, period: Int): List<Double?> {
        if (values.isEmpty()) return emptyList()
        val out = MutableList<Double?>(values.size) { null }
        val effectivePeriod = period.coerceAtLeast(2)
        var runningSum = 0.0
        for (i in values.indices) {
            runningSum += values[i]
            if (i >= effectivePeriod) {
                runningSum -= values[i - effectivePeriod]
            }
            val count = min(i + 1, effectivePeriod)
            if (count >= min(effectivePeriod, 8) || i >= effectivePeriod - 1) {
                out[i] = runningSum / count
            }
        }
        return out
    }

    private fun computeRollingSmaOnNullable(values: List<Double?>, period: Int): List<Double?> {
        val out = MutableList<Double?>(values.size) { null }
        val buf = ArrayDeque<Double>()
        for (i in values.indices) {
            val v = values[i]
            if (v != null) {
                buf.addLast(v)
                if (buf.size > period) buf.removeFirst()
                if (buf.size >= 4) out[i] = buf.average()
            }
        }
        return out
    }

    private fun computeRollingRsi(closes: List<Double>, period: Int = 14): List<Double?> {
        if (closes.size < 3) return List(closes.size) { 50.0 }
        val out = MutableList<Double?>(closes.size) { null }
        var avgGain = 0.0
        var avgLoss = 0.0
        val effective = min(period, closes.size - 1).coerceAtLeast(2)

        for (i in 1..effective) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) avgGain += diff else avgLoss -= diff
        }
        avgGain /= effective
        avgLoss /= effective
        out[effective] = if (avgLoss < 1e-9) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))

        for (i in (effective + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = (avgGain * (effective - 1) + gain) / effective
            avgLoss = (avgLoss * (effective - 1) + loss) / effective
            out[i] = if (avgLoss < 1e-9) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        }
        return out
    }

    private fun computeLatestAtr14(candles: List<CandleBar>, period: Int = 14): Double {
        if (candles.isEmpty()) return 0.0
        if (candles.size == 1) return abs(candles.first().high - candles.first().low)
        val trueRanges = DoubleArray(candles.size)
        trueRanges[0] = abs(candles[0].high - candles[0].low)
        for (i in 1 until candles.size) {
            val prevClose = candles[i - 1].close
            val hl = candles[i].high - candles[i].low
            val hpc = abs(candles[i].high - prevClose)
            val lpc = abs(candles[i].low - prevClose)
            trueRanges[i] = max(hl, max(hpc, lpc))
        }
        val window = trueRanges.takeLast(min(period, candles.size))
        return window.average()
    }
}
