package com.example.ui.pos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.data.local.LookupEntity
import com.example.data.local.PaymentMethods
import com.example.data.local.ProductEntity
import com.example.data.repository.CartLineDraft
import com.example.ui.PosCartState
import com.example.ui.components.GlassContainer
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GoldGradientButton
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GlassFillSubtle
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDarkInk
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.example.util.formatDecimalQty
import com.example.util.formatMoney
import com.example.util.formatNumberPlain
import com.example.util.resolvePerfumeDrawable
import kotlin.math.max

/**
 * Flexible Quantity & Price Bottom Sheet:
 * - Free decimal input (e.g. 12.5 ml)
 * - Non-restrictive quick-tap chips (10, 30, 50, 100...)
 * - "البيع بالمبلغ" toggle (enter amount directly & compute decimal volume or custom total)
 * - Editable Unit Price & Line Discount live
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSaleBottomSheet(
    product: ProductEntity,
    quickSizes: List<LookupEntity>,
    currencySymbol: String,
    onAddToCart: (CartLineDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isMlUnit = product.unitLabel.contains("مل") || product.unitLabel.equals("ml", ignoreCase = true)

    var sellByAmount by remember { mutableStateOf(false) }
    var quantityInput by remember { mutableStateOf(if (isMlUnit) "30" else "1") }
    var amountInput by remember {
        mutableStateOf(formatDecimalQty(if (isMlUnit) product.sellPrice * 30.0 else product.sellPrice))
    }
    var unitPriceInput by remember { mutableStateOf(formatDecimalQty(product.sellPrice)) }
    var discountInput by remember { mutableStateOf("0") }

    val parsedUnitPrice = (unitPriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val parsedDiscount = (discountInput.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

    val computedQuantity: Double
    val computedTotal: Double
    if (!sellByAmount) {
        computedQuantity = (quantityInput.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        computedTotal = max(0.0, (computedQuantity * parsedUnitPrice) - parsedDiscount)
    } else {
        val targetAmount = (amountInput.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        computedQuantity = if (parsedUnitPrice > 0.0) {
            ((targetAmount / parsedUnitPrice) * 100.0).toInt() / 100.0
        } else 1.0
        computedTotal = max(0.0, targetAmount - parsedDiscount)
    }

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Product Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = resolvePerfumeDrawable(product.imageKey)),
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, ActiveGoldBorderBrush, RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmCream
                        )
                        Text(
                            text = "المتوفر: ${formatDecimalQty(product.currentStock)} ${product.unitLabel} • ${product.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (product.currentStock <= product.minStockAlert) CrimsonSoft else EmeraldLuminous
                        )
                    }
                }
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "إغلاق",
                    onClick = onDismiss,
                    tint = SoftSilver
                )
            }

            // Segmented Toggle: By Quantity vs By Amount (البيع بالمبلغ)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianDeep)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val qtySelected = !sellByAmount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (qtySelected) GoldPrimary else Color.Transparent)
                        .clickable { sellByAmount = false }
                        .testTag("pos_mode_quantity"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = null,
                            tint = if (qtySelected) GoldDarkInk else SoftSilver,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "البيع بالكمية (${product.unitLabel})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (qtySelected) GoldDarkInk else SoftSilver
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sellByAmount) GoldPrimary else Color.Transparent)
                        .clickable { sellByAmount = true }
                        .testTag("pos_mode_amount"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = if (sellByAmount) GoldDarkInk else SoftSilver,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "البيع بالمبلغ ($currencySymbol)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (sellByAmount) GoldDarkInk else SoftSilver
                        )
                    }
                }
            }

            // Primary Free Decimal Quantity OR Fixed Amount Input
            if (!sellByAmount) {
                GlassContainer(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "الكمية المطلوبة (يدعم الفواصل مثل 12.5)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftSilver
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassIconButton(
                                icon = Icons.Rounded.Remove,
                                contentDescription = "إنقاص الكمية",
                                onClick = {
                                    val step = if (isMlUnit) 2.5 else 0.5
                                    val cur = quantityInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    quantityInput = formatDecimalQty(max(0.5, cur - step))
                                }
                            )
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { quantityInput = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pos_sheet_qty_input"),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    textAlign = TextAlign.Center,
                                    color = GoldBright
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(product.unitLabel, color = SoftSilver) },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedContainerColor = ObsidianDeep,
                                    unfocusedContainerColor = ObsidianDeep
                                )
                            )
                            GlassIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "زيادة الكمية",
                                highlighted = true,
                                onClick = {
                                    val step = if (isMlUnit) 2.5 else 0.5
                                    val cur = quantityInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    quantityInput = formatDecimalQty(cur + step)
                                }
                            )
                        }

                        // Optional Quick-Tap Speed Chips (Non-restrictive)
                        if (quickSizes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("سريع:", style = MaterialTheme.typography.labelSmall, color = SoftSilver)
                                quickSizes.forEach { sizeEntry ->
                                    val isCurrent = quantityInput == sizeEntry.label
                                    GlassFilterChip(
                                        label = "${sizeEntry.label} ${product.unitLabel}",
                                        selected = isCurrent,
                                        onClick = { quantityInput = sizeEntry.label }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                GlassContainer(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "أدخل المبلغ المطلوب من الزبون (مثل 500 $currencySymbol)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftSilver
                        )
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pos_sheet_amount_input"),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                color = GoldBright
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text(currencySymbol, color = GoldPrimary) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = ObsidianDeep,
                                unfocusedContainerColor = ObsidianDeep
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("500", "1000", "1500", "2000", "3000").forEach { amt ->
                                GlassFilterChip(
                                    label = "$amt $currencySymbol",
                                    selected = amountInput == amt,
                                    onClick = { amountInput = amt }
                                )
                            }
                        }
                        Text(
                            text = "الكمية المقابلة تلقائياً: ${formatDecimalQty(computedQuantity)} ${product.unitLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldLuminous
                        )
                    }
                }
            }

            // Live Editable Unit Price & Line Discount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = unitPriceInput,
                    onValueChange = { unitPriceInput = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_sheet_unit_price_input"),
                    label = { Text("سعر الوحدة ($currencySymbol)", color = SoftSilver) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    value = discountInput,
                    onValueChange = { discountInput = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_sheet_discount_input"),
                    label = { Text("خصم الصنف ($currencySymbol)", color = SoftSilver) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            }

            // Live Summary + Add CTA
            GlassContainer(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي الصنف", style = MaterialTheme.typography.labelMedium, color = SoftSilver)
                        Text(
                            text = "${formatDecimalQty(computedQuantity)} ${product.unitLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmCream
                        )
                    }
                    Text(
                        text = formatMoney(computedTotal, currencySymbol),
                        style = MaterialTheme.typography.displayMedium,
                        color = GoldBright
                    )
                }
            }

            GoldGradientButton(
                text = "إضافة إلى السلة",
                icon = Icons.Rounded.AddShoppingCart,
                modifier = Modifier.fillMaxWidth(),
                enabled = computedQuantity > 0.0 && computedTotal >= 0.0,
                tag = "pos_sheet_add_to_cart",
                onClick = {
                    val effectiveUnitPrice = if (sellByAmount && computedQuantity > 0.0) {
                        (computedTotal + parsedDiscount) / computedQuantity
                    } else {
                        parsedUnitPrice
                    }
                    onAddToCart(
                        CartLineDraft(
                            productId = product.id,
                            itemName = product.name,
                            unitLabel = product.unitLabel,
                            quantity = computedQuantity,
                            costUnitPrice = product.buyPrice,
                            actualUnitPrice = effectiveUnitPrice,
                            lineDiscount = parsedDiscount,
                            soldByAmount = sellByAmount
                        )
                    )
                    onDismiss()
                }
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Quick Custom Sale Bottom Sheet ("بيع حر سريع") for uncatalogued items (productId = null).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCustomSaleSheet(
    currencySymbol: String,
    onAddCustomSale: (CartLineDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var unitLabel by remember { mutableStateOf("مل") }
    var quantityStr by remember { mutableStateOf("1") }
    var totalOrUnitPriceStr by remember { mutableStateOf("") }
    var costPriceStr by remember { mutableStateOf("0") }

    val qty = (quantityStr.replace(",", ".").toDoubleOrNull() ?: 1.0).coerceAtLeast(0.1)
    val unitPrice = (totalOrUnitPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val costPrice = (costPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldBright)
                    Column {
                        Text("بيع حر سريع", style = MaterialTheme.typography.titleLarge, color = WarmCream)
                        Text("بيع فوري لأي تركيبة أو منتج غير مسجل في المخزون", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                    }
                }
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "إغلاق",
                    onClick = onDismiss,
                    tint = SoftSilver
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_sale_name_input"),
                label = { Text("اسم التركيبة أو المنتج الحر", color = SoftSilver) },
                placeholder = { Text("مثلاً: تركيبة عود ومسك خاصة 25مل", color = SoftSilver.copy(alpha = 0.6f)) },
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    modifier = Modifier.weight(1f),
                    label = { Text("الكمية", color = SoftSilver) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    value = unitLabel,
                    onValueChange = { unitLabel = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("الوحدة", color = SoftSilver) },
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
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = totalOrUnitPriceStr,
                    onValueChange = { totalOrUnitPriceStr = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_sale_price_input"),
                    label = { Text("سعر الوحدة ($currencySymbol)", color = SoftSilver) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    modifier = Modifier.weight(1f),
                    label = { Text("التكلفة التقديرية (اختياري)", color = SoftSilver) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            }

            GoldGradientButton(
                text = "إضافة للسلة • ${formatMoney(qty * unitPrice, currencySymbol)}",
                icon = Icons.Rounded.AddShoppingCart,
                modifier = Modifier.fillMaxWidth(),
                enabled = unitPrice > 0.0,
                tag = "custom_sale_submit_button",
                onClick = {
                    onAddCustomSale(
                        CartLineDraft(
                            productId = null,
                            itemName = name.trim().ifBlank { "بيع حر خاص" },
                            unitLabel = unitLabel.trim().ifBlank { "قطعة" },
                            quantity = qty,
                            costUnitPrice = costPrice,
                            actualUnitPrice = unitPrice,
                            lineDiscount = 0.0,
                            soldByAmount = false
                        )
                    )
                    onDismiss()
                }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Full Cart & Checkout Modal Bottom Sheet:
 * - Review/delete line items
 * - Live cart-level discount
 * - Cash vs Credit segmented toggle
 * - Attached VIP customer + QR scan trigger
 * - One-tap Checkout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCheckoutSheet(
    cart: PosCartState,
    attachedCustomer: CustomerEntity?,
    stampTarget: Int,
    currencySymbol: String,
    onRemoveLine: (String) -> Unit,
    onClearCart: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onCartDiscountChange: (Double) -> Unit,
    onScanCustomerClick: () -> Unit,
    onDetachCustomer: () -> Unit,
    onCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var discountText by remember(cart.cartDiscount) {
        mutableStateOf(if (cart.cartDiscount == 0.0) "" else formatDecimalQty(cart.cartDiscount))
    }

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("سلة المشتريات وإتمام البيع", style = MaterialTheme.typography.titleLarge, color = WarmCream)
                    Text("${cart.itemCount} أصناف في الفاتورة الحالية", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cart.items.isNotEmpty()) {
                        GlassIconButton(
                            icon = Icons.Rounded.DeleteOutline,
                            contentDescription = "إفراغ السلة",
                            tint = CrimsonSoft,
                            onClick = onClearCart
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "إغلاق",
                        tint = SoftSilver,
                        onClick = onDismiss
                    )
                }
            }

            // Cart Lines
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cart.items.forEach { line ->
                    GlassContainer(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.itemName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmCream
                                )
                                Text(
                                    text = "${formatDecimalQty(line.quantity)} ${line.unitLabel} × ${formatNumberPlain(line.actualUnitPrice)} $currencySymbol" +
                                        if (line.lineDiscount > 0) " • خصم ${formatNumberPlain(line.lineDiscount)}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftSilver
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatMoney(line.lineTotal, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GoldBright
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "حذف ${line.itemName}",
                                    tint = CrimsonSoft,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { onRemoveLine(line.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Attached Customer Card or Scan CTA
            GlassContainer(
                modifier = Modifier.fillMaxWidth(),
                highlighted = attachedCustomer != null,
                contentPadding = PaddingValues(12.dp),
                onClick = onScanCustomerClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (attachedCustomer != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${attachedCustomer.name} (${attachedCustomer.loyaltyCode})",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = "الأختام: ${attachedCustomer.stamps}/$stampTarget ← سيُضاف +1 ختم تلقائياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLuminous
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "إلغاء ربط الزبون",
                            tint = SoftSilver,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onDetachCustomer() }
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = GoldBright)
                            Column {
                                Text("ربط زبون عبر QR (للختم أو الكريدي)", style = MaterialTheme.typography.titleSmall, color = GoldBright)
                                Text("اضغط لمسح بطاقة الولاء قبل إتمام البيع", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                            }
                        }
                    }
                }
            }

            // Payment Method Segmented Glass Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isCash = cart.paymentMethod == PaymentMethods.CASH
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCash) GoldPrimary.copy(alpha = 0.22f) else ObsidianElevated)
                        .border(
                            1.dp,
                            if (isCash) GoldPrimary else Color(0x22FFFFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onPaymentMethodChange(PaymentMethods.CASH) }
                        .testTag("checkout_cash_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, tint = if (isCash) GoldBright else SoftSilver, modifier = Modifier.size(18.dp))
                        Text("نقداً (Cash)", style = MaterialTheme.typography.labelLarge, color = if (isCash) GoldBright else SoftSilver)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isCash) GoldPrimary.copy(alpha = 0.22f) else ObsidianElevated)
                        .border(
                            1.dp,
                            if (!isCash) GoldPrimary else Color(0x22FFFFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onPaymentMethodChange(PaymentMethods.CREDIT) }
                        .testTag("checkout_credit_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = if (!isCash) GoldBright else SoftSilver, modifier = Modifier.size(18.dp))
                        Text("آجل / كريدي", style = MaterialTheme.typography.labelLarge, color = if (!isCash) GoldBright else SoftSilver)
                    }
                }
            }

            // Editable Cart Discount
            OutlinedTextField(
                value = discountText,
                onValueChange = { raw ->
                    val filtered = raw.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                    discountText = filtered
                    onCartDiscountChange(filtered.replace(",", ".").toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("خصم إضافي على الفاتورة ($currencySymbol)", color = SoftSilver) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            // Totals Card
            GlassContainer(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع الفرعي", style = MaterialTheme.typography.bodyMedium, color = SoftSilver)
                        Text(formatMoney(cart.subtotal, currencySymbol), style = MaterialTheme.typography.titleMedium, color = WarmCream)
                    }
                    if (cart.cartDiscount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("خصم الفاتورة", style = MaterialTheme.typography.bodyMedium, color = EmeraldLuminous)
                            Text("- ${formatMoney(cart.cartDiscount, currencySymbol)}", style = MaterialTheme.typography.titleMedium, color = EmeraldLuminous)
                        }
                    }
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المبلغ الصافي للدفع", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                        Text(
                            text = formatMoney(cart.netTotal, currencySymbol),
                            style = MaterialTheme.typography.displayMedium,
                            color = GoldBright
                        )
                    }
                }
            }

            GoldGradientButton(
                text = "إتمام البيع وتأكيد الفاتورة",
                icon = Icons.Rounded.Bolt,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                enabled = cart.items.isNotEmpty(),
                tag = "confirm_checkout_button",
                onClick = {
                    onCheckout()
                    onDismiss()
                }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
