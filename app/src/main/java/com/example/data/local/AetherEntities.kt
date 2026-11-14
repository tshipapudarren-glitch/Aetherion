package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_constitution")
data class RiskConstitutionEntity(
    @PrimaryKey val id: String = "constitution_singleton",
    val maxDrawdownPct: Double = 8.0,
    val dailyLossLimitPct: Double = 2.5,
    val maxOpenPositions: Int = 6,
    val maxTotalExposurePct: Double = 25.0,
    val maxSpreadBps: Double = 18.0,
    val maxSlippageBps: Double = 12.0,
    val newsBlackoutEnabled: Boolean = true,
    val emergencyKillSwitch: Boolean = false,
    val permissionLevel: Int = 3,
    val currentDrawdownPct: Double = 0.8,
    val currentDailyLossPct: Double = 0.2,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_strategies")
data class LocalStrategyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val assetClass: String,
    val symbol: String,
    val timeframe: String,
    val stage: String,
    val sharpeRatio: Double,
    val profitFactor: Double,
    val maxDrawdownPct: Double,
    val winRatePct: Double,
    val sampleSize: Int,
    val genomeSummary: String,
    val creatorAgent: String,
    val fastPeriod: Int,
    val slowPeriod: Int,
    val atrStopMult: Double,
    val atrTargetMult: Double,
    val isPaperActive: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_memories")
data class LocalMemoryEntity(
    @PrimaryKey val id: String,
    val tier: String,
    val title: String,
    val content: String,
    val symbol: String,
    val agentName: String,
    val confidenceScore: Double,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_audit_logs")
data class LocalAuditLogEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val actor: String,
    val action: String,
    val outcome: String,
    val details: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "paper_orders")
data class PaperOrderEntity(
    @PrimaryKey val id: String,
    val strategyId: String,
    val strategyName: String,
    val symbol: String,
    val side: String, // BUY or SELL
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val exposurePct: Double,
    val status: String, // OPEN or CLOSED
    val pnlPct: Double = 0.0,
    val openedAtMs: Long = System.currentTimeMillis(),
    val closedAtMs: Long? = null
)
