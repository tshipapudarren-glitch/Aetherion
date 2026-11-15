package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.MarketInstrument
import com.example.data.model.MarketQuote
import com.example.domain.engine.SetupArchitectEngine
import com.example.domain.engine.TradeSetupPlan
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.IceTelemetry
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SlateMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class TradingViewChartMode(val label: String) {
    NATIVE_SETUP("Chart + Setup Drawings"),
    MULTI_GRID("2×2 Multi-Chart"),
    TRADINGVIEW_WEB("TradingView Live")
}

object TradingViewSymbolMapper {
    fun resolve(instrument: MarketInstrument): String = when (instrument.symbol) {
        "XAUUSD" -> "OANDA:XAUUSD"
        "XAGUSD" -> "OANDA:XAGUSD"
        "EURUSD" -> "OANDA:EURUSD"
        "GBPUSD" -> "OANDA:GBPUSD"
        "USDJPY" -> "OANDA:USDJPY"
        "AUDUSD" -> "OANDA:AUDUSD"
        "USDZAR" -> "OANDA:USDZAR"
        "BTCUSD" -> "BINANCE:BTCUSDT"
        "ETHUSD" -> "BINANCE:ETHUSDT"
        "SOLUSD" -> "BINANCE:SOLUSDT"
        "NAS100" -> "NASDAQ:QQQ"
        "SPX500" -> "AMEX:SPY"
        "US30" -> "AMEX:DIA"
        "GER40" -> "XETR:DAX"
        "UK100" -> "LSE:UKX"
        "WTI" -> "TVC:USOIL"
        "BRENT" -> "TVC:UKOIL"
        "NVDA" -> "NASDAQ:NVDA"
        "AAPL" -> "NASDAQ:AAPL"
        "US10Y" -> "TVC:US10Y"
        else -> instrument.finnhubSymbol.ifBlank { instrument.symbol }
    }

    fun toTradingViewInterval(timeframe: String): String = when (timeframe) {
        "15m" -> "15"
        "1H" -> "60"
        "4H" -> "240"
        "1D" -> "D"
        else -> "60"
    }
}

@Composable
fun TradingViewTerminalCard(
    selectedQuote: MarketQuote?,
    allQuotes: List<MarketQuote>,
    setupPlan: TradeSetupPlan?,
    timeframe: String,
    onSelectTimeframe: (String) -> Unit,
    onSelectSymbol: (String) -> Unit,
    showModeSwitcher: Boolean = true,
    initialMode: TradingViewChartMode = TradingViewChartMode.NATIVE_SETUP,
    chartHeightDp: Int = 275
) {
    var mode by remember { mutableStateOf(initialMode) }
    var showSetupDrawings by remember { mutableStateOf(true) }
    val timeframes = remember { listOf("15m", "1H", "4H", "1D") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showModeSwitcher) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TradingViewChartMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = MetallicGoldBright
                        ),
                        modifier = Modifier.testTag("tv_mode_${m.name.lowercase(Locale.US)}")
                    )
                }
                if (mode == TradingViewChartMode.NATIVE_SETUP) {
                    FilterChip(
                        selected = showSetupDrawings,
                        onClick = { showSetupDrawings = !showSetupDrawings },
                        label = { Text(if (showSetupDrawings) "Drawings: ON" else "Drawings: OFF", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = LiveEmerald
                        ),
                        modifier = Modifier.testTag("toggle_setup_drawings_chip")
                    )
                }
            }
        }

        // Timeframe selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            timeframes.forEach { tf ->
                FilterChip(
                    selected = timeframe == tf,
                    onClick = { onSelectTimeframe(tf) },
                    label = { Text(tf, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGoldContainer,
                        selectedLabelColor = MetallicGoldBright
                    )
                )
            }
            if (selectedQuote != null) {
                Text(
                    text = "TV: ${TradingViewSymbolMapper.resolve(selectedQuote.instrument)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceTelemetry,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        when (mode) {
            TradingViewChartMode.NATIVE_SETUP -> {
                if (selectedQuote == null || selectedQuote.candles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeightDp.dp)
                            .background(ObsidianVoid, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Awaiting live OHLCV candles to draw chart & Setup Architect indicators.", color = SlateMuted, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    TradingViewProCanvas(
                        quote = selectedQuote,
                        setupPlan = if (showSetupDrawings) setupPlan else null,
                        timeframe = timeframe,
                        mainChartHeightDp = chartHeightDp
                    )
                }
            }

            TradingViewChartMode.MULTI_GRID -> {
                MultiChart2x2Grid(
                    quotes = allQuotes,
                    selectedSymbol = selectedQuote?.instrument?.symbol ?: "XAUUSD",
                    timeframe = timeframe,
                    onSelectSymbol = onSelectSymbol
                )
            }

            TradingViewChartMode.TRADINGVIEW_WEB -> {
                if (selectedQuote != null) {
                    TradingViewLiveWebWidget(
                        tvSymbol = TradingViewSymbolMapper.resolve(selectedQuote.instrument),
                        interval = TradingViewSymbolMapper.toTradingViewInterval(timeframe),
                        heightDp = chartHeightDp + 70
                    )
                }
            }
        }

        // Always render Setup Architect level summary pill strip beneath chart when setupPlan is available
        if (setupPlan != null) {
            SetupLevelsStrip(setupPlan = setupPlan)
        }
    }
}

