package com.example.domain.engine

import com.example.data.model.CandleBar
import com.example.data.model.MarketQuote
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class BacktestTrade(
    val entryIndex: Int,
    val exitIndex: Int,
    val entryTimestampMs: Long,
    val exitTimestampMs: Long,
    val side: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val pnlPct: Double,
    val mfePct: Double,
    val maePct: Double
)

data class BacktestReport(
    val symbol: String,
    val fastPeriod: Int,
    val slowPeriod: Int,
    val atrStopMult: Double,
    val atrTargetMult: Double,
    val totalTrades: Int,
    val winRatePct: Double,
    val profitFactor: Double,
    val sharpeRatio: Double,
    val oosSharpeRatio: Double,
    val maxDrawdownPct: Double,
    val stressTestReturnPct: Double,
    val stressTestSurvived: Boolean,
    val equityCurve: List<Double>,
    val trades: List<BacktestTrade>
)

data class MultiverseScenarioReport(
    val regimeName: String,
    val spotPrice: Double,
    val annualizedVolPct: Double,
    val horizonBars: Int,
    val pathCount: Int,
    val p5Curve: List<Double>,
    val p25Curve: List<Double>,
    val p50Curve: List<Double>,
    val p75Curve: List<Double>,
    val p95Curve: List<Double>,
    val var95Pct: Double,
    val cvar95Pct: Double,
    val survivalRatePct: Double
)

data class BlackSwanTelemetry(
    val symbol: String,
    val tailRiskScore: Int, // 0..100
    val realizedVolPct: Double,
    val volZScore: Double,
    val excessKurtosis: Double,
    val worstBarShockPct: Double,
    val regimeStatus: String
)

data class AllocationSlice(
    val label: String,
    val symbol: String,
    val weightPct: Double,
    val riskBudgetPct: Double,
    val volatilityPct: Double,
    val kellyFractionPct: Double
)

data class CurrencyStrengthItem(
    val code: String,
    val strengthScore: Double
)

object QuantitativeEngines {

