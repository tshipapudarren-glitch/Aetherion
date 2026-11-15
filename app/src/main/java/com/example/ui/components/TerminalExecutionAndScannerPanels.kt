package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PaperOrderEntity
import com.example.data.model.MarketConditionsSnapshot
import com.example.data.model.MarketQuote
import com.example.domain.engine.IntegratedSetupSignal
import com.example.domain.engine.MarketStructureSnapshot
import com.example.domain.engine.SetupArchitectEngine
import com.example.domain.engine.SetupScanStatus
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
import com.example.ui.viewmodel.AiTaskState
import java.util.Locale

@Composable
fun InstitutionalAccountAndSentimentHud(
    structure: MarketStructureSnapshot,
    conditions: MarketConditionsSnapshot
) {
    val acct = structure.accountSummary
    val totalPnlUsd = acct.realizedPnlUsd + acct.floatingPnlUsd
    val pnlColor = if (totalPnlUsd >= 0.0) LiveEmerald else CrimsonAlert

    GlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PAPER PORTFOLIO & SENTIMENT HUD",
                style = MaterialTheme.typography.labelSmall,
                color = MetallicGoldBright,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = structure.structureBias,
                style = MaterialTheme.typography.labelSmall,
                color = IceTelemetry
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = "PAPER EQUITY",
                value = "$${String.format(Locale.US, "%,.0f", acct.equityUsd)}",
                accent = IvoryText,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = "NET P&L",
                value = "${if (totalPnlUsd >= 0) "+" else "-"}$${String.format(Locale.US, "%,.0f", kotlin.math.abs(totalPnlUsd))}",
                accent = pnlColor,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = "MARGIN USED",
                value = "${String.format(Locale.US, "%.1f%%", acct.usedMarginPct)}",
                accent = SimulatedAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bullish vs Bearish Sentiment Bar (inspired by terminal reference)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("BULLS ${structure.bullishSentimentPct}%", style = MaterialTheme.typography.labelSmall, color = LiveEmerald, fontWeight = FontWeight.Bold)
            Text("BEARS ${structure.bearishSentimentPct}%", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianVoid)
        ) {
            Box(
                modifier = Modifier
                    .weight(structure.bullishSentimentPct.coerceAtLeast(5).toFloat())
                    .fillMaxHeight()
                    .background(LiveEmerald)
            )
            Box(
                modifier = Modifier
                    .weight(structure.bearishSentimentPct.coerceAtLeast(5).toFloat())
                    .fillMaxHeight()
                    .background(CrimsonAlert)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "MACRO PULSE · VIX ${conditions.vixLevel?.let { String.format(Locale.US, "%.2f", it) } ?: "--"} · US10Y ${conditions.us10yYield?.let { String.format(Locale.US, "%.2f%%", it) } ?: "--"} · Fib61.8 ${SetupArchitectEngine.formatPrice(structure.fib618Level)}",
            style = MaterialTheme.typography.labelSmall,
            color = ChampagneSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TwoWayExecutionTicketCard(
    quote: MarketQuote?,
    structure: MarketStructureSnapshot,
    permissionLevel: Int,
    slInput: String,
    tpInput: String,
    onSlChange: (String) -> Unit,
    onTpChange: (String) -> Unit,
    onSubmitOrder: (side: String, exposurePct: Double, customSl: Double?, customTp: Double?, routeMt5: Boolean) -> Unit
) {
    var selectedExposure by remember { mutableStateOf(1.0) }
    var routeMt5 by remember { mutableStateOf(false) }

    val midPrice = quote?.price ?: 0.0
    val halfSpread = if (midPrice > 0.0) midPrice * (structure.estimatedSpreadBps / 20_000.0) else 0.0
    val bidPrice = (midPrice - halfSpread).coerceAtLeast(0.0)
    val askPrice = (midPrice + halfSpread).coerceAtLeast(0.0)
    val estMarginUsd = structure.accountSummary.equityUsd * (selectedExposure / 100.0)

    GlassCard(borderColor = IceTelemetry.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXECUTION TICKET · ${quote?.instrument?.symbol ?: "--"}",
                style = MaterialTheme.typography.titleSmall,
                color = MetallicGoldBright,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Est. Spread ${String.format(Locale.US, "%.1f bps", structure.estimatedSpreadBps)}",
                style = MaterialTheme.typography.labelSmall,
                color = IceTelemetry
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Risk Exposure Sizing Stepper
        Text("RISK EXPOSURE (% OF EQUITY)", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.5, 1.0, 2.0, 3.5, 5.0).forEach { pct ->
                FilterChip(
                    selected = selectedExposure == pct,
                    onClick = { selectedExposure = pct },
                    label = { Text("${pct}%") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGoldContainer,
                        selectedLabelColor = MetallicGoldBright
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = slInput,
                onValueChange = onSlChange,
                label = { Text("Stop Loss") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("ticket_sl_input")
            )
            OutlinedTextField(
                value = tpInput,
                onValueChange = onTpChange,
                label = { Text("Take Profit") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("ticket_tp_input")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Est. Notional: $${String.format(Locale.US, "%,.0f", estMarginUsd)}", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
            FilterChip(
                selected = routeMt5,
                onClick = { routeMt5 = !routeMt5 },
                label = { Text(if (routeMt5) "Route: MT5 Live (L4+)" else "Route: Paper Sandbox") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CrimsonContainer,
                    selectedLabelColor = IvoryText
                )
            )
        }
        if (routeMt5 && permissionLevel < 4) {
            Text(
                text = "Note: Permission Level $permissionLevel < 4 — Risk Guard will veto live MT5 routing until upgraded in Risk Matrix.",
                style = MaterialTheme.typography.labelSmall,
                color = SimulatedAmber
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        val parsedSl = slInput.toDoubleOrNull()
        val parsedTp = tpInput.toDoubleOrNull()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onSubmitOrder("SELL", selectedExposure, parsedSl, parsedTp, routeMt5) },
                enabled = quote?.isLive == true,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = IvoryText),
                modifier = Modifier.weight(1f).height(54.dp).testTag("terminal_sell_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SELL BID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(if (midPrice > 0.0) SetupArchitectEngine.formatPrice(bidPrice) else "OFFLINE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { onSubmitOrder("BUY", selectedExposure, parsedSl, parsedTp, routeMt5) },
                enabled = quote?.isLive == true,
                colors = ButtonDefaults.buttonColors(containerColor = LiveEmerald, contentColor = ObsidianVoid),
                modifier = Modifier.weight(1f).height(54.dp).testTag("terminal_buy_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BUY ASK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(if (midPrice > 0.0) SetupArchitectEngine.formatPrice(askPrice) else "OFFLINE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SyntheticSpreadDepthCard(structure: MarketStructureSnapshot) {
    GlassCard(borderColor = SimulatedAmber.copy(alpha = 0.45f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SYNTHETIC ORDER BOOK DEPTH", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
            Text("DERIVED ATR SPREAD MODEL", style = MaterialTheme.typography.labelSmall, color = SimulatedAmber)
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (structure.spreadDepth.isEmpty()) {
            Text("Sync live feeds to model bid/ask depth bands.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BID SIZE", style = MaterialTheme.typography.labelSmall, color = SlateMuted, modifier = Modifier.weight(1f))
                Text("BID", style = MaterialTheme.typography.labelSmall, color = LiveEmerald, modifier = Modifier.weight(1f))
                Text("ASK", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert, modifier = Modifier.weight(1f))
                Text("ASK SIZE", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            structure.spreadDepth.forEach { lvl ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${String.format(Locale.US, "%.2f", lvl.bidLots)}L", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary, modifier = Modifier.weight(1f))
                    Text(SetupArchitectEngine.formatPrice(lvl.bidPrice), style = MaterialTheme.typography.labelSmall, color = LiveEmerald, modifier = Modifier.weight(1f))
                    Text(SetupArchitectEngine.formatPrice(lvl.askPrice), style = MaterialTheme.typography.labelSmall, color = CrimsonAlert, modifier = Modifier.weight(1f))
                    Text("${String.format(Locale.US, "%.2f", lvl.askLots)}L", style = MaterialTheme.typography.labelSmall, color = ChampagneSecondary)
                }
            }
        }
    }
}

@Composable
fun ChartScreenshotVisionCard(
    visionState: AiTaskState<com.example.data.remote.ChartVisionAnalysis>,
    onPickScreenshot: (Uri) -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onPickScreenshot(uri) }
    )

    GlassCard(borderColor = MetallicGold.copy(alpha = 0.55f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CHART INTELLIGENCE VISION", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
            Text("PHOTO PICKER + GEMINI", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Upload any candlestick chart screenshot using the zero-permission Photo Picker for AI structural breakdown (FVG, Order Block, Sweep, Entry/SL/TP).",
            style = MaterialTheme.typography.bodySmall,
            color = ChampagneSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = visionState !is AiTaskState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated, contentColor = MetallicGoldBright),
            border = BorderStroke(1.dp, MetallicGold),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("upload_chart_vision_button")
        ) {
            if (visionState is AiTaskState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MetallicGoldBright, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Chart Screenshot...")
            } else {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Upload Chart Screenshot", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Chart Screenshot for AI Vision")
            }
        }

        when (visionState) {
            is AiTaskState.Success -> {
                val res = visionState.data
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianVoid.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (res.bias == "BULLISH") LiveEmerald else if (res.bias == "BEARISH") CrimsonAlert else MetallicGold)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${res.detectedPattern} · ${res.bias} (${res.confidencePct}%)", style = MaterialTheme.typography.labelMedium, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
                            Text(res.providerUsed, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                        }
                        Text("Structure: ${res.marketStructureState}", style = MaterialTheme.typography.bodySmall, color = IvoryText)
                        Text("Entry: ${res.entryZone} | SL: ${res.stopLossLevel}", style = MaterialTheme.typography.labelSmall, color = IceTelemetry)
                        Text("Targets: ${res.takeProfitLevels}", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
                        Text("Invalidation: ${res.invalidationCondition}", style = MaterialTheme.typography.labelSmall, color = CrimsonAlert)
                        Text(res.fullExplanation, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                    }
                }
            }
            is AiTaskState.Error -> {
                Spacer(modifier = Modifier.height(8.dp))
                Text("VISION ERROR: ${visionState.message}", style = MaterialTheme.typography.bodySmall, color = CrimsonAlert)
            }
            else -> {}
        }
    }
}

@Composable
fun Integrated15SetupScannerCard(
    structure: MarketStructureSnapshot,
    onApplySetupToTicket: (IntegratedSetupSignal) -> Unit
) {
    var statusFilter by remember { mutableStateOf<SetupScanStatus?>(null) }
    val filtered = remember(structure.detectedSetups, statusFilter) {
        if (statusFilter == null) structure.detectedSetups
        else structure.detectedSetups.filter { it.status == statusFilter }
    }

    GlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("15 INTEGRATED SETUP DETECTORS", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright, fontWeight = FontWeight.Bold)
            Text("${structure.triggeredCount} TRIGGERED", style = MaterialTheme.typography.labelSmall, color = LiveEmerald)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { statusFilter = null },
                label = { Text("All (${structure.detectedSetups.size})") }
            )
            FilterChip(
                selected = statusFilter == SetupScanStatus.TRIGGERED,
                onClick = { statusFilter = if (statusFilter == SetupScanStatus.TRIGGERED) null else SetupScanStatus.TRIGGERED },
                label = { Text("Triggered (${structure.triggeredCount})") }
            )
            FilterChip(
                selected = statusFilter == SetupScanStatus.FORMING,
                onClick = { statusFilter = if (statusFilter == SetupScanStatus.FORMING) null else SetupScanStatus.FORMING },
                label = { Text("Forming (${structure.formingCount})") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Text("Sync live market feed to scan 15 institutional setups.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
        } else {
            filtered.forEach { item ->
                val statusCol = when (item.status) {
                    SetupScanStatus.TRIGGERED -> LiveEmerald
                    SetupScanStatus.FORMING -> SimulatedAmber
                    SetupScanStatus.SCANNING -> SlateMuted
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianVoid.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, if (item.status == SetupScanStatus.TRIGGERED) LiveEmerald.copy(alpha = 0.45f) else ObsidianBorder),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.number.toString().padStart(2, '0')} · ${item.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = IvoryText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.status.label} · ${item.direction} ${item.confidencePct}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusCol,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = ChampagneSecondary)
                        Text(
                            text = "Drawing (${item.drawingType}): Ent ${SetupArchitectEngine.formatPrice(item.entryPrice)} · SL ${SetupArchitectEngine.formatPrice(item.stopLoss)} · TP ${SetupArchitectEngine.formatPrice(item.takeProfit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceTelemetry
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Inval: ${item.invalidationRule}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonAlert,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { onApplySetupToTicket(item) },
                                border = BorderStroke(1.dp, MetallicGold),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Load Levels", color = MetallicGoldBright, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalOpenPositionsCard(
    orders: List<PaperOrderEntity>,
    onCloseOrder: (PaperOrderEntity) -> Unit
) {
    GlassCard(borderColor = ObsidianBorder) {
        val open = orders.filter { it.status == "OPEN" }
        val closed = orders.filter { it.status == "CLOSED" }.take(5)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("OPEN POSITIONS (${open.size}) & HISTORY", style = MaterialTheme.typography.titleSmall, color = MetallicGoldBright)
            Text("PAPER SANDBOX", style = MaterialTheme.typography.labelSmall, color = SimulatedAmber)
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (open.isEmpty() && closed.isEmpty()) {
            Text("No open or closed terminal positions yet.", style = MaterialTheme.typography.bodySmall, color = SlateMuted)
        } else {
            open.forEach { order ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianVoid.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${order.side} ${order.symbol} · ${order.exposurePct}%", style = MaterialTheme.typography.labelLarge, color = IvoryText)
                            Text(
                                "Entry ${SetupArchitectEngine.formatPrice(order.entryPrice)} · SL ${SetupArchitectEngine.formatPrice(order.stopLoss)} · TP ${SetupArchitectEngine.formatPrice(order.takeProfit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = IceTelemetry
                            )
                        }
                        OutlinedButton(
                            onClick = { onCloseOrder(order) },
                            border = BorderStroke(1.dp, CrimsonAlert),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Close", color = CrimsonAlert, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            closed.forEach { order ->
                Text(
                    text = "CLOSED · ${order.side} ${order.symbol} · Net ${String.format(Locale.US, "%+.2f%%", order.pnlPct)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (order.pnlPct >= 0) LiveEmerald else CrimsonAlert,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
