package com.example.domain.engine

import com.example.data.model.AssetClass
import com.example.data.model.MarketQuote
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class AssetClassFlowMetric(
    val assetClass: AssetClass,
    val displayName: String,
    val shortLabel: String,
    val flowScore: Int, // -100..+100 directional liquidity flow
    val avgChangePct: Double,
    val volumeImpulseRatio: Double, // latest bar volume vs baseline avg volume
    val avgVolPct: Double,
    val liveCount: Int,
    val topLeaderSymbol: String,
    val topLeaderChangePct: Double
)

data class RegionFlowMetric(
    val region: String,
    val shortLabel: String,
    val flowScore: Int, // -100..+100
    val avgChangePct: Double,
    val volumeImpulseRatio: Double,
    val avgVolPct: Double,
    val liveCount: Int,
    val leadSymbol: String,
    val sessionActive: Boolean
)

data class LiquidityTimelineBucket(
    val bucketLabel: String,
    val safeHavensPct: Double,
    val equitiesPct: Double,
    val cryptoPct: Double,
    val energyPct: Double
)

data class CapitalFlowCorridor(
    val fromLabel: String,
    val toLabel: String,
    val intensityPct: Int,
    val spreadPct: Double,
    val interpretation: String
)

data class GlobalCapitalFlowSnapshot(
    val assetFlows: List<AssetClassFlowMetric> = emptyList(),
    val regionFlows: List<RegionFlowMetric> = emptyList(),
    val timelineBuckets: List<LiquidityTimelineBucket> = emptyList(),
    val corridors: List<CapitalFlowCorridor> = emptyList(),
    val netLiquidityScore: Int = 50,
    val dominantInflow: String = "Evaluating",
    val dominantOutflow: String = "Evaluating",
    val updatedAtMs: Long = 0L
)

object CapitalFlowEngine {

    fun analyzeCapitalFlows(
        quotes: List<MarketQuote>,
        activeSessionsText: String = "",
        nowMs: Long = System.currentTimeMillis()
    ): GlobalCapitalFlowSnapshot {
        val liveQuotes = quotes.filter { it.isLive }
        if (liveQuotes.isEmpty()) {
            return GlobalCapitalFlowSnapshot(updatedAtMs = nowMs)
        }

        val assetFlows = AssetClass.entries
            .filter { it != AssetClass.SYNTHETIC }
            .mapNotNull { ac ->
                val list = liveQuotes.filter { it.instrument.assetClass == ac }
                if (list.isEmpty()) return@mapNotNull null
                val avgChange = list.map { it.changePct }.average()
                val avgVol = list.map { it.realizedVolPct }.average()
                val volImpulse = list.map { computeVolumeImpulse(it) }.average()
                val leader = list.maxByOrNull { abs(it.changePct) } ?: list.first()

                val rawScore = (avgChange * 18.5 * volImpulse.coerceIn(0.6, 2.2))
                    .roundToInt()
                    .coerceIn(-99, 99)

                AssetClassFlowMetric(
                    assetClass = ac,
                    displayName = ac.displayName,
                    shortLabel = shortAssetLabel(ac),
                    flowScore = rawScore,
                    avgChangePct = avgChange,
                    volumeImpulseRatio = volImpulse,
                    avgVolPct = avgVol,
                    liveCount = list.size,
                    topLeaderSymbol = leader.instrument.symbol,
                    topLeaderChangePct = leader.changePct
                )
            }
            .sortedByDescending { it.flowScore }

        val regionFlows = liveQuotes
            .groupBy { normalizeRegion(it.instrument.region) }
            .map { (region, list) ->
                val avgChange = list.map { it.changePct }.average()
                val avgVol = list.map { it.realizedVolPct }.average()
                val volImpulse = list.map { computeVolumeImpulse(it) }.average()
                val leader = list.maxByOrNull { it.changePct } ?: list.first()
                val rawScore = (avgChange * 19.0 * volImpulse.coerceIn(0.6, 2.1))
                    .roundToInt()
                    .coerceIn(-99, 99)

                RegionFlowMetric(
                    region = region,
                    shortLabel = shortRegionLabel(region),
                    flowScore = rawScore,
                    avgChangePct = avgChange,
                    volumeImpulseRatio = volImpulse,
                    avgVolPct = avgVol,
                    liveCount = list.size,
                    leadSymbol = leader.instrument.symbol,
                    sessionActive = isRegionSessionActive(region, activeSessionsText)
                )
            }
            .sortedByDescending { it.flowScore }

        val timelineBuckets = buildTimelineBuckets(liveQuotes)

        val sortedPositive = assetFlows.filter { it.avgChangePct >= 0.0 }
        val sortedNegative = assetFlows.filter { it.avgChangePct < 0.0 }.sortedBy { it.avgChangePct }
        val corridors = buildCorridors(assetFlows, regionFlows)

        val domIn = sortedPositive.firstOrNull()?.displayName
            ?: regionFlows.firstOrNull()?.region
            ?: "Balanced"
        val domOut = sortedNegative.firstOrNull()?.displayName
            ?: regionFlows.lastOrNull()?.region
            ?: "Balanced"

        val riskOnAvg = liveQuotes
            .filter { it.instrument.assetClass in setOf(AssetClass.INDICES, AssetClass.STOCKS, AssetClass.CRYPTO) }
            .map { it.changePct }
            .ifEmpty { listOf(0.0) }
            .average()
        val netLiquidityScore = (50.0 + riskOnAvg * 14.0).roundToInt().coerceIn(5, 98)

        return GlobalCapitalFlowSnapshot(
            assetFlows = assetFlows,
            regionFlows = regionFlows,
            timelineBuckets = timelineBuckets,
            corridors = corridors,
            netLiquidityScore = netLiquidityScore,
            dominantInflow = domIn,
            dominantOutflow = domOut,
            updatedAtMs = nowMs
        )
    }

