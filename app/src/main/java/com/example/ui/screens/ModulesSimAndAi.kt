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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalStrategyEntity
import com.example.data.model.VipModuleId
import com.example.ui.components.EquityCurveCanvas
import com.example.ui.components.GlassCard
import com.example.ui.components.ModuleAuditLogCard
import com.example.ui.components.ModuleHeader
import com.example.ui.components.MultiverseFanCanvas
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
import java.util.Locale

@Composable
fun MultiverseSimulatorScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onSelectRegime: (String) -> Unit,
    onBack: () -> Unit
) {
    val regimes = listOf("Base Live Drift", "Bull Expansion", "Liquidity Crunch", "Stagflation Shock", "Flash Crash Tail")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.MULTIVERSE_SIMULATOR, onBack) }
        item { com.example.ui.components.ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol) }
        item {
            GlassCard(borderColor = SimulatedAmber.copy(alpha = 0.5f)) {
                Text(
                    text = "SIMULATED TIMELINES · SEEDED FROM LIVE ${state.selectedSymbol} VOLATILITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SimulatedAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    regimes.forEach { regime ->
                        FilterChip(
                            selected = state.selectedMultiverseRegime == regime,
                            onClick = { onSelectRegime(regime) },
                            label = { Text(regime) }
                        )
                    }
                }
            }
        }
        val report = state.multiverseReport
        if (report != null) {
            item {
                GlassCard {
                    Text(
                        text = "${report.pathCount} Jump-Diffusion Paths · ${report.horizonBars}H Horizon",
                        style = MaterialTheme.typography.titleSmall,
                        color = MetallicGoldBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MultiverseFanCanvas(report)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryStatTile("95% VaR", String.format(Locale.US, "%.2f%%", report.var95Pct), CrimsonAlert, Modifier.weight(1f))
                        TelemetryStatTile("95% CVaR", String.format(Locale.US, "%.2f%%", report.cvar95Pct), CrimsonAlert, Modifier.weight(1f))
                        TelemetryStatTile("SURVIVAL (<8% DD)", String.format(Locale.US, "%.1f%%", report.survivalRatePct), LiveEmerald, Modifier.weight(1f))
                    }
                }
            }
        } else {
            item {
                GlassCard {
                    Text("Awaiting live quote to seed Monte Carlo paths.", color = SlateMuted)
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "MULTIVERSE") }
    }
}

@Composable
fun SovereignCouncilScreen(
    state: AetherDashboardState,
    onSelectSymbol: (String) -> Unit,
    onConveneCouncil: (String) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.AI_SOVEREIGN_COUNCIL, onBack) }
        item { com.example.ui.components.ActiveSymbolStrip(state.quotes, state.selectedSymbol, onSelectSymbol) }
        item {
            GlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                Text(
                    text = "AI TRADING PARLIAMENT · ACTIVE PROVIDER: ${state.providerSettings.activeProvider.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGoldBright
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ORACLE forecasts, CRITIC challenges, MACRO INTEL assesses liquidity, and ARBITER resolves. RISK GUARD enforces deterministic veto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onConveneCouncil(state.selectedSymbol) },
                    enabled = state.councilState !is AiTaskState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("convene_council_button")
                ) {
                    if (state.councilState is AiTaskState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianVoid, strokeWidth = 2.dp)
                    } else {
                        Text("Convene Council on ${state.selectedSymbol}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            when (val res = state.councilState) {
                is AiTaskState.Idle -> GlassCard {
                    Text("Tap above to convene multi-agent debate on ${state.selectedSymbol}.", color = SlateMuted)
                }
                is AiTaskState.Loading -> GlassCard {
                    Text("ORACLE, CRITIC, MACRO INTEL, and ARBITER deliberating...", color = MetallicGoldBright)
                }
                is AiTaskState.Error -> GlassCard(borderColor = CrimsonAlert) {
                    Text("COUNCIL ERROR: ${res.message}", color = CrimsonAlert)
                }
                is AiTaskState.Success -> {
                    val v = res.data
                    GlassCard(borderColor = MetallicGold) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("VERDICT: ${v.consensusStance}", style = MaterialTheme.typography.titleMedium, color = MetallicGoldBright)
                            Text("${v.confidencePct}% CONFIDENCE", style = MaterialTheme.typography.labelLarge, color = LiveEmerald)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CouncilSpeechBlock("ORACLE (Forecasting)", v.oracleThesis, MetallicGoldBright)
                        CouncilSpeechBlock("CRITIC (Dissent & Risk)", v.criticObjection, CrimsonAlert)
                        CouncilSpeechBlock("MACRO INTEL", v.macroIntelBrief, ChampagneSecondary)
                        CouncilSpeechBlock("ARBITER (Resolution)", v.arbiterRuling, LiveEmerald)
                        Text(
                            text = "Matched Strategy Archetype: ${v.recommendedStrategy} · Provider: ${v.providerUsed}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "SOVEREIGN_COUNCIL") }
    }
}

@Composable
private fun CouncilSpeechBlock(speaker: String, text: String, accent: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianVoid.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(speaker, style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = IvoryText)
        }
    }
}

