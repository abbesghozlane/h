package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CreditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.HaiderDatabase
import com.example.data.local.LookupEntity
import com.example.data.local.LookupTypes
import com.example.data.local.PaymentMethods
import com.example.data.local.ProductEntity
import com.example.data.local.SaleItemEntity
import com.example.data.local.SaleWithItems
import com.example.data.local.ShopSettingsEntity
import com.example.data.remote.FirestoreSyncService
import com.example.data.repository.CartLineDraft
import com.example.data.repository.CheckoutResult
import com.example.data.repository.ShopRepository
import com.example.data.repository.SyncState
import com.example.util.formatShortArabicDay
import com.example.util.generateNextLoyaltyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class DaySalesPoint(
    val dayLabel: String,
    val totalSales: Double,
    val invoiceCount: Int
)

data class TopPerfumeStat(
    val name: String,
    val totalRevenue: Double,
    val totalQty: Double,
    val unitLabel: String
)

data class AnalyticsSummary(
    val todaySales: Double = 0.0,
    val estimatedNetProfit: Double = 0.0,
    val totalCreditOwed: Double = 0.0,
    val lowStockCount: Int = 0,
    val weeklySales: List<DaySalesPoint> = emptyList(),
    val topPerfumes: List<TopPerfumeStat> = emptyList()
)

data class PosCartState(
    val items: List<CartLineDraft> = emptyList(),
    val attachedCustomerId: String? = null,
    val paymentMethod: String = PaymentMethods.CASH,
    val cartDiscount: Double = 0.0
) {
    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val netTotal: Double get() = (subtotal - cartDiscount).coerceAtLeast(0.0)
    val itemCount: Int get() = items.size
}