    private fun computeVolumeImpulse(quote: MarketQuote): Double {
        val vols = quote.candles.map { it.volume }.filter { it > 0.0 }
        if (vols.size < 6) return 1.0
        val baseline = vols.dropLast(3).average().takeIf { it > 0.0 } ?: return 1.0
        val recent = vols.takeLast(3).average()
        return (recent / baseline).coerceIn(0.45, 2.8)
    }

    private fun buildTimelineBuckets(liveQuotes: List<MarketQuote>): List<LiquidityTimelineBucket> {
        val safeHavens = liveQuotes.filter { it.instrument.assetClass in setOf(AssetClass.PRECIOUS_METALS, AssetClass.BONDS) }
        val equities = liveQuotes.filter { it.instrument.assetClass in setOf(AssetClass.INDICES, AssetClass.STOCKS) }
        val crypto = liveQuotes.filter { it.instrument.assetClass == AssetClass.CRYPTO }
        val energy = liveQuotes.filter { it.instrument.assetClass == AssetClass.ENERGY }

        val labels = listOf("-24h", "-20h", "-16h", "-12h", "-8h", "-4h", "-2h", "LIVE")
        return labels.mapIndexed { idx, label ->
            val progress = idx.toDouble() / (labels.size - 1)
            LiquidityTimelineBucket(
                bucketLabel = label,
                safeHavensPct = sampleGroupReturnPct(safeHavens, progress),
                equitiesPct = sampleGroupReturnPct(equities, progress),
                cryptoPct = sampleGroupReturnPct(crypto, progress),
                energyPct = sampleGroupReturnPct(energy, progress)
            )
        }
    }

    private fun sampleGroupReturnPct(group: List<MarketQuote>, progress: Double): Double {
        if (group.isEmpty()) return 0.0
        val rets = group.mapNotNull { q ->
            val tail = q.candles.takeLast(24)
            if (tail.size < 4) return@mapNotNull q.changePct * progress
            val base = tail.first().close.takeIf { it > 0.0 } ?: return@mapNotNull null
            val idx = (progress * (tail.size - 1)).roundToInt().coerceIn(0, tail.lastIndex)
            ((tail[idx].close - base) / base) * 100.0
        }
        return if (rets.isEmpty()) 0.0 else rets.average()
    }

