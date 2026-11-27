const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "haider_owner_001";
const OTHER_UID = "other_shop_999";

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

test("Unauthenticated users cannot read or write any Haider Parfums collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(OWNER_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(OWNER_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(OWNER_UID).collection("sales").get());
  await assertFails(
    unauthDb.collection("users").doc(OWNER_UID).collection("products").doc("prod_1").set({
      userId: OWNER_UID,
      name: "رويال عود",
      category: "عطور شرقية",
      unitLabel: "مل",
      buyPrice: 40,
      sellPrice: 95,
      currentStock: 250.5,
      minStockAlert: 50,
      imageKey: "royal_oud",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can create, list, update, and delete products; ghost fields and immutable changes fail", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const productsRef = ownerDb.collection("users").doc(OWNER_UID).collection("products");

  await assertSucceeds(productsRef.get());

  await assertSucceeds(
    productsRef.doc("prod_royal_oud").set({
      userId: OWNER_UID,
      name: "رويال عود فاخر",
      subtitle: "Royal Oud Prestige",
      barcode: "HP-1001",
      category: "عطور شرقية",
      unitLabel: "مل",
      buyPrice: 55.0,
      sellPrice: 120.0,
      currentStock: 450.5,
      minStockAlert: 50.0,
      imageKey: "royal_oud",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    productsRef.doc("prod_royal_oud").update({
      currentStock: 415.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt overwrite
  await assertFails(
    productsRef.doc("prod_royal_oud").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow/ghost field injection & @DocumentId 'id' field
  await assertFails(
    productsRef.doc("prod_ghost").set({
      id: "prod_ghost",
      userId: OWNER_UID,
      name: "Ghost",
      category: "رجالي",
      unitLabel: "مل",
      buyPrice: 10,
      sellPrice: 20,
      currentStock: 10,
      minStockAlert: 2,
      imageKey: "royal_oud",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    productsRef.doc("prod_future").set({
      userId: OWNER_UID,
      name: "Future",
      category: "رجالي",
      unitLabel: "مل",
      buyPrice: 10,
      sellPrice: 20,
      currentStock: 10,
      minStockAlert: 2,
      imageKey: "royal_oud",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(productsRef.doc("prod_royal_oud").delete());
});

test("Cross-user isolation blocks other authenticated users from reading or mutating owner data", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(OWNER_UID).collection("customers").doc("cust_1").set({
      userId: OWNER_UID,
      loyaltyCode: "PRF-1001",
      name: "محمد بن علي",
      phone: "0550123456",
      creditBalance: 1200,
      stamps: 8,
      points: 80,
      loyaltyMode: "STAMPS",
      createdAt: created,
      updatedAt: created,
    });
  });

  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(otherDb.collection("users").doc(OWNER_UID).collection("customers").get());
  await assertFails(otherDb.collection("users").doc(OWNER_UID).collection("customers").doc("cust_1").get());
  await assertFails(
    otherDb.collection("users").doc(OWNER_UID).collection("customers").doc("cust_1").update({
      stamps: 10,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can manage customers, credit logs, editable sales, lookups, and shop settings", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const userRoot = ownerDb.collection("users").doc(OWNER_UID);

  // Customer create + stamp update
  await assertSucceeds(
    userRoot.collection("customers").doc("cust_1").set({
      userId: OWNER_UID,
      loyaltyCode: "PRF-2026",
      name: "محمد بن علي",
      creditBalance: 0,
      stamps: 8,
      points: 80,
      loyaltyMode: "STAMPS",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    userRoot.collection("customers").doc("cust_1").update({
      stamps: 9,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Credit log create (updates forbidden)
  await assertSucceeds(
    userRoot.collection("creditLogs").doc("log_1").set({
      userId: OWNER_UID,
      customerId: "cust_1",
      amountDelta: -500,
      note: "تسديد دفعة نقدية",
      createdAt: created,
    })
  );
  await assertFails(
    userRoot.collection("creditLogs").doc("log_1").update({
      amountDelta: -100,
    })
  );

  // Sale create + editable invoice update
  await assertSucceeds(
    userRoot.collection("sales").doc("sale_1").set({
      userId: OWNER_UID,
      invoiceNumber: "INV-1001",
      customerId: "cust_1",
      customerNameSnapshot: "محمد بن علي",
      paymentMethod: "CASH",
      subtotal: 3372.5,
      discount: 200.0,
      totalAmount: 3172.5,
      estimatedProfit: 1450.0,
      stampAwarded: true,
      returned: false,
      items: [
        {
          id: "item_1",
          productId: "prod_1",
          itemName: "توباكو فانيلا",
          unitLabel: "مل",
          quantity: 35.5,
          costUnitPrice: 45.0,
          actualUnitPrice: 95.0,
          lineDiscount: 0.0,
          totalPrice: 3372.5,
          soldByAmount: false,
        },
      ],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    userRoot.collection("sales").doc("sale_1").update({
      returned: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Lookups & settings
  await assertSucceeds(
    userRoot.collection("lookups").doc("cat_1").set({
      userId: OWNER_UID,
      lookupType: "CATEGORY",
      label: "عطور شرقية",
      sortOrder: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    userRoot.collection("settings").doc("shop").set({
      userId: OWNER_UID,
      shopName: "حيدر للعطور",
      currencySymbol: "د.ج",
      defaultLoyaltyMode: "STAMPS",
      stampTarget: 10,
      rewardText: "بعد إكمال 10 ختمات .. تحصل على عطر هدية",
      createdAt: created,
      updatedAt: created,
    })
  );
});
