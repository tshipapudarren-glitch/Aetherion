package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EnvironmentMode
import com.example.domain.engine.ConflictingReportPair
import com.example.domain.engine.MarketReactionComparison
import com.example.domain.engine.NarrativeCluster
import com.example.domain.engine.NarrativeLifecycleStage
import com.example.domain.engine.NarrativeTimelineEvent
import com.example.domain.engine.NewsSourceComparison
import com.example.ui.components.EnvironmentBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.TelemetryStatTile
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
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SimulatedAmberContainer
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.util.Locale

private enum class NarrativeTab(val label: String) {
    OVERVIEW("Overview & AI"),
    REACTION("Market Reaction"),
    GROWTH_DECAY("Growth & Decay"),
    SOURCES("Source Compare"),
    CONFLICTS("Conflicts"),
    TIMELINE("Timeline")
}

@Composable
fun NarrativeIntelligenceScreen(
    state: AetherDashboardState,
    onSelectCluster: (String) -> Unit,
    onSelectSymbol: (String) -> Unit,
    onRefreshNarratives: () -> Unit,
    onAnalyzeNarrativeWithAi: (String) -> Unit,
    onBack: () -> Unit
) {
    val snapshot = state.narrativeSnapshot
    val activeCluster = snapshot.clusters.find { it.id == state.selectedNarrativeId }
        ?: snapshot.clusters.firstOrNull()
    var activeTab by remember { mutableStateOf(NarrativeTab.OVERVIEW) }
    var selectedReactionIdx by remember(activeCluster?.id) { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("narrative_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MetallicGold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OMNISENSE · NARRATIVE ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetallicGold
                        )
                        EnvironmentBadge(EnvironmentMode.LIVE)
                    }
                    Text(
                        text = "Narrative & Attention Radar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = IvoryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onRefreshNarratives,
                    modifier = Modifier.size(48.dp).testTag("refresh_narratives_button")
                ) {
                    if (state.isRefreshingNarratives) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MetallicGoldBright, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Live Narratives", tint = MetallicGoldBright)
                    }
                }
            }
        }

        // Global KPI Matrix
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatTile("ATTENTION", "${snapshot.globalAttentionIndex}/100", MetallicGoldBright, Modifier.weight(1f))
                TelemetryStatTile("CLUSTERS", "${snapshot.clusters.size}", IceTelemetry, Modifier.weight(1f))
                TelemetryStatTile("SOURCES", "${snapshot.totalSourcesCount}", LiveEmerald, Modifier.weight(1f))
                TelemetryStatTile(
                    "CONFLICTS",
                    "${snapshot.totalConflictsDetected}",
                    if (snapshot.totalConflictsDetected > 0) CrimsonAlert else SlateMuted,
                    Modifier.weight(1f)
                )
            }
        }

        if (!state.headlineError.isNullOrBlank()) {
            item {
                GlassCard(borderColor = CrimsonAlert) {
                    Text("FEED WARNING: ${state.headlineError}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                }
            }
        }

        if (activeCluster == null) {
            item {
                GlassCard {
                    Text("AWAITING MULTI-SOURCE NARRATIVE FEEDS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tap Refresh to ingest Finnhub, FMP, CNBC, BBC, and Yahoo Finance wires for live topic clustering, conflict analysis, and price reaction mapping.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneSecondary
                    )
                }
            }
        } else {
            // Topic Clusters Selector Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOPIC CLUSTERS (${snapshot.clusters.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                        Text("${snapshot.totalArticlesAnalyzed} live articles", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(snapshot.clusters, key = { it.id }) { cluster ->
                            val selected = cluster.id == activeCluster.id
                            ClusterSelectorCard(
                                cluster = cluster,
                                selected = selected,
                                onClick = { onSelectCluster(cluster.id) }
                            )
                        }
                    }
                }
            }

            // Analytical Tab Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NarrativeTab.entries.forEach { tab ->
                        val badgeSuffix = when (tab) {
                            NarrativeTab.CONFLICTS -> if (activeCluster.conflictingReports.isNotEmpty()) " (${activeCluster.conflictingReports.size})" else ""
                            NarrativeTab.SOURCES -> " (${activeCluster.distinctSourcesCount})"
                            NarrativeTab.TIMELINE -> " (${activeCluster.timelineEvents.size})"
                            else -> ""
                        }
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text("${tab.label}$badgeSuffix") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            ),
                            modifier = Modifier.testTag("narrative_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            when (activeTab) {
                NarrativeTab.OVERVIEW -> {
                    item {
                        NarrativeOverviewCard(
                            cluster = activeCluster,
                            aiBriefState = state.narrativeAiBriefState,
                            activeProviderLabel = state.providerSettings.activeProvider.displayName,
                            onAnalyzeAi = { onAnalyzeNarrativeWithAi(activeCluster.id) },
                            onSelectSymbol = onSelectSymbol
                        )
                    }
                }

                NarrativeTab.REACTION -> {
                    item {
                        MarketReactionPanel(
                            cluster = activeCluster,
                            selectedIdx = selectedReactionIdx,
                            onSelectReaction = { idx ->
                                selectedReactionIdx = idx
                                activeCluster.marketReactions.getOrNull(idx)?.let { onSelectSymbol(it.symbol) }
                            }
                        )
                    }
                }

                NarrativeTab.GROWTH_DECAY -> {
                    item {
                        GrowthAndDecayPanel(
                            cluster = activeCluster,
                            fastestGrowing = snapshot.fastestGrowingNarrative,
                            fastestDecaying = snapshot.fastestDecayingNarrative
                        )
                    }
                }

                NarrativeTab.SOURCES -> {
                    item {
                        Text(
                            "NEWS-SOURCE FRAMING & SENTIMENT COMPARISON (${activeCluster.sourceComparisons.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MetallicGoldBright
                        )
                    }
                    items(activeCluster.sourceComparisons, key = { it.sourceName }) { src ->
                        SourceComparisonCard(src)
                    }
                }

                NarrativeTab.CONFLICTS -> {
                    item {
                        GlassCard(borderColor = if (activeCluster.conflictingReports.isNotEmpty()) CrimsonAlert.copy(alpha = 0.6f) else ObsidianBorder) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CONFLICT & DIVERGENCE INTENSITY", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                                Text(
                                    "${activeCluster.conflictIntensityPct}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (activeCluster.conflictIntensityPct >= 60) CrimsonAlert else LiveEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (activeCluster.conflictIntensityPct / 100f).coerceIn(0f, 1f) },
                                color = if (activeCluster.conflictIntensityPct >= 60) CrimsonAlert else MetallicGold,
                                trackColor = ObsidianVoid,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (activeCluster.conflictingReports.isEmpty()) {
                        item {
                            GlassCard {
                                Text(
                                    "No sharp binary contradictions detected inside this cluster right now. Check Source Compare for subtle framing variance or trigger AI Synthesis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ChampagneSecondary
                                )
                            }
                        }
                    } else {
                        items(activeCluster.conflictingReports, key = { it.id }) { conflict ->
                            ConflictingReportCard(conflict)
                        }
                    }
                }

                NarrativeTab.TIMELINE -> {
                    item {
                        Text(
                            "CHRONOLOGICAL NARRATIVE EVENT TIMELINE (${activeCluster.timelineEvents.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MetallicGoldBright
                        )
                    }
                    items(activeCluster.timelineEvents, key = { it.id }) { event ->
                        NarrativeTimelineRow(event)
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "NARRATIVE_ENGINE") }
    }
}

