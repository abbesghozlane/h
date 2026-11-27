package com.example.ui.loyalty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CreditLogEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LoyaltyModes
import com.example.data.local.ShopSettingsEntity
import com.example.ui.components.GlassContainer
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GoldGradientButton
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGoldGradient
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDarkInk
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.example.util.LoyaltyCardExporter
import com.example.util.formatDateTimeArabic
import com.example.util.formatMoney

@Composable
fun CustomersLoyaltyScreen(
    customers: List<CustomerEntity>,
    creditLogs: List<CreditLogEntity>,
    settings: ShopSettingsEntity,
    focusedCustomerId: String?,
    onClearFocusedCustomer: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onLinkCustomerToPos: (CustomerEntity) -> Unit,
    onSaveCustomer: (
        existingId: String?,
        name: String,
        phone: String?,
        notes: String?,
        loyaltyCode: String?,
        loyaltyMode: String
    ) -> Unit,
    onAdjustStamps: (String, Int) -> Unit,
    onAdjustPoints: (String, Int) -> Unit,
    onRedeemReward: (String) -> Unit,
    onToggleLoyaltyMode: (String) -> Unit,
    onRecordCreditAdjustment: (String, Double, String) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, CREDIT, GIFT_READY
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var showCreateCustomerSheet by remember { mutableStateOf(false) }

    val activeProfileId = focusedCustomerId ?: selectedCustomerId
    val activeCustomer = remember(activeProfileId, customers) {
        activeProfileId?.let { id -> customers.firstOrNull { it.id == id } }
    }

    val filteredCustomers = remember(customers, searchQuery, filterMode, settings.stampTarget) {
        customers.filter { c ->
            val matchesFilter = when (filterMode) {
                "CREDIT" -> c.creditBalance > 0.0
                "GIFT_READY" -> c.stamps >= settings.stampTarget
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.loyaltyCode.contains(searchQuery, ignoreCase = true) ||
                (c.phone?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search + Scan QR + New Customer
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customers_search_input"),
                            placeholder = { Text("ابحث بالاسم، الهاتف، أو PRF...", color = SoftSilver) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0x2EFFD782),
                                focusedContainerColor = ObsidianElevated.copy(alpha = 0.85f),
                                unfocusedContainerColor = ObsidianElevated.copy(alpha = 0.72f),
                                focusedTextColor = WarmCream,
                                unfocusedTextColor = WarmCream
                            )
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.QrCodeScanner,
                            contentDescription = "مسح QR الزبون",
                            highlighted = true,
                            onClick = onOpenQrScanner,
                            modifier = Modifier.testTag("loyalty_scan_qr_button")
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.PersonAdd,
                            contentDescription = "إضافة زبون جديد",
                            highlighted = true,
                            onClick = { showCreateCustomerSheet = true },
                            modifier = Modifier.testTag("loyalty_add_customer_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassFilterChip(
                            label = "الكل (${customers.size})",
                            selected = filterMode == "ALL",
                            onClick = { filterMode = "ALL" }
                        )
                        GlassFilterChip(
                            label = "أصحاب الكريدي",
                            selected = filterMode == "CREDIT",
                            onClick = { filterMode = "CREDIT" }
                        )
                        GlassFilterChip(
                            label = "مؤهل للهدية 🎁",
                            selected = filterMode == "GIFT_READY",
                            onClick = { filterMode = "GIFT_READY" }
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    GlassContainer(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                            Text("لا يوجد زبائن مطابقون", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                            Text("أضف زبوناً جديداً لإصدار بطاقة ولاء QR ذهبية فوراً", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    CustomerGlassCard(
                        customer = customer,
                        stampTarget = settings.stampTarget,
                        currencySymbol = settings.currencySymbol,
                        onClick = { selectedCustomerId = customer.id }
                    )
                }
            }
        }
    }

    if (activeCustomer != null) {
        val customerLogs = remember(creditLogs, activeCustomer.id) {
            creditLogs.filter { it.customerId == activeCustomer.id }
        }
        CustomerProfileLoyaltySheet(
            customer = activeCustomer,
            creditLogs = customerLogs,
            settings = settings,
            onLinkToPos = {
                onLinkCustomerToPos(activeCustomer)
                selectedCustomerId = null
                onClearFocusedCustomer()
            },
            onAdjustStamps = { delta -> onAdjustStamps(activeCustomer.id, delta) },
            onAdjustPoints = { delta -> onAdjustPoints(activeCustomer.id, delta) },
            onRedeemReward = { onRedeemReward(activeCustomer.id) },
            onToggleMode = { onToggleLoyaltyMode(activeCustomer.id) },
            onRecordCredit = { delta, note -> onRecordCreditAdjustment(activeCustomer.id, delta, note) },
            onDelete = {
                onDeleteCustomer(activeCustomer.id)
                selectedCustomerId = null
                onClearFocusedCustomer()
            },
            onShowMessage = onShowMessage,
            onDismiss = {
                selectedCustomerId = null
                onClearFocusedCustomer()
            }
        )
    }

    if (showCreateCustomerSheet) {
        AddCustomerSheet(
            defaultLoyaltyMode = settings.defaultLoyaltyMode,
            onSave = { name, phone, notes, mode ->
                onSaveCustomer(null, name, phone, notes, null, mode)
                showCreateCustomerSheet = false
            },
            onDismiss = { showCreateCustomerSheet = false }
        )
    }
}

@Composable
private fun CustomerGlassCard(
    customer: CustomerEntity,
    stampTarget: Int,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val giftReady = customer.stamps >= stampTarget

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
        highlighted = giftReady,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ChampagneGoldGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldDarkInk
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCream
                    )
                    Text(
                        text = customer.loyaltyCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary
                    )
                }
                if (!customer.phone.isNullOrBlank()) {
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftSilver
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (giftReady) EmeraldLuminous.copy(alpha = 0.2f)
                                else GoldPrimary.copy(alpha = 0.16f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (customer.loyaltyMode == LoyaltyModes.STAMPS) {
                                "${customer.stamps}/$stampTarget أختام" + if (giftReady) " 🎁" else ""
                            } else {
                                "${customer.points} نقطة"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (giftReady) EmeraldLuminous else GoldBright
                        )
                    }
                    if (customer.creditBalance > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CrimsonContainer.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "كريدي: ${formatMoney(customer.creditBalance, currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerProfileLoyaltySheet(
    customer: CustomerEntity,
    creditLogs: List<CreditLogEntity>,
    settings: ShopSettingsEntity,
    onLinkToPos: () -> Unit,
    onAdjustStamps: (Int) -> Unit,
    onAdjustPoints: (Int) -> Unit,
    onRedeemReward: () -> Unit,
    onToggleMode: () -> Unit,
    onRecordCredit: (Double, String) -> Unit,
    onDelete: () -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var creditAmountStr by remember { mutableStateOf("") }
    var creditNote by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = WarmCream,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(customer.name, style = MaterialTheme.typography.titleLarge, color = WarmCream)
                    Text(
                        text = "${customer.loyaltyCode}" + (customer.phone?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف الزبون",
                        tint = CrimsonSoft,
                        onClick = onDelete
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "إغلاق",
                        tint = SoftSilver,
                        onClick = onDismiss
                    )
                }
            }

            // Interactive Black & Gold Digital Loyalty Card
            HaiderDigitalLoyaltyCard(
                customer = customer,
                settings = settings,
                onStampTapped = { onAdjustStamps(1) }
            )

            // Export & Share PNG Actions (WhatsApp + Save to Gallery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoldGradientButton(
                    text = "مشاركة واتساب (PNG)",
                    icon = Icons.Rounded.Share,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                    tag = "loyalty_share_whatsapp_button",
                    onClick = {
                        val bmp = LoyaltyCardExporter.renderLoyaltyCardBitmap(context, customer, settings)
                        val res = LoyaltyCardExporter.shareLoyaltyCard(
                            context = context,
                            bitmap = bmp,
                            customerName = customer.name,
                            loyaltyCode = customer.loyaltyCode,
                            preferWhatsApp = true
                        )
                        if (res.isFailure) {
                            onShowMessage("تعذرت مشاركة البطاقة")
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianElevated)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .clickable {
                            val bmp = LoyaltyCardExporter.renderLoyaltyCardBitmap(context, customer, settings)
                            val res = LoyaltyCardExporter.saveLoyaltyCardToGallery(
                                context = context,
                                bitmap = bmp,
                                loyaltyCode = customer.loyaltyCode
                            )
                            onShowMessage(res.getOrElse { "تعذر حفظ الصورة" })
                        }
                        .padding(horizontal = 14.dp)
                        .testTag("loyalty_save_gallery_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "حفظ في المعرض", tint = GoldBright, modifier = Modifier.size(18.dp))
                        Text("حفظ", style = MaterialTheme.typography.labelLarge, color = GoldBright)
                    }
                }
            }

            // Manual Loyalty Override Controls + Link to POS
            GlassContainer(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التحكم اليدوي بالولاء", style = MaterialTheme.typography.titleMedium, color = GoldBright)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ObsidianDeep)
                                .clickable { onToggleMode() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = GoldBright, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (customer.loyaltyMode == LoyaltyModes.STAMPS) "تبديل للنقاط" else "تبديل للأختام",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldBright
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isStamps = customer.loyaltyMode == LoyaltyModes.STAMPS
                        GlassIconButton(
                            icon = Icons.Rounded.Remove,
                            contentDescription = "إنقاص",
                            onClick = { if (isStamps) onAdjustStamps(-1) else onAdjustPoints(-10) }
                        )
                        GoldGradientButton(
                            text = if (isStamps) "+1 ختم شراء" else "+10 نقاط",
                            icon = Icons.Rounded.Add,
                            height = 48.dp,
                            modifier = Modifier.weight(1f),
                            tag = "loyalty_plus_stamp_button",
                            onClick = { if (isStamps) onAdjustStamps(1) else onAdjustPoints(10) }
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = "صرف الهدية وتصفير",
                            tint = EmeraldLuminous,
                            onClick = onRedeemReward
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldPrimary.copy(alpha = 0.16f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { onLinkToPos() }
                                .testTag("loyalty_link_pos_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.PointOfSale, contentDescription = null, tint = GoldBright, modifier = Modifier.size(18.dp))
                                Text("ربط بنقطة البيع", style = MaterialTheme.typography.labelLarge, color = GoldBright)
                            }
                        }
                        if (customer.stamps >= settings.stampTarget) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldLuminous.copy(alpha = 0.18f))
                                    .border(1.dp, EmeraldLuminous, RoundedCornerShape(14.dp))
                                    .clickable { onRedeemReward() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = EmeraldLuminous, modifier = Modifier.size(18.dp))
                                    Text("صرف الهدية", style = MaterialTheme.typography.labelLarge, color = EmeraldLuminous)
                                }
                            }
                        }
                    }
                }
            }

            // Credit Balance & Payment History Card
            GlassContainer(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = AmberWarning)
                            Text("حساب الكريدي (الآجل)", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                        }
                        Text(
                            text = formatMoney(customer.creditBalance, settings.currencySymbol),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (customer.creditBalance > 0.0) CrimsonSoft else EmeraldLuminous
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = creditAmountStr,
                            onValueChange = { creditAmountStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            modifier = Modifier.weight(1f),
                            label = { Text("المبلغ (${settings.currencySymbol})", color = SoftSilver) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = ObsidianDeep,
                                unfocusedContainerColor = ObsidianDeep,
                                focusedTextColor = WarmCream,
                                unfocusedTextColor = WarmCream
                            )
                        )
                        OutlinedTextField(
                            value = creditNote,
                            onValueChange = { creditNote = it },
                            modifier = Modifier.weight(1.2f),
                            label = { Text("ملاحظة", color = SoftSilver) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = ObsidianDeep,
                                unfocusedContainerColor = ObsidianDeep,
                                focusedTextColor = WarmCream,
                                unfocusedTextColor = WarmCream
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val parsedAmount = creditAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldLuminous.copy(alpha = 0.16f))
                                .border(1.dp, EmeraldLuminous.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable(enabled = parsedAmount > 0.0) {
                                    onRecordCredit(-parsedAmount, creditNote)
                                    creditAmountStr = ""
                                    creditNote = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تسديد دفعة (-)", style = MaterialTheme.typography.labelLarge, color = EmeraldLuminous)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CrimsonContainer.copy(alpha = 0.7f))
                                .border(1.dp, CrimsonSoft.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable(enabled = parsedAmount > 0.0) {
                                    onRecordCredit(parsedAmount, creditNote)
                                    creditAmountStr = ""
                                    creditNote = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("إضافة كريدي (+)", style = MaterialTheme.typography.labelLarge, color = CrimsonSoft)
                        }
                    }

                    if (creditLogs.isNotEmpty()) {
                        Text("سجل حركات الكريدي", style = MaterialTheme.typography.labelMedium, color = SoftSilver)
                        creditLogs.take(5).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianDeep.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.note, style = MaterialTheme.typography.bodySmall, color = WarmCream)
                                    Text(formatDateTimeArabic(log.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = SoftSilver)
                                }
                                Text(
                                    text = (if (log.amountDelta > 0) "+" else "") + formatMoney(log.amountDelta, settings.currencySymbol),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (log.amountDelta > 0) CrimsonSoft else EmeraldLuminous
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomerSheet(
    defaultLoyaltyMode: String,
    onSave: (name: String, phone: String?, notes: String?, loyaltyMode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(defaultLoyaltyMode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = WarmCream,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إصدار بطاقة زبون جديدة (QR)", style = MaterialTheme.typography.titleLarge, color = WarmCream)
                GlassIconButton(icon = Icons.Rounded.Close, contentDescription = "إغلاق", onClick = onDismiss, tint = SoftSilver)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_customer_name_input"),
                label = { Text("اسم الزبون الكامل *", color = SoftSilver) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = ObsidianElevated,
                    unfocusedContainerColor = ObsidianElevated,
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream
                )
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رقم الهاتف (للمراسلة عبر واتساب)", color = SoftSilver) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = ObsidianElevated,
                    unfocusedContainerColor = ObsidianElevated,
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream
                )
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("العطور المفضلة / ملاحظات", color = SoftSilver) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = ObsidianElevated,
                    unfocusedContainerColor = ObsidianElevated,
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassFilterChip(
                    label = "بطاقة 10 أختام",
                    selected = mode == LoyaltyModes.STAMPS,
                    onClick = { mode = LoyaltyModes.STAMPS },
                    modifier = Modifier.weight(1f)
                )
                GlassFilterChip(
                    label = "عداد نقاط مرن",
                    selected = mode == LoyaltyModes.POINTS,
                    onClick = { mode = LoyaltyModes.POINTS },
                    modifier = Modifier.weight(1f)
                )
            }
            GoldGradientButton(
                text = "إنشاء الزبون وإصدار QR",
                icon = Icons.Rounded.PersonAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                tag = "confirm_add_customer_button",
                onClick = { onSave(name, phone.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() }, mode) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
