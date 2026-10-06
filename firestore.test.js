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

test("Unauthenticated user: cannot read stories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("stories").get());
});

test("Authenticated user: cannot read another user's story", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("stories").doc("story_bob").set({
      id: "story_bob",
      userId: BOB_UID,
      title: "Bob's Story",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("stories").doc("story_bob").get());
});

test("Authenticated user: can create and read own story", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const storyRef = aliceDb.collection("users").doc(ALICE_UID).collection("stories").doc("story_alice");

  await assertSucceeds(
    storyRef.set({
      id: "story_alice",
      userId: ALICE_UID,
      title: "Alice's Creative Adventure",
      prompt: "A magical quest",
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  await assertSucceeds(storyRef.get());
});
