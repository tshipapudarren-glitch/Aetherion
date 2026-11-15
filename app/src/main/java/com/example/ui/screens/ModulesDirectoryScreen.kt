package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.IntelligenceLayer
import com.example.data.model.VipModuleId
import com.example.ui.components.EnvironmentBadge
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.IceTelemetry
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.util.Locale

@Composable
fun ModulesDirectoryScreen(
    state: AetherDashboardState,
    onOpenNarratives: () -> Unit = {},
    onOpenTradingTerminal: () -> Unit = {},
    onOpenModule: (VipModuleId) -> Unit
) {
    var selectedLayer by remember { mutableStateOf<IntelligenceLayer?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredModules = remember(selectedLayer, searchQuery) {
        VipModuleId.entries.filter { mod ->
            val layerOk = selectedLayer == null || mod.layer == selectedLayer
            val queryOk = searchQuery.isBlank() ||
                mod.title.contains(searchQuery, ignoreCase = true) ||
                mod.subtitle.contains(searchQuery, ignoreCase = true) ||
                mod.leadAgent.contains(searchQuery, ignoreCase = true)
            layerOk && queryOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AETHERION ARCHITECTURE · 8 LAYERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGold
                )
                Text(
                    text = "15 VIP Intelligence Modules",
                    style = MaterialTheme.typography.headlineSmall,
                    color = IvoryText
                )
                Text(
                    text = "Select any dedicated VIP capability screen below. Status badges distinguish Live APIs, Hybrid Backtests, Simulated Sandboxes, and Config-Required Bridges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
            }
        }

        item {
            val nar = state.narrativeSnapshot
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MetallicGoldContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenNarratives() }
                    .testTag("directory_narrative_intelligence_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "OMNISENSE EXPANSION · NARRATIVE & ATTENTION ENGINE →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${nar.clusters.size} Topic Clusters · ${nar.totalSourcesCount} Sources · ${nar.totalConflictsDetected} Conflicts · Attention ${nar.globalAttentionIndex}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryText
                    )
                }
            }
        }

        item {
            val setup = state.activeSetupPlan
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianElevated,
                border = BorderStroke(1.dp, IceTelemetry.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenTradingTerminal() }
                    .testTag("directory_trading_terminal_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "TRADINGVIEW TERMINAL & SETUP ARCHITECT →",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceTelemetry,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (setup != null) {
                            "${setup.symbol} ${setup.direction} · Auto-Drawn Entry / SL / TP1 / TP2 / TP3 (1:${String.format(Locale.US, "%.1f", setup.riskRewardTp3)}R)"
                        } else {
                            "TradingView charts, 2×2 Multi-Chart workspace & auto-drawn Entry/SL/TP1–TP3 setups."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryText
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search 15 Modules or Agents") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("modules_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLayer == null,
                    onClick = { selectedLayer = null },
                    label = { Text("All 15 Modules") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGoldContainer,
                        selectedLabelColor = MetallicGoldBright
                    )
                )
                IntelligenceLayer.entries.forEach { layer ->
                    FilterChip(
                        selected = selectedLayer == layer,
                        onClick = { selectedLayer = if (selectedLayer == layer) null else layer },
                        label = { Text("${layer.code} ${layer.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = MetallicGoldBright
                        )
                    )
                }
            }
        }

        items(filteredModules, key = { it.number }) { module ->
            val liveSummary = when (module) {
                VipModuleId.MULTIVERSE_SIMULATOR -> state.multiverseReport?.let {
                    "VaR95: ${String.format(Locale.US, "%.2f%%", it.var95Pct)} · Survival: ${String.format(Locale.US, "%.1f%%", it.survivalRatePct)}"
                } ?: "350 timelines ready"
                VipModuleId.AI_SOVEREIGN_COUNCIL -> when (val c = state.councilState) {
                    is AiTaskState.Success -> "Latest Verdict: ${c.data.consensusStance} (${c.data.confidencePct}%)"
                    else -> "Active AI: ${state.providerSettings.activeProvider.displayName}"
                }
                VipModuleId.STRATEGY_DNA_LAB -> "${state.strategies.size} DNA genomes cataloged"
                VipModuleId.GLOBAL_CAPITAL_FLOW_RADAR -> if (state.capitalFlowSnapshot.assetFlows.isNotEmpty()) {
                    "Inflow: ${state.capitalFlowSnapshot.dominantInflow} · Flow ${state.capitalFlowSnapshot.netLiquidityScore}/100"
                } else {
                    "${state.currencyStrengths.size} FX/Gold flow indices active"
                }
                VipModuleId.BLACK_SWAN_PREDICTOR -> state.blackSwanAlerts.firstOrNull()?.let {
                    "Peak Tail: ${it.symbol} (${it.tailRiskScore}/100)"
                } ?: "Awaiting candles"
                VipModuleId.STRATEGY_CHAMPIONSHIP_ARENA -> "${state.strategies.count { it.stage == "PAPER_TRADING" || it.stage == "LIVE_ELIGIBLE" }} strategies graduated"
                VipModuleId.AUTONOMOUS_BROKER_INTELLIGENCE -> state.brokerHealth?.statusLabel
                    ?: "${state.paperOrders.count { it.status == "OPEN" }} Open Paper · MT5 Config"
                VipModuleId.MARKET_TIME_RECONSTRUCTION -> "Replay Step ${state.timeReplayIndex} on ${state.selectedSymbol}"
                VipModuleId.QUANTUM_CAPITAL_ALLOCATOR -> "${state.allocations.size} risk-parity budgets computed"
                VipModuleId.COLLECTIVE_INTELLIGENCE_MEMORY -> "${state.memories.size} Room+Firestore memories stored"
                VipModuleId.COMMANDER_VOICE_INTELLIGENCE -> "${state.voiceHistory.size} voice/command transcripts"
                VipModuleId.AUTONOMOUS_RISK_DEFENCE_MATRIX -> if (state.constitution.emergencyKillSwitch)
                    "EMERGENCY KILL SWITCH ENGAGED"
                else "MaxDD ${state.constitution.maxDrawdownPct}% · Permission L${state.constitution.permissionLevel}"
                VipModuleId.GLOBAL_EVENT_IMPACT_ENGINE -> {
                    val ei = state.eventIntelligenceSnapshot
                    "${ei.centralBankCount} CB · ${ei.economicNewsCount} Econ · ${ei.geopoliticalCount} Geo · ${ei.positioningRows.size} Pos"
                }
                VipModuleId.AUTONOMOUS_STRATEGY_FACTORY -> state.factoryStatusMessage
                VipModuleId.AETHERION_OVERMIND -> "Cycle ${if (state.autonomousCycle.isRunning) "RUNNING" else "STANDBY"} · ${state.agents.size} Agents"
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenModule(module) }
                    .testTag("directory_module_${module.number}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VIP-${module.number.toString().padStart(2, '0')} · LAYER ${module.layer.code} ${module.layer.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetallicGold
                        )
                        EnvironmentBadge(module.environment)
                    }
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = module.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lead: ${module.leadAgent} · $liveSummary",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceTelemetry,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
