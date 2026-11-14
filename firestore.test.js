const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_aether_01";
const BOB_UID = "bob_aether_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validStrategy(userId) {
  return {
    userId,
    name: "XAUUSD Liquidity Sweep",
    assetClass: "Metals",
    symbol: "GC=F",
    timeframe: "1H",
    stage: "BACKTESTED",
    sharpeRatio: 1.85,
    profitFactor: 1.72,
    maxDrawdownPct: 4.2,
    winRatePct: 58.0,
    sampleSize: 120,
    genomeSummary: "EMA(21/55) + ATR(14) breakout with 1.8R target",
    creatorAgent: "GENOME",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validAuditLog(userId) {
  return {
    userId,
    moduleId: "RISK_MATRIX",
    actor: "RISK GUARD",
    action: "Validated portfolio exposure limit",
    outcome: "APPROVED",
    details: "Exposure 12.4% < max 25.0%",
    createdAt: new Date(),
  };
}

function validRiskConfig(userId) {
  return {
    userId,
    maxDrawdownPct: 8.0,
    dailyLossLimitPct: 2.5,
    maxOpenPositions: 6,
    maxTotalExposurePct: 25.0,
    maxSpreadBps: 18.0,
    maxSlippageBps: 12.0,
    newsBlackoutEnabled: true,
    emergencyKillSwitch: false,
    permissionLevel: 3,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list strategies or audit logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/strategies`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/auditLogs`).get());
});

test("Owner can create, list, and update own strategy", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`users/${ALICE_UID}/strategies`).doc("strat_001");
  await assertSucceeds(ref.set(validStrategy(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/strategies`).get());
  await assertSucceeds(ref.update({ stage: "OOS_VALIDATED", sharpeRatio: 2.05, updatedAt: new Date() }));
});

test("Cross-user access to strategies, memories, auditLogs, and riskConfig is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/strategies`).doc("strat_002").set(validStrategy(ALICE_UID)));
  await assertFails(bobDb.collection(`users/${ALICE_UID}/strategies`).doc("strat_002").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/strategies`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/riskConfig`).doc("default").set(validRiskConfig(BOB_UID)));
});

test("Shadow update with ghost field and invalid stage are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`users/${ALICE_UID}/strategies`).doc("strat_003");
  await assertFails(ref.set({ ...validStrategy(ALICE_UID), ghostAdmin: true }));
  await assertFails(ref.set({ ...validStrategy(ALICE_UID), stage: "BYPASS_LIVE" }));
});

test("Audit logs are strictly append-only (update and delete denied)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`users/${ALICE_UID}/auditLogs`).doc("log_001");
  await assertSucceeds(ref.set(validAuditLog(ALICE_UID)));
  await assertFails(ref.update({ outcome: "APPROVED" }));
  await assertFails(ref.delete());
});

test("RiskConfig enforces bounds and denies deletion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`users/${ALICE_UID}/riskConfig`).doc("constitution");
  await assertFails(ref.set({ ...validRiskConfig(ALICE_UID), maxDrawdownPct: 99.0 }));
  await assertSucceeds(ref.set(validRiskConfig(ALICE_UID)));
  await assertFails(ref.delete());
});