@Composable
private fun ClusterSelectorCard(
    cluster: NarrativeCluster,
    selected: Boolean,
    onClick: () -> Unit
) {
    val stageColor = when (cluster.lifecycleStage) {
        NarrativeLifecycleStage.ACCELERATING -> LiveEmerald
        NarrativeLifecycleStage.CONTESTED -> CrimsonAlert
        NarrativeLifecycleStage.DECAYING -> SimulatedAmber
        NarrativeLifecycleStage.EMERGING -> IceTelemetry
        NarrativeLifecycleStage.PEAK_CONSENSUS -> MetallicGoldBright
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MetallicGoldContainer else ObsidianElevated,
        border = BorderStroke(1.dp, if (selected) MetallicGold else ObsidianBorder),
        modifier = Modifier
            .width(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("narrative_cluster_${cluster.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cluster.lifecycleStage.label, style = MaterialTheme.typography.labelSmall, color = stageColor, fontWeight = FontWeight.Bold)
                Text("${cluster.articleCount} arts", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            }
            Text(
                text = cluster.topicTitle,
                style = MaterialTheme.typography.titleSmall,
                color = IvoryText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Attn ${cluster.attentionScore}/100", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
                Text(
                    text = "Growth ${String.format(Locale.US, "%+d%%", cluster.growthRatePct)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cluster.growthRatePct >= 0) LiveEmerald else CrimsonAlert
                )
            }
            if (cluster.conflictingReports.isNotEmpty()) {
                Text(
                    text = "${cluster.conflictingReports.size} Conflicting Report(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonAlert
                )
            }
        }
    }
}

@Composable
private fun NarrativeOverviewCard(
    cluster: NarrativeCluster,
    aiBriefState: AiTaskState<com.example.data.remote.NarrativeAiBrief>,
    activeProviderLabel: String,
    onAnalyzeAi: () -> Unit,
    onSelectSymbol: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(borderColor = MetallicGold.copy(alpha = 0.55f)) {
            Text(cluster.categoryTag, style = MaterialTheme.typography.labelSmall, color = MetallicGold)
            Text(cluster.topicTitle, style = MaterialTheme.typography.titleLarge, color = IvoryText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(cluster.summaryThesis, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatTile("ATTENTION", "${cluster.attentionScore}/100", MetallicGoldBright, Modifier.weight(1f))
                TelemetryStatTile("IMPORTANCE", "${cluster.importanceScore}/100", IceTelemetry, Modifier.weight(1f))
                TelemetryStatTile(
                    "ESCALATION",
                    "${cluster.escalationRiskPct}%",
                    if (cluster.escalationRiskPct >= 65) CrimsonAlert else SimulatedAmber,
                    Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("NET SENTIMENT: ${cluster.sentimentLabel}", style = MaterialTheme.typography.labelMedium, color = IvoryText)
            if (cluster.keywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Entities: ${cluster.keywords.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceTelemetry
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAnalyzeAi,
                enabled = aiBriefState !is AiTaskState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("analyze_narrative_ai_button")
            ) {
                if (aiBriefState is AiTaskState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Synthesizing Narrative Arc...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Psychology, contentDescription = "AI Narrative Synthesis", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("AI Synthesize Narrative & Conflicts (${activeProviderLabel.substringAfter("· ").trim()})", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            when (aiBriefState) {
                is AiTaskState.Success -> if (aiBriefState.data.clusterId == cluster.id) {
                    val brief = aiBriefState.data
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianVoid.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("AI NARRATIVE INTELLIGENCE BRIEF", style = MaterialTheme.typography.labelSmall, color = LiveEmerald, fontWeight = FontWeight.Bold)
                                Text(brief.providerUsed, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                            }
                            Text(brief.executiveSummary, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                            Text("Driver: ${brief.dominantDriver}", style = MaterialTheme.typography.bodySmall, color = MetallicGoldBright)
                            Text("Conflict Resolution: ${brief.conflictResolutionThesis}", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                            Text("Cross-Asset Transmission: ${brief.crossAssetTransmission}", style = MaterialTheme.typography.bodySmall, color = IceTelemetry)
                            Text("Attention Forecast: ${brief.attentionForecast}", style = MaterialTheme.typography.labelMedium, color = LiveEmerald)
                        }
                    }
                }
                is AiTaskState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI ERROR: ${aiBriefState.message}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                }
                else -> {}
            }
        }

        // Instrument Relevance Matrix
        GlassCard {
            Text("INSTRUMENT RELEVANCE & BETA TRANSMISSION", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
            Spacer(modifier = Modifier.height(8.dp))
            cluster.instrumentImpacts.forEach { inst ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianVoid.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectSymbol(inst.symbol) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${inst.symbol} · ${inst.displayName}", style = MaterialTheme.typography.titleSmall, color = IvoryText)
                            Text("${inst.assetClass} · ${inst.betaDirection}", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rel ${inst.relevanceScore}/100", style = MaterialTheme.typography.labelMedium, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (inst.isLive) String.format(Locale.US, "%+.2f%% (Vol %.1f%%)", inst.liveChangePct, inst.realizedVolPct) else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!inst.isLive) SlateMuted else if (inst.liveChangePct >= 0) LiveEmerald else CrimsonAlert
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketReactionPanel(
    cluster: NarrativeCluster,
    selectedIdx: Int,
    onSelectReaction: (Int) -> Unit
) {
    val reactions = cluster.marketReactions
    val primary = reactions.getOrNull(selectedIdx) ?: reactions.firstOrNull()
    val secondary = reactions.firstOrNull { it.symbol != primary?.symbol && it.normalizedPriceSeries.isNotEmpty() }

    GlassCard(borderColor = IceTelemetry.copy(alpha = 0.55f)) {
        Text("NARRATIVE ATTENTION VS LIVE MARKET REACTION", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
        Text(
            "Overlays normalized narrative growth trajectory (Gold) against actual hourly price path (Emerald/Crimson) & benchmark (Cyan dashed).",
            style = MaterialTheme.typography.bodySmall,
            color = ChampagneSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reactions.forEachIndexed { idx, rx ->
                    FilterChip(
                        selected = idx == selectedIdx,
                        onClick = { onSelectReaction(idx) },
                        label = {
                            Text("${rx.symbol} ${String.format(Locale.US, "%+.2f%%", rx.priceChangePct)}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = MetallicGoldBright
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (primary != null && primary.normalizedPriceSeries.isNotEmpty()) {
            val priceColor = if (primary.priceChangePct >= 0.0) LiveEmerald else CrimsonAlert
            NarrativeComparisonCanvas(
                primaryCurve = primary.normalizedAttentionSeries,
                secondaryCurve = primary.normalizedPriceSeries,
                tertiaryCurve = secondary?.normalizedPriceSeries.orEmpty(),
                primaryColor = MetallicGoldBright,
                secondaryColor = priceColor,
                tertiaryColor = IceTelemetry
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("● Gold: Attention", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
                Text("● ${primary.symbol} Return", style = MaterialTheme.typography.labelSmall, color = priceColor)
                if (secondary != null) {
                    Text("-- ${secondary.symbol} Compare", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatTile("ALIGNMENT", "${primary.alignmentScorePct}%", MetallicGoldBright, Modifier.weight(1f))
                TelemetryStatTile("RETURN", String.format(Locale.US, "%+.2f%%", primary.priceChangePct), priceColor, Modifier.weight(1f))
                TelemetryStatTile("REALIZED VOL", String.format(Locale.US, "%.1f%%", primary.realizedVolPct), SimulatedAmber, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("REGIME VERDICT: ${primary.regimeVerdict}", style = MaterialTheme.typography.labelMedium, color = IceTelemetry)
        } else {
            Text(
                "Live hourly candles required to overlay attention vs price reaction. Ensure Yahoo Finance / Twelve Data / FMP feed is active.",
                style = MaterialTheme.typography.bodySmall,
                color = SimulatedAmber
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("CROSS-ASSET REACTION COMPARISON", style = MaterialTheme.typography.labelSmall, color = MetallicGoldBright)
        Spacer(modifier = Modifier.height(6.dp))
        reactions.forEach { rx ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${rx.symbol} · ${rx.displayName}", style = MaterialTheme.typography.labelMedium, color = IvoryText)
                    Text("${rx.regimeVerdict} (Align ${rx.alignmentScorePct}%)", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
                }
                Text(
                    text = String.format(Locale.US, "%+.2f%%", rx.priceChangePct),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (rx.priceChangePct >= 0) LiveEmerald else CrimsonAlert,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GrowthAndDecayPanel(
    cluster: NarrativeCluster,
    fastestGrowing: String,
    fastestDecaying: String
) {
    val decay = cluster.decayTelemetry
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
            Text("NARRATIVE GROWTH & 48H DECAY PROJECTION", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
            Text(
                "Solid Gold: Observed Attention Velocity · Dashed Amber: Projected Half-Life Exponential Decay Curve",
                style = MaterialTheme.typography.bodySmall,
                color = ChampagneSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            NarrativeComparisonCanvas(
                primaryCurve = cluster.growthSeries,
                secondaryCurve = decay.projectedDecay48h,
                tertiaryCurve = emptyList(),
                primaryColor = MetallicGoldBright,
                secondaryColor = SimulatedAmber,
                tertiaryColor = IceTelemetry,
                secondaryDashed = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatTile(
                    "GROWTH",
                    String.format(Locale.US, "%+d%%", cluster.growthRatePct),
                    if (cluster.growthRatePct >= 0) LiveEmerald else CrimsonAlert,
                    Modifier.weight(1f)
                )
                TelemetryStatTile(
                    "HALF-LIFE",
                    String.format(Locale.US, "%.1fh", decay.halfLifeHours),
                    IceTelemetry,
                    Modifier.weight(1f)
                )
                TelemetryStatTile(
                    "DECAYED",
                    "${decay.decayProgressPct}%",
                    SimulatedAmber,
                    Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${decay.statusLabel} (${String.format(Locale.US, "%.1fh", decay.hoursSincePeak)} since peak velocity)", style = MaterialTheme.typography.labelMedium, color = IvoryText)
            LinearProgressIndicator(
                progress = { (decay.decayProgressPct / 100f).coerceIn(0f, 1f) },
                color = SimulatedAmber,
                trackColor = ObsidianVoid,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Global Fastest Surging: $fastestGrowing", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
            Text("Global Most Exhausted: $fastestDecaying", style = MaterialTheme.typography.labelSmall, color = SimulatedAmber)
        }
    }
}

@Composable
private fun SourceComparisonCard(src: NewsSourceComparison) {
    val sentColor = when {
        src.avgSentiment <= -0.15 -> CrimsonAlert
        src.avgSentiment >= 0.15 -> LiveEmerald
        else -> MetallicGoldBright
    }
    GlassCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(src.sourceName, style = MaterialTheme.typography.titleSmall, color = IvoryText, fontWeight = FontWeight.Bold)
                Text("${src.category} · ${src.articleCount} articles (${src.shareOfVoicePct}% share)", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(Locale.US, "%+.2f", src.avgSentiment), style = MaterialTheme.typography.titleSmall, color = sentColor, fontWeight = FontWeight.Bold)
                Text(src.framingLabel, style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (src.shareOfVoicePct / 100f).coerceIn(0.05f, 1f) },
            color = sentColor,
            trackColor = ObsidianVoid,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("Lead Wire: ${src.leadHeadline}", style = MaterialTheme.typography.bodySmall, color = IvoryText)
    }
}

@Composable
private fun ConflictingReportCard(conflict: ConflictingReportPair) {
    GlassCard(borderColor = CrimsonAlert.copy(alpha = 0.65f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Conflicting report", tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                Text(conflict.conflictAxis.uppercase(Locale.US), style = MaterialTheme.typography.labelSmall, color = CrimsonAlert, fontWeight = FontWeight.Bold)
            }
            Text("Split ${conflict.divergenceScorePct}%", style = MaterialTheme.typography.labelMedium, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = LiveEmeraldContainer.copy(alpha = 0.55f), border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("CAMP A · ${conflict.sourceA} (${String.format(Locale.US, "%+.2f", conflict.sentimentA)})", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                Text(conflict.headlineA, style = MaterialTheme.typography.bodySmall, color = IvoryText)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = CrimsonContainer.copy(alpha = 0.65f), border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("CAMP B · ${conflict.sourceB} (${String.format(Locale.US, "%+.2f", conflict.sentimentB)})", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert)
                Text(conflict.headlineB, style = MaterialTheme.typography.bodySmall, color = IvoryText)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Trading Implication: ${conflict.implication}", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
    }
}

@Composable
private fun NarrativeTimelineRow(event: NarrativeTimelineEvent) {
    val nodeColor = when (event.markerType) {
        "CATALYST" -> MetallicGoldBright
        "ESCALATION" -> CrimsonAlert
        "DIVERGENCE" -> SimulatedAmber
        "COOLING" -> IceTelemetry
        else -> LiveEmerald
    }
    GlassCard(borderColor = nodeColor.copy(alpha = 0.4f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .background(nodeColor, CircleShape)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${event.markerType} · ${event.timeLabel}", style = MaterialTheme.typography.labelSmall, color = nodeColor, fontWeight = FontWeight.Bold)
                    Text(event.deltaLabel, style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
                }
                Text(event.headline, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                Text("Source: ${event.source}", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            }
        }
    }
}

@Composable
private fun NarrativeComparisonCanvas(
    primaryCurve: List<Double>,
    secondaryCurve: List<Double>,
    tertiaryCurve: List<Double>,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    secondaryDashed: Boolean = false
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp)
            .background(ObsidianVoid.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
                color = ObsidianBorder.copy(alpha = 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun drawSeries(values: List<Double>, color: Color, strokeDp: Float, dashed: Boolean) {
            if (values.size < 2) return
            val path = Path()
            values.forEachIndexed { idx, v ->
                val x = (idx.toFloat() / (values.size - 1)) * size.width
                val y = size.height - ((v.toFloat().coerceIn(0f, 100f) / 100f) * size.height)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeDp.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                )
            )
        }

        drawSeries(tertiaryCurve, tertiaryColor, 1.8f, dashed = true)
        drawSeries(secondaryCurve, secondaryColor, 2.3f, dashed = secondaryDashed)
        drawSeries(primaryCurve, primaryColor, 2.8f, dashed = false)
    }
}