    fun runBacktest(
        symbol: String,
        candles: List<CandleBar>,
        fastPeriod: Int,
        slowPeriod: Int,
        atrStopMult: Double,
        atrTargetMult: Double,
        costBpsPerTrade: Double = 8.0
    ): BacktestReport {
        if (candles.size < slowPeriod + 10) {
            return BacktestReport(
                symbol, fastPeriod, slowPeriod, atrStopMult, atrTargetMult,
                0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, listOf(100.0), emptyList()
            )
        }

        val closes = candles.map { it.close }
        val fastEma = computeEma(closes, fastPeriod)
        val slowEma = computeEma(closes, slowPeriod)
        val atr = computeAtr(candles, 14)

        val trades = mutableListOf<BacktestTrade>()
        val equity = mutableListOf(100.0)
        var currentEq = 100.0
        var peakEq = 100.0
        var maxDdPct = 0.0

        var inPosition = false
        var isLong = true
        var entryIdx = 0
        var entryPrice = 0.0
        var stopPrice = 0.0
        var targetPrice = 0.0
        var bestMfe = 0.0
        var worstMae = 0.0

        val costFraction = costBpsPerTrade / 10000.0

        for (i in (slowPeriod + 1) until candles.size) {
            val bar = candles[i]
            if (!inPosition) {
                val crossUp = fastEma[i - 1] <= slowEma[i - 1] && fastEma[i] > slowEma[i]
                val crossDown = fastEma[i - 1] >= slowEma[i - 1] && fastEma[i] < slowEma[i]
                val currentAtr = max(atr[i], bar.close * 0.002)
                if (crossUp) {
                    inPosition = true
                    isLong = true
                    entryIdx = i
                    entryPrice = bar.close
                    stopPrice = entryPrice - currentAtr * atrStopMult
                    targetPrice = entryPrice + currentAtr * atrTargetMult
                    bestMfe = 0.0
                    worstMae = 0.0
                } else if (crossDown) {
                    inPosition = true
                    isLong = false
                    entryIdx = i
                    entryPrice = bar.close
                    stopPrice = entryPrice + currentAtr * atrStopMult
                    targetPrice = entryPrice - currentAtr * atrTargetMult
                    bestMfe = 0.0
                    worstMae = 0.0
                }
            } else {
                val favorable = if (isLong) (bar.high - entryPrice) / entryPrice * 100.0 else (entryPrice - bar.low) / entryPrice * 100.0
                val adverse = if (isLong) (bar.low - entryPrice) / entryPrice * 100.0 else (entryPrice - bar.high) / entryPrice * 100.0
                bestMfe = max(bestMfe, favorable)
                worstMae = min(worstMae, adverse)

                var exitPrice: Double? = null
                if (isLong) {
                    if (bar.low <= stopPrice) exitPrice = stopPrice
                    else if (bar.high >= targetPrice) exitPrice = targetPrice
                    else if (fastEma[i] < slowEma[i]) exitPrice = bar.close
                } else {
                    if (bar.high >= stopPrice) exitPrice = stopPrice
                    else if (bar.low <= targetPrice) exitPrice = targetPrice
                    else if (fastEma[i] > slowEma[i]) exitPrice = bar.close
                }

                if (exitPrice != null || i == candles.lastIndex) {
                    val actualExit = exitPrice ?: bar.close
                    val rawRet = if (isLong) (actualExit - entryPrice) / entryPrice else (entryPrice - actualExit) / entryPrice
                    val netRetPct = (rawRet - costFraction) * 100.0
                    currentEq *= (1.0 + netRetPct / 100.0)
                    peakEq = max(peakEq, currentEq)
                    val dd = ((peakEq - currentEq) / peakEq) * 100.0
                    maxDdPct = max(maxDdPct, dd)
                    equity.add(currentEq)
                    trades.add(
                        BacktestTrade(
                            entryIndex = entryIdx,
                            exitIndex = i,
                            entryTimestampMs = candles[entryIdx].timestampMs,
                            exitTimestampMs = bar.timestampMs,
                            side = if (isLong) "BUY" else "SELL",
                            entryPrice = entryPrice,
                            exitPrice = actualExit,
                            stopLoss = stopPrice,
                            takeProfit = targetPrice,
                            pnlPct = netRetPct,
                            mfePct = bestMfe,
                            maePct = worstMae
                        )
                    )
                    inPosition = false
                }
            }
        }

        val wins = trades.filter { it.pnlPct > 0.0 }
        val losses = trades.filter { it.pnlPct <= 0.0 }
        val winRate = if (trades.isNotEmpty()) (wins.size.toDouble() / trades.size) * 100.0 else 0.0
        val grossWin = wins.sumOf { it.pnlPct }
        val grossLoss = abs(losses.sumOf { it.pnlPct })
        val profitFactor = if (grossLoss > 0.0) grossWin / grossLoss else if (grossWin > 0.0) 3.0 else 0.0

        val sharpe = computeTradeSharpe(trades.map { it.pnlPct })
        val splitIdx = (trades.size * 0.7).toInt()
        val oosTrades = if (splitIdx < trades.size) trades.drop(splitIdx) else trades
        val oosSharpe = computeTradeSharpe(oosTrades.map { it.pnlPct })

        // Stress test: apply 2.5x slippage/spread penalty per trade
        val stressReturnPct = trades.fold(100.0) { acc, t ->
            acc * (1.0 + (t.pnlPct - (costBpsPerTrade * 1.5 / 100.0)) / 100.0)
        } - 100.0
        val stressSurvived = stressReturnPct > -5.0 && maxDdPct < 15.0

        return BacktestReport(
            symbol = symbol,
            fastPeriod = fastPeriod,
            slowPeriod = slowPeriod,
            atrStopMult = atrStopMult,
            atrTargetMult = atrTargetMult,
            totalTrades = trades.size,
            winRatePct = winRate,
            profitFactor = profitFactor.coerceIn(0.0, 25.0),
            sharpeRatio = sharpe,
            oosSharpeRatio = oosSharpe,
            maxDrawdownPct = maxDdPct,
            stressTestReturnPct = stressReturnPct,
            stressTestSurvived = stressSurvived,
            equityCurve = equity,
            trades = trades
        )
    }

