package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.VipModuleId
import com.example.domain.engine.EventDomainCategory
import com.example.domain.engine.SetupArchitectEngine
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.TelemetryStatTile
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
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

@Composable
fun GlobalEventImpactScreen(
    state: AetherDashboardState,
    onRefreshHeadlines: () -> Unit,
    onAnalyzeHeadline: (String) -> Unit,
    onSelectSymbolAndOpenChart: (String) -> Unit,
    onOpenNarratives: () -> Unit = {},
    onOpenTradingTerminal: () -> Unit = {},
    onBack: () -> Unit
) {
    var customHeadline by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<EventDomainCategory?>(null) }

    val intel = state.eventIntelligenceSnapshot
    val mc = state.marketConditions
    val filteredEvents = remember(intel.categorizedEvents, selectedCategory) {
        if (selectedCategory == null) intel.categorizedEvents
        else intel.categorizedEvents.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.GLOBAL_EVENT_IMPACT_ENGINE, onBack) }

        // 1. Central Bank & Macro Backdrop + Positioning Regime Summary
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.6f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.AccountBalance, contentDescription = "Central Bank & Event Intel", tint = MetallicGoldBright, modifier = Modifier.size(18.dp))
                        Text("EVENT IMPACT INTELLIGENCE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
                    }
                    Text(mc.overallRegime, style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(intel.positioningRiskSummary, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryStatTile("CENTRAL BANKS", "${intel.centralBankCount} events", MetallicGoldBright, Modifier.weight(1f))
                    TelemetryStatTile("ECONOMIC NEWS", "${intel.economicNewsCount} prints", IceTelemetry, Modifier.weight(1f))
                    TelemetryStatTile("GEOPOLITICAL", "${intel.geopoliticalCount} alerts", CrimsonAlert, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenTradingTerminal,
                        border = BorderStroke(1.dp, MetallicGold),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "TradingView Setup Terminal", tint = MetallicGoldBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Setup Terminal", color = MetallicGoldBright, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onOpenNarratives,
                        border = BorderStroke(1.dp, IceTelemetry),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("module13_open_narratives_card")
                    ) {
                        Icon(Icons.Filled.Public, contentDescription = "Narratives", tint = IceTelemetry, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Narratives (${state.narrativeSnapshot.clusters.size})", color = IceTelemetry, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 2. Market Positioning Alongside Events Matrix
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.45f)) {
                Text("MARKET POSITIONING ALONGSIDE EVENTS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
                Text(
                    text = "Cross-asset RSI(14) stretch, MA50 divergence, and active account exposure highlighting crowded squeeze risks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (intel.positioningRows.isEmpty()) {
                    Text("Sync live market feeds to evaluate positioning sensitivity.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                } else {
                    intel.positioningRows.take(6).forEach { row ->
                        val riskColor = when {
                            row.eventVulnerabilityScore >= 70 -> CrimsonAlert
                            row.eventVulnerabilityScore >= 52 -> SimulatedAmber
                            else -> LiveEmerald
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianVoid.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${row.symbol} · ${row.postureLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IvoryText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Price ${SetupArchitectEngine.formatPrice(row.price)} (${String.format(Locale.US, "%+.2f%%", row.changePct)}) · MA50 Δ ${String.format(Locale.US, "%+.1f%%", row.stretchVsMa50Pct)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                                Text(
                                    text = "Sens ${row.eventVulnerabilityScore}/100",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = riskColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. AI Macro Shock Impact Assessor
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("AI MACRO SHOCK & POSITIONING ASSESSOR", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customHeadline,
                    onValueChange = { customHeadline = it },
                    label = { Text("Enter Central Bank, Economic Print, or Geopolitical Shock") },
                    placeholder = { Text("e.g. Surprise 50bps FOMC cut or Strait energy shock") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_event_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAnalyzeHeadline(customHeadline.ifBlank { state.headlines.firstOrNull()?.title ?: "Global central bank liquidity shift" }) },
                        enabled = state.eventImpactState !is AiTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("analyze_event_button")
                    ) {
                        if (state.eventImpactState is AiTaskState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                        } else {
                            Text("Assess Event + Positioning", fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = onRefreshHeadlines,
                        modifier = Modifier.size(48.dp).testTag("refresh_headlines_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh live events", tint = MetallicGoldBright)
                    }
                }

                when (val s = state.eventImpactState) {
                    is AiTaskState.Success -> {
                        val a = s.data
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianVoid.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, if (a.severityScore >= 70) CrimsonAlert else MetallicGold)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SEVERITY SCORE: ${a.severityScore}/100", style = MaterialTheme.typography.labelLarge, color = if (a.severityScore >= 70) CrimsonAlert else MetallicGoldBright)
                                    Text(a.providerUsed, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                }
                                Text("Event: ${a.headline}", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                                Text("Affected Assets: ${a.affectedClasses.joinToString(", ")}", style = MaterialTheme.typography.labelMedium, color = IceTelemetry)
                                Text("Directional Bias: ${a.directionalBias}", style = MaterialTheme.typography.bodySmall, color = IvoryText)
                                Text("Risk Action: ${a.riskMitigationAction}", style = MaterialTheme.typography.bodySmall, color = LiveEmerald)
                                OutlinedButton(
                                    onClick = onOpenTradingTerminal,
                                    border = BorderStroke(1.dp, MetallicGold),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Architect Setup on ${state.selectedSymbol} Chart →", color = MetallicGoldBright)
                                }
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ANALYSIS ERROR: ${s.message}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
                }
            }
        }

        if (!state.headlineError.isNullOrBlank()) {
            item {
                GlassCard(borderColor = CrimsonAlert.copy(alpha = 0.6f)) {
                    Text("LIVE EVENT FEED WARNING: ${state.headlineError}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                }
            }
        }

        // 4. Categorized Economic News, Central Banks & Geopolitical Stream
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("CATEGORIZED CATALYSTS (${filteredEvents.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${intel.categorizedEvents.size})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MetallicGoldContainer, selectedLabelColor = MetallicGoldBright)
                    )
                    EventDomainCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MetallicGoldContainer, selectedLabelColor = MetallicGoldBright)
                        )
                    }
                }
            }
        }

        items(filteredEvents.take(18), key = { it.id }) { ev ->
            val catColor = when (ev.category) {
                EventDomainCategory.CENTRAL_BANKS -> MetallicGoldBright
                EventDomainCategory.ECONOMIC_NEWS -> IceTelemetry
                EventDomainCategory.GEOPOLITICAL -> CrimsonAlert
            }
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = catColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f))
                    ) {
                        Text(ev.category.label.uppercase(Locale.US), style = MaterialTheme.typography.labelSmall, color = catColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("Shock ${ev.shockPotentialScore}/100 · ${ev.publishedLabel}", style = MaterialTheme.typography.labelSmall, color = SimulatedAmber)
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(ev.title, style = MaterialTheme.typography.titleSmall, color = IvoryText)
                Spacer(modifier = Modifier.height(3.dp))
                Text("Positioning Read: ${ev.positioningInteraction}", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val primarySym = ev.affectedInstruments.firstOrNull() ?: state.selectedSymbol
                    OutlinedButton(
                        onClick = { onSelectSymbolAndOpenChart(primarySym) },
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Setup $primarySym →", color = MetallicGoldBright, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { onAnalyzeHeadline(ev.title) },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = IceTelemetry),
                        border = BorderStroke(1.dp, IceTelemetry.copy(alpha = 0.55f)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("AI Assess", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "EVENT_ENGINE") }
    }
}
