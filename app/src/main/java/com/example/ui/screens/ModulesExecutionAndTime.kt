package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalStrategyEntity
import com.example.data.local.PaperOrderEntity
import com.example.data.model.MemoryTier
import com.example.data.model.StrategyStage
import com.example.data.model.VipModuleId
import com.example.ui.components.CandleChartCanvas
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.TelemetryStatTile
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StrategyArenaScreen(
    state: AetherDashboardState,
    onPromoteStrategy: (LocalStrategyEntity, StrategyStage) -> Unit,
    onExecutePaper: (LocalStrategyEntity, String, Double) -> Unit,
    onBack: () -> Unit
) {
    val ranked = remember(state.strategies) {
        state.strategies.sortedByDescending {
            it.sharpeRatio * 2.0 + it.profitFactor - (it.maxDrawdownPct * 0.25)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.STRATEGY_CHAMPIONSHIP_ARENA, onBack) }
        ranked.forEachIndexed { idx, strat ->
            item(key = strat.id) {
                val compositeScore = (strat.sharpeRatio * 20.0 + strat.profitFactor * 15.0 - strat.maxDrawdownPct * 2.0).coerceIn(0.0, 100.0)
                GlassCard(borderColor = if (idx == 0) MetallicGold else ObsidianBorder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("#${idx + 1} ${strat.name}", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                        Text("${String.format(Locale.US, "%.0f", compositeScore)} PTS", style = MaterialTheme.typography.labelLarge, color = LiveEmerald)
                    }
                    Text(
                        "${strat.symbol} · Stage: ${strat.stage} · Sharpe ${String.format(Locale.US, "%.2f", strat.sharpeRatio)} · DD ${String.format(Locale.US, "%.1f%%", strat.maxDrawdownPct)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onExecutePaper(strat, "BUY", 2.5) },
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Paper Trade 2.5%", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                val next = if (strat.stage == StrategyStage.PAPER_TRADING.name) StrategyStage.LIVE_ELIGIBLE else StrategyStage.PAPER_TRADING
                                onPromoteStrategy(strat, next)
                            },
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Advance Gate", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "CHAMPIONSHIP_ARENA") }
    }
}

@Composable
fun BrokerIntelligenceScreen(
    state: AetherDashboardState,
    onPingBridge: () -> Unit,
    onExecuteOrder: (LocalStrategyEntity, String, Double, Boolean) -> Unit,
    onClosePaperOrder: (PaperOrderEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val primaryStrategy = state.strategies.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.AUTONOMOUS_BROKER_INTELLIGENCE, onBack) }
        item {
            val health = state.brokerHealth
            GlassCard(
                borderColor = if (health?.connected == true) LiveEmerald else CrimsonAlert.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("METATRADER 5 EA BRIDGE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                    Text(
                        text = health?.statusLabel ?: if (state.providerSettings.mt5BridgeUrl.isBlank()) "DISCONNECTED" else "READY TO PING",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (health?.connected == true) LiveEmerald else CrimsonAlert
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = health?.errorDetails ?: if (state.providerSettings.mt5BridgeUrl.isBlank())
                        "No live MT5 bridge URL configured. Paper Trading Sandbox remains active for simulation."
                    else "Endpoint: ${state.providerSettings.mt5BridgeUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPingBridge,
                        enabled = !state.isPingingBroker,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("ping_mt5_button")
                    ) {
                        if (state.isPingingBroker) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                        } else {
                            Text("Ping MT5 Bridge")
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Configure URL")
                    }
                }
            }
        }

        val riskCheck = state.lastRiskEvaluation
        if (riskCheck != null) {
            item {
                GlassCard(borderColor = if (riskCheck.approved) LiveEmerald else CrimsonAlert) {
                    Text(
                        text = riskCheck.reason,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (riskCheck.approved) LiveEmerald else CrimsonAlert
                    )
                    riskCheck.violatedRules.forEach { rule ->
                        Text("• $rule", style = MaterialTheme.typography.bodySmall, color = IvoryText)
                    }
                }
            }
        }

        if (primaryStrategy != null) {
            item {
                GlassCard {
                    Text("RISK-GATED EXECUTION ROUTER (${primaryStrategy.symbol})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onExecuteOrder(primaryStrategy, "BUY", 2.0, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = SimulatedAmber, contentColor = ObsidianVoid),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Paper BUY 2%")
                        }
                        Button(
                            onClick = { onExecuteOrder(primaryStrategy, "BUY", 2.0, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonContainer, contentColor = IvoryText),
                            border = BorderStroke(1.dp, CrimsonAlert),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Route MT5 Live")
                        }
                    }
                }
            }
        }

        item {
            Text("PAPER SANDBOX ORDERS (${state.paperOrders.size})", style = MaterialTheme.typography.titleSmall, color = SimulatedAmber)
        }
        state.paperOrders.forEach { order ->
            item(key = order.id) {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${order.side} ${order.symbol} · ${order.status}", style = MaterialTheme.typography.titleSmall, color = IvoryText)
                            Text(
                                "Entry ${String.format(Locale.US, "%.4f", order.entryPrice)} · Exp ${order.exposurePct}% · PnL ${String.format(Locale.US, "%+.2f%%", order.pnlPct)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (order.pnlPct >= 0) LiveEmerald else CrimsonAlert
                            )
                        }
                        if (order.status == "OPEN") {
                            OutlinedButton(onClick = { onClosePaperOrder(order) }) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "EXECUTION") }
    }
}