    fun runMultiverseSimulation(
        spotPrice: Double,
        annualizedVolPct: Double,
        regimeName: String,
        pathCount: Int = 350,
        horizonBars: Int = 48,
        seed: Int = 42
    ): MultiverseScenarioReport {
        val safeSpot = if (spotPrice > 0.0) spotPrice else 100.0
        val baseVol = (annualizedVolPct / 100.0).coerceIn(0.05, 1.50)
        val (driftAnn, volMult, jumpProb, jumpMean, jumpStd) = when (regimeName) {
            "Liquidity Crunch" -> listOf(-0.22, 1.85, 0.08, -0.025, 0.035)
            "Stagflation Shock" -> listOf(-0.08, 1.45, 0.05, -0.015, 0.025)
            "Flash Crash Tail" -> listOf(-0.35, 2.30, 0.12, -0.04, 0.045)
            "Bull Expansion" -> listOf(0.24, 0.90, 0.03, 0.015, 0.015)
            else -> listOf(0.04, 1.05, 0.02, 0.0, 0.012) // Base Live Drift
        }

        val dt = 1.0 / (252.0 * 6.0)
        val rng = Random(seed + regimeName.hashCode())
        val paths = Array(pathCount) { DoubleArray(horizonBars + 1) }

        var survivedCount = 0
        for (p in 0 until pathCount) {
            paths[p][0] = safeSpot
            var minPathPrice = safeSpot
            for (t in 1..horizonBars) {
                val z = nextGaussian(rng)
                val effVol = baseVol * volMult
                var jump = 0.0
                if (rng.nextDouble() < jumpProb) {
                    jump = jumpMean + jumpStd * nextGaussian(rng)
                }
                val logRet = (driftAnn - 0.5 * effVol * effVol) * dt + effVol * sqrt(dt) * z + jump
                val nextP = (paths[p][t - 1] * exp(logRet)).coerceAtLeast(safeSpot * 0.25)
                paths[p][t] = nextP
                minPathPrice = min(minPathPrice, nextP)
            }
            if ((safeSpot - minPathPrice) / safeSpot < 0.08) {
                survivedCount++
            }
        }

        val p5 = mutableListOf<Double>()
        val p25 = mutableListOf<Double>()
        val p50 = mutableListOf<Double>()
        val p75 = mutableListOf<Double>()
        val p95 = mutableListOf<Double>()

        for (t in 0..horizonBars) {
            val slice = DoubleArray(pathCount) { paths[it][t] }.sortedArray()
            p5.add(slice[(pathCount * 0.05).toInt().coerceIn(0, pathCount - 1)])
            p25.add(slice[(pathCount * 0.25).toInt().coerceIn(0, pathCount - 1)])
            p50.add(slice[(pathCount * 0.50).toInt().coerceIn(0, pathCount - 1)])
            p75.add(slice[(pathCount * 0.75).toInt().coerceIn(0, pathCount - 1)])
            p95.add(slice[(pathCount * 0.95).toInt().coerceIn(0, pathCount - 1)])
        }

        val terminalReturnsPct = DoubleArray(pathCount) {
            ((paths[it][horizonBars] - safeSpot) / safeSpot) * 100.0
        }.sortedArray()
        val varIdx = (pathCount * 0.05).toInt().coerceIn(1, pathCount - 1)
        val var95 = -terminalReturnsPct[varIdx]
        val cvar95 = -terminalReturnsPct.take(varIdx).average()

        return MultiverseScenarioReport(
            regimeName = regimeName,
            spotPrice = safeSpot,
            annualizedVolPct = baseVol * volMult * 100.0,
            horizonBars = horizonBars,
            pathCount = pathCount,
            p5Curve = p5,
            p25Curve = p25,
            p50Curve = p50,
            p75Curve = p75,
            p95Curve = p95,
            var95Pct = var95.coerceAtLeast(0.0),
            cvar95Pct = cvar95.coerceAtLeast(0.0),
            survivalRatePct = (survivedCount.toDouble() / pathCount) * 100.0
        )
    }

    fun analyzeBlackSwan(quote: MarketQuote): BlackSwanTelemetry {
        val candles = quote.candles
        if (candles.size < 12) {
            return BlackSwanTelemetry(
                symbol = quote.instrument.symbol,
                tailRiskScore = if (quote.isLive) 18 else 0,
                realizedVolPct = quote.realizedVolPct,
                volZScore = 0.0,
                excessKurtosis = 0.0,
                worstBarShockPct = 0.0,
                regimeStatus = if (quote.isLive) "LOW SAMPLE" else "DISCONNECTED"
            )
        }
        val returns = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val prev = candles[i - 1].close
            val curr = candles[i].close
            if (prev > 0.0 && curr > 0.0) {
                returns.add((curr - prev) / prev)
            }
        }
        if (returns.size < 8) {
            return BlackSwanTelemetry(quote.instrument.symbol, 20, quote.realizedVolPct, 0.0, 0.0, 0.0, "INSUFFICIENT BARS")
        }
        val mean = returns.average()
        val std = sqrt(returns.sumOf { (it - mean).pow(2) } / (returns.size - 1)).coerceAtLeast(1e-7)
        val recentStd = sqrt(returns.takeLast(8).sumOf { (it - mean).pow(2) } / 7.0)
        val volZ = (recentStd - std) / (std * 0.45)

        val fourthMoment = returns.sumOf { ((it - mean) / std).pow(4) } / returns.size
        val excessKurtosis = (fourthMoment - 3.0).coerceIn(-2.0, 30.0)
        val worstShockPct = returns.maxOf { abs(it) } * 100.0

