package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AssetClass
import com.example.data.model.VipModuleId
import com.example.ui.components.CandleChartCanvas
import com.example.ui.components.EnvironmentBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.TelemetryStatTile
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommandCentreScreen(
    state: AetherDashboardState,
    onRefreshMarkets: () -> Unit,
    onSelectSymbol: (String) -> Unit,
    onToggleAutonomousCycle: () -> Unit,
    onToggleKillSwitch: (Boolean) -> Unit,
    onOpenConditions: () -> Unit,
    onOpenNarratives: () -> Unit = {},
    onOpenModule: (VipModuleId) -> Unit
) {
    var selectedAssetFilter by remember { mutableStateOf<AssetClass?>(null) }
    val filteredQuotes = remember(state.quotes, selectedAssetFilter) {
        if (selectedAssetFilter == null) state.quotes
        else state.quotes.filter { it.instrument.assetClass == selectedAssetFilter }
    }
    val selectedQuote = state.quotes.find { it.instrument.symbol == state.selectedSymbol }
        ?: state.quotes.firstOrNull { it.isLive }

    val liveCount = state.quotes.count { it.isLive }
    val activeAgentsCount = state.agents.count { it.status == "ACTIVE" || it.status == "ENFORCING" }
    val openPaperCount = state.paperOrders.count { it.status == "OPEN" }
    val mc = state.marketConditions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Command Banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianElevated,
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(145.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_aetherion_command_hero_1794624585480),
                        contentDescription = "AETHERION Command Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianVoid.copy(alpha = 0.94f),
                                        ObsidianVoid.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AETHER CORE · VIP BLACK LABEL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MetallicGoldBright
                                )
                                Text(
                                    text = "Global Command Centre",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = IvoryText
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onOpenModule(VipModuleId.COMMANDER_VOICE_INTELLIGENCE) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("quick_open_live_voice")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = "Open Gemini 3.8 Live Voice Conversations",
                                        tint = if (state.geminiLive.isLiveActive) LiveEmerald else MetallicGoldBright
                                    )
                                }
                                IconButton(
                                    onClick = onRefreshMarkets,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("refresh_markets_button")
                                ) {
                                    if (state.isRefreshingMarkets) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MetallicGold,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Refresh Live Markets",
                                            tint = MetallicGoldBright
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onToggleAutonomousCycle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.autonomousCycle.isRunning) LiveEmerald else MetallicGold,
                                    contentColor = ObsidianVoid
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("toggle_autonomous_cycle_button")
                            ) {
                                Icon(
                                    imageVector = if (state.autonomousCycle.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Toggle Autonomous Cycle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.autonomousCycle.isRunning) "CYCLE LIVE" else "RUN CYCLE",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick = { onToggleKillSwitch(!state.constitution.emergencyKillSwitch) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.constitution.emergencyKillSwitch) CrimsonAlert else CrimsonContainer,
                                    contentColor = IvoryText
                                ),
                                border = BorderStroke(1.dp, CrimsonAlert),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("home_kill_switch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Kill Switch",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.constitution.emergencyKillSwitch) "KILL ACTIVE" else "KILL SWITCH",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Core Operational KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatTile(
                    label = "LIVE FEEDS",
                    value = "$liveCount/${state.quotes.size}",
                    accent = if (liveCount > 0) LiveEmerald else CrimsonAlert,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = "AGENTS ACTIVE",
                    value = "$activeAgentsCount/30",
                    accent = MetallicGoldBright,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = "PAPER POS",
                    value = "$openPaperCount OPEN",
                    accent = SimulatedAmber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatTile(
                    label = "PERM LEVEL",
                    value = "LVL ${state.constitution.permissionLevel}/5",
                    accent = if (state.constitution.emergencyKillSwitch) CrimsonAlert else MetallicGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Market Conditions Card (tap to open full Live Market Conditions screen)
        item {
            GlassCard(
                borderColor = MetallicGold.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenConditions() }
                    .testTag("home_market_conditions_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE MARKET CONDITIONS · ${mc.overallRegime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGoldBright,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "RISK ${mc.riskAppetiteScore}/100 →",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mc.riskAppetiteScore >= 55) LiveEmerald else CrimsonAlert
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${mc.activeTradingSessions} · VIX ${mc.vixLevel?.let { String.format(Locale.US, "%.2f", it) } ?: "--"} · 10Y ${mc.us10yYield?.let { String.format(Locale.US, "%.2f%%", it) } ?: "--"} · Adv/Dec ${mc.advancersCount}/${mc.declinersCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
            }
        }

        // Narrative & Attention Radar Summary Card
        item {
            val nar = state.narrativeSnapshot
            val topCluster = nar.clusters.firstOrNull()
            GlassCard(
                borderColor = if (nar.totalConflictsDetected > 0) CrimsonAlert.copy(alpha = 0.55f) else MetallicGold.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenNarratives() }
                    .testTag("home_narrative_radar_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NARRATIVE ATTENTION RADAR · ${nar.clusters.size} CLUSTERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGoldBright,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ATTN ${nar.globalAttentionIndex}/100 →",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nar.totalConflictsDetected > 0) CrimsonAlert else LiveEmerald
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (topCluster != null) {
                        "${topCluster.topicTitle} (${String.format(Locale.US, "%+d%%", topCluster.growthRatePct)} growth) · ${nar.totalConflictsDetected} conflicts · ${nar.totalArticlesAnalyzed} articles"
                    } else {
                        "Tap to inspect live topic clustering, news-source splits, event timelines, decay & market reaction overlays."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Autonomous Cycle Status Strip
        item {
            GlassCard(
                borderColor = if (state.autonomousCycle.isRunning) LiveEmerald.copy(alpha = 0.55f) else ObsidianBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUTONOMOUS CYCLE · ${state.autonomousCycle.completedCycles} COMPLETED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGoldBright
                    )
                    Text(
                        text = if (state.autonomousCycle.isRunning) "RUNNING" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.autonomousCycle.isRunning) LiveEmerald else SlateMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.autonomousCycle.lastStepSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = IvoryText
                )
            }
        }

        // Market Fetch Error Banner if offline
        if (!state.marketFetchError.isNullOrBlank()) {
            item {
                GlassCard(borderColor = CrimsonAlert) {
                    Text(
                        text = "MARKET TELEMETRY ERROR: ${state.marketFetchError}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonAlert
                    )
                }
            }
        }

        // Selected Instrument Live Chart
        if (selectedQuote != null) {
            item {
                GlassCard(borderColor = MetallicGold.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedQuote.instrument.symbol} · ${selectedQuote.instrument.displayName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryText
                            )
                            Text(
                                text = "${selectedQuote.sourceName} · ${selectedQuote.latencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedQuote.isLive) LiveEmerald else CrimsonAlert
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (selectedQuote.isLive) String.format(Locale.US, "%.4f", selectedQuote.price) else "OFFLINE",
                                style = MaterialTheme.typography.titleMedium,
                                color = MetallicGoldBright
                            )
                            Text(
                                text = String.format(Locale.US, "%+.2f%% · Vol %.1f%%", selectedQuote.changePct, selectedQuote.realizedVolPct),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedQuote.changePct >= 0) LiveEmerald else CrimsonAlert
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    CandleChartCanvas(candles = selectedQuote.candles)
                }
            }
        }

        // Market Universe Filter Chips + Live Tickers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIVE MARKET UNIVERSE",
                    style = MaterialTheme.typography.titleSmall,
                    color = MetallicGoldBright
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAssetFilter == null,
                        onClick = { selectedAssetFilter = null },
                        label = { Text("All (${state.quotes.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = MetallicGoldBright
                        )
                    )
                    AssetClass.entries.forEach { asset ->
                        FilterChip(
                            selected = selectedAssetFilter == asset,
                            onClick = { selectedAssetFilter = if (selectedAssetFilter == asset) null else asset },
                            label = { Text(asset.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            )
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredQuotes, key = { it.instrument.symbol }) { q ->
                        val isSelected = q.instrument.symbol == state.selectedSymbol
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MetallicGoldContainer else ObsidianElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MetallicGold else ObsidianBorder
                            ),
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectSymbol(q.instrument.symbol) }
                                .testTag("ticker_${q.instrument.symbol}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = q.instrument.symbol,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = IvoryText
                                    )
                                    Text(
                                        text = if (q.isLive) "LIVE" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (q.isLive) LiveEmerald else CrimsonAlert
                                    )
                                }
                                Text(
                                    text = q.instrument.assetClass.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (q.isLive) String.format(Locale.US, "%.4f", q.price) else "CONFIG",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MetallicGoldBright
                                )
                                Text(
                                    text = if (q.isLive) String.format(Locale.US, "%+.2f%%", q.changePct) else "Disconnected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!q.isLive) CrimsonAlert else if (q.changePct >= 0) LiveEmerald else CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }

        // 15 VIP Black Label Capabilities Launcher
        item {
            Text(
                text = "15 VIP BLACK LABEL INTELLIGENCE MODULES",
                style = MaterialTheme.typography.titleSmall,
                color = MetallicGoldBright
            )
        }

        items(VipModuleId.entries.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { module ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenModule(module) }
                            .testTag("module_card_${module.number}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VIP-${module.number.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MetallicGold
                                )
                                EnvironmentBadge(module.environment)
                            }
                            Text(
                                text = module.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = IvoryText,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${module.layer.label} · ${module.leadAgent}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Cross-Module Communication Event Bus
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = "Event Bus", tint = MetallicGold)
                    Text(
                        text = "CROSS-MODULE EVENT BUS",
                        style = MaterialTheme.typography.titleSmall,
                        color = MetallicGoldBright
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                state.crossModuleEvents.take(6).forEach { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ev.sourceModule} → ${ev.targetModule}: ${ev.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ev.severity == "ALERT" || ev.severity == "CRITICAL") CrimsonAlert else ChampagneSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = fmt.format(Date(ev.timestampMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
            }
        }
    }
}