@Composable
private fun TradingViewProCanvas(
    quote: MarketQuote,
    setupPlan: TradeSetupPlan?,
    timeframe: String,
    mainChartHeightDp: Int
) {
    val candles = quote.candles.takeLast(60)
    val overlays = setupPlan?.overlays ?: remember(quote.candles) { SetupArchitectEngine.computeOverlays(quote.candles) }
    val ma50Tail = overlays.ma50.takeLast(candles.size)
    val ma200Tail = overlays.ma200.takeLast(candles.size)
    val volSmaTail = overlays.volSma9.takeLast(candles.size)
    val rsiTail = overlays.rsi14.takeLast(candles.size)
    val rsiSmaTail = overlays.rsiSma14.takeLast(candles.size)
    val lastBar = candles.last()
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // TradingView-style OHLC & MA Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${quote.instrument.symbol} · $timeframe · ${quote.sourceName}",
                style = MaterialTheme.typography.labelSmall,
                color = IvoryText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "O:${SetupArchitectEngine.formatPrice(lastBar.open)} H:${SetupArchitectEngine.formatPrice(lastBar.high)} L:${SetupArchitectEngine.formatPrice(lastBar.low)} C:${SetupArchitectEngine.formatPrice(lastBar.close)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (lastBar.close >= lastBar.open) LiveEmerald else CrimsonAlert
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            overlays.latestMa50?.let {
                Text("MA50 ${SetupArchitectEngine.formatPrice(it)}", style = MaterialTheme.typography.labelSmall, color = SimulatedAmber)
            }
            overlays.latestMa200?.let {
                Text("MA200 ${SetupArchitectEngine.formatPrice(it)}", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
            }
            overlays.latestRsi14?.let {
                Text("RSI14 ${String.format(Locale.US, "%.1f", it)}", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
            }
        }

        // Main Candlestick + MA50/200 + Volume Bars + Setup Architect Chart Drawings Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(mainChartHeightDp.dp)
                .testTag("tradingview_pro_canvas")
        ) {
            val rightAxisWidth = 62.dp.toPx()
            val plotW = (size.width - rightAxisWidth).coerceAtLeast(40f)
            val pricePlotH = size.height * 0.78f
            val volTop = pricePlotH + 4.dp.toPx()
            val volH = (size.height - volTop).coerceAtLeast(18f)

            val levelPrices = if (setupPlan != null) {
                listOf(setupPlan.entryPrice, setupPlan.stopLoss, setupPlan.tp1, setupPlan.tp2, setupPlan.tp3)
            } else emptyList()

            val minLow = (candles.minOf { it.low } * 0.999).let { low ->
                levelPrices.minOrNull()?.let { min(low, it) } ?: low
            }
            val maxHigh = (candles.maxOf { it.high } * 1.001).let { high ->
                levelPrices.maxOrNull()?.let { max(high, it) } ?: high
            }
            val range = (maxHigh - minLow).coerceAtLeast(1e-6)
            val stepX = plotW / candles.size.coerceAtLeast(1)

            fun mapY(price: Double): Float =
                (pricePlotH - ((price - minLow) / range) * pricePlotH).toFloat().coerceIn(2f, pricePlotH - 2f)

            // Subtle institutional grid
            for (g in 0..4) {
                val y = pricePlotH * (g / 4f)
                drawLine(ObsidianBorder.copy(alpha = 0.55f), Offset(0f, y), Offset(plotW, y), strokeWidth = 1f)
            }

            // Setup Architect Risk/Reward Shaded Zones
            if (setupPlan != null) {
                val yEntry = mapY(setupPlan.entryPrice)
                val ySl = mapY(setupPlan.stopLoss)
                val yTp3 = mapY(setupPlan.tp3)

                drawRect(
                    color = CrimsonAlert.copy(alpha = 0.11f),
                    topLeft = Offset(0f, min(yEntry, ySl)),
                    size = Size(plotW, abs(ySl - yEntry))
                )
                drawRect(
                    color = LiveEmerald.copy(alpha = 0.10f),
                    topLeft = Offset(0f, min(yEntry, yTp3)),
                    size = Size(plotW, abs(yTp3 - yEntry))
                )
            }

            // Candles
            candles.forEachIndexed { idx, bar ->
                val cx = idx * stepX + stepX / 2f
                val bull = bar.close >= bar.open
                val col = if (bull) LiveEmerald else CrimsonAlert
                drawLine(col, Offset(cx, mapY(bar.high)), Offset(cx, mapY(bar.low)), strokeWidth = 1.6f)
                val top = min(mapY(bar.open), mapY(bar.close))
                val bodyH = max(2.5f, abs(mapY(bar.close) - mapY(bar.open)))
                val bodyW = max(2.5f, stepX * 0.62f)
                drawRect(col, Offset(cx - bodyW / 2f, top), Size(bodyW, bodyH))
            }

            // MA50 & MA200 curves
            fun drawOverlayLine(series: List<Double?>, color: Color) {
                val p = Path()
                var started = false
                series.forEachIndexed { idx, v ->
                    if (v != null) {
                        val x = idx * stepX + stepX / 2f
                        val y = mapY(v)
                        if (!started) {
                            p.moveTo(x, y); started = true
                        } else {
                            p.lineTo(x, y)
                        }
                    }
                }
                if (started) drawPath(p, color = color, style = Stroke(width = 1.6.dp.toPx()))
            }
            drawOverlayLine(ma200Tail, IceTelemetry)
            drawOverlayLine(ma50Tail, SimulatedAmber)

            // Setup Architect Horizontal Drawing Lines + Right Axis Price Scale Tags
            fun drawPriceTag(label: String, price: Double, bg: Color, dashed: Boolean) {
                val y = mapY(price)
                drawLine(
                    color = bg,
                    start = Offset(0f, y),
                    end = Offset(plotW, y),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(9f, 5f), 0f) else null
                )
                val text = "$label ${SetupArchitectEngine.formatPrice(price)}"
                val measured = textMeasurer.measure(
                    text = text,
                    style = TextStyle(color = ObsidianVoid, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                )
                val tagW = (rightAxisWidth - 2.dp.toPx()).coerceAtLeast(40f)
                val tagH = measured.size.height + 4.dp.toPx()
                val topY = (y - tagH / 2f).coerceIn(0f, pricePlotH - tagH)
                drawRect(bg, topLeft = Offset(plotW + 2.dp.toPx(), topY), size = Size(tagW, tagH))
                drawText(measured, topLeft = Offset(plotW + 4.dp.toPx(), topY + 2.dp.toPx()))
            }

            if (setupPlan != null) {
                drawPriceTag("TP3", setupPlan.tp3, LiveEmerald, dashed = true)
                drawPriceTag("TP2", setupPlan.tp2, LiveEmerald.copy(alpha = 0.85f), dashed = true)
                drawPriceTag("TP1", setupPlan.tp1, MetallicGoldBright, dashed = true)
                drawPriceTag("ENT", setupPlan.entryPrice, IceTelemetry, dashed = false)
                drawPriceTag("SL", setupPlan.stopLoss, CrimsonAlert, dashed = true)
            } else {
                drawPriceTag("LAST", lastBar.close, if (lastBar.close >= lastBar.open) LiveEmerald else CrimsonAlert, dashed = false)
            }

            // Volume Bars + Vol SMA 9
            val maxVol = max(candles.maxOfOrNull { it.volume } ?: 1.0, 1.0)
            candles.forEachIndexed { idx, bar ->
                val cx = idx * stepX + stepX / 2f
                val hFrac = (bar.volume / maxVol).toFloat().coerceIn(0f, 1f)
                val barH = max(2f, volH * hFrac)
                val col = if (bar.close >= bar.open) LiveEmerald.copy(alpha = 0.48f) else CrimsonAlert.copy(alpha = 0.48f)
                drawRect(col, topLeft = Offset(cx - stepX * 0.3f, size.height - barH), size = Size(max(2f, stepX * 0.6f), barH))
            }
            val volPath = Path()
            var volStarted = false
            volSmaTail.forEachIndexed { idx, v ->
                if (v != null && maxVol > 0) {
                    val x = idx * stepX + stepX / 2f
                    val y = size.height - ((v / maxVol).toFloat().coerceIn(0f, 1f) * volH)
                    if (!volStarted) {
                        volPath.moveTo(x, y); volStarted = true
                    } else volPath.lineTo(x, y)
                }
            }
            if (volStarted) drawPath(volPath, color = MetallicGoldBright, style = Stroke(width = 1.2.dp.toPx()))
        }

        // RSI(14) + RSI SMA(14) Oscillator Subpane
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val rightAxisWidth = 62.dp.toPx()
            val plotW = (size.width - rightAxisWidth).coerceAtLeast(40f)
            val stepX = plotW / candles.size.coerceAtLeast(1)

            fun mapRsiY(v: Double): Float = (size.height - (v.coerceIn(0.0, 100.0) / 100.0) * size.height).toFloat()

            // 30 / 70 Overbought-Oversold Band
            drawRect(
                color = IceTelemetry.copy(alpha = 0.07f),
                topLeft = Offset(0f, mapRsiY(70.0)),
                size = Size(plotW, mapRsiY(30.0) - mapRsiY(70.0))
            )
            listOf(30.0, 50.0, 70.0).forEach { band ->
                val y = mapRsiY(band)
                drawLine(
                    ObsidianBorder,
                    Offset(0f, y),
                    Offset(plotW, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            }

            fun drawRsiCurve(series: List<Double?>, col: Color, w: Float) {
                val p = Path()
                var started = false
                series.forEachIndexed { idx, v ->
                    if (v != null) {
                        val x = idx * stepX + stepX / 2f
                        val y = mapRsiY(v)
                        if (!started) { p.moveTo(x, y); started = true } else p.lineTo(x, y)
                    }
                }
                if (started) drawPath(p, col, style = Stroke(width = w, cap = StrokeCap.Round))
            }
            drawRsiCurve(rsiSmaTail, SimulatedAmber.copy(alpha = 0.7f), 1.1.dp.toPx())
            drawRsiCurve(rsiTail, Color(0xFFA78BFA), 1.6.dp.toPx())

            overlays.latestRsi14?.let { rsi ->
                val measured = textMeasurer.measure(
                    text = "RSI ${String.format(Locale.US, "%.1f", rsi)}",
                    style = TextStyle(color = IvoryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                drawRect(
                    color = Color(0xFF5B21B6),
                    topLeft = Offset(plotW + 2.dp.toPx(), mapRsiY(rsi).coerceIn(0f, size.height - 16.dp.toPx())),
                    size = Size(rightAxisWidth - 2.dp.toPx(), 16.dp.toPx())
                )
                drawText(measured, topLeft = Offset(plotW + 4.dp.toPx(), mapRsiY(rsi).coerceIn(0f, size.height - 14.dp.toPx())))
            }
        }
    }
}

@Composable
private fun MultiChart2x2Grid(
    quotes: List<MarketQuote>,
    selectedSymbol: String,
    timeframe: String,
    onSelectSymbol: (String) -> Unit
) {
    val topFour = remember(quotes, selectedSymbol) {
        val selected = quotes.find { it.instrument.symbol == selectedSymbol && it.isLive }
        val others = quotes.filter { it.isLive && it.instrument.symbol != selectedSymbol }
        (listOfNotNull(selected) + others).take(4)
    }

    if (topFour.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(ObsidianVoid, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Sync live feeds to render 2×2 multi-chart workspace.", color = SlateMuted, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val rows = topFour.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("tradingview_multichart_grid")) {
        rows.forEach { rowQuotes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowQuotes.forEach { q ->
                    val isSelected = q.instrument.symbol == selectedSymbol
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianVoid,
                        border = BorderStroke(1.dp, if (isSelected) MetallicGold else ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSymbol(q.instrument.symbol) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "${q.instrument.symbol} $timeframe",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MetallicGoldBright else IvoryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    String.format(Locale.US, "%+.2f%%", q.changePct),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (q.changePct >= 0) LiveEmerald else CrimsonAlert
                                )
                            }
                            Text(
                                SetupArchitectEngine.formatPrice(q.price),
                                style = MaterialTheme.typography.labelMedium,
                                color = ChampagneSecondary
                            )
                            CandleChartCanvas(candles = q.candles.takeLast(28), modifier = Modifier.height(82.dp))
                        }
                    }
                }
                if (rowQuotes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TradingViewLiveWebWidget(
    tvSymbol: String,
    interval: String,
    heightDp: Int
) {
    var loadError by remember(tvSymbol, interval) { mutableStateOf<String?>(null) }
    val htmlPayload = remember(tvSymbol, interval) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html, body { margin:0; padding:0; width:100%; height:100%; background:#060609; overflow:hidden; }
            .tradingview-widget-container { width:100%; height:100%; }
          </style>
        </head>
        <body>
          <div class="tradingview-widget-container">
            <div id="tv_chart" style="width:100%;height:100%;"></div>
            <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
            <script type="text/javascript">
              try {
                new TradingView.widget({
                  "autosize": true,
                  "symbol": "$tvSymbol",
                  "interval": "$interval",
                  "timezone": "Etc/UTC",
                  "theme": "dark",
                  "style": "1",
                  "locale": "en",
                  "toolbar_bg": "#0C0C12",
                  "backgroundColor": "#060609",
                  "gridColor": "rgba(40, 37, 52, 0.55)",
                  "enable_publishing": false,
                  "allow_symbol_change": true,
                  "studies": ["MASimple@tv-basicstudies", "RSI@tv-basicstudies"],
                  "container_id": "tv_chart"
                });
              } catch (e) {
                document.body.innerHTML = '<div style="color:#F3DF99;padding:16px;font-family:sans-serif;font-size:13px;">TradingView widget offline: ' + e.message + '</div>';
              }
            </script>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianVoid)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#060609"))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                loadError = error?.description?.toString() ?: "TradingView feed unreachable"
                            }
                        }
                    }
                    loadDataWithBaseURL("https://s3.tradingview.com", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                val tagKey = "$tvSymbol|$interval"
                if (webView.tag != tagKey) {
                    webView.tag = tagKey
                    webView.loadDataWithBaseURL("https://s3.tradingview.com", htmlPayload, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize().testTag("tradingview_webview")
        )
        if (!loadError.isNullOrBlank()) {
            Surface(
                color = ObsidianElevated.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "TradingView Embed Warning: $loadError (Switch to Native Chart + Setup Drawings for instant local candle rendering)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonAlert,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SetupLevelsStrip(setupPlan: TradeSetupPlan) {
    val dirColor = if (setupPlan.direction == "LONG") LiveEmerald else CrimsonAlert
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianElevated,
        border = BorderStroke(1.dp, dirColor.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SETUP ARCHITECT · ${setupPlan.symbol} ${setupPlan.direction} (${setupPlan.confidencePct}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGoldBright,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (setupPlan.isAiRefined) "AI REFINED" else "AUTO-DRAWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (setupPlan.isAiRefined) IceTelemetry else LiveEmerald
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LevelBadgePill("ENTRY", SetupArchitectEngine.formatPrice(setupPlan.entryPrice), IceTelemetry)
                LevelBadgePill("SL", SetupArchitectEngine.formatPrice(setupPlan.stopLoss), CrimsonAlert)
                LevelBadgePill("TP1", SetupArchitectEngine.formatPrice(setupPlan.tp1), MetallicGoldBright)
                LevelBadgePill("TP2", SetupArchitectEngine.formatPrice(setupPlan.tp2), LiveEmerald)
                LevelBadgePill("TP3", SetupArchitectEngine.formatPrice(setupPlan.tp3), LiveEmerald)
            }
        }
    }
}

@Composable
private fun LevelBadgePill(label: String, priceText: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Text(
            text = "$label $priceText",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
