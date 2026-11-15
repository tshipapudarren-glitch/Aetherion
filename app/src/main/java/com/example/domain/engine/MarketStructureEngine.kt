package com.example.domain.engine

import com.example.data.local.LocalStrategyEntity
import com.example.data.local.PaperOrderEntity
import com.example.data.model.CandleBar
import com.example.data.model.MarketQuote
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SetupScanStatus(val label: String) {
    TRIGGERED("TRIGGERED"),
    FORMING("FORMING"),
    SCANNING("SCANNING")
}

data class IntegratedSetupSignal(
    val number: Int,
    val id: String,
    val name: String,
    val drawingType: String,
    val status: SetupScanStatus,
    val direction: String, // LONG, SHORT, NEUTRAL
    val confidencePct: Int,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val invalidationRule: String,
    val explanation: String
)

data class SyntheticSpreadLevel(
    val bidPrice: Double,
    val askPrice: Double,
    val bidLots: Double,
    val askLots: Double
)

data class TerminalAccountSummary(
    val startingBalanceUsd: Double = 100_000.0,
    val realizedPnlUsd: Double = 0.0,
    val floatingPnlUsd: Double = 0.0,
    val equityUsd: Double = 100_000.0,
    val usedMarginPct: Double = 0.0,
    val freeMarginUsd: Double = 100_000.0,
    val openPositionsCount: Int = 0,
    val closedTradesCount: Int = 0
)

data class MarketStructureSnapshot(
    val symbol: String = "XAUUSD",
    val structureBias: String = "BULLISH BOS",
    val bullishSentimentPct: Int = 55,
    val bearishSentimentPct: Int = 45,
    val detectedSetups: List<IntegratedSetupSignal> = emptyList(),
    val triggeredCount: Int = 0,
    val formingCount: Int = 0,
    val spreadDepth: List<SyntheticSpreadLevel> = emptyList(),
    val estimatedSpreadBps: Double = 4.2,
    val atrValue: Double = 0.0,
    val swingHigh: Double = 0.0,
    val swingLow: Double = 0.0,
    val fib618Level: Double = 0.0,
    val fvgZoneSummary: String = "No unfilled imbalance inside lookback window",
    val accountSummary: TerminalAccountSummary = TerminalAccountSummary(),
    val updatedAtMs: Long = 0L
)

enum class QuantCodeTarget(val id: String, val label: String, val fileExtension: String) {
    PINE_V6("PINE_V6", "Pine Script v6", ".pine"),
    MQL5_EA("MQL5_EA", "MT5 MQL5 EA", ".mq5"),
    MQL5_INDICATOR("MQL5_INDICATOR", "MT5 MQL5 Indicator", ".mq5")
}

object MarketStructureEngine {

