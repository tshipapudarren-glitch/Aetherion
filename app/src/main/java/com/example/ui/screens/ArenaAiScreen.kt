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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalStrategyEntity
import com.example.data.model.StrategyStage
import com.example.data.model.VipModuleId
import com.example.data.remote.ArenaAiVerdict
import com.example.ui.components.ActiveSymbolStrip
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

@Composable
fun StrategyArenaScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onPromoteStrategy: (LocalStrategyEntity, StrategyStage) -> Unit,
    onExecutePaper: (LocalStrategyEntity, String, Double) -> Unit,
    onRunArenaAi: () -> Unit,
    onSaveArenaLessonToKnowledge: (ArenaAiVerdict) -> Unit,
    onOpenKnowledge: () -> Unit,
    onBack: () -> Unit
) {
    val snap = state.knowledgeArenaSnapshot
    val ranked = snap.contenderRankings
    val matchup = snap.featuredMatchup

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.STRATEGY_CHAMPIONSHIP_ARENA, onBack) }
        item { ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol) }

        // Connected AETHERION Knowledge Context Banner
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONNECTED AETHERION KNOWLEDGE · ${state.selectedSymbol}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceTelemetry,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${snap.activeSymbolCitations.size} active playbooks (${snap.totalKnowledgeEntries} total in Vault) injecting relevance & confidence buffs into Arena AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Hub,
                        contentDescription = "Connected Knowledge Base",
                        tint = MetallicGoldBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (snap.activeSymbolCitations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snap.activeSymbolCitations.forEach { cit ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MetallicGoldContainer.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = "${cit.categoryBadge} · ${cit.relevanceScorePct}% MATCH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MetallicGoldBright
                                    )
                                    Text(
                                        text = cit.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IvoryText
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenKnowledge,
                    border = BorderStroke(1.dp, IceTelemetry.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("arena_open_knowledge_button")
                ) {
                    Text(
                        text = "Inspect & Distill AETHERION Knowledge →",
                        color = IceTelemetry,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Featured Head-to-Head Matchup Card
        if (matchup != null) {
            item {
                GlassCard(borderColor = MetallicGold.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HEAD-TO-HEAD TOURNAMENT · ${matchup.symbol}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetallicGoldBright
                        )
                        Text(
                            text = "+${String.format(Locale.US, "%.1f", matchup.winMarginPts)} PTS LEAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiveEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${matchup.challengerAName} VS ${matchup.challengerBName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = IvoryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Leader: ${matchup.winnerName} (${String.format(Locale.US, "%.1f", matchup.winnerScorePts)} pts) · Regime: ${matchup.regimeContext}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneSecondary
                    )
                }
            }
        }

        // Arena AI Adjudicator Card (Knowledge-Informed Jury)
        item {
            GlassCard(borderColor = MetallicGoldBright.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARENA AI ADJUDICATOR (KNOWLEDGE-INFORMED)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MetallicGoldBright
                    )
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Arena AI",
                        tint = MetallicGoldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Evaluates contender backtests alongside AETHERION Knowledge playbooks, emits tactical parameter refinements, and feeds post-match lessons back into the Knowledge Vault.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRunArenaAi,
                    enabled = state.arenaAiState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_arena_ai_button")
                ) {
                    if (state.arenaAiState is AiTaskState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Run Arena AI Tournament (Knowledge-Linked)")
                    }
                }

                when (val verdictState = state.arenaAiState) {
                    is AiTaskState.Success -> {
                        val v = verdictState.data
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianElevated,
                            border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CROWNED: ${v.championStrategyName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LiveEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${v.confidencePct}% CONF",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MetallicGoldBright
                                    )
                                }
                                Text(v.matchupSummary, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                                if (v.knowledgeCitationsUsed.isNotEmpty()) {
                                    Text(
                                        text = "Cited Knowledge: ${v.knowledgeCitationsUsed.joinToString(" • ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IceTelemetry
                                    )
                                }
                                Text(
                                    text = "Tactical Adjustment: ${v.tacticalAdjustment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MetallicGoldBright
                                )
                                Text(
                                    text = "Risk Guard: ${v.riskGuardCompliance} · Engine: ${v.providerUsed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneSecondary
                                )
                                Text(
                                    text = "Post-Match Lesson: ${v.postMatchLessonTitle} — ${v.postMatchLessonBody}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SimulatedAmber
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { onSaveArenaLessonToKnowledge(v) },
                                    border = BorderStroke(1.dp, LiveEmerald),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("save_arena_lesson_button")
                                ) {
                                    Icon(Icons.Filled.BookmarkAdded, contentDescription = null, tint = LiveEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Commit Post-Match Lesson to AETHERION Knowledge", color = LiveEmerald)
                                }
                            }
                        }
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(verdictState.message, style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
                }
            }
        }

        // Ranked Contender Leaderboard
        items(ranked, key = { it.strategy.id }) { item ->
            val strat = item.strategy
            GlassCard(borderColor = if (item.rank == 1) MetallicGold else ObsidianBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${item.rank} ${strat.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MetallicGoldBright,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", item.compositeScore)} PTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = LiveEmerald
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (item.compositeScore / 100.0).toFloat().coerceIn(0f, 1f) },
                    color = if (item.rank == 1) MetallicGoldBright else IceTelemetry,
                    trackColor = ObsidianVoid,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryStatTile("QUANT BASE", String.format(Locale.US, "%.1f", item.baseQuantScore), IvoryText, Modifier.weight(1f))
                    TelemetryStatTile("KNOWLEDGE BUFF", "+${String.format(Locale.US, "%.1f", item.knowledgeBuffPts)}", IceTelemetry, Modifier.weight(1f))
                    TelemetryStatTile("STAGE", strat.stage, ChampagneSecondary, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${strat.symbol} · ${item.headToHeadEdgeSummary} · DD ${String.format(Locale.US, "%.1f%%", strat.maxDrawdownPct)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                if (item.matchedKnowledge.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Applied Knowledge: ${item.matchedKnowledge.joinToString(" | ") { "${it.title} (${it.relevanceScorePct}%)" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceTelemetry
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onExecutePaper(strat, "BUY", 2.5) },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Paper Trade 2.5%", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            val next = if (strat.stage == StrategyStage.PAPER_TRADING.name) StrategyStage.LIVE_ELIGIBLE else StrategyStage.PAPER_TRADING
                            onPromoteStrategy(strat, next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Advance Gate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "CHAMPIONSHIP_ARENA") }
    }
}
