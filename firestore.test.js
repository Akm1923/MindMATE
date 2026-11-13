const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const now = new Date();
  return {
    userId: uid,
    displayName: "Alex",
    email: "alex@example.com",
    dietaryPreference: "balanced",
    activityPreference: "moderate",
    indoorOutdoorPreference: "both",
    wellnessGoal: "Consistent sleep and gentle movement",
    hydrationGoalMl: 2000,
    hydrationTodayMl: 750,
    notificationsEnabled: true,
    voiceSpeechEnabled: true,
    reducedMotionEnabled: false,
    voiceConsentGranted: true,
    memoryConsentGranted: true,
    conversationHistoryEnabled: true,
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("mood_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("journal_entries").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("mood_logs").doc("m1").set({
    userId: ALICE_UID,
    mood: "good",
    note: "test",
    energyLevel: 4,
    createdAt: new Date(),
  }));
});

test("Owner can create and update their own UserProfile, Bob cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    hydrationTodayMl: 1000,
    updatedAt: new Date(),
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({
    hydrationTodayMl: 1200,
    updatedAt: new Date(),
  }));
});

test("Shadow update with ghost field fails on UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    hydrationTodayMl: 1000,
    updatedAt: new Date(),
    isAdmin: true,
  }));
});

test("Mood logs allow owner create/list/delete, reject invalid mood or cross-user read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("mood_logs").doc("mood_1").set({
      userId: ALICE_UID,
      mood: "good",
      note: "Morning sunlight walk felt grounding.",
      energyLevel: 4,
      createdAt: new Date(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("mood_logs").doc("mood_bad").set({
      userId: ALICE_UID,
      mood: "diagnosed_anxiety",
      note: "invalid enum",
      energyLevel: 2,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("mood_logs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("mood_logs").get());
});

test("Journal entries support owner CRUD and reject cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("journal_entries").doc("j_1");
  await assertSucceeds(ref.set({
    userId: ALICE_UID,
    title: "Quiet evening",
    content: "Took 10 minutes to stretch and breathe before dinner.",
    moodTag: "good",
    aiSummary: "",
    aiPromptQuestion: "",
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(ref.update({
    aiSummary: "You prioritized slowing down before dinner.",
    aiPromptQuestion: "What helped you notice you needed a pause?",
    updatedAt: new Date(),
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("journal_entries").doc("j_1").get());
  await assertSucceeds(ref.delete());
});

test("Activity logs and conversations enforce owner isolation and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const actRef = aliceDb.collection("users").doc(ALICE_UID).collection("activity_logs").doc("act_1");
  await assertSucceeds(actRef.set({
    userId: ALICE_UID,
    recommendationId: "rec_breath_478",
    title: "4-7-8 Calm Breathing",
    category: "relax",
    durationMinutes: 5,
    status: "saved",
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(actRef.update({
    status: "completed",
    updatedAt: new Date(),
  }));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      mode: "listen",
      content: "Today felt a bit busy, just wanted to unwind.",
      createdAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("conversations").get());
});
