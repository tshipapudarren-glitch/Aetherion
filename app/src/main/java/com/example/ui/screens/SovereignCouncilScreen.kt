package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VipModuleId
import com.example.ui.components.ActiveSymbolStrip
import com.example.ui.components.GlassCard
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
import com.example.ui.theme.MetallicGoldMuted
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SovereignCouncilScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onSelectMandate: (String) -> Unit,
    onConveneCouncil: (String) -> Unit,
    onBack: () -> Unit
) {
    val riskLocked = state.constitution.emergencyKillSwitch ||
        state.constitution.currentDrawdownPct >= state.constitution.maxDrawdownPct ||
        state.constitution.currentDailyLossPct >= state.constitution.dailyLossLimitPct

    val liveQuote = state.quotes.find { it.instrument.symbol == state.selectedSymbol }
    val swanAlert = state.blackSwanAlerts.find { it.symbol == state.selectedSymbol }
    val councilResult = (state.councilState as? AiTaskState.Success)?.data
    val isDeliberating = state.councilState is AiTaskState.Loading

    var selectedAuditFilter by remember { mutableStateOf(VoteAuditFilter.ALL) }
    var expandedSeat by remember { mutableStateOf<String?>("ARBITER") }
    var expandedArchiveId by remember { mutableStateOf<String?>(null) }

    val mandates = remember {
        listOf("Standard Quorum", "Tail-Risk Audit", "Macro Liquidity", "Strategy Deployment")
    }

    val councilSeats = remember(state.agents, state.councilState, state.constitution, state.selectedSymbol) {
        buildCouncilSeatModels(
            agents = state.agents,
            councilState = state.councilState,
            riskLocked = riskLocked,
            symbol = state.selectedSymbol,
            maxDdPct = state.constitution.maxDrawdownPct,
            currentDdPct = state.constitution.currentDrawdownPct,
            maxExposurePct = state.constitution.maxTotalExposurePct
        )
    }

    val councilAuditLogs = remember(state.auditLogs, selectedAuditFilter) {
        val base = state.auditLogs.filter { it.moduleId == "SOVEREIGN_COUNCIL" }
        when (selectedAuditFilter) {
            VoteAuditFilter.ALL -> base
            VoteAuditFilter.APPROVED -> base.filter { it.outcome == "APPROVED" || it.outcome == "INFO" }
            VoteAuditFilter.VETOED -> base.filter { it.outcome == "VETOED" }
            VoteAuditFilter.ERRORS -> base.filter { it.outcome == "ERROR" }
        }
    }

    val archivedDecisions = remember(state.memories) {
        state.memories.filter {
            it.tier == "DECISION_MEMORY" && it.title.startsWith("Council Vote")
        }.take(6)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeader(VipModuleId.AI_SOVEREIGN_COUNCIL, onBack)
        }

        item {
            ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol)
        }

        // High-Contrast Obsidian & Gold Executive Chamber Banner
        item {
            GlassCard(borderColor = if (riskLocked) CrimsonAlert else MetallicGold.copy(alpha = 0.72f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = "Sovereign Council Chamber",
                            tint = MetallicGoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SOVEREIGN QUORUM CHAMBER",
                            style = MaterialTheme.typography.titleSmall,
                            color = MetallicGoldBright,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (riskLocked) CrimsonContainer else MetallicGoldContainer,
                        border = BorderStroke(1.dp, if (riskLocked) CrimsonAlert else MetallicGoldMuted)
                    ) {
                        Text(
                            text = if (riskLocked) "RISK VETO LOCKED" else "QUORUM ARMED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (riskLocked) CrimsonAlert else MetallicGoldBright,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Provider: ${state.providerSettings.activeProvider.displayName} · " +
                        if (liveQuote?.isLive == true) {
                            "${liveQuote.instrument.symbol} ${String.format(Locale.US, "%.4f (%+.2f%%)", liveQuote.price, liveQuote.changePct)}"
                        } else {
                            "${state.selectedSymbol} Feed Disconnected"
                        } +
                        (swanAlert?.let { " · Tail Risk ${it.tailRiskScore}/100" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))
                val stanceColor = when (councilResult?.consensusStance) {
                    "BULLISH" -> LiveEmerald
                    "BEARISH", "HALT" -> CrimsonAlert
                    "NEUTRAL" -> MetallicGoldBright
                    else -> ChampagneSecondary
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryStatTile(
                        label = "CONSENSUS VERDICT",
                        value = when (state.councilState) {
                            is AiTaskState.Loading -> "DELIBERATING"
                            is AiTaskState.Error -> "FAULT"
                            is AiTaskState.Success -> councilResult?.consensusStance ?: "NEUTRAL"
                            is AiTaskState.Idle -> "STANDBY"
                        },
                        accent = stanceColor,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatTile(
                        label = "CONVICTION",
                        value = councilResult?.let { "${it.confidencePct}%" } ?: "—",
                        accent = if ((councilResult?.confidencePct ?: 0) >= 65) MetallicGoldBright else ChampagneSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatTile(
                        label = "RISK GUARD",
                        value = if (riskLocked) "VETO ON" else "CLEAR",
                        accent = if (riskLocked) CrimsonAlert else LiveEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                ConsensusAlignmentBar(
                    confidencePct = councilResult?.confidencePct ?: 0,
                    stance = councilResult?.consensusStance ?: "IDLE",
                    riskLocked = riskLocked
                )
            }
        }

        // Active Decision-Making Processes & Quorum Mandate Trigger
        item {
            GlassCard(borderColor = ObsidianBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE DECISION-MAKING PROCESS",
                        style = MaterialTheme.typography.titleSmall,
                        color = MetallicGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isDeliberating) "LIVE DEBATE" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDeliberating) SimulatedAmber else LiveEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.councilActivePhase,
                    style = MaterialTheme.typography.bodySmall,
                    color = IvoryText
                )

                Spacer(modifier = Modifier.height(8.dp))
                val activeStep = when (state.councilState) {
                    is AiTaskState.Idle -> 0
                    is AiTaskState.Loading -> 2
                    is AiTaskState.Success -> 3
                    is AiTaskState.Error -> 2
                }
                DecisionPipelineStepper(activeStep = activeStep, isLoading = isDeliberating, isError = state.councilState is AiTaskState.Error)

                Spacer(modifier = Modifier.height(10.dp))
                Text("SESSION MANDATE FOCUS", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mandates.forEach { mandate ->
                        FilterChip(
                            selected = state.councilFocusMandate == mandate,
                            onClick = { onSelectMandate(mandate) },
                            label = { Text(mandate, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onConveneCouncil(state.selectedSymbol) },
                    enabled = !isDeliberating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MetallicGold,
                        contentColor = ObsidianVoid,
                        disabledContainerColor = MetallicGoldMuted.copy(alpha = 0.45f),
                        disabledContentColor = IvoryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("convene_council_button")
                ) {
                    if (isDeliberating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convening Council Quorum...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Gavel, contentDescription = "Convene Council Vote", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convene Council Vote on ${state.selectedSymbol}", fontWeight = FontWeight.Bold)
                    }
                }

                if (state.councilState is AiTaskState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonContainer,
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DELIBERATION ERROR: ${state.councilState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonAlert,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (councilResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, MetallicGoldMuted.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RECOMMENDED ARCHETYPE", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                Text(councilResult.recommendedStrategy, style = MaterialTheme.typography.labelLarge, color = MetallicGoldBright)
                            }
                            Text(councilResult.providerUsed, style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
                        }
                    }
                }
            }
        }

        // Current Agent Consensus Statuses (Parliament Chamber Roster)
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text(
                    text = "AGENT CONSENSUS STATUSES · PARLIAMENT SEATS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MetallicGoldBright,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any executive seat to inspect full thesis, adversarial objection, or deterministic gate status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                councilSeats.forEach { seat ->
                    val isExpanded = expandedSeat == seat.agentName
                    CouncilSeatCard(
                        seat = seat,
                        isExpanded = isExpanded,
                        onToggle = { expandedSeat = if (isExpanded) null else seat.agentName }
                    )
                }
            }
        }

        // Dedicated 'Council Vote' Audit Log + Decision Memory Archive
        item {
            GlassCard(
                borderColor = MetallicGold.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("council_vote_audit_log")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COUNCIL VOTE AUDIT LOG",
                        style = MaterialTheme.typography.titleSmall,
                        color = MetallicGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${councilAuditLogs.size} records",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoteAuditFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedAuditFilter == filter,
                            onClick = { selectedAuditFilter = filter },
                            label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (councilAuditLogs.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianVoid.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Council Vote records match this filter yet. Tap 'Convene Council Vote' above to log an immutable Room & Firestore ruling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    val fmt = remember { SimpleDateFormat("MMM dd HH:mm:ss", Locale.US) }
                    councilAuditLogs.take(12).forEach { log ->
                        CouncilAuditVoteRow(log = log, timestampText = fmt.format(Date(log.createdAtMs)))
                    }
                }

                if (archivedDecisions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ARCHIVED DEBATE TRANSCRIPTS (${archivedDecisions.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceTelemetry,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    archivedDecisions.forEach { mem ->
                        val isOpen = expandedArchiveId == mem.id
                        ArchivedDecisionCard(
                            memory = mem,
                            isExpanded = isOpen,
                            onToggle = { expandedArchiveId = if (isOpen) null else mem.id }
                        )
                    }
                }
            }
        }
    }
}
