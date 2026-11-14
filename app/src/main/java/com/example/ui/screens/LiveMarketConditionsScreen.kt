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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.core.security.ApiSecretsResolver
import com.example.data.model.EnvironmentMode
import com.example.ui.components.EnvironmentBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.TelemetryStatTile
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.IceTelemetry
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
fun LiveMarketConditionsScreen(
    state: AetherDashboardState,
    onRefreshConditions: () -> Unit,
    onAnalyzeConditionsWithAi: () -> Unit,
    onOpenIntegrations: () -> Unit
) {
    val mc = state.marketConditions
    val settings = state.providerSettings
    val activeSources = state.quotes.filter { it.isLive }.map { it.sourceName }.distinct()
    val hasFred = ApiSecretsResolver.resolveFredKey(settings).isNotBlank()
    val hasFmp = ApiSecretsResolver.resolveFmpKey(settings).isNotBlank()
    val hasTd = ApiSecretsResolver.resolveTwelveDataKey(settings).isNotBlank()
    val hasFinnhub = ApiSecretsResolver.resolveFinnhubKey(settings).isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OMNISENSE · MACRO TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MetallicGold
                        )
                        EnvironmentBadge(EnvironmentMode.LIVE)
                    }
                    Text(
                        text = "Live Market Conditions",
                        style = MaterialTheme.typography.headlineSmall,
                        color = IvoryText
                    )
                    Text(
                        text = mc.activeTradingSessions,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneSecondary
                    )
                }
                IconButton(
                    onClick = onRefreshConditions,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_conditions_button")
                ) {
                    if (state.isRefreshingConditions) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MetallicGoldBright, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Market Conditions", tint = MetallicGoldBright)
                    }
                }
            }
        }

        // Composite Regime & Risk Appetite Banner
        item {
            val regimeAccent = when {
                mc.riskAppetiteScore >= 64 -> LiveEmerald
                mc.riskAppetiteScore <= 38 -> CrimsonAlert
                else -> MetallicGoldBright
            }
            GlassCard(borderColor = regimeAccent.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OVERALL MARKET REGIME", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        Text(mc.overallRegime, style = MaterialTheme.typography.titleMedium, color = regimeAccent, fontWeight = FontWeight.Bold)
                    }
                    TelemetryStatTile("RISK INDEX", "${mc.riskAppetiteScore}/100", regimeAccent)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (mc.riskAppetiteScore / 100f).coerceIn(0f, 1f) },
                    color = regimeAccent,
                    trackColor = ObsidianVoid,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryStatTile("VOLATILITY", mc.volatilityRegime.substringBefore(" ("), IceTelemetry, Modifier.weight(1f))
                    TelemetryStatTile("YIELD CURVE", mc.yieldCurveStatus.substringBefore(" ("), MetallicGoldBright, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryStatTile("ADV / DEC", "${mc.advancersCount} / ${mc.declinersCount}", LiveEmerald, Modifier.weight(1f))
                    TelemetryStatTile(
                        "AVG REALIZED VOL",
                        String.format(Locale.US, "%.1f%%", mc.averageRealizedVolPct),
                        SimulatedAmber,
                        Modifier.weight(1f)
                    )
                }
                if (mc.updatedAtMs > 0L) {
                    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Last telemetry sync: ${fmt.format(Date(mc.updatedAtMs))} · Sources: ${activeSources.joinToString(", ").ifBlank { "Yahoo / Proxy" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }
            }
        }

        // Connected Institutional Data Feed Matrix
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DATA PROVIDER CONNECTION STATUS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                    OutlinedButton(onClick = onOpenIntegrations, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configure Keys", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Keys", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelemetryStatTile("FRED", if (hasFred) "KEY ACTIVE" else "PROXY/KEY REQ", if (hasFred) LiveEmerald else SimulatedAmber, Modifier.weight(1f))
                    TelemetryStatTile("FMP", if (hasFmp) "KEY ACTIVE" else "OPTIONAL", if (hasFmp) LiveEmerald else SlateMuted, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TelemetryStatTile("12DATA", if (hasTd) "KEY ACTIVE" else "OPTIONAL", if (hasTd) LiveEmerald else SlateMuted, Modifier.weight(1f))
                    TelemetryStatTile("FINNHUB", if (hasFinnhub) "KEY ACTIVE" else "OPTIONAL", if (hasFinnhub) LiveEmerald else SlateMuted, Modifier.weight(1f))
                }
            }
        }

        // FRED & Proxy Macro Indicators
        item {
            Text("MACRO & VOLATILITY INDICATORS (${mc.indicators.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
        }
        mc.indicators.forEach { ind ->
            item(key = ind.seriesId) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianVoid.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, if (ind.isLive) ObsidianBorder else SimulatedAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ind.title, style = MaterialTheme.typography.titleSmall, color = IvoryText)
                            Text(
                                "${ind.seriesId} · ${ind.sourceName} (${ind.observationDate})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ind.isLive) ChampagneSecondary else SimulatedAmber
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                ind.valueText,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (ind.isLive) MetallicGoldBright else SimulatedAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                ind.deltaText,
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }
        }

        // AI Macro Regime Synthesis
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoGraph, contentDescription = "AI Regime Synthesis", tint = MetallicGoldBright)
                    Text("AI MACRO CONDITIONS SYNTHESIS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Active Provider: ${settings.activeProvider.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAnalyzeConditionsWithAi,
                    enabled = state.conditionsAiBriefState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("analyze_conditions_ai_button")
                ) {
                    if (state.conditionsAiBriefState is AiTaskState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                    } else {
                        Text("Generate Macro Regime Briefing", fontWeight = FontWeight.Bold)
                    }
                }
                when (val brief = state.conditionsAiBriefState) {
                    is AiTaskState.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(brief.data, style = MaterialTheme.typography.bodySmall, color = IvoryText)
                    }
                    is AiTaskState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI ERROR: ${brief.message}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
                    }
                    else -> {}
                }
            }
        }

        // Economic Calendar Catalysts (FMP / Finnhub)
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Public, contentDescription = "Economic Calendar", tint = IceTelemetry)
                    Text("UPCOMING ECONOMIC CATALYSTS (${mc.economicEvents.size})", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (mc.economicEvents.isEmpty()) {
                    Text(
                        text = if (hasFmp || hasFinnhub)
                            "No scheduled high/medium events returned for the upcoming 72h window."
                        else
                            "Configure FMP_API_KEY or FINNHUB_API_KEY in Secrets or Integrations Settings to stream live economic calendar releases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                } else {
                    mc.economicEvents.forEach { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${ev.country}] ${ev.eventName}", style = MaterialTheme.typography.labelMedium, color = IvoryText)
                                Text(
                                    "${ev.timeUtc} · Est ${ev.estimate} · Prev ${ev.previous} (${ev.sourceName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneSecondary
                                )
                            }
                            Text(
                                ev.impact.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ev.impact.equals("high", ignoreCase = true)) CrimsonAlert else SimulatedAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
