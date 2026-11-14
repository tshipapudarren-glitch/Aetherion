package com.example.domain.engine

import com.example.data.local.RiskConstitutionEntity
import com.example.data.model.RiskCheckResult
import com.example.data.model.StrategyStage

class DeterministicRiskEngine {

    fun evaluateExecutionProposal(
        constitution: RiskConstitutionEntity,
        strategyStage: StrategyStage,
        proposedExposurePct: Double,
        currentExposurePct: Double,
        openPositionsCount: Int,
        estimatedSpreadBps: Double,
        estimatedSlippageBps: Double,
        isHighImpactNewsWindow: Boolean,
        isLiveBrokerRoute: Boolean
    ): RiskCheckResult {
        val violations = mutableListOf<String>()

        if (constitution.emergencyKillSwitch) {
            violations.add("EMERGENCY KILL SWITCH IS ACTIVE: All new orders blocked.")
        }
        if (constitution.currentDrawdownPct >= constitution.maxDrawdownPct) {
            violations.add("MAX ACCOUNT DRAWDOWN BREACHED (${formatPct(constitution.currentDrawdownPct)} >= ${formatPct(constitution.maxDrawdownPct)})")
        }
        if (constitution.currentDailyLossPct >= constitution.dailyLossLimitPct) {
            violations.add("DAILY LOSS LIMIT BREACHED (${formatPct(constitution.currentDailyLossPct)} >= ${formatPct(constitution.dailyLossLimitPct)})")
        }
        if (openPositionsCount >= constitution.maxOpenPositions) {
            violations.add("MAX OPEN POSITIONS REACHED ($openPositionsCount >= ${constitution.maxOpenPositions})")
        }
        if (currentExposurePct + proposedExposurePct > constitution.maxTotalExposurePct) {
            violations.add(
                "TOTAL EXPOSURE CAP EXCEEDED (${formatPct(currentExposurePct + proposedExposurePct)} > ${formatPct(constitution.maxTotalExposurePct)})"
            )
        }
        if (estimatedSpreadBps > constitution.maxSpreadBps) {
            violations.add("SPREAD TOLERANCE EXCEEDED (${estimatedSpreadBps}bps > ${constitution.maxSpreadBps}bps)")
        }
        if (estimatedSlippageBps > constitution.maxSlippageBps) {
            violations.add("SLIPPAGE TOLERANCE EXCEEDED (${estimatedSlippageBps}bps > ${constitution.maxSlippageBps}bps)")
        }
        if (constitution.newsBlackoutEnabled && isHighImpactNewsWindow) {
            violations.add("NEWS PROTECTION BLACKOUT ACTIVE")
        }

        if (isLiveBrokerRoute) {
            if (constitution.permissionLevel < 4) {
                violations.add("PERMISSION LEVEL ${constitution.permissionLevel} < LEVEL 4 REQUIRED FOR LIVE DEPLOYMENT")
            }
            if (strategyStage != StrategyStage.LIVE_ELIGIBLE) {
                violations.add("GRADUATION GATE: Strategy is ${strategyStage.label}; must pass Backtest, OOS, Stress & Paper before Live.")
            }
        } else {
            if (constitution.permissionLevel < 3) {
                violations.add("PERMISSION LEVEL ${constitution.permissionLevel} < LEVEL 3 REQUIRED FOR PAPER TRADING")
            }
            if (strategyStage.order < StrategyStage.STRESS_TESTED.order || strategyStage == StrategyStage.RETIRED) {
                violations.add("VALIDATION GATE: Strategy (${strategyStage.label}) must pass Stress Testing before Paper execution.")
            }
        }

        return if (violations.isEmpty()) {
            RiskCheckResult(
                approved = true,
                reason = "DETERMINISTIC RISK GUARD APPROVED · Exposure ${formatPct(currentExposurePct + proposedExposurePct)} / ${formatPct(constitution.maxTotalExposurePct)}"
            )
        } else {
            RiskCheckResult(
                approved = false,
                reason = "VETOED BY RISK CONSTITUTION (${violations.size} violation${if (violations.size > 1) "s" else ""})",
                violatedRules = violations
            )
        }
    }

    fun evaluateGraduationEligibility(
        sampleSize: Int,
        profitFactor: Double,
        maxDrawdownPct: Double,
        oosSharpe: Double,
        stressSurvived: Boolean,
        constitution: RiskConstitutionEntity
    ): StrategyStage {
        if (sampleSize < 5 || profitFactor < 1.05 || maxDrawdownPct > constitution.maxDrawdownPct * 2.0) {
            return StrategyStage.EXPERIMENTAL
        }
        if (profitFactor < 1.2 || maxDrawdownPct > constitution.maxDrawdownPct) {
            return StrategyStage.BACKTESTED
        }
        if (oosSharpe <= 0.4) {
            return StrategyStage.BACKTESTED
        }
        if (!stressSurvived) {
            return StrategyStage.OOS_VALIDATED
        }
        return StrategyStage.PAPER_TRADING
    }

    private fun formatPct(v: Double): String = String.format("%.2f%%", v)
}
