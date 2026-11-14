package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.core.security.OperationType
import com.example.core.security.handleFirestoreError
import com.example.data.model.AuditLogRecord
import com.example.data.model.MemoryRecord
import com.example.data.model.RiskConfigRecord
import com.example.data.model.StrategyRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing Firestore.")
    }

    fun observeStrategies(): Flow<List<StrategyRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/strategies"
        emitAll(
            db.collection("users").document(uid).collection("strategies")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StrategyRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertStrategy(strategy: StrategyRecord): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = strategy.id.takeIf { it.isNotBlank() } ?: "strat_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("users").document(uid).collection("strategies").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "stage" to strategy.stage,
                    "sharpeRatio" to strategy.sharpeRatio,
                    "profitFactor" to strategy.profitFactor,
                    "maxDrawdownPct" to strategy.maxDrawdownPct,
                    "winRatePct" to strategy.winRatePct,
                    "sampleSize" to strategy.sampleSize,
                    "genomeSummary" to strategy.genomeSummary.take(490),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to strategy.name.take(78).ifBlank { "Unnamed Strategy" },
                    "assetClass" to strategy.assetClass.take(30).ifBlank { "Multi" },
                    "symbol" to strategy.symbol.take(22).ifBlank { "XAUUSD" },
                    "timeframe" to strategy.timeframe.take(10).ifBlank { "1H" },
                    "stage" to strategy.stage,
                    "sharpeRatio" to strategy.sharpeRatio.coerceIn(-19.0, 49.0),
                    "profitFactor" to strategy.profitFactor.coerceIn(0.0, 99.0),
                    "maxDrawdownPct" to strategy.maxDrawdownPct.coerceIn(0.0, 100.0),
                    "winRatePct" to strategy.winRatePct.coerceIn(0.0, 100.0),
                    "sampleSize" to strategy.sampleSize.coerceIn(0L, 999999L),
                    "genomeSummary" to strategy.genomeSummary.take(490).ifBlank { "Quantitative Genome" },
                    "creatorAgent" to strategy.creatorAgent.take(38).ifBlank { "GENOME" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMemories(): Flow<List<MemoryRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/memories"
        emitAll(
            db.collection("users").document(uid).collection("memories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MemoryRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createMemory(memory: MemoryRecord): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = memory.id.takeIf { it.isNotBlank() } ?: "mem_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("users").document(uid).collection("memories").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "tier" to memory.tier,
            "title" to memory.title.take(118).ifBlank { "Intelligence Note" },
            "content" to memory.content.take(1950).ifBlank { "No content" },
            "symbol" to memory.symbol.take(22).ifBlank { "GLOBAL" },
            "agentName" to memory.agentName.take(38).ifBlank { "MEMORY MATRIX" },
            "confidenceScore" to memory.confidenceScore.coerceIn(0.0, 1.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLogRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/auditLogs"
        emitAll(
            db.collection("users").document(uid).collection("auditLogs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(120)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AuditLogRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun appendAuditLog(log: AuditLogRecord): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = log.id.takeIf { it.isNotBlank() } ?: "log_${UUID.randomUUID().toString().take(12)}"
        val docRef = db.collection("users").document(uid).collection("auditLogs").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "moduleId" to log.moduleId.take(46).ifBlank { "AETHER_CORE" },
            "actor" to log.actor.take(46).ifBlank { "WATCHMAN" },
            "action" to log.action.take(118).ifBlank { "System event" },
            "outcome" to log.outcome,
            "details" to log.details.take(1450).ifBlank { "-" },
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveRiskConfig(config: RiskConfigRecord): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("riskConfig").document("constitution")
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "maxDrawdownPct" to config.maxDrawdownPct.coerceIn(0.5, 50.0),
                        "dailyLossLimitPct" to config.dailyLossLimitPct.coerceIn(0.2, 20.0),
                        "maxOpenPositions" to config.maxOpenPositions.coerceIn(1L, 50L),
                        "maxTotalExposurePct" to config.maxTotalExposurePct.coerceIn(1.0, 100.0),
                        "maxSpreadBps" to config.maxSpreadBps.coerceIn(1.0, 500.0),
                        "maxSlippageBps" to config.maxSlippageBps.coerceIn(1.0, 500.0),
                        "newsBlackoutEnabled" to config.newsBlackoutEnabled,
                        "emergencyKillSwitch" to config.emergencyKillSwitch,
                        "permissionLevel" to config.permissionLevel.coerceIn(1L, 5L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "maxDrawdownPct" to config.maxDrawdownPct.coerceIn(0.5, 50.0),
                        "dailyLossLimitPct" to config.dailyLossLimitPct.coerceIn(0.2, 20.0),
                        "maxOpenPositions" to config.maxOpenPositions.coerceIn(1L, 50L),
                        "maxTotalExposurePct" to config.maxTotalExposurePct.coerceIn(1.0, 100.0),
                        "maxSpreadBps" to config.maxSpreadBps.coerceIn(1.0, 500.0),
                        "maxSlippageBps" to config.maxSlippageBps.coerceIn(1.0, 500.0),
                        "newsBlackoutEnabled" to config.newsBlackoutEnabled,
                        "emergencyKillSwitch" to config.emergencyKillSwitch,
                        "permissionLevel" to config.permissionLevel.coerceIn(1L, 5L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