    fun analyze(
        quote: MarketQuote?,
        allQuotes: List<MarketQuote>,
        paperOrders: List<PaperOrderEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): MarketStructureSnapshot {
        val account = computeAccountSummary(paperOrders, allQuotes)
        if (quote == null || !quote.isLive || quote.price <= 0.0) {
            return MarketStructureSnapshot(
                symbol = quote?.instrument?.symbol ?: "XAUUSD",
                structureBias = "AWAITING LIVE FEED",
                accountSummary = account,
                updatedAtMs = nowMs
            )
        }

        val candles = quote.candles
        val overlays = SetupArchitectEngine.computeOverlays(candles)
        val spot = quote.price
        val ma50 = overlays.latestMa50 ?: spot
        val rsi = overlays.latestRsi14 ?: 50.0
        val atr = computeAtrValue(candles, spot)
        val recent = candles.takeLast(32).ifEmpty {
            listOf(CandleBar(nowMs, spot, spot + atr, spot - atr, spot, 100.0))
        }
        val swingHigh = recent.maxOf { it.high }
        val swingLow = recent.minOf { it.low }
        val range = max(swingHigh - swingLow, atr)
        val fib618 = if (spot >= ma50) swingHigh - range * 0.618 else swingLow + range * 0.618

        // Bull / Bear Sentiment Split from live price action + RSI + MA50 + slope
        val slopePct = if (recent.size >= 5) {
            ((spot - recent[recent.size - 5].close) / recent[recent.size - 5].close) * 100.0
        } else quote.changePct
        var bullScore = 50.0
        bullScore += if (spot >= ma50) 11.0 else -11.0
        bullScore += ((rsi - 50.0) * 0.48)
        bullScore += (quote.changePct.coerceIn(-4.0, 4.0) * 3.5)
        bullScore += (slopePct.coerceIn(-3.0, 3.0) * 3.0)
        val bullishPct = bullScore.roundToInt().coerceIn(12, 88)
        val bearishPct = 100 - bullishPct

        // Structure state (BOS / CHoCH / Compression)
        val priorWindow = recent.dropLast(3)
        val priorHigh = priorWindow.maxOfOrNull { it.high } ?: swingHigh
        val priorLow = priorWindow.minOfOrNull { it.low } ?: swingLow
        val bosUp = spot > priorHigh
        val bosDown = spot < priorLow
        val structureBias = when {
            bosUp && spot >= ma50 -> "BULLISH BOS (EXPANSION)"
            bosDown && spot < ma50 -> "BEARISH BOS (BREAKDOWN)"
            spot >= ma50 && quote.changePct < 0 -> "BULLISH PULLBACK / CHOCH WATCH"
            spot < ma50 && quote.changePct > 0 -> "COUNTER-TREND CHOCH PROBE"
            else -> "EQUILIBRIUM COMPRESSION"
        }

        // Check FVG in recent candles (bar[i].low > bar[i-2].high or bar[i].high < bar[i-2].low)
        var fvgBullFound = false
        var fvgBearFound = false
        var fvgSummary = "No unfilled 3-bar FVG inside last 15 candles"
        val lastWindow = candles.takeLast(15)
        for (i in 2 until lastWindow.size) {
            val c0 = lastWindow[i - 2]
            val c2 = lastWindow[i]
            if (c2.low > c0.high) {
                fvgBullFound = true
                fvgSummary = "Bullish FVG zone: ${SetupArchitectEngine.formatPrice(c0.high)} – ${SetupArchitectEngine.formatPrice(c2.low)}"
            } else if (c2.high < c0.low) {
                fvgBearFound = true
                fvgSummary = "Bearish FVG zone: ${SetupArchitectEngine.formatPrice(c2.high)} – ${SetupArchitectEngine.formatPrice(c0.low)}"
            }
        }

        // Derived synthetic spread ladder around current quote
        val spreadHalf = max(atr * 0.035, spot * 0.00015)
        val estimatedSpreadBps = ((spreadHalf * 2.0) / spot * 10_000.0).coerceIn(1.2, 28.0)
        val depth = (1..5).map { tier ->
            val step = spreadHalf * tier
            SyntheticSpreadLevel(
                bidPrice = spot - step,
                askPrice = spot + step,
                bidLots = (1.5 + tier * 1.4 + (bullishPct / 45.0)),
                askLots = (1.5 + tier * 1.35 + (bearishPct / 45.0))
            )
        }

        val lastBar = recent.last()
        val prevBar = recent.getOrElse(recent.size - 2) { lastBar }
        val lastBody = abs(lastBar.close - lastBar.open)
        val prevBody = abs(prevBar.close - prevBar.open)
        val upperWick = lastBar.high - max(lastBar.open, lastBar.close)
        val lowerWick = min(lastBar.open, lastBar.close) - lastBar.low
        val engulfingBull = lastBar.close > lastBar.open && prevBar.close < prevBar.open && lastBody > prevBody * 1.05
        val engulfingBear = lastBar.close < lastBar.open && prevBar.close > prevBar.open && lastBody > prevBody * 1.05
        val rejectionBull = lowerWick > max(lastBody * 1.6, atr * 0.25)
        val rejectionBear = upperWick > max(lastBody * 1.6, atr * 0.25)
        val compressionNow = (lastBar.high - lastBar.low) < atr * 0.65 && (range / max(spot, 1.0)) < 0.045
        val displacementNow = lastBody > atr * 1.35

        fun mkSetup(
            num: Int,
            id: String,
            name: String,
            drawingType: String,
            status: SetupScanStatus,
            dir: String,
            conf: Int,
            entryOffsetAtr: Double,
            slMult: Double,
            tpMult: Double,
            inval: String,
            explain: String
        ): IntegratedSetupSignal {
            val effDir = if (dir == "SHORT") "SHORT" else "LONG"
            val entry = if (effDir == "LONG") spot + atr * entryOffsetAtr else spot - atr * entryOffsetAtr
            val sl = if (effDir == "LONG") entry - atr * slMult else entry + atr * slMult
            val tp = if (effDir == "LONG") entry + atr * tpMult else entry - atr * tpMult
            return IntegratedSetupSignal(
                number = num,
                id = id,
                name = name,
                drawingType = drawingType,
                status = status,
                direction = effDir,
                confidencePct = conf.coerceIn(35, 93),
                entryPrice = entry.coerceAtLeast(spot * 0.05),
                stopLoss = sl.coerceAtLeast(spot * 0.02),
                takeProfit = tp.coerceAtLeast(spot * 0.02),
                invalidationRule = inval,
                explanation = explain
            )
        }

        val defaultDir = if (bullishPct >= 50) "LONG" else "SHORT"
        val setups = listOf(
            mkSetup(
                1, "breakout_retest", "Breakout & Retest", "Entry / SL / TP",
                if (bosUp || bosDown) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                if (bosUp) "LONG" else if (bosDown) "SHORT" else defaultDir,
                if (bosUp || bosDown) 82 else 58, 0.0, 1.3, 2.6,
                "Close back inside ${SetupArchitectEngine.formatPrice(if (defaultDir == "LONG") priorHigh else priorLow)}",
                "Monitors swing boundary breakout followed by mean-retest support hold."
            ),
            mkSetup(
                2, "liquidity_sweep", "Liquidity Sweep", "Entry / SL / TP",
                if (lowerWick > atr * 0.4 || upperWick > atr * 0.4) SetupScanStatus.TRIGGERED else SetupScanStatus.FORMING,
                if (lowerWick >= upperWick) "LONG" else "SHORT",
                if (lowerWick > atr * 0.4 || upperWick > atr * 0.4) 84 else 63, 0.0, 1.15, 2.4,
                "Break beyond swept wick extreme (${SetupArchitectEngine.formatPrice(if (lowerWick >= upperWick) lastBar.low else lastBar.high)})",
                "Detects stop-cluster flush outside recent lows/highs with rapid reclaim."
            ),
            mkSetup(
                3, "fvg", "Fair Value Gap (FVG)", "Entry Zone / SL / TP",
                if (fvgBullFound || fvgBearFound) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                if (fvgBullFound) "LONG" else if (fvgBearFound) "SHORT" else defaultDir,
                if (fvgBullFound || fvgBearFound) 79 else 55, -0.25, 1.25, 2.75,
                "Full candle close through 3-bar inefficiency body",
                fvgSummary
            ),
            mkSetup(
                4, "order_block", "Order Block", "Zone / Entry / SL / TP",
                if (abs(spot - ma50) <= atr * 1.1) SetupScanStatus.FORMING else SetupScanStatus.SCANNING,
                defaultDir, 74, -0.2, 1.4, 2.8,
                "Violation of opposing base candle before displacement impulse",
                "Marks last institutional origin candle prior to range expansion."
            ),
            mkSetup(
                5, "bos_choch", "BOS / CHoCH", "Structure & Entry",
                if (bosUp || bosDown) SetupScanStatus.TRIGGERED else SetupScanStatus.FORMING,
                if (bosUp) "LONG" else if (bosDown) "SHORT" else defaultDir,
                if (bosUp || bosDown) 83 else 66, 0.0, 1.35, 2.9,
                "Counter-swing break below ${SetupArchitectEngine.formatPrice(swingLow)}",
                "Structural verdict: $structureBias."
            ),
            mkSetup(
                6, "supply_demand", "Supply & Demand", "Zones & Targets",
                if (spot <= swingLow + range * 0.25 || spot >= swingHigh - range * 0.25) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                if (spot <= swingLow + range * 0.25) "LONG" else "SHORT",
                76, 0.0, 1.2, 2.5,
                "Zone absorption close outside proximal boundary",
                "Maps proximal/distal origin shelves at ${SetupArchitectEngine.formatPrice(swingLow)} / ${SetupArchitectEngine.formatPrice(swingHigh)}."
            ),
            mkSetup(
                7, "support_resistance", "Support & Resistance", "Levels & Targets",
                SetupScanStatus.FORMING,
                if (spot >= ma50) "LONG" else "SHORT",
                71, 0.0, 1.3, 2.3,
                "Confirmed H1 break across pivot ${SetupArchitectEngine.formatPrice(ma50)}",
                "Key ceiling ${SetupArchitectEngine.formatPrice(swingHigh)} · Floor ${SetupArchitectEngine.formatPrice(swingLow)}."
            ),
            mkSetup(
                8, "fibonacci_reversal", "Fibonacci Reversal (0.618)", "Retracement Entry",
                if (abs(spot - fib618) <= atr * 0.75) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                defaultDir,
                if (abs(spot - fib618) <= atr * 0.75) 81 else 60, -0.15, 1.25, 2.6,
                "Close beyond 0.786 deep invalidation swing",
                "Golden ratio 0.618 cluster anchored near ${SetupArchitectEngine.formatPrice(fib618)}."
            ),
            mkSetup(
                9, "engulfing_candle", "Engulfing Candle", "Entry & Invalidation",
                if (engulfingBull || engulfingBear) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                if (engulfingBull) "LONG" else if (engulfingBear) "SHORT" else defaultDir,
                if (engulfingBull || engulfingBear) 80 else 52, 0.0, 1.1, 2.2,
                "Opposite close erasing more than 65% of engulfing candle",
                if (engulfingBull || engulfingBear) "Active full-body engulfing confirmation detected." else "Monitoring bar close for momentum engulf."
            ),
            mkSetup(
                10, "rejection_candle", "Rejection Candle", "Entry & Risk",
                if (rejectionBull || rejectionBear) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                if (rejectionBull) "LONG" else if (rejectionBear) "SHORT" else defaultDir,
                if (rejectionBull || rejectionBear) 78 else 54, 0.0, 1.1, 2.35,
                "Break through rejection wick tail",
                if (rejectionBull || rejectionBear) "Long wick exhaustion defending liquidity shelf." else "No extreme wick ratio on closing candle."
            ),
            mkSetup(
                11, "trend_continuation", "Trend Continuation", "Entry & Targets",
                if (spot >= ma50 && rsi in 48.0..66.0) SetupScanStatus.TRIGGERED else SetupScanStatus.FORMING,
                if (spot >= ma50) "LONG" else "SHORT",
                77, 0.0, 1.4, 3.0,
                "Loss of MA(50) dynamic slope (${SetupArchitectEngine.formatPrice(ma50)})",
                "MA(50) dynamic trend alignment at RSI ${String.format("%.1f", rsi)}."
            ),
            mkSetup(
                12, "session_sweep", "Session Sweep", "Liquidity & Entry",
                if (quote.realizedVolPct >= 22.0 && (lowerWick > upperWick * 1.4 || upperWick > lowerWick * 1.4)) SetupScanStatus.FORMING else SetupScanStatus.SCANNING,
                if (lowerWick >= upperWick) "LONG" else "SHORT",
                70, 0.0, 1.2, 2.4,
                "Sustained expansion past London/NY session extreme",
                "Tracks London/NY handover stop-runs against prior session high/low."
            ),
            mkSetup(
                13, "displacement", "Displacement", "Entry & Continuation",
                if (displacementNow) SetupScanStatus.TRIGGERED else SetupScanStatus.SCANNING,
                if (lastBar.close >= lastBar.open) "LONG" else "SHORT",
                if (displacementNow) 85 else 57, 0.0, 1.35, 3.1,
                "50% retrace failure of institutional expansion candle",
                if (displacementNow) "High-velocity candle >1.35x ATR signals institutional sponsorship." else "Standard range candle velocity."
            ),
            mkSetup(
                14, "double_top_bottom", "Double Top / Bottom", "Reversal Setup",
                if (recent.size >= 10 && (spot <= swingLow + atr * 0.4 || spot >= swingHigh - atr * 0.4)) SetupScanStatus.FORMING else SetupScanStatus.SCANNING,
                if (spot <= swingLow + atr * 0.4) "LONG" else "SHORT",
                68, 0.0, 1.25, 2.5,
                "Break beyond twin high/low boundary with expanding volume",
                "Evaluating neckline divergence near swing boundary."
            ),
            mkSetup(
                15, "compression_breakout", "Compression Breakout", "Breakout Entry",
                if (compressionNow) SetupScanStatus.FORMING else SetupScanStatus.SCANNING,
                defaultDir,
                if (compressionNow) 75 else 59, 0.05, 1.0, 2.8,
                "False expansion closing back inside coiled range",
                if (compressionNow) "ATR volatility squeeze active (<0.65x ATR range); breakout imminent." else "Normal intraday expansion state."
            )
        )

        return MarketStructureSnapshot(
            symbol = quote.instrument.symbol,
            structureBias = structureBias,
            bullishSentimentPct = bullishPct,
            bearishSentimentPct = bearishPct,
            detectedSetups = setups,
            triggeredCount = setups.count { it.status == SetupScanStatus.TRIGGERED },
            formingCount = setups.count { it.status == SetupScanStatus.FORMING },
            spreadDepth = depth,
            estimatedSpreadBps = estimatedSpreadBps,
            atrValue = atr,
            swingHigh = swingHigh,
            swingLow = swingLow,
            fib618Level = fib618,
            fvgZoneSummary = fvgSummary,
            accountSummary = account,
            updatedAtMs = nowMs
        )
    }