class ShopViewModel(
    private val repository: ShopRepository
) : ViewModel() {

    val syncState: StateFlow<SyncState> = repository.syncState

    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val lookups: StateFlow<List<LookupEntity>> = repository.lookups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val categories: StateFlow<List<LookupEntity>> = lookups
        .map { list -> list.filter { it.lookupType == LookupTypes.CATEGORY } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val units: StateFlow<List<LookupEntity>> = lookups
        .map { list -> list.filter { it.lookupType == LookupTypes.UNIT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quickSizes: StateFlow<List<LookupEntity>> = lookups
        .map { list -> list.filter { it.lookupType == LookupTypes.QUICK_SIZE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val creditLogs: StateFlow<List<CreditLogEntity>> = repository.creditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val salesWithItems: StateFlow<List<SaleWithItems>> = repository.salesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shopSettings: StateFlow<ShopSettingsEntity> = repository.shopSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ShopSettingsEntity())

    private val _cart = MutableStateFlow(PosCartState())
    val cart: StateFlow<PosCartState> = _cart.asStateFlow()

    val attachedCustomer: StateFlow<CustomerEntity?> = combine(cart, customers) { c, list ->
        c.attachedCustomerId?.let { id -> list.firstOrNull { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _lastCheckout = MutableStateFlow<CheckoutResult?>(null)
    val lastCheckout: StateFlow<CheckoutResult?> = _lastCheckout.asStateFlow()

    val analytics: StateFlow<AnalyticsSummary> = combine(
        products,
        customers,
        salesWithItems
    ) { prodList, custList, salesList ->
        val activeSales = salesList.filter { !it.sale.returned }
        val calStartOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySales = activeSales
            .filter { it.sale.createdAtMillis >= calStartOfToday }
            .sumOf { it.sale.totalAmount }

        val netProfit = activeSales.sumOf { it.sale.estimatedProfit }
        val totalCredit = custList.sumOf { it.creditBalance }
        val lowStock = prodList.count { it.currentStock <= it.minStockAlert }

        // 7-day spline points (oldest -> today)
        val dayMs = 86_400_000L
        val weekly = (6 downTo 0).map { daysAgo ->
            val dayStart = calStartOfToday - daysAgo * dayMs
            val dayEnd = dayStart + dayMs
            val daySales = activeSales.filter {
                it.sale.createdAtMillis in dayStart until dayEnd
            }
            DaySalesPoint(
                dayLabel = if (daysAgo == 0) "اليوم" else formatShortArabicDay(dayStart),
                totalSales = daySales.sumOf { it.sale.totalAmount },
                invoiceCount = daySales.size
            )
        }

        // Top 5 selling perfumes across active invoices
        val top5 = activeSales
            .flatMap { it.items }
            .groupBy { it.itemName }
            .map { (name, lines) ->
                TopPerfumeStat(
                    name = name,
                    totalRevenue = lines.sumOf { it.totalPrice },
                    totalQty = lines.sumOf { it.quantity },
                    unitLabel = lines.firstOrNull()?.unitLabel ?: "مل"
                )
            }
            .sortedByDescending { it.totalRevenue }
            .take(5)

        AnalyticsSummary(
            todaySales = todaySales,
            estimatedNetProfit = netProfit,
            totalCreditOwed = totalCredit,
            lowStockCount = lowStock,
            weeklySales = weekly,
            topPerfumes = top5
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AnalyticsSummary())

    init {
        viewModelScope.launch {
            repository.initializeAndSync()
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun dismissCheckoutBanner() {
        _lastCheckout.value = null
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val res = repository.manualFullSync()
            showToast(if (res.isSuccess) "تمت المزامنة السحابية بنجاح" else "المحفوظات متوفرة محلياً • تعذّر الاتصال بالسحابة")
        }
    }

    // --- POS CART ACTIONS ---
    fun addToCart(draft: CartLineDraft) {
        _cart.update { it.copy(items = it.items + draft) }
        showToast("تمت إضافة ${draft.itemName} إلى السلة")
    }

    fun updateCartItem(updated: CartLineDraft) {
        _cart.update { state ->
            state.copy(items = state.items.map { if (it.id == updated.id) updated else it })
        }
    }

    fun removeCartItem(draftId: String) {
        _cart.update { state ->
            state.copy(items = state.items.filterNot { it.id == draftId })
        }
    }

    fun clearCart() {
        _cart.value = PosCartState()
    }

    fun attachCustomerToCart(customerId: String?) {
        _cart.update { it.copy(attachedCustomerId = customerId) }
        if (customerId != null) {
            val name = customers.value.firstOrNull { it.id == customerId }?.name
            if (name != null) showToast("تم ربط الزبون $name (+1 ختم عند البيع)")
        }
    }

    fun setPaymentMethod(method: String) {
        _cart.update { it.copy(paymentMethod = method) }
    }

    fun setCartDiscount(discount: Double) {
        _cart.update { it.copy(cartDiscount = discount.coerceAtLeast(0.0)) }
    }

    fun checkoutCart() {
        val current = _cart.value
        if (current.items.isEmpty()) {
            showToast("السلة فارغة — أضف عطراً أولاً")
            return
        }
        if (current.paymentMethod == PaymentMethods.CREDIT && current.attachedCustomerId == null) {
            showToast("يرجى اختيار أو مسح QR الزبون للبيع بالآجل (كريدي)")
            return
        }
        viewModelScope.launch {
            val result = repository.completeCheckout(
                drafts = current.items,
                customerId = current.attachedCustomerId,
                paymentMethod = current.paymentMethod,
                cartDiscount = current.cartDiscount
            )
            _cart.value = PosCartState()
            _lastCheckout.value = result
            val stampMsg = when {
                result.reachedRewardGoal -> "🎁 مبروك! الزبون أكمل الأختام ومؤهل لعطر هدية!"
                result.updatedCustomer != null -> "تم البيع بنجاح وإضافة +1 ختم لـ ${result.updatedCustomer.name}"
                else -> "تم تسجيل الفاتورة ${result.sale.invoiceNumber} بنجاح"
            }
            showToast(stampMsg)
        }
    }

    // --- QR / BARCODE LOOKUP ---
    suspend fun resolveScannedCustomer(code: String): CustomerEntity? {
        val cleaned = code.trim()
        return repository.findCustomerByCode(cleaned)
            ?: customers.value.firstOrNull {
                it.loyaltyCode.equals(cleaned, ignoreCase = true) ||
                    it.phone?.replace(" ", "")?.equals(cleaned.replace(" ", ""), ignoreCase = true) == true
            }
    }

    fun findProductByBarcode(code: String): ProductEntity? {
        val cleaned = code.trim()
        return products.value.firstOrNull {
            it.barcode?.equals(cleaned, ignoreCase = true) == true ||
                it.name.contains(cleaned, ignoreCase = true)
        }
    }

    // --- INVENTORY ACTIONS ---
    fun saveProduct(
        existingId: String?,
        name: String,
        subtitle: String?,
        barcode: String?,
        category: String,
        unitLabel: String,
        buyPrice: Double,
        sellPrice: Double,
        currentStock: Double,
        minStockAlert: Double,
        imageKey: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = existingId ?: "prd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = existingId?.let { eid -> products.value.firstOrNull { it.id == eid } }
            val entity = ProductEntity(
                id = id,
                name = name.trim(),
                subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
                barcode = barcode?.trim()?.takeIf { it.isNotEmpty() },
                category = category.trim().ifBlank { "شرقي" },
                unitLabel = unitLabel.trim().ifBlank { "مل" },
                buyPrice = buyPrice.coerceAtLeast(0.0),
                sellPrice = sellPrice.coerceAtLeast(0.0),
                currentStock = currentStock.coerceAtLeast(0.0),
                minStockAlert = minStockAlert.coerceAtLeast(0.0),
                imageKey = imageKey,
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveProduct(entity)
            showToast(if (existingId == null) "تمت إضافة العطر للمخزون" else "تم تحديث بيانات العطر")
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            showToast("تم حذف المنتج من المخزون")
        }
    }

    fun addLookup(type: String, label: String) {
        viewModelScope.launch {
            repository.addLookup(type, label, lookups.value.size)
            showToast("تمت إضافة \"$label\"")
        }
    }

    fun deleteLookup(lookupId: String) {
        viewModelScope.launch {
            repository.deleteLookup(lookupId)
        }
    }

    // --- CUSTOMERS & LOYALTY ACTIONS ---
    fun saveCustomer(
        existingId: String?,
        name: String,
        phone: String?,
        notes: String?,
        loyaltyCode: String?,
        loyaltyMode: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = existingId ?: "cus_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val existing = existingId?.let { eid -> customers.value.firstOrNull { it.id == eid } }
            val existingCodes = customers.value.map { it.loyaltyCode }.toSet()
            val code = loyaltyCode?.trim()?.takeIf { it.isNotEmpty() }
                ?: existing?.loyaltyCode
                ?: generateNextLoyaltyCode(existingCodes)
            val entity = CustomerEntity(
                id = id,
                loyaltyCode = code,
                name = name.trim(),
                phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                creditBalance = existing?.creditBalance ?: 0.0,
                stamps = existing?.stamps ?: 0,
                points = existing?.points ?: 0,
                loyaltyMode = loyaltyMode,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveCustomer(entity)
            showToast(if (existingId == null) "تم تسجيل الزبون $name ($code)" else "تم تحديث ملف الزبون")
        }
    }

    fun adjustCustomerStamps(customerId: String, delta: Int) {
        viewModelScope.launch {
            val updated = repository.adjustLoyalty(customerId, stampsDelta = delta)
            if (updated != null && updated.stamps >= shopSettings.value.stampTarget && delta > 0) {
                showToast("🎁 ${updated.name} أكمل ${updated.stamps} أختام ومؤهل لعطر هدية!")
            }
        }
    }

    fun adjustCustomerPoints(customerId: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustLoyalty(customerId, pointsDelta = delta)
        }
    }

    fun redeemCustomerReward(customerId: String) {
        viewModelScope.launch {
            repository.adjustLoyalty(customerId, resetStamps = true)
            showToast("تم صرف العطر الهدية وتصفير بطاقة الأختام ✨")
        }
    }

    fun toggleCustomerLoyaltyMode(customerId: String) {
        viewModelScope.launch {
            repository.toggleCustomerLoyaltyMode(customerId)
        }
    }

    fun recordCreditPaymentOrAdjustment(customerId: String, delta: Double, note: String) {
        if (delta == 0.0) return
        viewModelScope.launch {
            repository.recordCreditAdjustment(customerId, delta, note)
            showToast(if (delta < 0) "تم تسجيل دفعة التسديد وتحديث الكريدي" else "تم تحديث رصيد الكريدي")
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            if (_cart.value.attachedCustomerId == customerId) {
                _cart.update { it.copy(attachedCustomerId = null) }
            }
            repository.deleteCustomer(customerId)
            showToast("تم حذف الزبون")
        }
    }

    // --- EDITABLE INVOICES & RETURNS ---
    fun updateInvoice(
        saleId: String,
        items: List<SaleItemEntity>,
        paymentMethod: String,
        discount: Double
    ) {
        viewModelScope.launch {
            repository.updateExistingInvoice(saleId, items, paymentMethod, discount)
            showToast("تم تحديث الفاتورة وتعديل المخزون والرصيد تلقائياً")
        }
    }

    fun returnInvoice(saleId: String) {
        viewModelScope.launch {
            repository.processInvoiceReturn(saleId)
            showToast("تم تسجيل المرتجع واستعادة المخزون وتحديث الكريدي")
        }
    }

    // --- SETTINGS ---
    fun saveShopSettings(settings: ShopSettingsEntity) {
        viewModelScope.launch {
            repository.saveShopSettings(settings)
            showToast("تم حفظ إعدادات المحل")
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val db = HaiderDatabase.getInstance(appContext)
                val cloud = FirestoreSyncService(appContext)
                val repo = ShopRepository(db.shopDao(), cloud)
                return ShopViewModel(repo) as T
            }
        }
    }
}
