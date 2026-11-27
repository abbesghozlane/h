package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.CreditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LookupEntity
import com.example.data.local.LookupTypes
import com.example.data.local.LoyaltyModes
import com.example.data.local.PaymentMethods
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.local.SaleWithItems
import com.example.data.local.ShopSettingsEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class CloudSnapshotBundle(
    val products: List<ProductEntity>,
    val lookups: List<LookupEntity>,
    val customers: List<CustomerEntity>,
    val creditLogs: List<CreditLogEntity>,
    val salesWithItems: List<SaleWithItems>,
    val settings: ShopSettingsEntity?
)

/**
 * Cloud backup and sync adapter for Firebase Firestore Enterprise named database.
 * Strictly follows zero-trust rules parity:
 * - Always resolves named DB via R.string.firestore_database_id
 * - Requires authenticated Google UID (never anonymous auth)
 * - Uses partial updates preserving immutable createdAt & userId
 * - Strips null optional fields & never writes @DocumentId 'id' inside doc payload
 */
class FirestoreSyncService(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userRoot(uid: String): DocumentReference = db.collection("users").document(uid)

    private suspend fun upsertPreservingCreatedAt(
        docRef: DocumentReference,
        mutableFields: Map<String, Any?>,
        createOnlyFields: Map<String, Any?>
    ) {
        val cleanMutable = mutableFields.filterValues { it != null }.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(cleanMutable).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                val createPayload = (createOnlyFields + mutableFields)
                    .filterValues { it != null }
                    .toMutableMap()
                    .apply {
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                docRef.set(createPayload).await()
            } else {
                throw e
            }
        }
    }

    suspend fun syncProduct(product: ProductEntity): Result<Unit> {
        var path = "users/unauthenticated/products/${product.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("products").document(product.id)
            path = ref.path
            val mutable = mapOf<String, Any?>(
                "name" to product.name.trim().take(120),
                "subtitle" to product.subtitle?.trim()?.take(160)?.takeIf { it.isNotEmpty() },
                "barcode" to product.barcode?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
                "category" to product.category.trim().take(60),
                "unitLabel" to product.unitLabel.trim().take(40),
                "buyPrice" to product.buyPrice.coerceAtLeast(0.0),
                "sellPrice" to product.sellPrice.coerceAtLeast(0.0),
                "currentStock" to product.currentStock.coerceAtLeast(0.0),
                "minStockAlert" to product.minStockAlert.coerceAtLeast(0.0),
                "imageKey" to product.imageKey.take(60)
            )
            upsertPreservingCreatedAt(ref, mutable, mapOf("userId" to uid))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        var path = "users/unauthenticated/products/$productId"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("products").document(productId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncLookup(lookup: LookupEntity): Result<Unit> {
        var path = "users/unauthenticated/lookups/${lookup.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("lookups").document(lookup.id)
            path = ref.path
            val validType = when (lookup.lookupType) {
                LookupTypes.CATEGORY, LookupTypes.UNIT, LookupTypes.QUICK_SIZE -> lookup.lookupType
                else -> LookupTypes.CATEGORY
            }
            val mutable = mapOf<String, Any?>(
                "lookupType" to validType,
                "label" to lookup.label.trim().take(60),
                "sortOrder" to lookup.sortOrder.coerceIn(0, 10000)
            )
            upsertPreservingCreatedAt(ref, mutable, mapOf("userId" to uid))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLookup(lookupId: String): Result<Unit> {
        var path = "users/unauthenticated/lookups/$lookupId"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("lookups").document(lookupId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncCustomer(customer: CustomerEntity): Result<Unit> {
        var path = "users/unauthenticated/customers/${customer.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("customers").document(customer.id)
            path = ref.path
            val validMode = if (customer.loyaltyMode == LoyaltyModes.POINTS) {
                LoyaltyModes.POINTS
            } else {
                LoyaltyModes.STAMPS
            }
            val mutable = mapOf<String, Any?>(
                "loyaltyCode" to customer.loyaltyCode.trim().take(32),
                "name" to customer.name.trim().take(120),
                "phone" to customer.phone?.trim()?.take(40)?.takeIf { it.isNotEmpty() },
                "notes" to customer.notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                "creditBalance" to customer.creditBalance.coerceAtLeast(0.0),
                "stamps" to customer.stamps.coerceIn(0, 1000),
                "points" to customer.points.coerceIn(0, 10000000),
                "loyaltyMode" to validMode
            )
            upsertPreservingCreatedAt(ref, mutable, mapOf("userId" to uid))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        var path = "users/unauthenticated/customers/$customerId"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("customers").document(customerId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun syncCreditLog(log: CreditLogEntity): Result<Unit> {
        var path = "users/unauthenticated/creditLogs/${log.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("creditLogs").document(log.id)
            path = ref.path
            val payload = mapOf<String, Any>(
                "userId" to uid,
                "customerId" to log.customerId.take(128),
                "amountDelta" to log.amountDelta,
                "note" to log.note.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncSale(sale: SaleEntity, items: List<SaleItemEntity>): Result<Unit> {
        var path = "users/unauthenticated/sales/${sale.id}"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("sales").document(sale.id)
            path = ref.path
            val validPayment = if (sale.paymentMethod == PaymentMethods.CREDIT) {
                PaymentMethods.CREDIT
            } else {
                PaymentMethods.CASH
            }
            val itemMaps = items.take(100).map { item ->
                buildMap<String, Any> {
                    put("id", item.id)
                    if (!item.productId.isNullOrBlank()) put("productId", item.productId)
                    put("itemName", item.itemName.take(120))
                    put("unitLabel", item.unitLabel.take(40))
                    put("quantity", item.quantity)
                    put("costUnitPrice", item.costUnitPrice)
                    put("actualUnitPrice", item.actualUnitPrice)
                    put("lineDiscount", item.lineDiscount)
                    put("totalPrice", item.totalPrice)
                    put("soldByAmount", item.soldByAmount)
                }
            }
            val mutable = mapOf<String, Any?>(
                "invoiceNumber" to sale.invoiceNumber.take(40),
                "customerId" to sale.customerId?.take(128)?.takeIf { it.isNotBlank() },
                "customerNameSnapshot" to sale.customerNameSnapshot?.take(120)?.takeIf { it.isNotBlank() },
                "paymentMethod" to validPayment,
                "subtotal" to sale.subtotal.coerceAtLeast(0.0),
                "discount" to sale.discount.coerceAtLeast(0.0),
                "totalAmount" to sale.totalAmount.coerceAtLeast(0.0),
                "estimatedProfit" to sale.estimatedProfit,
                "stampAwarded" to sale.stampAwarded,
                "returned" to sale.returned,
                "items" to itemMaps
            )
            upsertPreservingCreatedAt(ref, mutable, mapOf("userId" to uid))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncSettings(settings: ShopSettingsEntity): Result<Unit> {
        var path = "users/unauthenticated/settings/shop"
        return try {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("settings").document("shop")
            path = ref.path
            val validMode = if (settings.defaultLoyaltyMode == LoyaltyModes.POINTS) {
                LoyaltyModes.POINTS
            } else {
                LoyaltyModes.STAMPS
            }
            val mutable = mapOf<String, Any?>(
                "shopName" to settings.shopName.trim().take(80),
                "currencySymbol" to settings.currencySymbol.trim().take(16),
                "defaultLoyaltyMode" to validMode,
                "stampTarget" to settings.stampTarget.coerceIn(1, 50),
                "rewardText" to settings.rewardText.trim().take(160)
            )
            upsertPreservingCreatedAt(ref, mutable, mapOf("userId" to uid))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun pullAllFromCloud(): Result<CloudSnapshotBundle> {
        var currentPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val root = userRoot(uid)

            currentPath = "${root.path}/products"
            val productsSnap = root.collection("products").get().await()
            val products = productsSnap.documents.mapNotNull { doc -> parseProduct(doc) }

            currentPath = "${root.path}/lookups"
            val lookupsSnap = root.collection("lookups").get().await()
            val lookups = lookupsSnap.documents.mapNotNull { doc -> parseLookup(doc) }

            currentPath = "${root.path}/customers"
            val customersSnap = root.collection("customers").get().await()
            val customers = customersSnap.documents.mapNotNull { doc -> parseCustomer(doc) }

            currentPath = "${root.path}/creditLogs"
            val logsSnap = root.collection("creditLogs").get().await()
            val creditLogs = logsSnap.documents.mapNotNull { doc -> parseCreditLog(doc) }

            currentPath = "${root.path}/sales"
            val salesSnap = root.collection("sales").get().await()
            val sales = salesSnap.documents.mapNotNull { doc -> parseSaleWithItems(doc) }

            currentPath = "${root.path}/settings/shop"
            val settingsDoc = root.collection("settings").document("shop").get().await()
            val settings = if (settingsDoc.exists()) parseSettings(settingsDoc) else null

            Result.success(
                CloudSnapshotBundle(
                    products = products,
                    lookups = lookups,
                    customers = customers,
                    creditLogs = creditLogs,
                    salesWithItems = sales,
                    settings = settings
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, currentPath)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.timestampMillis(field: String): Long {
        val ts = getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return ts?.toDate()?.time ?: System.currentTimeMillis()
    }

    private fun parseProduct(doc: DocumentSnapshot): ProductEntity? {
        val data = doc.data ?: return null
        val name = data["name"] as? String ?: return null
        return ProductEntity(
            id = doc.id,
            name = name,
            subtitle = data["subtitle"] as? String,
            barcode = data["barcode"] as? String,
            category = (data["category"] as? String).orEmpty().ifBlank { "عطور شرقية" },
            unitLabel = (data["unitLabel"] as? String).orEmpty().ifBlank { "مل" },
            buyPrice = (data["buyPrice"] as? Number)?.toDouble() ?: 0.0,
            sellPrice = (data["sellPrice"] as? Number)?.toDouble() ?: 0.0,
            currentStock = (data["currentStock"] as? Number)?.toDouble() ?: 0.0,
            minStockAlert = (data["minStockAlert"] as? Number)?.toDouble() ?: 0.0,
            imageKey = (data["imageKey"] as? String).orEmpty().ifBlank { "royal_oud" },
            createdAtMillis = doc.timestampMillis("createdAt"),
            updatedAtMillis = doc.timestampMillis("updatedAt")
        )
    }

    private fun parseLookup(doc: DocumentSnapshot): LookupEntity? {
        val data = doc.data ?: return null
        val label = data["label"] as? String ?: return null
        return LookupEntity(
            id = doc.id,
            lookupType = (data["lookupType"] as? String) ?: LookupTypes.CATEGORY,
            label = label,
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAtMillis = doc.timestampMillis("updatedAt")
        )
    }

    private fun parseCustomer(doc: DocumentSnapshot): CustomerEntity? {
        val data = doc.data ?: return null
        val name = data["name"] as? String ?: return null
        val loyaltyCode = data["loyaltyCode"] as? String ?: "PRF-1000"
        return CustomerEntity(
            id = doc.id,
            loyaltyCode = loyaltyCode,
            name = name,
            phone = data["phone"] as? String,
            creditBalance = (data["creditBalance"] as? Number)?.toDouble() ?: 0.0,
            stamps = (data["stamps"] as? Number)?.toInt() ?: 0,
            points = (data["points"] as? Number)?.toInt() ?: 0,
            loyaltyMode = (data["loyaltyMode"] as? String) ?: LoyaltyModes.STAMPS,
            notes = data["notes"] as? String,
            createdAtMillis = doc.timestampMillis("createdAt"),
            updatedAtMillis = doc.timestampMillis("updatedAt")
        )
    }

    private fun parseCreditLog(doc: DocumentSnapshot): CreditLogEntity? {
        val data = doc.data ?: return null
        val customerId = data["customerId"] as? String ?: return null
        return CreditLogEntity(
            id = doc.id,
            customerId = customerId,
            amountDelta = (data["amountDelta"] as? Number)?.toDouble() ?: 0.0,
            note = (data["note"] as? String).orEmpty(),
            createdAtMillis = doc.timestampMillis("createdAt")
        )
    }

    private fun parseSaleWithItems(doc: DocumentSnapshot): SaleWithItems? {
        val data = doc.data ?: return null
        val invoiceNumber = data["invoiceNumber"] as? String ?: return null
        val sale = SaleEntity(
            id = doc.id,
            invoiceNumber = invoiceNumber,
            customerId = data["customerId"] as? String,
            customerNameSnapshot = data["customerNameSnapshot"] as? String,
            paymentMethod = (data["paymentMethod"] as? String) ?: PaymentMethods.CASH,
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            discount = (data["discount"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            estimatedProfit = (data["estimatedProfit"] as? Number)?.toDouble() ?: 0.0,
            stampAwarded = (data["stampAwarded"] as? Boolean) ?: false,
            returned = (data["returned"] as? Boolean) ?: false,
            createdAtMillis = doc.timestampMillis("createdAt"),
            updatedAtMillis = doc.timestampMillis("updatedAt")
        )

        val rawItems = data["items"] as? List<*> ?: emptyList<Any>()
        val items = rawItems.mapIndexedNotNull { index, element ->
            val map = element as? Map<*, *> ?: return@mapIndexedNotNull null
            SaleItemEntity(
                id = (map["id"] as? String)?.ifBlank { "${doc.id}_item_$index" } ?: "${doc.id}_item_$index",
                saleId = doc.id,
                productId = map["productId"] as? String,
                itemName = (map["itemName"] as? String).orEmpty(),
                unitLabel = (map["unitLabel"] as? String).orEmpty().ifBlank { "مل" },
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0,
                costUnitPrice = (map["costUnitPrice"] as? Number)?.toDouble() ?: 0.0,
                actualUnitPrice = (map["actualUnitPrice"] as? Number)?.toDouble() ?: 0.0,
                lineDiscount = (map["lineDiscount"] as? Number)?.toDouble() ?: 0.0,
                totalPrice = (map["totalPrice"] as? Number)?.toDouble() ?: 0.0,
                soldByAmount = (map["soldByAmount"] as? Boolean) ?: false
            )
        }
        return SaleWithItems(sale = sale, items = items)
    }

    private fun parseSettings(doc: DocumentSnapshot): ShopSettingsEntity? {
        val data = doc.data ?: return null
        return ShopSettingsEntity(
            id = "shop",
            shopName = (data["shopName"] as? String).orEmpty().ifBlank { "حيدر للعطور" },
            currencySymbol = (data["currencySymbol"] as? String).orEmpty().ifBlank { "د.ج" },
            defaultLoyaltyMode = (data["defaultLoyaltyMode"] as? String) ?: LoyaltyModes.STAMPS,
            stampTarget = (data["stampTarget"] as? Number)?.toInt() ?: 10,
            rewardText = (data["rewardText"] as? String).orEmpty()
                .ifBlank { "بعد إكمال 10 ختمات .. تحصل على عطر هدية" },
            updatedAtMillis = doc.timestampMillis("updatedAt")
        )
    }
}