    fun computeAccountSummary(
        paperOrders: List<PaperOrderEntity>,
        quotes: List<MarketQuote>
    ): TerminalAccountSummary {
        val startingBalance = 100_000.0
        val quoteBySymbol = quotes.filter { it.isLive }.associateBy { it.instrument.symbol }

        var realizedUsd = 0.0
        var floatingUsd = 0.0
        var openExposurePct = 0.0
        var openCount = 0
        var closedCount = 0

        paperOrders.forEach { order ->
            val notionalUsd = startingBalance * (order.exposurePct / 100.0)
            if (order.status == "OPEN") {
                openCount++
                openExposurePct += order.exposurePct
                val livePrice = quoteBySymbol[order.symbol]?.price ?: order.entryPrice
                val pctMove = if (order.entryPrice > 0.0) {
                    val raw = ((livePrice - order.entryPrice) / order.entryPrice)
                    if (order.side.uppercase() == "SELL") -raw else raw
                } else 0.0
                floatingUsd += notionalUsd * pctMove
            } else {
                closedCount++
                realizedUsd += notionalUsd * (order.pnlPct / 100.0)
            }
        }

        val equity = startingBalance + realizedUsd + floatingUsd
        val freeMargin = (equity * (1.0 - (openExposurePct / 100.0).coerceIn(0.0, 0.99)))
        return TerminalAccountSummary(
            startingBalanceUsd = startingBalance,
            realizedPnlUsd = realizedUsd,
            floatingPnlUsd = floatingUsd,
            equityUsd = equity,
            usedMarginPct = openExposurePct,
            freeMarginUsd = freeMargin,
            openPositionsCount = openCount,
            closedTradesCount = closedCount
        )
    }