        val rawScore = (20.0 + max(0.0, volZ) * 18.0 + max(0.0, excessKurtosis) * 4.5 + worstShockPct * 4.0)
            .toInt().coerceIn(5, 99)
        val status = when {
            rawScore >= 75 -> "CRITICAL TAIL ALERT"
            rawScore >= 52 -> "ELEVATED ANOMALY"
            else -> "NORMAL DISTRIBUTION"
        }

        return BlackSwanTelemetry(
            symbol = quote.instrument.symbol,
            tailRiskScore = rawScore,
            realizedVolPct = quote.realizedVolPct,
            volZScore = volZ,
            excessKurtosis = excessKurtosis,
            worstBarShockPct = worstShockPct,
            regimeStatus = status
        )
    }

    fun computeRiskParityAllocation(
        quotes: List<MarketQuote>,
        totalExposureBudgetPct: Double
    ): List<AllocationSlice> {
        val live = quotes.filter { it.isLive && it.realizedVolPct > 0.1 }.take(6)
        if (live.isEmpty()) return emptyList()
        val invVols = live.map { 1.0 / it.realizedVolPct.coerceAtLeast(2.0) }
        val invSum = invVols.sum()

        return live.mapIndexed { idx, q ->
            val weight = (invVols[idx] / invSum) * 100.0
            val riskBudget = (invVols[idx] / invSum) * totalExposureBudgetPct
            val mu = q.changePct / 100.0
            val sigma = (q.realizedVolPct / 100.0).coerceAtLeast(0.05)
            val halfKelly = (abs(mu) / (sigma * sigma) * 0.5 * 100.0).coerceIn(0.5, totalExposureBudgetPct * 0.4)
            AllocationSlice(
                label = q.instrument.displayName,
                symbol = q.instrument.symbol,
                weightPct = weight,
                riskBudgetPct = riskBudget,
                volatilityPct = q.realizedVolPct,
                kellyFractionPct = halfKelly
            )
        }
    }

    fun computeCurrencyStrength(quotes: List<MarketQuote>): List<CurrencyStrengthItem> {
        val map = quotes.filter { it.isLive }.associateBy { it.instrument.symbol }
        val eur = map["EURUSD"]?.changePct ?: 0.0
        val gbp = map["GBPUSD"]?.changePct ?: 0.0
        val aud = map["AUDUSD"]?.changePct ?: 0.0
        val jpy = -(map["USDJPY"]?.changePct ?: 0.0)
        val zar = -(map["USDZAR"]?.changePct ?: 0.0)
        val usd = -(eur + gbp + aud - jpy - zar) / 5.0
        val xau = map["XAUUSD"]?.changePct ?: 0.0

        return listOf(
            CurrencyStrengthItem("USD", usd),
            CurrencyStrengthItem("EUR", eur),
            CurrencyStrengthItem("GBP", gbp),
            CurrencyStrengthItem("JPY", jpy),
            CurrencyStrengthItem("AUD", aud),
            CurrencyStrengthItem("ZAR", zar),
            CurrencyStrengthItem("XAU", xau)
        ).sortedByDescending { it.strengthScore }
    }

    private fun computeEma(values: List<Double>, period: Int): DoubleArray {
        val out = DoubleArray(values.size)
        if (values.isEmpty()) return out
        val k = 2.0 / (period + 1)
        out[0] = values[0]
        for (i in 1 until values.size) {
            out[i] = values[i] * k + out[i - 1] * (1.0 - k)
        }
        return out
    }

    private fun computeAtr(candles: List<CandleBar>, period: Int): DoubleArray {
        val out = DoubleArray(candles.size)
        if (candles.isEmpty()) return out
        val tr = DoubleArray(candles.size)
        tr[0] = candles[0].high - candles[0].low
        for (i in 1 until candles.size) {
            val h = candles[i].high
            val l = candles[i].low
            val pc = candles[i - 1].close
            tr[i] = max(h - l, max(abs(h - pc), abs(l - pc)))
        }
        var sum = 0.0
        for (i in candles.indices) {
            sum += tr[i]
            if (i >= period) sum -= tr[i - period]
            out[i] = if (i >= period - 1) sum / period else sum / (i + 1)
        }
        return out
    }

    private fun computeTradeSharpe(returnsPct: List<Double>): Double {
        if (returnsPct.size < 2) return 0.0
        val mean = returnsPct.average()
        val std = sqrt(returnsPct.sumOf { (it - mean).pow(2) } / (returnsPct.size - 1))
        return if (std > 1e-6) ((mean / std) * sqrt(returnsPct.size.toDouble())).coerceIn(-10.0, 15.0) else 0.0
    }

    private fun nextGaussian(rng: Random): Double {
        val u1 = rng.nextDouble().coerceAtLeast(1e-9)
        val u2 = rng.nextDouble()
        return sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }
}