    private fun buildCorridors(
        assetFlows: List<AssetClassFlowMetric>,
        regionFlows: List<RegionFlowMetric>
    ): List<CapitalFlowCorridor> {
        val corridors = mutableListOf<CapitalFlowCorridor>()
        val topAsset = assetFlows.firstOrNull()
        val bottomAsset = assetFlows.lastOrNull()
        if (topAsset != null && bottomAsset != null && topAsset.assetClass != bottomAsset.assetClass) {
            val spread = topAsset.avgChangePct - bottomAsset.avgChangePct
            corridors.add(
                CapitalFlowCorridor(
                    fromLabel = bottomAsset.displayName,
                    toLabel = topAsset.displayName,
                    intensityPct = (abs(spread) * 24.0).roundToInt().coerceIn(18, 98),
                    spreadPct = spread,
                    interpretation = "Primary cross-asset rotation into ${topAsset.topLeaderSymbol} (${String.format("%+.2f%%", topAsset.topLeaderChangePct)})"
                )
            )
        }

        val topRegion = regionFlows.firstOrNull()
        val bottomRegion = regionFlows.lastOrNull()
        if (topRegion != null && bottomRegion != null && topRegion.region != bottomRegion.region) {
            val spread = topRegion.avgChangePct - bottomRegion.avgChangePct
            corridors.add(
                CapitalFlowCorridor(
                    fromLabel = bottomRegion.region,
                    toLabel = topRegion.region,
                    intensityPct = (abs(spread) * 26.0).roundToInt().coerceIn(15, 96),
                    spreadPct = spread,
                    interpretation = "Regional capital bid favoring ${topRegion.region} led by ${topRegion.leadSymbol}"
                )
            )
        }

        val secondAsset = assetFlows.getOrNull(1)
        val secondBottom = assetFlows.getOrNull(assetFlows.lastIndex - 1)
        if (secondAsset != null && secondBottom != null && secondAsset.assetClass != secondBottom.assetClass) {
            val spread = secondAsset.avgChangePct - secondBottom.avgChangePct
            corridors.add(
                CapitalFlowCorridor(
                    fromLabel = secondBottom.displayName,
                    toLabel = secondAsset.displayName,
                    intensityPct = (abs(spread) * 20.0).roundToInt().coerceIn(12, 92),
                    spreadPct = spread,
                    interpretation = "Secondary institutional allocation tracking ${secondAsset.shortLabel} vol-adjusted strength"
                )
            )
        }
        return corridors
    }

    private fun shortAssetLabel(ac: AssetClass): String = when (ac) {
        AssetClass.PRECIOUS_METALS -> "METALS"
        AssetClass.FOREX -> "G10 FX"
        AssetClass.CRYPTO -> "CRYPTO"
        AssetClass.INDICES -> "INDICES"
        AssetClass.ENERGY -> "ENERGY"
        AssetClass.STOCKS -> "EQUITY"
        AssetClass.BONDS -> "YIELDS"
        AssetClass.SYNTHETIC -> "SYNTH"
    }

    private fun normalizeRegion(raw: String): String = when (raw) {
        "Europe", "UK" -> "Europe & UK"
        "Asia", "APAC" -> "Asia-Pacific"
        else -> raw
    }

    private fun shortRegionLabel(region: String): String = when (region) {
        "North America" -> "NAM"
        "Europe & UK" -> "EUR/UK"
        "Asia-Pacific" -> "APAC"
        "Global" -> "GLOBAL"
        "Africa" -> "EM/AFR"
        else -> region.take(6).uppercase()
    }

    private fun isRegionSessionActive(region: String, activeSessions: String): Boolean {
        val lower = activeSessions.lowercase()
        return when (region) {
            "North America" -> lower.contains("new york")
            "Europe & UK" -> lower.contains("london")
            "Asia-Pacific" -> lower.contains("tokyo") || lower.contains("sydney")
            "Global" -> true
            else -> lower.contains("london") || lower.contains("new york")
        }
    }
}