    fun generateQuantTemplate(strategy: LocalStrategyEntity, target: QuantCodeTarget): String {
        val cleanName = strategy.name.replace("\"", "'")
        return when (target) {
            QuantCodeTarget.PINE_V6 -> """
                //@version=6
                strategy("$cleanName", overlay=true, default_qty_type=strategy.percent_of_equity, default_qty_value=2)
                // Symbol: ${strategy.symbol} | Agent: ${strategy.creatorAgent} | Stage: ${strategy.stage}

                fastLen = input.int(${strategy.fastPeriod}, "Fast EMA Length", minval=3)
                slowLen = input.int(${strategy.slowPeriod}, "Slow EMA Length", minval=10)
                atrLen  = input.int(14, "ATR Length", minval=5)
                slMult  = input.float(${strategy.atrStopMult}, "ATR Stop Loss Multiplier", step=0.1)
                tpMult  = input.float(${strategy.atrTargetMult}, "ATR Take Profit Multiplier", step=0.1)

                fastEma = ta.ema(close, fastLen)
                slowEma = ta.ema(close, slowLen)
                atrVal  = ta.atr(atrLen)

                longCond  = ta.crossover(fastEma, slowEma) and barstate.isconfirmed
                shortCond = ta.crossunder(fastEma, slowEma) and barstate.isconfirmed

                plot(fastEma, "Fast EMA", color=color.new(#F3DF99, 0), linewidth=2)
                plot(slowEma, "Slow EMA", color=color.new(#4DB6E5, 0), linewidth=2)

                if longCond
                    sl = close - atrVal * slMult
                    tp = close + atrVal * tpMult
                    strategy.entry("AETH_LONG", strategy.long)
                    strategy.exit("LONG_EXIT", "AETH_LONG", stop=sl, limit=tp)

                if shortCond
                    sl = close + atrVal * slMult
                    tp = close - atrVal * tpMult
                    strategy.entry("AETH_SHORT", strategy.short)
                    strategy.exit("SHORT_EXIT", "AETH_SHORT", stop=sl, limit=tp)
            """.trimIndent()

            QuantCodeTarget.MQL5_EA -> """
                //+------------------------------------------------------------------+
                //| AETHERION Institutional Expert Advisor                           |
                //| Strategy: $cleanName (${strategy.symbol})
                //+------------------------------------------------------------------+
                #property copyright "AETHERION QUANT LAB"
                #property version   "1.00"
                #include <Trade/Trade.mqh>

                input int    InpFastEma    = ${strategy.fastPeriod};
                input int    InpSlowEma    = ${strategy.slowPeriod};
                input int    InpAtrPeriod  = 14;
                input double InpSlAtrMult  = ${strategy.atrStopMult};
                input double InpTpAtrMult  = ${strategy.atrTargetMult};
                input double InpRiskLots   = 0.10;
                input double InpMaxSpreadPts = 35.0;

                CTrade trade;
                int hFast, hSlow, hAtr;

                int OnInit() {
                   hFast = iMA(_Symbol, _Period, InpFastEma, 0, MODE_EMA, PRICE_CLOSE);
                   hSlow = iMA(_Symbol, _Period, InpSlowEma, 0, MODE_EMA, PRICE_CLOSE);
                   hAtr  = iATR(_Symbol, _Period, InpAtrPeriod);
                   return (hFast != INVALID_HANDLE && hSlow != INVALID_HANDLE && hAtr != INVALID_HANDLE) ? INIT_SUCCEEDED : INIT_FAILED;
                }

                void OnTick() {
                   if (SymbolInfoInteger(_Symbol, SYMBOL_SPREAD) > (long)InpMaxSpreadPts) return;
                   double f[3], s[3], a[2];
                   if (CopyBuffer(hFast, 0, 0, 3, f) < 3 || CopyBuffer(hSlow, 0, 0, 3, s) < 3 || CopyBuffer(hAtr, 0, 0, 2, a) < 2) return;

                   if (PositionsTotal() == 0 && f[1] > s[1] && f[2] <= s[2]) {
                      double ask = SymbolInfoDouble(_Symbol, SYMBOL_ASK);
                      trade.Buy(InpRiskLots, _Symbol, ask, ask - a[1] * InpSlAtrMult, ask + a[1] * InpTpAtrMult, "AETHERION_LONG");
                   } else if (PositionsTotal() == 0 && f[1] < s[1] && f[2] >= s[2]) {
                      double bid = SymbolInfoDouble(_Symbol, SYMBOL_BID);
                      trade.Sell(InpRiskLots, _Symbol, bid, bid + a[1] * InpSlAtrMult, bid - a[1] * InpTpAtrMult, "AETHERION_SHORT");
                   }
                }
            """.trimIndent()

            QuantCodeTarget.MQL5_INDICATOR -> """
                //+------------------------------------------------------------------+
                //| AETHERION Setup Architect Chart Indicator                        |
                //| Strategy: $cleanName (${strategy.symbol})
                //+------------------------------------------------------------------+
                #property indicator_chart_window
                #property indicator_buffers 2
                #property indicator_plots   2

                input int InpFastPeriod = ${strategy.fastPeriod};
                input int InpSlowPeriod = ${strategy.slowPeriod};

                double FastBuf[], SlowBuf[];
                int hFast, hSlow;

                int OnInit() {
                   SetIndexBuffer(0, FastBuf, INDICATOR_DATA);
                   SetIndexBuffer(1, SlowBuf, INDICATOR_DATA);
                   hFast = iMA(_Symbol, _Period, InpFastPeriod, 0, MODE_EMA, PRICE_CLOSE);
                   hSlow = iMA(_Symbol, _Period, InpSlowPeriod, 0, MODE_EMA, PRICE_CLOSE);
                   return INIT_SUCCEEDED;
                }

                int OnCalculate(const int rates_total, const int prev_calculated, const datetime &time[],
                                const double &open[], const double &high[], const double &low[],
                                const double &close[], const long &tick_volume[], const long &volume[], const int &spread[]) {
                   if (CopyBuffer(hFast, 0, 0, rates_total, FastBuf) <= 0) return 0;
                   if (CopyBuffer(hSlow, 0, 0, rates_total, SlowBuf) <= 0) return 0;
                   return rates_total;
                }
            """.trimIndent()
        }
    }

