const test = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
  serverTimestamp,
  Timestamp,
} = require("@firebase/firestore");

const PROJECT_ID = "gen-lang-client-0066829062";
const DATABASE_ID = "ai-studio-android-amarhisa-49fa6844-e1d0-4f66-a917-6042d5964bc7";
const ALICE = "user_alice_123";
const BOB = "user_bob_456";

let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8"),
    },
  });
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

function dbFor(uid) {
  const ctx = uid ? testEnv.authenticatedContext(uid) : testEnv.unauthenticatedContext();
  return ctx.firestore(DATABASE_ID);
}

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "MD Shariful Islam",
    userCountryCode: "BD",
    mainCurrencyCode: "BDT",
    secondaryCurrencyCode: "KWD",
    monthlyBudget: 45000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validAccount(uid) {
  return {
    userId: uid,
    localId: 1,
    name: "Trust Bank Salary",
    type: "DEBIT_BANK",
    balance: 52000.5,
    creditLimit: 0,
    bankOrProvider: "Trust Bank",
    accountSubtype: "Savings",
    cardNetwork: "Visa",
    accountNumberLast4: "4321",
    colorArgb: 4278355143,
    currencyCode: "BDT",
    localCreatedAtMillis: 1763800000000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validTransaction(uid, includeTransferTo = false) {
  const payload = {
    userId: uid,
    localId: 101,
    title: "Weekly Bazaar",
    amount: 1500.0,
    type: includeTransferTo ? "TRANSFER" : "EXPENSE",
    category: includeTransferTo ? "TRANSFER" : "GROCERIES",
    subcategory: "Market",
    accountLocalId: 1,
    note: "Parsed via Voice Entry",
    currencyCode: "BDT",
    timestampMillis: 1763800001000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  if (includeTransferTo) {
    payload.toAccountLocalId = 2;
  }
  return payload;
}

function validGoal(uid) {
  return {
    userId: uid,
    localId: 10,
    name: "Emergency Fund",
    targetAmount: 100000,
    savedAmount: 25000,
    dueDateMillis: 1795000000000,
    colorArgb: 4279286657,
    localCreatedAtMillis: 1763800000000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validLoan(uid) {
  return {
    userId: uid,
    localId: 20,
    personName: "Rahim",
    loanType: "OWED_TO_ME",
    totalAmount: 10000,
    paidAmount: 2000,
    note: "Short-term loan",
    dueDateMillis: 1795000000000,
    currencyCode: "BDT",
    localCreatedAtMillis: 1763800000000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validBudget(uid) {
  return {
    userId: uid,
    categoryName: "GROCERIES",
    limitAmount: 15000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("denies all unauthenticated reads and writes across all collections", async () => {
  const anonDb = dbFor(null);
  await assertFails(getDoc(doc(anonDb, "users", ALICE)));
  await assertFails(setDoc(doc(anonDb, "users", ALICE), validProfile(ALICE)));
  await assertFails(getDocs(collection(anonDb, "users", ALICE, "accounts")));
  await assertFails(setDoc(doc(anonDb, "users", ALICE, "accounts", "acc_1"), validAccount(ALICE)));
  await assertFails(getDocs(collection(anonDb, "users", ALICE, "transactions")));
  await assertFails(setDoc(doc(anonDb, "users", ALICE, "transactions", "tx_101"), validTransaction(ALICE)));
  await assertFails(getDocs(collection(anonDb, "users", ALICE, "goals")));
  await assertFails(getDocs(collection(anonDb, "users", ALICE, "loans")));
  await assertFails(getDocs(collection(anonDb, "users", ALICE, "budgets")));
});

test("enforces strict cross-user isolation (Bob cannot read, list, create, update, or delete Alice's records)", async () => {
  const aliceDb = dbFor(ALICE);
  const bobDb = dbFor(BOB);

  await assertSucceeds(setDoc(doc(aliceDb, "users", ALICE), validProfile(ALICE)));
  await assertSucceeds(setDoc(doc(aliceDb, "users", ALICE, "accounts", "acc_1"), validAccount(ALICE)));
  await assertSucceeds(setDoc(doc(aliceDb, "users", ALICE, "transactions", "tx_101"), validTransaction(ALICE)));

  await assertFails(getDoc(doc(bobDb, "users", ALICE)));
  await assertFails(getDocs(collection(bobDb, "users")));
  await assertFails(getDocs(collection(bobDb, "users", ALICE, "accounts")));
  await assertFails(getDoc(doc(bobDb, "users", ALICE, "accounts", "acc_1")));
  await assertFails(setDoc(doc(bobDb, "users", ALICE, "accounts", "acc_2"), validAccount(BOB)));
  await assertFails(updateDoc(doc(bobDb, "users", ALICE, "accounts", "acc_1"), { balance: 0, updatedAt: serverTimestamp() }));
  await assertFails(deleteDoc(doc(bobDb, "users", ALICE, "accounts", "acc_1")));
  await assertFails(getDocs(collection(bobDb, "users", ALICE, "transactions")));
});

test("allows owner CRUD & partial updates across profile, accounts, transactions, goals, loans, and budgets", async () => {
  const aliceDb = dbFor(ALICE);

  // Profile
  const profileRef = doc(aliceDb, "users", ALICE);
  await assertSucceeds(setDoc(profileRef, validProfile(ALICE)));
  await assertSucceeds(getDoc(profileRef));
  await assertSucceeds(updateDoc(profileRef, { displayName: "Shariful Updated", updatedAt: serverTimestamp() }));

  // Accounts / Cards
  const accRef = doc(aliceDb, "users", ALICE, "accounts", "acc_1");
  await assertSucceeds(setDoc(accRef, validAccount(ALICE)));
  await assertSucceeds(getDocs(collection(aliceDb, "users", ALICE, "accounts")));
  await assertSucceeds(updateDoc(accRef, { balance: 48500.5, updatedAt: serverTimestamp() }));

  // Transactions (expense without optional toAccountLocalId + transfer with toAccountLocalId)
  const txRef = doc(aliceDb, "users", ALICE, "transactions", "tx_101");
  await assertSucceeds(setDoc(txRef, validTransaction(ALICE, false)));
  await assertSucceeds(setDoc(doc(aliceDb, "users", ALICE, "transactions", "tx_102"), validTransaction(ALICE, true)));
  await assertSucceeds(getDocs(collection(aliceDb, "users", ALICE, "transactions")));
  await assertSucceeds(updateDoc(txRef, { amount: 1650.0, updatedAt: serverTimestamp() }));
  await assertSucceeds(deleteDoc(txRef));

  // Goals
  const goalRef = doc(aliceDb, "users", ALICE, "goals", "goal_10");
  await assertSucceeds(setDoc(goalRef, validGoal(ALICE)));
  await assertSucceeds(getDocs(collection(aliceDb, "users", ALICE, "goals")));
  await assertSucceeds(updateDoc(goalRef, { savedAmount: 35000, updatedAt: serverTimestamp() }));
  await assertSucceeds(deleteDoc(goalRef));

  // Loans
  const loanRef = doc(aliceDb, "users", ALICE, "loans", "loan_20");
  await assertSucceeds(setDoc(loanRef, validLoan(ALICE)));
  await assertSucceeds(getDocs(collection(aliceDb, "users", ALICE, "loans")));
  await assertSucceeds(updateDoc(loanRef, { paidAmount: 5000, updatedAt: serverTimestamp() }));
  await assertSucceeds(deleteDoc(loanRef));

  // Budgets
  const budgetRef = doc(aliceDb, "users", ALICE, "budgets", "budget_GROCERIES");
  await assertSucceeds(setDoc(budgetRef, validBudget(ALICE)));
  await assertSucceeds(getDocs(collection(aliceDb, "users", ALICE, "budgets")));
  await assertSucceeds(updateDoc(budgetRef, { limitAmount: 18000, updatedAt: serverTimestamp() }));
  await assertSucceeds(deleteDoc(budgetRef));

  await assertSucceeds(deleteDoc(accRef));
});

test("rejects shadow-field injection, immutable createdAt/userId mutation, future timestamps, and invalid amounts", async () => {
  const aliceDb = dbFor(ALICE);
  const accRef = doc(aliceDb, "users", ALICE, "accounts", "acc_1");

  // Shadow field injection (including unexpected "id" or "isAdmin")
  await assertFails(setDoc(accRef, { ...validAccount(ALICE), isAdmin: true }));
  await assertFails(setDoc(accRef, { ...validAccount(ALICE), id: "acc_1" }));

  // Oversized string
  await assertFails(setDoc(accRef, { ...validAccount(ALICE), name: "X".repeat(121) }));

  // Future timestamp spoofing
  const futureTs = Timestamp.fromMillis(Date.now() + 3600_000);
  await assertFails(setDoc(accRef, { ...validAccount(ALICE), createdAt: futureTs }));

  // Invalid zero/negative transaction amount
  await assertFails(setDoc(doc(aliceDb, "users", ALICE, "transactions", "tx_bad"), {
    ...validTransaction(ALICE),
    amount: 0,
  }));

  // Valid create, then immutable createdAt / userId update rejection
  await assertSucceeds(setDoc(accRef, validAccount(ALICE)));
  await assertFails(updateDoc(accRef, { createdAt: Timestamp.fromMillis(1700000000000), updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(accRef, { userId: BOB, updatedAt: serverTimestamp() }));
});
