package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalAuditLogEntity
import com.example.data.model.CandleBar
import com.example.data.model.EnvironmentMode
import com.example.data.model.VipModuleId
import com.example.domain.engine.CurrencyStrengthItem
import com.example.domain.engine.MultiverseScenarioReport
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.IceTelemetry
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.LiveEmeraldContainer
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SimulatedAmberContainer
import com.example.ui.theme.SlateMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ObsidianBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianGlass,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ObsidianElevated.copy(alpha = 0.78f),
                            ObsidianVoid.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
fun EnvironmentBadge(mode: EnvironmentMode, modifier: Modifier = Modifier) {
    val (bg, fg) = when (mode) {
        EnvironmentMode.LIVE -> LiveEmeraldContainer to LiveEmerald
        EnvironmentMode.HYBRID -> MetallicGoldContainer to MetallicGoldBright
        EnvironmentMode.SIMULATED -> SimulatedAmberContainer to SimulatedAmber
        EnvironmentMode.CONFIG_REQUIRED -> CrimsonContainer to CrimsonAlert
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = mode.badge,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ModuleHeader(
    module: VipModuleId,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("module_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Command Centre",
                tint = MetallicGold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "VIP-${module.number.toString().padStart(2, '0')} · ${module.layer.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGold
                )
                EnvironmentBadge(module.environment)
            }
            Text(
                text = module.title,
                style = MaterialTheme.typography.headlineSmall,
                color = IvoryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Lead Agent: ${module.leadAgent} · ${module.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = ChampagneSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActiveSymbolStrip(
    quotes: List<com.example.data.model.MarketQuote>,
    selectedSymbol: String,
    onSelectSymbol: (String) -> Unit
) {
    val symbols = quotes.filter { it.isLive }.take(10).ifEmpty {
        com.example.data.model.MarketUniverse.instruments.take(8).map {
            com.example.data.model.MarketQuote(it, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList(), "", 0, 0, false)
        }
    }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(symbols.size) { index ->
            val sym = symbols[index].instrument.symbol
            androidx.compose.material3.FilterChip(
                selected = sym == selectedSymbol,
                onClick = { onSelectSymbol(sym) },
                label = { Text(sym, style = MaterialTheme.typography.labelSmall) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MetallicGoldContainer,
                    selectedLabelColor = MetallicGoldBright
                )
            )
        }
    }
}

@Composable
fun TelemetryStatTile(
    label: String,
    value: String,
    accent: Color = MetallicGoldBright,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianVoid.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CandleChartCanvas(
    candles: List<CandleBar>,
    highlightIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(ObsidianVoid.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No OHLCV candles loaded for selected instrument",
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )
        }
        return
    }
    val visible = candles.takeLast(60)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(ObsidianVoid.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        val minLow = visible.minOf { it.low }
        val maxHigh = visible.maxOf { it.high }
        val range = (maxHigh - minLow).coerceAtLeast(1e-6)
        val stepX = size.width / visible.size.coerceAtLeast(1)

        // Grid lines
        for (g in 0..3) {
            val y = size.height * (g / 3f)
            drawLine(
                color = ObsidianBorder.copy(alpha = 0.6f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        visible.forEachIndexed { i, bar ->
            val cx = i * stepX + stepX / 2f
            val yHigh = (size.height - ((bar.high - minLow) / range) * size.height).toFloat()
            val yLow = (size.height - ((bar.low - minLow) / range) * size.height).toFloat()
            val yOpen = (size.height - ((bar.open - minLow) / range) * size.height).toFloat()
            val yClose = (size.height - ((bar.close - minLow) / range) * size.height).toFloat()
            val bullish = bar.close >= bar.open
            val color = if (bullish) LiveEmerald else CrimsonAlert

            drawLine(color = color, start = Offset(cx, yHigh), end = Offset(cx, yLow), strokeWidth = 2f)
            val top = min(yOpen, yClose)
            val bodyH = max(3f, abs(yClose - yOpen))
            val bodyW = max(3f, stepX * 0.58f)
            drawRect(
                color = color,
                topLeft = Offset(cx - bodyW / 2f, top),
                size = Size(bodyW, bodyH)
            )
            if (highlightIndex != null && i == highlightIndex.coerceIn(0, visible.lastIndex)) {
                drawLine(
                    color = MetallicGoldBright,
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = 2f
                )
            }
        }
    }
}

@Composable
fun MultiverseFanCanvas(
    report: MultiverseScenarioReport,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(ObsidianVoid.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val allVals = report.p5Curve + report.p95Curve
        val minV = allVals.minOrNull() ?: 0.0
        val maxV = allVals.maxOrNull() ?: 1.0
        val range = (maxV - minV).coerceAtLeast(1e-6)
        val count = report.p50Curve.size.coerceAtLeast(2)
        val stepX = size.width / (count - 1)

        fun mapY(v: Double): Float = (size.height - ((v - minV) / range) * size.height).toFloat()

        // Shaded P5..P95 band
        val bandPath = Path().apply {
            report.p95Curve.forEachIndexed { i, v ->
                val x = i * stepX
                val y = mapY(v)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            for (i in report.p5Curve.indices.reversed()) {
                lineTo(i * stepX, mapY(report.p5Curve[i]))
            }
            close()
        }
        drawPath(bandPath, color = MetallicGold.copy(alpha = 0.14f))

        fun drawCurve(values: List<Double>, color: Color, strokeW: Float) {
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = mapY(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = strokeW))
        }

        drawCurve(report.p95Curve, LiveEmerald, 2.5f)
        drawCurve(report.p75Curve, IceTelemetry.copy(alpha = 0.7f), 1.8f)
        drawCurve(report.p50Curve, MetallicGoldBright, 4f)
        drawCurve(report.p25Curve, SimulatedAmber.copy(alpha = 0.75f), 1.8f)
        drawCurve(report.p5Curve, CrimsonAlert, 2.8f)
    }
}

@Composable
fun EquityCurveCanvas(
    equity: List<Double>,
    modifier: Modifier = Modifier
) {
    if (equity.size < 2) return
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(ObsidianVoid.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        val minV = equity.min()
        val maxV = equity.max()
        val range = (maxV - minV).coerceAtLeast(1e-4)
        val stepX = size.width / (equity.size - 1)
        val path = Path()
        equity.forEachIndexed { i, v ->
            val x = i * stepX
            val y = (size.height - ((v - minV) / range) * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val color = if (equity.last() >= equity.first()) MetallicGoldBright else CrimsonAlert
        drawPath(path, color = color, style = Stroke(width = 3.5f))
    }
}

@Composable
fun CapitalRadarCanvas(
    strengths: List<CurrencyStrengthItem>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .background(ObsidianVoid.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy) * 0.88f

        for (ring in 1..3) {
            drawCircle(
                color = MetallicGold.copy(alpha = 0.22f),
                radius = maxR * (ring / 3f),
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }
        if (strengths.isNotEmpty()) {
            val poly = Path()
            strengths.forEachIndexed { idx, item ->
                val angle = (2.0 * Math.PI * idx / strengths.size) - Math.PI / 2.0
                val norm = ((item.strengthScore + 3.0) / 6.0).coerceIn(0.2, 1.0).toFloat()
                val r = maxR * norm
                val px = cx + (r * cos(angle)).toFloat()
                val py = cy + (r * sin(angle)).toFloat()
                drawLine(
                    color = ObsidianBorder,
                    start = Offset(cx, cy),
                    end = Offset(cx + (maxR * cos(angle)).toFloat(), cy + (maxR * sin(angle)).toFloat()),
                    strokeWidth = 1f
                )
                drawCircle(
                    color = if (item.strengthScore >= 0) LiveEmerald else CrimsonAlert,
                    radius = 5f,
                    center = Offset(px, py)
                )
                if (idx == 0) poly.moveTo(px, py) else poly.lineTo(px, py)
            }
            poly.close()
            drawPath(poly, color = MetallicGold.copy(alpha = 0.24f))
            drawPath(poly, color = MetallicGoldBright, style = Stroke(width = 2.5f))
        }
    }
}

@Composable
fun ModuleAuditLogCard(
    logs: List<LocalAuditLogEntity>,
    moduleFilter: String? = null
) {
    val filtered = if (moduleFilter == null) logs.take(8) else logs.filter { it.moduleId == moduleFilter }.take(6)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "OPERATIONAL AUDIT TRAIL",
            style = MaterialTheme.typography.titleSmall,
            color = MetallicGoldBright
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Text(
                text = "No audit events recorded for this module yet.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateMuted
            )
        } else {
            val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
            filtered.forEach { log ->
                val color = when (log.outcome) {
                    "VETOED", "ERROR" -> CrimsonAlert
                    "APPROVED", "EXECUTED" -> LiveEmerald
                    "SIMULATED" -> SimulatedAmber
                    else -> ChampagneSecondary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${log.outcome}] ${log.action}",
                            style = MaterialTheme.typography.labelMedium,
                            color = color
                        )
                        Text(
                            text = "${log.actor}: ${log.details}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = fmt.format(Date(log.createdAtMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
            }
        }
    }
}
