package com.example.data.repository

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
import com.example.data.local.ShopDao
import com.example.data.local.ShopSettingsEntity
import com.example.data.remote.FirestoreSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Synced(val timestampMillis: Long = System.currentTimeMillis()) : SyncState()
    data class OfflineOrError(val message: String) : SyncState()
}

data class CartLineDraft(
    val id: String = UUID.randomUUID().toString(),
    val productId: String? = null,
    val itemName: String,
    val unitLabel: String,
    val quantity: Double,
    val costUnitPrice: Double,
    val actualUnitPrice: Double,
    val lineDiscount: Double = 0.0,
    val soldByAmount: Boolean = false
) {
    val lineTotal: Double
        get() = max(0.0, (quantity * actualUnitPrice) - lineDiscount)

    val lineProfit: Double
        get() = lineTotal - (costUnitPrice * quantity)
}

data class CheckoutResult(
    val sale: SaleEntity,
    val updatedCustomer: CustomerEntity?,
    val reachedRewardGoal: Boolean
)

/**
 * Unified Offline-First ShopRepository:
 * - Room SQLite is the immediate zero-lag local source of truth.
 * - Every mutation mirrors asynchronously to Firebase Firestore for multi-device backup.
 */
class ShopRepository(
    private val dao: ShopDao,
    private val cloudSync: FirestoreSyncService
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val products: Flow<List<ProductEntity>> = dao.observeProducts()
    val lookups: Flow<List<LookupEntity>> = dao.observeLookups()
    val customers: Flow<List<CustomerEntity>> = dao.observeCustomers()
    val creditLogs: Flow<List<CreditLogEntity>> = dao.observeAllCreditLogs()
    val salesWithItems: Flow<List<SaleWithItems>> = dao.observeSalesWithItems()
    val shopSettings: Flow<ShopSettingsEntity> = dao.observeShopSettings().map {
        it ?: ShopSettingsEntity()
    }

    suspend fun initializeAndSync(): Result<Unit> {
        _syncState.value = SyncState.Syncing
        val cloudResult = cloudSync.pullAllFromCloud()
        cloudResult.onSuccess { bundle ->
            val hasCloudData = bundle.products.isNotEmpty() ||
                bundle.customers.isNotEmpty() ||
                bundle.salesWithItems.isNotEmpty()

            if (hasCloudData) {
                if (bundle.products.isNotEmpty()) dao.upsertProducts(bundle.products)
                if (bundle.lookups.isNotEmpty()) dao.upsertLookups(bundle.lookups)
                if (bundle.customers.isNotEmpty()) dao.upsertCustomers(bundle.customers)
                if (bundle.creditLogs.isNotEmpty()) dao.upsertCreditLogs(bundle.creditLogs)
                bundle.salesWithItems.forEach { swi ->
                    dao.saveSaleWithItems(swi.sale, swi.items)
                }
                bundle.settings?.let { dao.upsertShopSettings(it) }
                if (dao.countLookups() == 0) seedDefaultLookups()
                if (dao.getShopSettings() == null) dao.upsertShopSettings(ShopSettingsEntity())
            } else if (dao.countProducts() == 0 && dao.countCustomers() == 0) {
                seedInitialSampleDataAndBackup()
            } else {
                pushAllLocalToCloud()
            }
            _syncState.value = SyncState.Synced()
        }.onFailure { err ->
            // Offline-first fallback: seed locally if fresh install so POS works immediately with zero lag
            if (dao.countProducts() == 0 && dao.countCustomers() == 0) {
                seedInitialSampleDataLocalOnly()
            }
            _syncState.value = SyncState.OfflineOrError(err.localizedMessage ?: "وضع غير متصل")
        }
        return cloudResult.map { Unit }
    }

    suspend fun manualFullSync(): Result<Unit> {
        _syncState.value = SyncState.Syncing
        val pushOk = pushAllLocalToCloud()
        val pullResult = cloudSync.pullAllFromCloud()
        pullResult.onSuccess { bundle ->
            if (bundle.products.isNotEmpty()) dao.upsertProducts(bundle.products)
            if (bundle.lookups.isNotEmpty()) dao.upsertLookups(bundle.lookups)
            if (bundle.customers.isNotEmpty()) dao.upsertCustomers(bundle.customers)
            if (bundle.creditLogs.isNotEmpty()) dao.upsertCreditLogs(bundle.creditLogs)
            bundle.salesWithItems.forEach { dao.saveSaleWithItems(it.sale, it.items) }
            bundle.settings?.let { dao.upsertShopSettings(it) }
            _syncState.value = if (pushOk) SyncState.Synced() else SyncState.OfflineOrError("مزامنة جزئية")
        }.onFailure { err ->
            _syncState.value = SyncState.OfflineOrError(err.localizedMessage ?: "تعذرت المزامنة")
        }
        return pullResult.map { Unit }
    }

    private suspend fun pushAllLocalToCloud(): Boolean {
        var allOk = true
        // Read current settings & lookups if needed via sync calls triggered by mutations
        dao.getShopSettings()?.let { if (cloudSync.syncSettings(it).isFailure) allOk = false }
        return allOk
    }

    private fun launchCloudMirror(block: suspend () -> Result<Unit>) {
        syncScope.launch {
            _syncState.value = SyncState.Syncing
            val res = block()
            _syncState.value = if (res.isSuccess) {
                SyncState.Synced()
            } else {
                SyncState.OfflineOrError("محفوظ محلياً • بانتظار المزامنة")
            }
        }
    }

    // --- PRODUCTS ---
    suspend fun saveProduct(product: ProductEntity) {
        val stamped = product.copy(updatedAtMillis = System.currentTimeMillis())
        dao.upsertProduct(stamped)
        launchCloudMirror { cloudSync.syncProduct(stamped) }
    }

    suspend fun deleteProduct(productId: String) {
        dao.deleteProductById(productId)
        launchCloudMirror { cloudSync.deleteProduct(productId) }
    }

    // --- LOOKUPS (Editable Categories, Units, Quick Sizes) ---
    suspend fun addLookup(type: String, label: String, sortOrder: Int = 99) {
        val clean = label.trim()
        if (clean.isEmpty()) return
        val entry = LookupEntity(
            id = "lkp_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            lookupType = type,
            label = clean,
            sortOrder = sortOrder,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertLookup(entry)
        launchCloudMirror { cloudSync.syncLookup(entry) }
    }

    suspend fun deleteLookup(lookupId: String) {
        dao.deleteLookupById(lookupId)
        launchCloudMirror { cloudSync.deleteLookup(lookupId) }
    }

    // --- CUSTOMERS & LOYALTY ---
    suspend fun saveCustomer(customer: CustomerEntity) {
        val stamped = customer.copy(updatedAtMillis = System.currentTimeMillis())
        dao.upsertCustomer(stamped)
        launchCloudMirror { cloudSync.syncCustomer(stamped) }
    }

    suspend fun findCustomerByCode(code: String): CustomerEntity? {
        return dao.findCustomerByLoyaltyCode(code.trim())
    }

    suspend fun adjustLoyalty(
        customerId: String,
        stampsDelta: Int = 0,
        pointsDelta: Int = 0,
        resetStamps: Boolean = false
    ): CustomerEntity? {
        val current = dao.getCustomerById(customerId) ?: return null
        val updated = current.copy(
            stamps = if (resetStamps) 0 else (current.stamps + stampsDelta).coerceAtLeast(0),
            points = (current.points + pointsDelta).coerceAtLeast(0),
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertCustomer(updated)
        launchCloudMirror { cloudSync.syncCustomer(updated) }
        return updated
    }

    suspend fun toggleCustomerLoyaltyMode(customerId: String) {
        val current = dao.getCustomerById(customerId) ?: return
        val nextMode = if (current.loyaltyMode == LoyaltyModes.STAMPS) {
            LoyaltyModes.POINTS
        } else {
            LoyaltyModes.STAMPS
        }
        val updated = current.copy(
            loyaltyMode = nextMode,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertCustomer(updated)
        launchCloudMirror { cloudSync.syncCustomer(updated) }
    }

    suspend fun recordCreditAdjustment(
        customerId: String,
        amountDelta: Double,
        note: String
    ): CustomerEntity? {
        val current = dao.getCustomerById(customerId) ?: return null
        val newBalance = max(0.0, current.creditBalance + amountDelta)
        val updated = current.copy(
            creditBalance = newBalance,
            updatedAtMillis = System.currentTimeMillis()
        )
        val log = CreditLogEntity(
            id = "crd_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            customerId = customerId,
            amountDelta = amountDelta,
            note = note.ifBlank { if (amountDelta < 0) "تسديد دفعة كريدي" else "إضافة كريدي" },
            createdAtMillis = System.currentTimeMillis()
        )
        dao.upsertCustomer(updated)
        dao.upsertCreditLog(log)
        launchCloudMirror {
            cloudSync.syncCustomer(updated)
            cloudSync.syncCreditLog(log)
        }
        return updated
    }

    suspend fun deleteCustomer(customerId: String) {
        dao.deleteCustomerById(customerId)
        launchCloudMirror { cloudSync.deleteCustomer(customerId) }
    }

    // --- CHECKOUT (Atomic Stock Deduction + Credit + Per-Purchase QR Loyalty Stamp) ---
    suspend fun completeCheckout(
        drafts: List<CartLineDraft>,
        customerId: String?,
        paymentMethod: String,
        cartDiscount: Double
    ): CheckoutResult {
        val settings = dao.getShopSettings() ?: ShopSettingsEntity()
        val now = System.currentTimeMillis()
        val saleId = "sal_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val invoiceNum = "HP-${(1000 + dao.countSales() + 1)}"

        val subtotal = drafts.sumOf { it.lineTotal }
        val safeDiscount = cartDiscount.coerceIn(0.0, subtotal)
        val netTotal = max(0.0, subtotal - safeDiscount)
        val totalCost = drafts.sumOf { it.costUnitPrice * it.quantity }
        val estimatedProfit = netTotal - totalCost

        val customer = customerId?.let { dao.getCustomerById(it) }
        val stampAwarded = customer != null

        val sale = SaleEntity(
            id = saleId,
            invoiceNumber = invoiceNum,
            customerId = customer?.id,
            customerNameSnapshot = customer?.name,
            paymentMethod = paymentMethod,
            subtotal = subtotal,
            discount = safeDiscount,
            totalAmount = netTotal,
            estimatedProfit = estimatedProfit,
            stampAwarded = stampAwarded,
            returned = false,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        val saleItems = drafts.mapIndexed { idx, d ->
            SaleItemEntity(
                id = "${saleId}_ln_$idx",
                saleId = saleId,
                productId = d.productId,
                itemName = d.itemName,
                unitLabel = d.unitLabel,
                quantity = d.quantity,
                costUnitPrice = d.costUnitPrice,
                actualUnitPrice = d.actualUnitPrice,
                lineDiscount = d.lineDiscount,
                totalPrice = d.lineTotal,
                soldByAmount = d.soldByAmount
            )
        }

        // 1. Save Sale & SaleItems locally
        dao.saveSaleWithItems(sale, saleItems)

        // 2. Deduct decimal stock for catalogued products
        val updatedProducts = mutableListOf<ProductEntity>()
        saleItems.forEach { item ->
            val pid = item.productId
            if (!pid.isNullOrBlank()) {
                dao.getProductById(pid)?.let { prod ->
                    val nextStock = max(0.0, prod.currentStock - item.quantity)
                    val updatedProd = prod.copy(
                        currentStock = nextStock,
                        updatedAtMillis = now
                    )
                    dao.upsertProduct(updatedProd)
                    updatedProducts.add(updatedProd)
                }
            }
        }

        // 3. Update attached customer: +1 stamp (or points) & credit balance if CREDIT
        var updatedCustomer: CustomerEntity? = null
        var creditLog: CreditLogEntity? = null
        var reachedRewardGoal = false

        if (customer != null) {
            val nextStamps = customer.stamps + 1
            val pointsGain = max(1, (netTotal / 100.0).roundToInt())
            val nextPoints = customer.points + pointsGain
            val nextCredit = if (paymentMethod == PaymentMethods.CREDIT) {
                customer.creditBalance + netTotal
            } else {
                customer.creditBalance
            }
            reachedRewardGoal = nextStamps >= settings.stampTarget
            updatedCustomer = customer.copy(
                stamps = nextStamps,
                points = nextPoints,
                creditBalance = nextCredit,
                updatedAtMillis = now
            )
            dao.upsertCustomer(updatedCustomer)

            if (paymentMethod == PaymentMethods.CREDIT && netTotal > 0.0) {
                creditLog = CreditLogEntity(
                    id = "crd_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                    customerId = customer.id,
                    amountDelta = netTotal,
                    note = "فاتورة آجل رقم $invoiceNum",
                    createdAtMillis = now
                )
                dao.upsertCreditLog(creditLog)
            }
        }

        // 4. Mirror asynchronously to Firebase Firestore
        launchCloudMirror {
            var lastResult: Result<Unit> = cloudSync.syncSale(sale, saleItems)
            updatedProducts.forEach { p ->
                val r = cloudSync.syncProduct(p)
                if (r.isFailure) lastResult = r
            }
            updatedCustomer?.let { c ->
                val r = cloudSync.syncCustomer(c)
                if (r.isFailure) lastResult = r
            }
            creditLog?.let { l ->
                val r = cloudSync.syncCreditLog(l)
                if (r.isFailure) lastResult = r
            }
            lastResult
        }

        return CheckoutResult(
            sale = sale,
            updatedCustomer = updatedCustomer,
            reachedRewardGoal = reachedRewardGoal
        )
    }

    // --- EDITABLE INVOICES & RETURNS (Auto-restores Stock & Updates Credit) ---
    suspend fun updateExistingInvoice(
        saleId: String,
        updatedItems: List<SaleItemEntity>,
        newPaymentMethod: String,
        newDiscount: Double
    ) {
        val original = dao.getSaleWithItems(saleId) ?: return
        if (original.sale.returned) return
        val now = System.currentTimeMillis()

        // Compute stock deltas per productId (oldQty - newQty)
        val oldQtyByProduct = original.items
            .filter { !it.productId.isNullOrBlank() }
            .groupBy { it.productId!! }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }

        val newQtyByProduct = updatedItems
            .filter { !it.productId.isNullOrBlank() }
            .groupBy { it.productId!! }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }

        val affectedProductIds = (oldQtyByProduct.keys + newQtyByProduct.keys).toSet()
        val updatedProducts = mutableListOf<ProductEntity>()
        affectedProductIds.forEach { pid ->
            val oldQty = oldQtyByProduct[pid] ?: 0.0
            val newQty = newQtyByProduct[pid] ?: 0.0
            val restoreDelta = oldQty - newQty
            if (restoreDelta != 0.0) {
                dao.getProductById(pid)?.let { prod ->
                    val nextProd = prod.copy(
                        currentStock = max(0.0, prod.currentStock + restoreDelta),
                        updatedAtMillis = now
                    )
                    dao.upsertProduct(nextProd)
                    updatedProducts.add(nextProd)
                }
            }
        }

        val subtotal = updatedItems.sumOf { it.totalPrice }
        val safeDiscount = newDiscount.coerceIn(0.0, subtotal)
        val newTotal = max(0.0, subtotal - safeDiscount)
        val totalCost = updatedItems.sumOf { it.costUnitPrice * it.quantity }
        val newProfit = newTotal - totalCost

        val updatedSale = original.sale.copy(
            paymentMethod = newPaymentMethod,
            subtotal = subtotal,
            discount = safeDiscount,
            totalAmount = newTotal,
            estimatedProfit = newProfit,
            updatedAtMillis = now
        )
        dao.saveSaleWithItems(updatedSale, updatedItems)

        // Update customer credit delta if customer attached
        var updatedCustomer: CustomerEntity? = null
        val cid = original.sale.customerId
        if (!cid.isNullOrBlank()) {
            val oldCreditImpact = if (original.sale.paymentMethod == PaymentMethods.CREDIT) {
                original.sale.totalAmount
            } else 0.0
            val newCreditImpact = if (newPaymentMethod == PaymentMethods.CREDIT) {
                newTotal
            } else 0.0
            val creditDelta = newCreditImpact - oldCreditImpact
            if (creditDelta != 0.0) {
                dao.getCustomerById(cid)?.let { cust ->
                    updatedCustomer = cust.copy(
                        creditBalance = max(0.0, cust.creditBalance + creditDelta),
                        updatedAtMillis = now
                    )
                    dao.upsertCustomer(updatedCustomer!!)
                }
            }
        }

        launchCloudMirror {
            var res = cloudSync.syncSale(updatedSale, updatedItems)
            updatedProducts.forEach { p ->
                val r = cloudSync.syncProduct(p)
                if (r.isFailure) res = r
            }
            updatedCustomer?.let { c ->
                val r = cloudSync.syncCustomer(c)
                if (r.isFailure) res = r
            }
            res
        }
    }

    suspend fun processInvoiceReturn(saleId: String) {
        val original = dao.getSaleWithItems(saleId) ?: return
        if (original.sale.returned) return
        val now = System.currentTimeMillis()

        // 1. Restore stock for all catalogued line items
        val restoredProducts = mutableListOf<ProductEntity>()
        original.items.forEach { item ->
            val pid = item.productId
            if (!pid.isNullOrBlank()) {
                dao.getProductById(pid)?.let { prod ->
                    val nextProd = prod.copy(
                        currentStock = prod.currentStock + item.quantity,
                        updatedAtMillis = now
                    )
                    dao.upsertProduct(nextProd)
                    restoredProducts.add(nextProd)
                }
            }
        }

        // 2. Mark invoice returned
        val returnedSale = original.sale.copy(
            returned = true,
            updatedAtMillis = now
        )
        dao.upsertSale(returnedSale)

        // 3. Reverse credit & awarded stamp on customer if linked
        var updatedCustomer: CustomerEntity? = null
        var creditLog: CreditLogEntity? = null
        val cid = original.sale.customerId
        if (!cid.isNullOrBlank()) {
            dao.getCustomerById(cid)?.let { cust ->
                val creditReduction = if (original.sale.paymentMethod == PaymentMethods.CREDIT) {
                    original.sale.totalAmount
                } else 0.0
                val nextStamps = if (original.sale.stampAwarded) {
                    max(0, cust.stamps - 1)
                } else cust.stamps
                updatedCustomer = cust.copy(
                    creditBalance = max(0.0, cust.creditBalance - creditReduction),
                    stamps = nextStamps,
                    updatedAtMillis = now
                )
                dao.upsertCustomer(updatedCustomer!!)
                if (creditReduction > 0.0) {
                    creditLog = CreditLogEntity(
                        id = "crd_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                        customerId = cid,
                        amountDelta = -creditReduction,
                        note = "مرتجع فاتورة رقم ${original.sale.invoiceNumber}",
                        createdAtMillis = now
                    )
                    dao.upsertCreditLog(creditLog!!)
                }
            }
        }

        launchCloudMirror {
            var res = cloudSync.syncSale(returnedSale, original.items)
            restoredProducts.forEach { p ->
                val r = cloudSync.syncProduct(p)
                if (r.isFailure) res = r
            }
            updatedCustomer?.let { c ->
                val r = cloudSync.syncCustomer(c)
                if (r.isFailure) res = r
            }
            creditLog?.let { l ->
                val r = cloudSync.syncCreditLog(l)
                if (r.isFailure) res = r
            }
            res
        }
    }

    // --- SHOP SETTINGS ---
    suspend fun saveShopSettings(settings: ShopSettingsEntity) {
        val stamped = settings.copy(id = "shop", updatedAtMillis = System.currentTimeMillis())
        dao.upsertShopSettings(stamped)
        launchCloudMirror { cloudSync.syncSettings(stamped) }
    }

    // --- SAMPLE DATA SEEDING (Haider Parfums Arabic Boutique) ---
    private suspend fun seedDefaultLookups(): List<LookupEntity> {
        val now = System.currentTimeMillis()
        val entries = listOf(
            LookupEntity("cat_men", LookupTypes.CATEGORY, "رجالي", 0, now),
            LookupEntity("cat_women", LookupTypes.CATEGORY, "نسائي", 1, now),
            LookupEntity("cat_oriental", LookupTypes.CATEGORY, "شرقي", 2, now),
            LookupEntity("cat_decants", LookupTypes.CATEGORY, "تعبئة", 3, now),
            LookupEntity("cat_bottles", LookupTypes.CATEGORY, "قارورات", 4, now),
            LookupEntity("unit_ml", LookupTypes.UNIT, "مل", 0, now),
            LookupEntity("unit_bottle", LookupTypes.UNIT, "قارورة", 1, now),
            LookupEntity("unit_tola", LookupTypes.UNIT, "تولة", 2, now),
            LookupEntity("unit_piece", LookupTypes.UNIT, "قطعة", 3, now),
            LookupEntity("size_10", LookupTypes.QUICK_SIZE, "10", 0, now),
            LookupEntity("size_30", LookupTypes.QUICK_SIZE, "30", 1, now),
            LookupEntity("size_50", LookupTypes.QUICK_SIZE, "50", 2, now),
            LookupEntity("size_100", LookupTypes.QUICK_SIZE, "100", 3, now)
        )
        dao.upsertLookups(entries)
        return entries
    }

    private suspend fun seedInitialSampleDataLocalOnly(): Triple<List<ProductEntity>, List<CustomerEntity>, List<SaleWithItems>> {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        seedDefaultLookups()
        dao.upsertShopSettings(ShopSettingsEntity())

        val products = listOf(
            ProductEntity(
                id = "prod_royal_oud",
                name = "رويال عود فاخر",
                subtitle = "Royal Oud Prestige • بخاخ فاخر",
                barcode = "HP-1001",
                category = "شرقي",
                unitLabel = "قارورة",
                buyPrice = 2900.0,
                sellPrice = 4800.0,
                currentStock = 14.0,
                minStockAlert = 5.0,
                imageKey = "royal_oud",
                createdAtMillis = now - 6 * dayMs,
                updatedAtMillis = now
            ),
            ProductEntity(
                id = "prod_black_afgano",
                name = "بلاك أفغانو زيت خام",
                subtitle = "تركيز نقي فاخر للتعبئة",
                barcode = "HP-1002",
                category = "تعبئة",
                unitLabel = "مل",
                buyPrice = 55.0,
                sellPrice = 120.0,
                currentStock = 450.5,
                minStockAlert = 80.0,
                imageKey = "black_afgano",
                createdAtMillis = now - 6 * dayMs,
                updatedAtMillis = now
            ),
            ProductEntity(
                id = "prod_prince_spray",
                name = "سمو الأمير",
                subtitle = "عطر أرستقراطي 100 مل",
                barcode = "HP-1003",
                category = "رجالي",
                unitLabel = "قارورة",
                buyPrice = 3800.0,
                sellPrice = 6200.0,
                currentStock = 9.0,
                minStockAlert = 4.0,
                imageKey = "prince_spray",
                createdAtMillis = now - 5 * dayMs,
                updatedAtMillis = now
            ),
            ProductEntity(
                id = "prod_tobacco_vanille",
                name = "توباكو فانيلا",
                subtitle = "تركيب خاص • زيت + كحول فرنسي",
                barcode = "HP-1004",
                category = "تعبئة",
                unitLabel = "مل",
                buyPrice = 42.0,
                sellPrice = 95.0,
                currentStock = 310.0,
                minStockAlert = 60.0,
                imageKey = "tobacco_vanille",
                createdAtMillis = now - 5 * dayMs,
                updatedAtMillis = now
            ),
            ProductEntity(
                id = "prod_velvet_rose",
                name = "عنبر الورد المخملي",
                subtitle = "Velvet Amber Rose • نسائي فاخر",
                barcode = "HP-1005",
                category = "نسائي",
                unitLabel = "مل",
                buyPrice = 45.0,
                sellPrice = 110.0,
                currentStock = 42.0,
                minStockAlert = 60.0,
                imageKey = "prince_spray",
                createdAtMillis = now - 4 * dayMs,
                updatedAtMillis = now
            ),
            ProductEntity(
                id = "prod_crystal_50ml",
                name = "قارورة كريستال ذهبي 50مل",
                subtitle = "زجاج ثقيل بغطاء معدني مذهب",
                barcode = "HP-1006",
                category = "قارورات",
                unitLabel = "قطعة",
                buyPrice = 180.0,
                sellPrice = 400.0,
                currentStock = 11.0,
                minStockAlert = 15.0,
                imageKey = "royal_oud",
                createdAtMillis = now - 4 * dayMs,
                updatedAtMillis = now
            )
        )
        dao.upsertProducts(products)

        val customers = listOf(
            CustomerEntity(
                id = "cust_mohamed",
                loyaltyCode = "PRF-1042",
                name = "محمد بن علي",
                phone = "0550 42 18 90",
                creditBalance = 1200.0,
                stamps = 8,
                points = 185,
                loyaltyMode = LoyaltyModes.STAMPS,
                notes = "يفضل العطور الشرقية والعود الثقيل",
                createdAtMillis = now - 20 * dayMs,
                updatedAtMillis = now
            ),
            CustomerEntity(
                id = "cust_yasmine",
                loyaltyCode = "PRF-1088",
                name = "ياسمين بن عمر",
                phone = "0661 77 31 04",
                creditBalance = 0.0,
                stamps = 6,
                points = 120,
                loyaltyMode = LoyaltyModes.STAMPS,
                notes = "زبونة دائمة لتركيبات الفانيلا والورد",
                createdAtMillis = now - 14 * dayMs,
                updatedAtMillis = now
            ),
            CustomerEntity(
                id = "cust_amine",
                loyaltyCode = "PRF-1105",
                name = "أمين قاسمي",
                phone = "0770 89 55 12",
                creditBalance = 2600.0,
                stamps = 4,
                points = 95,
                loyaltyMode = LoyaltyModes.STAMPS,
                notes = "يسدد شهرياً",
                createdAtMillis = now - 10 * dayMs,
                updatedAtMillis = now
            ),
            CustomerEntity(
                id = "cust_karim",
                loyaltyCode = "PRF-1129",
                name = "كريم بوزيد",
                phone = "0554 19 63 20",
                creditBalance = 0.0,
                stamps = 10,
                points = 260,
                loyaltyMode = LoyaltyModes.STAMPS,
                notes = "أكمل 10 أختام • مؤهل لعطر هدية!",
                createdAtMillis = now - 30 * dayMs,
                updatedAtMillis = now
            )
        )
        dao.upsertCustomers(customers)

        val creditLogs = listOf(
            CreditLogEntity(
                id = "crd_seed_1",
                customerId = "cust_mohamed",
                amountDelta = 1200.0,
                note = "باقي فاتورة تركيب توباكو فانيلا",
                createdAtMillis = now - 3 * dayMs
            ),
            CreditLogEntity(
                id = "crd_seed_2",
                customerId = "cust_amine",
                amountDelta = 2600.0,
                note = "فاتورة آجل رويال عود",
                createdAtMillis = now - 2 * dayMs
            )
        )
        dao.upsertCreditLogs(creditLogs)

        val sampleSales = listOf(
            SaleWithItems(
                sale = SaleEntity(
                    id = "sal_seed_today1",
                    invoiceNumber = "HP-1005",
                    customerId = "cust_mohamed",
                    customerNameSnapshot = "محمد بن علي",
                    paymentMethod = PaymentMethods.CASH,
                    subtotal = 3372.5,
                    discount = 172.5,
                    totalAmount = 3200.0,
                    estimatedProfit = 1709.0,
                    stampAwarded = true,
                    returned = false,
                    createdAtMillis = now - 2 * 3_600_000L,
                    updatedAtMillis = now - 2 * 3_600_000L
                ),
                items = listOf(
                    SaleItemEntity(
                        id = "sal_seed_today1_ln0",
                        saleId = "sal_seed_today1",
                        productId = "prod_tobacco_vanille",
                        itemName = "توباكو فانيلا",
                        unitLabel = "مل",
                        quantity = 35.5,
                        costUnitPrice = 42.0,
                        actualUnitPrice = 95.0,
                        lineDiscount = 0.0,
                        totalPrice = 3372.5,
                        soldByAmount = false
                    )
                )
            ),
            SaleWithItems(
                sale = SaleEntity(
                    id = "sal_seed_today2",
                    invoiceNumber = "HP-1004",
                    customerId = "cust_yasmine",
                    customerNameSnapshot = "ياسمين بن عمر",
                    paymentMethod = PaymentMethods.CASH,
                    subtotal = 6200.0,
                    discount = 0.0,
                    totalAmount = 6200.0,
                    estimatedProfit = 2400.0,
                    stampAwarded = true,
                    returned = false,
                    createdAtMillis = now - 4 * 3_600_000L,
                    updatedAtMillis = now - 4 * 3_600_000L
                ),
                items = listOf(
                    SaleItemEntity(
                        id = "sal_seed_today2_ln0",
                        saleId = "sal_seed_today2",
                        productId = "prod_prince_spray",
                        itemName = "سمو الأمير",
                        unitLabel = "قارورة",
                        quantity = 1.0,
                        costUnitPrice = 3800.0,
                        actualUnitPrice = 6200.0,
                        lineDiscount = 0.0,
                        totalPrice = 6200.0,
                        soldByAmount = false
                    )
                )
            ),
            SaleWithItems(
                sale = SaleEntity(
                    id = "sal_seed_d1",
                    invoiceNumber = "HP-1003",
                    customerId = "cust_amine",
                    customerNameSnapshot = "أمين قاسمي",
                    paymentMethod = PaymentMethods.CREDIT,
                    subtotal = 4800.0,
                    discount = 0.0,
                    totalAmount = 4800.0,
                    estimatedProfit = 1900.0,
                    stampAwarded = true,
                    returned = false,
                    createdAtMillis = now - 1 * dayMs,
                    updatedAtMillis = now - 1 * dayMs
                ),
                items = listOf(
                    SaleItemEntity(
                        id = "sal_seed_d1_ln0",
                        saleId = "sal_seed_d1",
                        productId = "prod_royal_oud",
                        itemName = "رويال عود فاخر",
                        unitLabel = "قارورة",
                        quantity = 1.0,
                        costUnitPrice = 2900.0,
                        actualUnitPrice = 4800.0,
                        lineDiscount = 0.0,
                        totalPrice = 4800.0,
                        soldByAmount = false
                    )
                )
            ),
            SaleWithItems(
                sale = SaleEntity(
                    id = "sal_seed_d2",
                    invoiceNumber = "HP-1002",
                    customerId = null,
                    customerNameSnapshot = null,
                    paymentMethod = PaymentMethods.CASH,
                    subtotal = 6000.0,
                    discount = 200.0,
                    totalAmount = 5800.0,
                    estimatedProfit = 3050.0,
                    stampAwarded = false,
                    returned = false,
                    createdAtMillis = now - 2 * dayMs,
                    updatedAtMillis = now - 2 * dayMs
                ),
                items = listOf(
                    SaleItemEntity(
                        id = "sal_seed_d2_ln0",
                        saleId = "sal_seed_d2",
                        productId = "prod_black_afgano",
                        itemName = "بلاك أفغانو زيت خام",
                        unitLabel = "مل",
                        quantity = 50.0,
                        costUnitPrice = 55.0,
                        actualUnitPrice = 120.0,
                        lineDiscount = 0.0,
                        totalPrice = 6000.0,
                        soldByAmount = false
                    )
                )
            ),
            SaleWithItems(
                sale = SaleEntity(
                    id = "sal_seed_d4",
                    invoiceNumber = "HP-1001",
                    customerId = "cust_karim",
                    customerNameSnapshot = "كريم بوزيد",
                    paymentMethod = PaymentMethods.CASH,
                    subtotal = 3300.0,
                    discount = 0.0,
                    totalAmount = 3300.0,
                    estimatedProfit = 1950.0,
                    stampAwarded = true,
                    returned = false,
                    createdAtMillis = now - 4 * dayMs,
                    updatedAtMillis = now - 4 * dayMs
                ),
                items = listOf(
                    SaleItemEntity(
                        id = "sal_seed_d4_ln0",
                        saleId = "sal_seed_d4",
                        productId = "prod_velvet_rose",
                        itemName = "عنبر الورد المخملي",
                        unitLabel = "مل",
                        quantity = 30.0,
                        costUnitPrice = 45.0,
                        actualUnitPrice = 110.0,
                        lineDiscount = 0.0,
                        totalPrice = 3300.0,
                        soldByAmount = false
                    )
                )
            )
        )
        sampleSales.forEach { dao.saveSaleWithItems(it.sale, it.items) }

        return Triple(products, customers, sampleSales)
    }

    private suspend fun seedInitialSampleDataAndBackup() {
        val (products, customers, sales) = seedInitialSampleDataLocalOnly()
        val settings = dao.getShopSettings() ?: ShopSettingsEntity()
        val lookups = listOf(
            LookupEntity("cat_men", LookupTypes.CATEGORY, "رجالي", 0),
            LookupEntity("cat_women", LookupTypes.CATEGORY, "نسائي", 1),
            LookupEntity("cat_oriental", LookupTypes.CATEGORY, "شرقي", 2),
            LookupEntity("cat_decants", LookupTypes.CATEGORY, "تعبئة", 3),
            LookupEntity("cat_bottles", LookupTypes.CATEGORY, "قارورات", 4),
            LookupEntity("unit_ml", LookupTypes.UNIT, "مل", 0),
            LookupEntity("unit_bottle", LookupTypes.UNIT, "قارورة", 1),
            LookupEntity("unit_tola", LookupTypes.UNIT, "تولة", 2),
            LookupEntity("unit_piece", LookupTypes.UNIT, "قطعة", 3),
            LookupEntity("size_10", LookupTypes.QUICK_SIZE, "10", 0),
            LookupEntity("size_30", LookupTypes.QUICK_SIZE, "30", 1),
            LookupEntity("size_50", LookupTypes.QUICK_SIZE, "50", 2),
            LookupEntity("size_100", LookupTypes.QUICK_SIZE, "100", 3)
        )
        syncScope.launch {
            cloudSync.syncSettings(settings)
            lookups.forEach { cloudSync.syncLookup(it) }
            products.forEach { cloudSync.syncProduct(it) }
            customers.forEach { cloudSync.syncCustomer(it) }
            sales.forEach { cloudSync.syncSale(it.sale, it.items) }
        }
    }
}
