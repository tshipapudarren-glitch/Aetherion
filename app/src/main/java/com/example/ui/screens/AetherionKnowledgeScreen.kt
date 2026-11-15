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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalMemoryEntity
import com.example.data.model.MemoryTier
import com.example.data.model.VipModuleId
import com.example.domain.engine.KnowledgeDomainCategory
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
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherDashboardState
import com.example.ui.viewmodel.AiTaskState

@Composable
fun AetherionKnowledgeScreen(
    state: AetherDashboardState,
    onSearchChanged: (String) -> Unit,
    onSelectTierFilter: (MemoryTier?) -> Unit,
    onSaveKnowledge: (MemoryTier, String, String, String) -> Unit,
    onDistillKnowledgeWithAi: (String) -> Unit,
    onQueryMemoryAi: (String) -> Unit,
    onBoostConfidence: (LocalMemoryEntity) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onOpenArenaAi: () -> Unit,
    onBack: () -> Unit
) {
    var queryOrTopic by remember { mutableStateOf("Distill strongest risk-adjusted edge & drawdown guard for current regime") }
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }
    var selectedNewTier by remember { mutableStateOf(MemoryTier.STRATEGY_MEMORY) }

    val snap = state.knowledgeArenaSnapshot
    val filteredMemories = remember(state.memories, state.selectedKnowledgeTier, state.knowledgeSearchQuery) {
        state.memories.filter { mem ->
            val tierOk = state.selectedKnowledgeTier == null || mem.tier == state.selectedKnowledgeTier.name
            val q = state.knowledgeSearchQuery.trim()
            val qOk = q.isBlank() ||
                mem.title.contains(q, ignoreCase = true) ||
                mem.content.contains(q, ignoreCase = true) ||
                mem.symbol.contains(q, ignoreCase = true) ||
                mem.agentName.contains(q, ignoreCase = true)
            tierOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.COLLECTIVE_INTELLIGENCE_MEMORY, onBack) }

        // Connected Arena AI Bridge Banner
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.65f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AETHERION KNOWLEDGE ↔ ARENA AI BRIDGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetallicGoldBright,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${snap.linkedMemoryIds.size} entries actively buffing ${snap.contenderRankings.size} Arena AI contenders across ${snap.linkedArenaSymbolsCount} market scopes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampagneSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Hub,
                        contentDescription = "Arena AI Knowledge Link",
                        tint = LiveEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryStatTile("ENTRIES", "${snap.totalKnowledgeEntries}", MetallicGoldBright, Modifier.weight(1f))
                    TelemetryStatTile("ARENA LINKED", "${snap.linkedMemoryIds.size}", LiveEmerald, Modifier.weight(1f))
                    TelemetryStatTile("AVG CONF", "${snap.avgConfidencePct}%", IceTelemetry, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenArenaAi,
                    border = BorderStroke(1.dp, MetallicGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("knowledge_open_arena_ai_button")
                ) {
                    Text(
                        text = "Open Connected Arena AI Tournament →",
                        color = MetallicGoldBright,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // AI Knowledge Distiller & Synthesis Panel
        item {
            GlassCard(borderColor = IceTelemetry.copy(alpha = 0.45f)) {
                Text(
                    text = "AI KNOWLEDGE DISTILLER & MATRIX SYNTHESIS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MetallicGoldBright
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = queryOrTopic,
                    onValueChange = { queryOrTopic = it },
                    label = { Text("Distillation Topic or Matrix Investigation Prompt") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDistillKnowledgeWithAi(queryOrTopic) },
                        enabled = state.knowledgeDistillState !is AiTaskState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("distill_knowledge_ai_button")
                    ) {
                        if (state.knowledgeDistillState is AiTaskState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Distill Playbook", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = { onQueryMemoryAi(queryOrTopic) },
                        enabled = state.memorySynthesisState !is AiTaskState.Loading,
                        border = BorderStroke(1.dp, IceTelemetry.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("query_memory_matrix_button")
                    ) {
                        Text(
                            text = if (state.memorySynthesisState is AiTaskState.Loading) "Synthesizing..." else "Query Matrix",
                            color = IceTelemetry,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                when (val d = state.knowledgeDistillState) {
                    is AiTaskState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(d.data, style = MaterialTheme.typography.bodySmall, color = LiveEmerald)
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(d.message, style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
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

        // Manual Institutional Playbook Composer
        item {
            GlassCard {
                Text("AUTHOR INSTITUTIONAL KNOWLEDGE ENTRY", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MemoryTier.entries.forEach { tier ->
                        FilterChip(
                            selected = selectedNewTier == tier,
                            onClick = { selectedNewTier = tier },
                            label = { Text(tier.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Playbook Rule / Lesson Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    label = { Text("Execution Condition, Regime Filter or Heuristic (${state.selectedSymbol})") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank() && noteBody.isNotBlank()) {
                            onSaveKnowledge(selectedNewTier, noteTitle.trim(), noteBody.trim(), state.selectedSymbol)
                            noteTitle = ""
                            noteBody = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("commit_knowledge_button")
                ) {
                    Text("Commit to Room + Firestore & Sync Arena AI")
                }
            }
        }

        // Search & Filter Knowledge Vault
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "KNOWLEDGE VAULT (${filteredMemories.size}/${state.memories.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MetallicGoldBright
                )
                OutlinedTextField(
                    value = state.knowledgeSearchQuery,
                    onValueChange = onSearchChanged,
                    label = { Text("Search Playbooks, Symbols or Agents") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.selectedKnowledgeTier == null,
                        onClick = { onSelectTierFilter(null) },
                        label = { Text("All (${state.memories.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MetallicGoldContainer,
                            selectedLabelColor = MetallicGoldBright
                        )
                    )
                    MemoryTier.entries.forEach { tier ->
                        val count = snap.categoryCounts[tier] ?: 0
                        FilterChip(
                            selected = state.selectedKnowledgeTier == tier,
                            onClick = { onSelectTierFilter(if (state.selectedKnowledgeTier == tier) null else tier) },
                            label = { Text("${tier.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetallicGoldContainer,
                                selectedLabelColor = MetallicGoldBright
                            )
                        )
                    }
                }
            }
        }

        if (filteredMemories.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "No AETHERION Knowledge entries match the current filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted
                    )
                }
            }
        } else {
            items(filteredMemories, key = { it.id }) { mem ->
                val domainCat = KnowledgeDomainCategory.fromTierName(mem.tier)
                val isArenaLinked = snap.linkedMemoryIds.contains(mem.id)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isArenaLinked) MetallicGold.copy(alpha = 0.65f) else ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${domainCat.badge} · ${mem.symbol} · ${(mem.confidenceScore * 100).toInt()}% CONF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MetallicGold
                            )
                            if (isArenaLinked) {
                                Text(
                                    text = "ARENA AI LINKED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LiveEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(mem.title, style = MaterialTheme.typography.titleSmall, color = IvoryText, fontWeight = FontWeight.SemiBold)
                        Text(mem.content, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Source: ${mem.agentName}", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                            Row {
                                IconButton(
                                    onClick = { onBoostConfidence(mem) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ThumbUp,
                                        contentDescription = "Reinforce knowledge confidence",
                                        tint = LiveEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteEntry(mem.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Archive knowledge entry",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { ModuleAuditLogCard(state.auditLogs, "AETHERION_KNOWLEDGE") }
    }
}