@Composable
fun StrategyDnaLabScreen(
    state: AetherDashboardState,
    onMutateStrategy: (LocalStrategyEntity) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.STRATEGY_DNA_LAB, onBack) }
        val bt = state.activeBacktest
        if (bt != null) {
            item {
                GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                    Text(
                        text = "LIVE CANDLE BACKTEST · ${bt.symbol} · EMA(${bt.fastPeriod}/${bt.slowPeriod})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGoldBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EquityCurveCanvas(bt.equityCurve)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryStatTile("SHARPE", String.format(Locale.US, "%.2f", bt.sharpeRatio), MetallicGoldBright, Modifier.weight(1f))
                        TelemetryStatTile("OOS SHARPE", String.format(Locale.US, "%.2f", bt.oosSharpeRatio), LiveEmerald, Modifier.weight(1f))
                        TelemetryStatTile("MAX DD", String.format(Locale.US, "%.1f%%", bt.maxDrawdownPct), CrimsonAlert, Modifier.weight(1f))
                        TelemetryStatTile("STRESS", if (bt.stressTestSurvived) "PASS" else "FAIL", if (bt.stressTestSurvived) LiveEmerald else CrimsonAlert, Modifier.weight(1f))
                    }
                }
            }
        }

        state.strategies.forEach { strat ->
            item(key = strat.id) {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strat.name, style = MaterialTheme.typography.titleSmall, color = IvoryText)
                            Text(
                                "${strat.symbol} · ${strat.stage} · By ${strat.creatorAgent}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MetallicGold
                            )
                        }
                        Button(
                            onClick = { onMutateStrategy(strat) },
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianVoid),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Mutate DNA", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(strat.genomeSummary, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Sharpe ${String.format(Locale.US, "%.2f", strat.sharpeRatio)} · PF ${String.format(Locale.US, "%.2f", strat.profitFactor)} · Win ${String.format(Locale.US, "%.0f%%", strat.winRatePct)} · N=${strat.sampleSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LiveEmerald
                    )
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "DNA_LAB") }
    }
}

@Composable
fun BlackSwanPredictorScreen(
    state: AetherDashboardState,
    onEngageKillSwitch: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ModuleHeader(VipModuleId.BLACK_SWAN_PREDICTOR, onBack) }
        item {
            GlassCard(borderColor = CrimsonAlert.copy(alpha = 0.5f)) {
                Text(
                    "REALIZED VOLATILITY Z-SCORE & EXCESS KURTOSIS MONITOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonAlert
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Computes fat-tail kurtosis, volatility shocks, and worst-bar drawdowns directly from live OHLCV candles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onEngageKillSwitch(!state.constitution.emergencyKillSwitch) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.constitution.emergencyKillSwitch) LiveEmerald else CrimsonAlert,
                        contentColor = IvoryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.constitution.emergencyKillSwitch) "Release Black Swan Lockdown" else "Engage Black Swan Protective Kill Switch"
                    )
                }
            }
        }
        state.blackSwanAlerts.forEach { alert ->
            item(key = alert.symbol) {
                val color = when {
                    alert.tailRiskScore >= 75 -> CrimsonAlert
                    alert.tailRiskScore >= 52 -> SimulatedAmber
                    else -> LiveEmerald
                }
                GlassCard(borderColor = color.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${alert.symbol} · ${alert.regimeStatus}", style = MaterialTheme.typography.titleSmall, color = color)
                        Text("${alert.tailRiskScore}/100", style = MaterialTheme.typography.labelLarge, color = color)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { alert.tailRiskScore / 100f },
                        color = color,
                        trackColor = CrimsonContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "AnnVol: ${String.format(Locale.US, "%.1f%%", alert.realizedVolPct)} · Vol Z: ${String.format(Locale.US, "%.2f", alert.volZScore)} · Kurtosis: ${String.format(Locale.US, "%.2f", alert.excessKurtosis)} · Peak Bar Shock: ${String.format(Locale.US, "%.2f%%", alert.worstBarShockPct)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneSecondary
                    )
                }
            }
        }
        item { ModuleAuditLogCard(state.auditLogs, "RISK_MATRIX") }
    }
}
