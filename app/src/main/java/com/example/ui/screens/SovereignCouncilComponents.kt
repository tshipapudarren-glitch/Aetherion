package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalAuditLogEntity
import com.example.data.local.LocalMemoryEntity
import com.example.data.model.AgentProfile
import com.example.data.remote.CouncilVoteResult
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.IceTelemetry
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldContainer
import com.example.ui.theme.MetallicGoldMuted
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SimulatedAmber
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AiTaskState
import java.util.Locale

internal enum class VoteAuditFilter(val label: String) {
    ALL("All Votes"),
    APPROVED("Approved"),
    VETOED("Vetoed"),
    ERRORS("Errors")
}

internal data class CouncilSeatUi(
    val agentName: String,
    val seatTitle: String,
    val icon: ImageVector,
    val stanceTag: String,
    val stanceColor: Color,
    val argumentBody: String,
    val accuracyPct: Double,
    val providerLabel: String
)

@Composable
internal fun CouncilSeatCard(
    seat: CouncilSeatUi,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) MetallicGoldContainer.copy(alpha = 0.34f) else ObsidianVoid.copy(alpha = 0.82f),
        border = BorderStroke(
            1.dp,
            if (isExpanded) seat.stanceColor.copy(alpha = 0.72f) else ObsidianBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .testTag("council_seat_${seat.agentName.lowercase(Locale.US)}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(seat.stanceColor.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = seat.icon,
                            contentDescription = seat.agentName,
                            tint = seat.stanceColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${seat.agentName} · ${seat.seatTitle}",
                            style = MaterialTheme.typography.labelLarge,
                            color = IvoryText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${seat.providerLabel} · Acc ${String.format(Locale.US, "%.1f%%", seat.accuracyPct)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = seat.stanceColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, seat.stanceColor.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = seat.stanceTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = seat.stanceColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle seat details",
                        tint = ChampagneSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = seat.argumentBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun ConsensusAlignmentBar(
    confidencePct: Int,
    stance: String,
    riskLocked: Boolean
) {
    val barColor = when {
        riskLocked || stance == "HALT" || stance == "BEARISH" -> CrimsonAlert
        stance == "BULLISH" -> LiveEmerald
        stance == "NEUTRAL" -> MetallicGoldBright
        else -> SlateMuted
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("QUORUM CONVICTION METER", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            Text(
                text = if (stance == "IDLE") "0% · Awaiting Vote" else "$confidencePct% · $stance",
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            drawRoundRect(
                color = ObsidianVoid,
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                size = size
            )
            val fillFrac = (confidencePct / 100f).coerceIn(0f, 1f)
            if (fillFrac > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(MetallicGoldMuted, barColor)),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    size = Size(size.width * fillFrac, size.height)
                )
            }
            val thresholdX = size.width * 0.65f
            drawLine(
                color = MetallicGoldBright,
                start = Offset(thresholdX, 0f),
                end = Offset(thresholdX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
internal fun DecisionPipelineStepper(
    activeStep: Int,
    isLoading: Boolean,
    isError: Boolean
) {
    val steps = listOf("1. Telemetry & Risk", "2. AI Quorum Debate", "3. Vote & Audit Lock")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isLoading) {
            LinearProgressIndicator(
                color = MetallicGoldBright,
                trackColor = ObsidianElevated,
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEachIndexed { idx, label ->
                val stepNum = idx + 1
                val done = activeStep >= stepNum && (!isLoading || stepNum < activeStep) && !isError
                val active = isLoading && activeStep == stepNum
                val accent = when {
                    isError && stepNum == activeStep -> CrimsonAlert
                    done -> LiveEmerald
                    active -> MetallicGoldBright
                    else -> SlateMuted
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = if (done || active) 0.14f else 0.05f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = if (done || active) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CouncilAuditVoteRow(log: LocalAuditLogEntity, timestampText: String) {
    val badgeColor = when (log.outcome) {
        "APPROVED", "EXECUTED" -> LiveEmerald
        "VETOED", "ERROR" -> CrimsonAlert
        "SIMULATED" -> SimulatedAmber
        else -> MetallicGoldBright
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianVoid.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = log.outcome,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(timestampText, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            }
            Text(
                text = log.action,
                style = MaterialTheme.typography.labelMedium,
                color = IvoryText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${log.actor} · ${log.details}",
                style = MaterialTheme.typography.bodySmall,
                color = ChampagneSecondary
            )
        }
    }
}

@Composable
internal fun ArchivedDecisionCard(
    memory: LocalMemoryEntity,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianElevated,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MetallicGoldBright,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(memory.confidenceScore * 100).toInt()}% conf",
                    style = MaterialTheme.typography.labelSmall,
                    color = LiveEmerald
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

internal fun buildCouncilSeatModels(
    agents: List<AgentProfile>,
    councilState: AiTaskState<CouncilVoteResult>,
    riskLocked: Boolean,
    symbol: String,
    maxDdPct: Double,
    currentDdPct: Double,
    maxExposurePct: Double
): List<CouncilSeatUi> {
    fun agentMeta(name: String, fallbackAcc: Double, fallbackProv: String): Pair<Double, String> {
        val found = agents.find { it.name == name }
        return (found?.accuracyPct ?: fallbackAcc) to (found?.assignedProvider ?: fallbackProv)
    }

    val vote = (councilState as? AiTaskState.Success)?.data
    val loading = councilState is AiTaskState.Loading

    val (oracleAcc, oracleProv) = agentMeta("ORACLE", 79.6, "Gemini 4 Argon")
    val (criticAcc, criticProv) = agentMeta("CRITIC", 86.9, "Gemini 3.8 Flash")
    val (macroAcc, macroProv) = agentMeta("MACRO INTEL", 84.7, "Gemini 4 Argon")
    val (arbiterAcc, arbiterProv) = agentMeta("ARBITER", 90.1, "Gemini 3.8 Flash")
    val (guardAcc, guardProv) = agentMeta("RISK GUARD", 100.0, "Deterministic")

    val oracleStanceColor = when {
        loading -> SimulatedAmber
        vote?.consensusStance == "BULLISH" -> LiveEmerald
        vote?.consensusStance == "BEARISH" -> CrimsonAlert
        vote != null -> MetallicGoldBright
        else -> ChampagneSecondary
    }

    return listOf(
        CouncilSeatUi(
            agentName = "ORACLE",
            seatTitle = "Directional Forecasting",
            icon = Icons.Filled.Bolt,
            stanceTag = when {
                loading -> "FORECASTING"
                vote != null -> vote.consensusStance
                else -> "STANDBY"
            },
            stanceColor = oracleStanceColor,
            argumentBody = vote?.oracleThesis
                ?: "Ready to generate directional price hypothesis and volatility targets for $symbol.",
            accuracyPct = oracleAcc,
            providerLabel = oracleProv
        ),
        CouncilSeatUi(
            agentName = "CRITIC",
            seatTitle = "Adversarial Risk Challenge",
            icon = Icons.Filled.Warning,
            stanceTag = when {
                loading -> "AUDITING"
                vote != null -> "DISSENT FILED"
                else -> "ARMED"
            },
            stanceColor = if (vote != null) CrimsonAlert else SimulatedAmber,
            argumentBody = vote?.criticObjection
                ?: "Audits ORACLE hypotheses for crowded positioning, spread spikes, and invalidation traps.",
            accuracyPct = criticAcc,
            providerLabel = criticProv
        ),
        CouncilSeatUi(
            agentName = "MACRO INTEL",
            seatTitle = "Liquidity & Central Banks",
            icon = Icons.Filled.Psychology,
            stanceTag = when {
                loading -> "SYNTHESIZING"
                vote != null -> "MACRO BRIEF"
                else -> "MONITORING"
            },
            stanceColor = IceTelemetry,
            argumentBody = vote?.macroIntelBrief
                ?: "Evaluates G10 yield differentials, sovereign liquidity flows, and headline macro catalysts.",
            accuracyPct = macroAcc,
            providerLabel = macroProv
        ),
        CouncilSeatUi(
            agentName = "ARBITER",
            seatTitle = "Quorum Resolution Chair",
            icon = Icons.Filled.Gavel,
            stanceTag = when {
                loading -> "RESOLVING"
                vote != null -> "${vote.confidencePct}% BINDING"
                else -> "CHAIR READY"
            },
            stanceColor = if (vote != null) MetallicGoldBright else ChampagneSecondary,
            argumentBody = vote?.arbiterRuling
                ?: "Reconciles ORACLE, CRITIC, and MACRO INTEL submissions into an auditable quorum vote.",
            accuracyPct = arbiterAcc,
            providerLabel = arbiterProv
        ),
        CouncilSeatUi(
            agentName = "RISK GUARD",
            seatTitle = "Deterministic Veto Authority",
            icon = if (riskLocked) Icons.Filled.Shield else Icons.Filled.CheckCircle,
            stanceTag = if (riskLocked) "VETO ACTIVE" else "CLEARED",
            stanceColor = if (riskLocked) CrimsonAlert else LiveEmerald,
            argumentBody = if (riskLocked) {
                "DETERMINISTIC HALT ENFORCED: Drawdown ${String.format(Locale.US, "%.1f%%", currentDdPct)} / ${String.format(Locale.US, "%.1f%%", maxDdPct)} limit or Kill Switch engaged."
            } else {
                "Account guardrails clear: Max DD ${String.format(Locale.US, "%.1f%%", maxDdPct)}, Max Exposure ${String.format(Locale.US, "%.1f%%", maxExposurePct)}. Cannot be bypassed by AI."
            },
            accuracyPct = guardAcc,
            providerLabel = guardProv
        )
    )
}