@Composable
fun MarketTimeReconstructionScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onSeekReplay: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onBack: () -> Unit
) {
    val quote = state.quotes.find { it.instrument.symbol == state.selectedSymbol }
        ?: state.quotes.firstOrNull { it.isLive }
    val candles = quote?.candles?.takeLast(60).orEmpty()
    val safeIndex = state.timeReplayIndex.coerceIn(0, (candles.size - 1).coerceAtLeast(0))
    val activeBar = candles.getOrNull(safeIndex)
    val fmt = remember { SimpleDateFormat("MMM dd HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.MARKET_TIME_RECONSTRUCTION, onBack) }
        item { com.example.ui.components.ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol) }
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text(
                    "HISTORICAL BAR RECONSTRUCTION · ${quote?.instrument?.symbol ?: state.selectedSymbol}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGoldBright
                )
                Spacer(modifier = Modifier.height(8.dp))
                CandleChartCanvas(candles = candles, highlightIndex = safeIndex)
                if (candles.size > 1) {
                    Slider(
                        value = safeIndex.toFloat(),
                        onValueChange = { onSeekReplay(it.toInt()) },
                        valueRange = 0f..(candles.lastIndex.toFloat()),
                        colors = SliderDefaults.colors(thumbColor = MetallicGoldBright, activeTrackColor = MetallicGold)
                    )
                    Button(
                        onClick = onTogglePlayback,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isTimeReplaying) "Pause Forensic Replay" else "Play Historical Replay")
                    }
                }
                if (activeBar != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryStatTile("TIMESTAMP", fmt.format(Date(activeBar.timestampMs)), IvoryText, Modifier.weight(1f))
                        TelemetryStatTile("OPEN", String.format(Locale.US, "%.3f", activeBar.open), ChampagneSecondary, Modifier.weight(1f))
                        TelemetryStatTile("CLOSE", String.format(Locale.US, "%.3f", activeBar.close), MetallicGoldBright, Modifier.weight(1f))
                    }
                }
            }
        }
        val btTrades = state.activeBacktest?.trades.orEmpty()
        if (btTrades.isNotEmpty()) {
            item {
                Text("FORENSIC TRADE AUTOPSY ARCHIVE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
            }
            btTrades.take(8).forEach { tr ->
                item {
                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${tr.side} @ ${String.format(Locale.US, "%.3f", tr.entryPrice)}", style = MaterialTheme.typography.titleSmall, color = IvoryText)
                            Text(
                                String.format(Locale.US, "%+.2f%%", tr.pnlPct),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (tr.pnlPct >= 0) LiveEmerald else CrimsonAlert
                            )
                        }
                        Text(
                            "MFE: +${String.format(Locale.US, "%.2f%%", tr.mfePct)} · MAE: ${String.format(Locale.US, "%.2f%%", tr.maePct)} · Exit: ${String.format(Locale.US, "%.3f", tr.exitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuantumAllocatorScreen(
    state: AetherDashboardState,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.QUANTUM_CAPITAL_ALLOCATOR, onBack) }
        item {
            GlassCard {
                Text(
                    "RISK-PARITY BUDGET (CAPPED AT ${state.constitution.maxTotalExposurePct}% MAX EXPOSURE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGoldBright
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.allocations.isEmpty()) {
                    Text("Awaiting live market volatilities to compute inverse-vol allocation.", color = SlateMuted)
                } else {
                    state.allocations.forEach { slice ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${slice.symbol} (${slice.label})", style = MaterialTheme.typography.titleSmall, color = IvoryText)
                                Text(
                                    "Weight ${String.format(Locale.US, "%.1f%%", slice.weightPct)} · Cap ${String.format(Locale.US, "%.2f%%", slice.riskBudgetPct)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MetallicGoldBright
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (slice.weightPct / 100.0).toFloat().coerceIn(0f, 1f) },
                                color = MetallicGold,
                                trackColor = ObsidianVoid,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Realized Vol: ${String.format(Locale.US, "%.1f%%", slice.volatilityPct)} · Half-Kelly Cap: ${String.format(Locale.US, "%.2f%%", slice.kellyFractionPct)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneSecondary
                            )
                        }
                    }
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "RISK_MATRIX") }
    }
}

@Composable
fun CollectiveMemoryScreen(
    state: AetherDashboardState,
    onSaveMemory: (MemoryTier, String, String, String) -> Unit,
    onQueryMemoryAi: (String) -> Unit,
    onBack: () -> Unit
) {
    var queryText by remember { mutableStateOf("Which regimes or strategies showed the strongest edge?") }
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.COLLECTIVE_INTELLIGENCE_MEMORY, onBack) }
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("INVESTIGATE INSTITUTIONAL MEMORY", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Ask MEMORY MATRIX") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onQueryMemoryAi(queryText) },
                    enabled = state.memorySynthesisState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.memorySynthesisState is AiTaskState.Loading) "Synthesizing..." else "Query Memory Matrix")
                }
                when (val s = state.memorySynthesisState) {
                    is AiTaskState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(s.data, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(s.message, style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
                }
            }
        }

        item {
            GlassCard {
                Text("RECORD NEW MEMORY ENTRY", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Memory Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    label = { Text("Observation / Regime Finding") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank() && noteBody.isNotBlank()) {
                            onSaveMemory(MemoryTier.INTELLIGENCE_MEMORY, noteTitle, noteBody, state.selectedSymbol)
                            noteTitle = ""
                            noteBody = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Commit to Room + Firestore")
                }
            }
        }

        state.memories.forEach { mem ->
            item(key = mem.id) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianVoid.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${mem.tier} · ${mem.symbol} · ${mem.agentName}", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                        Text(mem.title, style = MaterialTheme.typography.titleSmall, color = IvoryText)
                        Text(mem.content, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                    }
                }
            }
        }
    }
}
