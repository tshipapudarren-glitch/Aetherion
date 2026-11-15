package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.PaperOrderEntity
import com.example.domain.engine.SetupArchitectEngine
import com.example.ui.components.ActiveSymbolStrip
import com.example.ui.components.ChartScreenshotVisionCard
import com.example.ui.components.GlassCard
import com.example.ui.components.InstitutionalAccountAndSentimentHud
import com.example.ui.components.Integrated15SetupScannerCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.SyntheticSpreadDepthCard
import com.example.ui.components.TelemetryStatTile
import com.example.ui.components.TerminalOpenPositionsCard
import com.example.ui.components.TradingViewTerminalCard
import com.example.ui.components.TwoWayExecutionTicketCard
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.IceTelemetry
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.util.Locale

@Composable
fun TradingTerminalScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onSelectTimeframe: (String) -> Unit,
    onSelectDirectionBias: (String?) -> Unit,
    onRefineSetupWithAi: () -> Unit,
    onExecuteSetupOrder: (routeToMt5Bridge: Boolean) -> Unit,
    onExecuteTerminalTicket: (side: String, exposurePct: Double, customSl: Double?, customTp: Double?, routeToMt5: Boolean) -> Unit = { _, _, _, _, _ -> },
    onClosePaperOrder: (PaperOrderEntity) -> Unit = {},
    onPickChartScreenshot: (Uri) -> Unit = {},
    onOpenEventEngine: () -> Unit,
    onBack: () -> Unit
) {
    val selectedQuote = state.quotes.find { it.instrument.symbol == state.selectedSymbol }
        ?: state.quotes.firstOrNull { it.isLive }
    val setup = state.activeSetupPlan

    var ticketSl by remember(state.selectedSymbol) {
        mutableStateOf(setup?.let { SetupArchitectEngine.formatPrice(it.stopLoss) }.orEmpty())
    }
    var ticketTp by remember(state.selectedSymbol) {
        mutableStateOf(setup?.let { SetupArchitectEngine.formatPrice(it.tp2) }.orEmpty())
    }
    LaunchedEffect(setup?.stopLoss, setup?.tp2, setup?.isAiRefined) {
        if (setup != null && (ticketSl.isBlank() || setup.isAiRefined)) {
            ticketSl = SetupArchitectEngine.formatPrice(setup.stopLoss)
            ticketTp = SetupArchitectEngine.formatPrice(setup.tp2)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("terminal_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MetallicGold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("AETHERION MACRO TERMINAL & SETUP LAB", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                    Text("TradingView · Setup Architect · 15 Setups · Vision", style = MaterialTheme.typography.titleLarge, color = IvoryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Chart Terminal", tint = MetallicGoldBright)
            }
        }

        item {
            InstitutionalAccountAndSentimentHud(
                structure = state.marketStructureSnapshot,
                conditions = state.marketConditions
            )
        }

        item {
            ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol)
        }

        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.6f)) {
                TradingViewTerminalCard(
                    selectedQuote = selectedQuote,
                    allQuotes = state.quotes,
                    setupPlan = setup,
                    timeframe = state.selectedChartTimeframe,
                    onSelectTimeframe = onSelectTimeframe,
                    onSelectSymbol = onSelectSymbol,
                    chartHeightDp = 300
                )
            }
        }

        // Two-Way Institutional Order Ticket (SELL Bid / BUY Ask)
        item {
            TwoWayExecutionTicketCard(
                quote = selectedQuote,
                structure = state.marketStructureSnapshot,
                permissionLevel = state.constitution.permissionLevel,
                slInput = ticketSl,
                tpInput = ticketTp,
                onSlChange = { ticketSl = it },
                onTpChange = { ticketTp = it },
                onSubmitOrder = onExecuteTerminalTicket
            )
        }

        // Setup Architect Control & AI Refinement Deck
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SETUP ARCHITECT ENGINE", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
                    Text(setup?.providerLabel ?: "Awaiting Feed", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Auto Bias", "LONG" to "Force Long", "SHORT" to "Force Short").forEach { (bias, label) ->
                        FilterChip(
                            selected = state.setupDirectionOverride == bias,
                            onClick = { onSelectDirectionBias(bias) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            )
                        )
                    }
                }

                if (setup != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryStatTile("DIRECTION", setup.direction, if (setup.direction == "LONG") LiveEmerald else CrimsonAlert, Modifier.weight(1f))
                        TelemetryStatTile("ATR(14)", SetupArchitectEngine.formatPrice(setup.atr14), MetallicGoldBright, Modifier.weight(1f))
                        TelemetryStatTile("MAX R:R", "1:${String.format(Locale.US, "%.1f", setup.riskRewardTp3)}", LiveEmerald, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianVoid.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("POSITIONING: ${setup.positioningLabel}", style = MaterialTheme.typography.labelMedium, color = IceTelemetry)
                            Text("MACRO & EVENTS: ${setup.macroAlignmentNote}", style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                            Text("STRUCTURE: ${setup.structuralRationale}", style = MaterialTheme.typography.bodySmall, color = IvoryText)
                            Text(
                                text = "RISK GUARD: ${setup.riskGuardNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (setup.riskGuardApproved) LiveEmerald else CrimsonAlert
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRefineSetupWithAi,
                        enabled = state.setupAiRefineState !is AiTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("refine_setup_ai_button")
                    ) {
                        if (state.setupAiRefineState is AiTaskState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refining Entry / SL / TP1–TP3...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Refine Setup", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Refine Entry, SL, TP1, TP2 & TP3", fontWeight = FontWeight.Bold)
                        }
                    }

                    when (val aiRes = state.setupAiRefineState) {
                        is AiTaskState.Success -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(aiRes.data, style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                        }
                        is AiTaskState.Error -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("AI REFINE ERROR: ${aiRes.message}", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert)
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onExecuteSetupOrder(false) },
                            border = BorderStroke(1.dp, LiveEmerald),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("execute_setup_paper_button")
                        ) {
                            Text("Paper Trade Setup", color = LiveEmerald, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { onExecuteSetupOrder(true) },
                            border = BorderStroke(1.dp, if (state.constitution.permissionLevel >= 4) MetallicGold else SlateMuted),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("execute_setup_mt5_button")
                        ) {
                            Text("Send to MT5", color = MetallicGoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenEventEngine,
                        border = BorderStroke(1.dp, IceTelemetry.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = "Event Impact Intelligence", tint = IceTelemetry, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inspect Event Impact & Market Positioning →", color = IceTelemetry)
                    }
                }
            }
        }

        // Photo Picker Multimodal Chart Vision
        item {
            ChartScreenshotVisionCard(
                visionState = state.chartVisionState,
                onPickScreenshot = onPickChartScreenshot
            )
        }

        // 15 Integrated Setup Detection Systems
        item {
            Integrated15SetupScannerCard(
                structure = state.marketStructureSnapshot,
                onApplySetupToTicket = { detected ->
                    ticketSl = SetupArchitectEngine.formatPrice(detected.stopLoss)
                    ticketTp = SetupArchitectEngine.formatPrice(detected.takeProfit)
                }
            )
        }

        // Derived ATR Order Book Spread Ladder
        item {
            SyntheticSpreadDepthCard(structure = state.marketStructureSnapshot)
        }

        // Open Positions & Recent Closed Trades
        item {
            TerminalOpenPositionsCard(
                orders = state.paperOrders,
                onCloseOrder = onClosePaperOrder
            )
        }

        item { ModuleAuditLogCard(state.auditLogs, "SETUP_ARCHITECT") }
    }
}