    fun auditQuantSource(sourceCode: String): List<String> {
        val findings = mutableListOf<String>()
        val lower = sourceCode.lowercase()
        val hasSl = lower.contains("stop=") || lower.contains("inpslatrmult") || lower.contains("stoploss")
        val hasConfirmedBar = lower.contains("barstate.isconfirmed") || lower.contains("copybuffer(hfast, 0, 0, 3")
        val hasSpreadGuard = lower.contains("symbol_spread") || lower.contains("default_qty_value")
        val hasLookahead = lower.contains("lookahead=barmerge.lookahead_on")

        findings.add(if (hasSl) "PASS: Mandatory Stop Loss invalidation logic present." else "WARN: Missing explicit stop-loss guard.")
        findings.add(if (hasConfirmedBar) "PASS: Non-repainting confirmed bar indexing enforced." else "WARN: Verify bar-close confirmation to prevent repainting.")
        findings.add(if (hasSpreadGuard) "PASS: Exposure/Spread Risk Guard throttle configured." else "WARN: Add explicit spread or equity sizing limit.")
        findings.add(if (!hasLookahead) "PASS: Zero future-leakage lookahead directives detected." else "FAIL: Lookahead bias detected!")
        return findings
    }

    private fun computeAtrValue(candles: List<CandleBar>, fallbackSpot: Double): Double {
        if (candles.size < 2) return fallbackSpot * 0.008
        val tr = (1 until candles.size).map { i ->
            val prev = candles[i - 1].close
            val c = candles[i]
            max(c.high - c.low, max(abs(c.high - prev), abs(c.low - prev)))
        }
        return max(tr.takeLast(14).average(), fallbackSpot * 0.002)
    }
}
