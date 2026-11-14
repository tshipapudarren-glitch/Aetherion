package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.VipModuleId
import com.example.domain.engine.LiquidityTimelineBucket
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.TelemetryStatTile
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
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class RadarScope(val label: String) {
    ASSET_CLASSES("Asset Classes"),
    REGIONS("Geographic Regions")
}

private data class RadarAxisEntry(
    val shortLabel: String,
    val fullName: String,
    val flowScore: Int,
    val volumeVelocityNorm: Int,
    val avgChangePct: Double,
    val realizedVolPct: Double,
    val volumeImpulseRatio: Double,
    val leadSymbol: String,
    val liveCount: Int,
    val extraBadge: String
)

@Composable
fun CapitalFlowRadarScreen(
    state: AetherDashboardState,
    onRefreshMarkets: () -> Unit = {},
    onSelectSymbol: (String) -> Unit = {},
    onAnalyzeFlowsAi: () -> Unit = {},
    onBack: () -> Unit
) {
    val flow = state.capitalFlowSnapshot
    var scope by remember { mutableStateOf(RadarScope.ASSET_CLASSES) }
    var selectedAxisIdx by remember(scope) { mutableIntStateOf(0) }
    var selectedTimelineIdx by remember { mutableIntStateOf(7) }

    val activeAxes = remember(flow, scope) {
        when (scope) {
            RadarScope.ASSET_CLASSES -> flow.assetFlows.map { a ->
                RadarAxisEntry(
                    shortLabel = a.shortLabel,
                    fullName = a.displayName,
                    flowScore = a.flowScore,
                    volumeVelocityNorm = ((a.volumeImpulseRatio / 2.2) * 100.0).toInt().coerceIn(15, 100),
                    avgChangePct = a.avgChangePct,
                    realizedVolPct = a.avgVolPct,
                    volumeImpulseRatio = a.volumeImpulseRatio,
                    leadSymbol = a.topLeaderSymbol,
                    liveCount = a.liveCount,
                    extraBadge = "${a.liveCount} feeds"
                )
            }
            RadarScope.REGIONS -> flow.regionFlows.map { r ->
                RadarAxisEntry(
                    shortLabel = r.shortLabel,
                    fullName = r.region,
                    flowScore = r.flowScore,
                    volumeVelocityNorm = ((r.volumeImpulseRatio / 2.2) * 100.0).toInt().coerceIn(15, 100),
                    avgChangePct = r.avgChangePct,
                    realizedVolPct = r.avgVolPct,
                    volumeImpulseRatio = r.volumeImpulseRatio,
                    leadSymbol = r.leadSymbol,
                    liveCount = r.liveCount,
                    extraBadge = if (r.sessionActive) "SESSION LIVE" else "AFTER-HOURS"
                )
            }
        }
    }
    val focusedAxis = activeAxes.getOrNull(selectedAxisIdx) ?: activeAxes.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    ModuleHeader(VipModuleId.GLOBAL_CAPITAL_FLOW_RADAR, onBack)
                }
                IconButton(
                    onClick = onRefreshMarkets,
                    modifier = Modifier.size(48.dp).testTag("refresh_capital_flows_button")
                ) {
                    if (state.isRefreshingMarkets) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MetallicGoldBright, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Liquidity Telemetry", tint = MetallicGoldBright)
                    }
                }
            }
        }

        // Top Liquidity KPI Summary
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatTile(
                    label = "RISK FLOW",
                    value = "${flow.netLiquidityScore}/100",
                    accent = if (flow.netLiquidityScore >= 55) LiveEmerald else SimulatedAmber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = "INFLOW LEAD",
                    value = flow.dominantInflow,
                    accent = LiveEmerald,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = "OUTFLOW LEAD",
                    value = flow.dominantOutflow,
                    accent = CrimsonAlert,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Scope Toggle: Asset Classes vs Geographic Regions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadarScope.entries.forEach { mode ->
                    FilterChip(
                        selected = scope == mode,
                        onClick = { scope = mode },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = MetallicGoldBright
                        ),
                        modifier = Modifier.testTag("radar_scope_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // 1. Labeled Multi-Series Spider / Radar Chart
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.55f)) {
                Text(
                    text = "MULTI-AXIS LIQUIDITY RADAR · ${scope.label.uppercase(Locale.US)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MetallicGoldBright
                )
                Text(
                    text = "Gold Polygon: Directional Capital Inflow (-100..+100) · Cyan: Volume/Vol Pressure",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (activeAxes.size >= 3) {
                    LiquiditySpiderRadarCanvas(
                        axes = activeAxes,
                        selectedIdx = selectedAxisIdx
                    )
                } else {
                    Text(
                        "Awaiting at least 3 live asset/region feeds to render radar geometry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SimulatedAmber
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("● Gold: Capital Flow Impulse", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
                    Text("● Cyan: Volume / Activity Pressure", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                }
            }
        }

        // 2. Diverging Inflow / Outflow Bar Chart + Interactive Tooltip Card
        item {
            GlassCard {
                Text(
                    text = "NET INFLOW VS OUTFLOW ROTATION BARS (TAP TO INSPECT)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MetallicGoldBright
                )
                Spacer(modifier = Modifier.height(8.dp))

                activeAxes.forEachIndexed { idx, axis ->
                    val isSelected = idx == selectedAxisIdx
                    val isPositive = axis.flowScore >= 0
                    val barColor = if (isPositive) LiveEmerald else CrimsonAlert
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MetallicGoldContainer.copy(alpha = 0.6f) else ObsidianVoid.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (isSelected) MetallicGold else ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedAxisIdx = idx }
                            .testTag("flow_bar_${axis.shortLabel}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${axis.fullName} (${axis.extraBadge})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IvoryText,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.2f%% · Score %+d", axis.avgChangePct, axis.flowScore),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = barColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { (abs(axis.flowScore) / 100f).coerceIn(0.05f, 1f) },
                                color = barColor,
                                trackColor = ObsidianElevated,
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                }

                if (focusedAxis != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, IceTelemetry.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOOLTIP INSPECTOR · ${focusedAxis.fullName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IceTelemetry,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { onSelectSymbol(focusedAxis.leadSymbol) },
                                    modifier = Modifier.testTag("tooltip_select_symbol_button")
                                ) {
                                    Text(
                                        text = "Select ${focusedAxis.leadSymbol} →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MetallicGoldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TelemetryStatTile("AVG Δ", String.format(Locale.US, "%+.2f%%", focusedAxis.avgChangePct), if (focusedAxis.avgChangePct >= 0) LiveEmerald else CrimsonAlert, Modifier.weight(1f))
                                TelemetryStatTile("VOL IMPULSE", String.format(Locale.US, "%.2fx", focusedAxis.volumeImpulseRatio), MetallicGoldBright, Modifier.weight(1f))
                                TelemetryStatTile("REALIZED VOL", String.format(Locale.US, "%.1f%%", focusedAxis.realizedVolPct), SimulatedAmber, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Multi-Series Liquidity Rotation AreaChart (24h Hourly Evolution)
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.45f)) {
                Text("24H CROSS-ASSET LIQUIDITY ROTATION CURVES", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Text(
                    "Stacked trajectory across Safe Havens (Gold), Equities (Emerald), Crypto (Cyan) & Energy (Amber) from live candles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (flow.timelineBuckets.size >= 2) {
                    LiquidityTimelineAreaCanvas(
                        buckets = flow.timelineBuckets,
                        selectedIdx = selectedTimelineIdx
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        flow.timelineBuckets.forEachIndexed { idx, bucket ->
                            FilterChip(
                                selected = idx == selectedTimelineIdx,
                                onClick = { selectedTimelineIdx = idx },
                                label = { Text(bucket.bucketLabel, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MetallicGoldContainer,
                                    selectedLabelColor = MetallicGoldBright
                                )
                            )
                        }
                    }
                    val b = flow.timelineBuckets.getOrNull(selectedTimelineIdx) ?: flow.timelineBuckets.last()
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TelemetryStatTile("HAVENS", String.format(Locale.US, "%+.2f%%", b.safeHavensPct), MetallicGoldBright, Modifier.weight(1f))
                        TelemetryStatTile("EQUITY", String.format(Locale.US, "%+.2f%%", b.equitiesPct), LiveEmerald, Modifier.weight(1f))
                        TelemetryStatTile("CRYPTO", String.format(Locale.US, "%+.2f%%", b.cryptoPct), IceTelemetry, Modifier.weight(1f))
                        TelemetryStatTile("ENERGY", String.format(Locale.US, "%+.2f%%", b.energyPct), SimulatedAmber, Modifier.weight(1f))
                    }
                }
            }
        }

        // 4. Capital Rotation Corridors & AI LIQUIDITY X Briefing
        item {
            GlassCard {
                Text("ACTIVE LIQUIDITY ROTATION CORRIDORS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(8.dp))
                if (flow.corridors.isEmpty()) {
                    Text("Balanced liquidity distribution across active regions.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                } else {
                    flow.corridors.forEach { c ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Capital corridor", tint = MetallicGoldBright, modifier = Modifier.size(16.dp))
                                    Text("${c.fromLabel}  →  ${c.toLabel}", style = MaterialTheme.typography.labelMedium, color = IvoryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(String.format(Locale.US, "%+.2f%% spread", c.spreadPct), style = MaterialTheme.typography.labelSmall, color = LiveEmerald, fontWeight = FontWeight.Bold)
                            }
                            Text(c.interpretation, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                            LinearProgressIndicator(
                                progress = { (c.intensityPct / 100f).coerceIn(0.1f, 1f) },
                                color = MetallicGoldBright,
                                trackColor = ObsidianVoid,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAnalyzeFlowsAi,
                    enabled = state.capitalFlowAiBriefState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("analyze_capital_flow_ai_button")
                ) {
                    if (state.capitalFlowAiBriefState is AiTaskState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("LIQUIDITY X Analyzing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Psychology, contentDescription = "Analyze Flows with LIQUIDITY X", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Analyze Liquidity Regime with AI", fontWeight = FontWeight.Bold)
                    }
                }

                when (val s = state.capitalFlowAiBriefState) {
                    is AiTaskState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = ObsidianVoid.copy(alpha = 0.85f), border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.55f))) {
                            Text(s.data, style = MaterialTheme.typography.bodySmall, color = IvoryText, modifier = Modifier.padding(12.dp))
                        }
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI ERROR: ${s.message}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
                }
            }
        }

        // 5. G10 Currency & Safe-Haven Strength Matrix
        item {
            GlassCard {
                Text("G10 FX & SAFE-HAVEN FLOW MATRIX", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(8.dp))
                state.currencyStrengths.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.code, style = MaterialTheme.typography.labelLarge, color = IvoryText)
                        Text(
                            String.format(Locale.US, "%+.2f%% flow delta", item.strengthScore),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.strengthScore >= 0) LiveEmerald else CrimsonAlert
                        )
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "CAPITAL_FLOW_RADAR") }
    }
}

@Composable
private fun LiquiditySpiderRadarCanvas(
    axes: List<RadarAxisEntry>,
    selectedIdx: Int
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(ObsidianVoid.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy) * 0.70f
        val n = axes.size

        // Concentric Web Polygon Rings
        for (ring in 1..4) {
            val r = maxR * (ring / 4f)
            val webPath = Path()
            for (i in 0 until n) {
                val angle = (2.0 * Math.PI * i / n) - Math.PI / 2.0
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
            }
            webPath.close()
            drawPath(
                path = webPath,
                color = if (ring == 2) MetallicGold.copy(alpha = 0.45f) else ObsidianBorder.copy(alpha = 0.7f),
                style = Stroke(width = if (ring == 2) 1.5.dp.toPx() else 1.dp.toPx())
            )
        }

        // Spokes + Axis Perimeter Labels
        axes.forEachIndexed { idx, axis ->
            val angle = (2.0 * Math.PI * idx / n) - Math.PI / 2.0
            val outerX = cx + (maxR * cos(angle)).toFloat()
            val outerY = cy + (maxR * sin(angle)).toFloat()
            drawLine(
                color = ObsidianBorder.copy(alpha = 0.85f),
                start = Offset(cx, cy),
                end = Offset(outerX, outerY),
                strokeWidth = 1.dp.toPx()
            )

            val labelR = maxR + 22.dp.toPx()
            val lx = cx + (labelR * cos(angle)).toFloat()
            val ly = cy + (labelR * sin(angle)).toFloat()
            val measured = textMeasurer.measure(
                text = axis.shortLabel,
                style = TextStyle(
                    color = if (idx == selectedIdx) MetallicGoldBright else ChampagneSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Medium
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (lx - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    y = (ly - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height)
                )
            )
        }

        // Volume / Activity Polygon (Cyan)
        val volPoly = Path()
        axes.forEachIndexed { idx, axis ->
            val angle = (2.0 * Math.PI * idx / n) - Math.PI / 2.0
            val r = maxR * (axis.volumeVelocityNorm / 100f).coerceIn(0.18f, 1.0f)
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (idx == 0) volPoly.moveTo(px, py) else volPoly.lineTo(px, py)
        }
        volPoly.close()
        drawPath(volPoly, color = IceTelemetry.copy(alpha = 0.14f))
        drawPath(volPoly, color = IceTelemetry, style = Stroke(width = 1.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)))

        // Directional Flow Polygon (Gold)
        val flowPoly = Path()
        axes.forEachIndexed { idx, axis ->
            val angle = (2.0 * Math.PI * idx / n) - Math.PI / 2.0
            val norm = ((axis.flowScore + 100.0) / 200.0).coerceIn(0.18, 1.0).toFloat()
            val r = maxR * norm
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (idx == 0) flowPoly.moveTo(px, py) else flowPoly.lineTo(px, py)
        }
        flowPoly.close()
        drawPath(flowPoly, color = MetallicGold.copy(alpha = 0.28f))
        drawPath(flowPoly, color = MetallicGoldBright, style = Stroke(width = 2.6.dp.toPx()))

        // Data Vertex Markers
        axes.forEachIndexed { idx, axis ->
            val angle = (2.0 * Math.PI * idx / n) - Math.PI / 2.0
            val norm = ((axis.flowScore + 100.0) / 200.0).coerceIn(0.18, 1.0).toFloat()
            val r = maxR * norm
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            drawCircle(
                color = if (axis.flowScore >= 0) LiveEmerald else CrimsonAlert,
                radius = if (idx == selectedIdx) 6.dp.toPx() else 4.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun LiquidityTimelineAreaCanvas(
    buckets: List<LiquidityTimelineBucket>,
    selectedIdx: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .background(ObsidianVoid.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        val allValues = buckets.flatMap { listOf(it.safeHavensPct, it.equitiesPct, it.cryptoPct, it.energyPct) } + 0.0
        val minV = (allValues.minOrNull() ?: -1.0).coerceAtMost(-0.5)
        val maxV = (allValues.maxOrNull() ?: 1.0).coerceAtLeast(0.5)
        val range = (maxV - minV).coerceAtLeast(0.2)
        val stepX = size.width / (buckets.size - 1).coerceAtLeast(1)

        fun mapY(v: Double): Float = (size.height - ((v - minV) / range) * size.height).toFloat()

        // Zero baseline
        val zeroY = mapY(0.0)
        drawLine(
            color = SlateMuted.copy(alpha = 0.65f),
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        fun drawAreaSeries(values: List<Double>, color: Color) {
            if (values.size < 2) return
            val line = Path()
            val fill = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = mapY(v)
                if (i == 0) {
                    line.moveTo(x, y)
                    fill.moveTo(x, zeroY)
                    fill.lineTo(x, y)
                } else {
                    line.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo((values.size - 1) * stepX, zeroY)
            fill.close()
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.22f), Color.Transparent)
                )
            )
            drawPath(line, color = color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
        }

        drawAreaSeries(buckets.map { it.energyPct }, SimulatedAmber)
        drawAreaSeries(buckets.map { it.cryptoPct }, IceTelemetry)
        drawAreaSeries(buckets.map { it.equitiesPct }, LiveEmerald)
        drawAreaSeries(buckets.map { it.safeHavensPct }, MetallicGoldBright)

        val focusX = selectedIdx.coerceIn(0, buckets.lastIndex) * stepX
        drawLine(
            color = IvoryText.copy(alpha = 0.5f),
            start = Offset(focusX, 0f),
            end = Offset(focusX, size.height),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
