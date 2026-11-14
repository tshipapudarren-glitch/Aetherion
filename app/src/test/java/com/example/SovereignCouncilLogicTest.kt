package com.example

import com.example.data.model.AetherAgentsRegistry
import com.example.data.remote.CouncilVoteResult
import com.example.ui.screens.buildCouncilSeatModels
import com.example.ui.viewmodel.AiTaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SovereignCouncilLogicTest {

    @Test
    fun buildCouncilSeatModels_reflectsVerdictAndDeterministicRiskGuardClearance() {
        val vote = CouncilVoteResult(
            symbol = "XAUUSD",
            consensusStance = "BULLISH",
            confidencePct = 78,
            oracleThesis = "Momentum breakout above prior range high.",
            criticObjection = "Crowded long positioning ahead of CPI.",
            macroIntelBrief = "Real yields easing supports gold bids.",
            arbiterRuling = "Proceed with capped risk allocation.",
            recommendedStrategy = "Adaptive Breakout",
            providerUsed = "Gemini 3.8 Flash"
        )

        val seats = buildCouncilSeatModels(
            agents = AetherAgentsRegistry.allAgents,
            councilState = AiTaskState.Success(vote),
            riskLocked = false,
            symbol = "XAUUSD",
            maxDdPct = 8.0,
            currentDdPct = 1.2,
            maxExposurePct = 25.0
        )

        assertEquals(5, seats.size)
        val oracle = seats.first { it.agentName == "ORACLE" }
        assertEquals("BULLISH", oracle.stanceTag)
        assertTrue(oracle.argumentBody.contains("Momentum breakout"))

        val arbiter = seats.first { it.agentName == "ARBITER" }
        assertEquals("78% BINDING", arbiter.stanceTag)

        val riskGuard = seats.first { it.agentName == "RISK GUARD" }
        assertEquals("CLEARED", riskGuard.stanceTag)
    }

    @Test
    fun buildCouncilSeatModels_surfacesRiskGuardVetoWhenLocked() {
        val seats = buildCouncilSeatModels(
            agents = AetherAgentsRegistry.allAgents,
            councilState = AiTaskState.Idle,
            riskLocked = true,
            symbol = "BTCUSD",
            maxDdPct = 8.0,
            currentDdPct = 8.5,
            maxExposurePct = 25.0
        )

        val riskGuard = seats.first { it.agentName == "RISK GUARD" }
        assertEquals("VETO ACTIVE", riskGuard.stanceTag)
        assertTrue(riskGuard.argumentBody.contains("DETERMINISTIC HALT ENFORCED"))
    }
}
