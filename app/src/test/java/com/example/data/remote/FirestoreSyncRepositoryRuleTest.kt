package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AuditLogRecord
import com.example.data.model.AuditOutcome
import com.example.data.model.MemoryRecord
import com.example.data.model.MemoryTier
import com.example.data.model.RiskConfigRecord
import com.example.data.model.StrategyRecord
import com.example.data.model.StrategyStage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertStrategy_authenticatedOwner_persistsAndObserves() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore, auth)
        val stratId = "strat_${UUID.randomUUID().toString().take(8)}"

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertStrategy(
                StrategyRecord(
                    id = stratId,
                    name = "Gold London Breakout",
                    assetClass = "Metals",
                    symbol = "XAUUSD",
                    timeframe = "1H",
                    stage = StrategyStage.BACKTESTED.name,
                    sharpeRatio = 1.92,
                    profitFactor = 1.75,
                    maxDrawdownPct = 3.8,
                    winRatePct = 60.0,
                    sampleSize = 140L,
                    genomeSummary = "EMA 12/34 + ATR 1.5x stop",
                    creatorAgent = "GENOME"
                )
            )
        }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStrategies().first { list -> list.any { it.id == stratId && it.userId == uid } }
        }
        assertTrue(observed.any { it.id == stratId && it.name == "Gold London Breakout" })
    }

    @Test
    fun appendAuditLog_andCreateMemory_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore, auth)
        val logId = "log_${UUID.randomUUID().toString().take(8)}"
        val memId = "mem_${UUID.randomUUID().toString().take(8)}"

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.appendAuditLog(
                AuditLogRecord(
                    id = logId,
                    moduleId = "RISK_MATRIX",
                    actor = "RISK GUARD",
                    action = "Validated daily drawdown threshold",
                    outcome = AuditOutcome.APPROVED.name,
                    details = "Current DD 0.8% <= 8.0%"
                )
            )
        }
        assertTrue(logRes.isSuccess)

        val memRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createMemory(
                MemoryRecord(
                    id = memId,
                    tier = MemoryTier.MARKET_MEMORY.name,
                    title = "XAUUSD Volatility Compression",
                    content = "Realized vol dropped below 12% prior to FOMC",
                    symbol = "XAUUSD",
                    agentName = "GOLDEN EYE",
                    confidenceScore = 0.88
                )
            )
        }
        assertTrue(memRes.isSuccess)

        val riskRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveRiskConfig(RiskConfigRecord(maxDrawdownPct = 7.5))
        }
        assertTrue(riskRes.isSuccess)
    }

    @Test
    fun observeStrategies_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreSyncRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeStrategies().first() }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        // Repository throws IllegalStateException when auth.currentUser == null (auth-gating)
        // OR PERMISSION_DENIED from Firestore if bypassed
        val fsEx = caught!!.unwrapFirestoreException()
        assertTrue(
            caught is IllegalStateException ||
                fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
    }

    @Test
    fun directCrossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreSyncRepository(firestore, auth)
        val stratId = "strat_${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertStrategy(
                StrategyRecord(
                    id = stratId,
                    name = "Alice Private Strategy",
                    assetClass = "Forex",
                    symbol = "EURUSD",
                    timeframe = "1H",
                    stage = StrategyStage.EXPERIMENTAL.name,
                    sharpeRatio = 1.2,
                    profitFactor = 1.3,
                    maxDrawdownPct = 5.0,
                    winRatePct = 52.0,
                    sampleSize = 60L,
                    genomeSummary = "RSI divergence",
                    creatorAgent = "GENOME"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("strategies").document(stratId)
                    .get().let { com.google.android.gms.tasks.Tasks.await(it) }
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertEquals(true, denied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@aetherion.test"
        const val BOB_EMAIL = "bob@aetherion.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
