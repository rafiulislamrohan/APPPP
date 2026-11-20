const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_vital_123";
const BOB_UID = "bob_vital_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read or write any VITALTRACK collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("daily_steps").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profile")
      .doc("settings")
      .set({
        userId: ALICE_UID,
        displayName: "Hacker",
        dailyStepGoal: 8000,
        dailyWaterGoalMl: 2200,
        useImperialUnits: false,
        languageCode: "en",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated owner: valid Kotlin-mirrored profile, steps, gps, and health_logs succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profile")
      .doc("settings")
      .set({
        userId: ALICE_UID,
        displayName: "Rafiul",
        dailyStepGoal: 10000,
        dailyWaterGoalMl: 2500,
        useImperialUnits: false,
        languageCode: "en",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_steps")
      .doc("2026-11-19")
      .set({
        userId: ALICE_UID,
        dateIso: "2026-11-19",
        steps: 6420,
        goal: 10000,
        distanceMeters: 4815.0,
        caloriesKcal: 256.8,
        activeMinutes: 64,
        source: "SENSOR_COUNTER",
        clientUpdatedAtMillis: Date.now() - 2000,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_steps")
      .doc("2026-11-19")
      .update({
        steps: 7100,
        updatedAt: updated,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gps_sessions")
      .doc("session_abc_1")
      .set({
        userId: ALICE_UID,
        activityType: "RUNNING",
        title: "Morning Park Run",
        dateIso: "2026-11-19",
        startTimeMillis: 1700000000000,
        endTimeMillis: 1700001800000,
        activeDurationMillis: 1800000,
        distanceMeters: 5120.5,
        avgSpeedMps: 2.84,
        caloriesKcal: 340.0,
        steps: 6100,
        notes: "Steady pace",
        clientUpdatedAtMillis: Date.now() - 2000,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("health_logs")
      .doc("water_123")
      .set({
        userId: ALICE_UID,
        logType: "WATER",
        dateIso: "2026-11-19",
        numericValue: 250.0,
        secondaryValue: 0.0,
        label: "Water Intake",
        notes: "",
        recordedAtMillis: Date.now() - 2000,
        clientUpdatedAtMillis: Date.now() - 2000,
        createdAt: created,
        updatedAt: created,
      })
  );
});

test("Cross-user isolation & shadow/immutable-field poisoning fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("health_logs")
      .doc("weight_1")
      .set({
        userId: ALICE_UID,
        logType: "WEIGHT",
        dateIso: "2026-11-19",
        numericValue: 70.5,
        secondaryValue: 23.8,
        label: "MANUAL",
        notes: "",
        recordedAtMillis: Date.now() - 2000,
        clientUpdatedAtMillis: Date.now() - 2000,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Bob cannot read or write Alice's data
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("health_logs").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("health_logs").doc("weight_1").delete()
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profile")
      .doc("settings")
      .set({
        userId: ALICE_UID,
        displayName: "Rafiul",
        dailyStepGoal: 8000,
        dailyWaterGoalMl: 2200,
        useImperialUnits: false,
        languageCode: "en",
        isAdmin: true,
        createdAt: created,
        updatedAt: created,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_steps")
      .doc("2026-11-20")
      .set({
        userId: ALICE_UID,
        dateIso: "2026-11-20",
        steps: 100,
        goal: 8000,
        distanceMeters: 75.0,
        caloriesKcal: 4.0,
        activeMinutes: 1,
        source: "SENSOR_COUNTER",
        clientUpdatedAtMillis: Date.now(),
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});
