package com.example.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomerEntity
import com.example.data.local.LookupEntity
import com.example.data.local.PaymentMethods
import com.example.data.local.ProductEntity
import com.example.data.local.ShopSettingsEntity
import com.example.data.repository.CartLineDraft
import com.example.ui.PosCartState
import com.example.ui.components.GlassContainer
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GoldGradientButton
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGoldGradient
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GlassDarkTint
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDarkInk
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.example.util.formatDecimalQty
import com.example.util.formatMoney
import com.example.util.formatNumberPlain
import com.example.util.resolvePerfumeDrawable

@Composable
fun PosScreen(
    products: List<ProductEntity>,
    categories: List<LookupEntity>,
    quickSizes: List<LookupEntity>,
    cart: PosCartState,
    attachedCustomer: CustomerEntity?,
    settings: ShopSettingsEntity,
    onAddToCart: (CartLineDraft) -> Unit,
    onRemoveCartLine: (String) -> Unit,
    onClearCart: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onCartDiscountChange: (Double) -> Unit,
    onDetachCustomer: () -> Unit,
    onOpenCustomerQrScanner: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedProductForSheet by remember { mutableStateOf<ProductEntity?>(null) }
    var showCustomSaleSheet by remember { mutableStateOf(false) }
    var showCartCheckoutSheet by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchesCat = selectedCategory == null || prod.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                (prod.subtitle?.contains(searchQuery, ignoreCase = true) == true) ||
                (prod.barcode?.contains(searchQuery, ignoreCase = true) == true)
            matchesCat && matchesQuery
        }
    }

    val cartProductIds = remember(cart.items) {
        cart.items.mapNotNull { it.productId }.toSet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (cart.items.isNotEmpty()) 196.dp else 110.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar + Scan Customer QR + Quick Custom Sale Row
            item(span = { GridItemSpan(maxLineSpan) }) {
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
                                .testTag("pos_search_input"),
                            placeholder = {
                                Text(
                                    text = "ابحث بالاسم أو الباركود...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SoftSilver,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = GoldPrimary)
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = "مسح باركود المنتج",
                                    tint = GoldBright,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { onOpenBarcodeScanner() }
                                )
                            },
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

                        // Glowing Gold Scan Customer QR Button
                        Box(
                            modifier = Modifier
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ChampagneGoldGradient)
                                .border(1.dp, GoldBright, RoundedCornerShape(16.dp))
                                .clickable { onOpenCustomerQrScanner() }
                                .padding(horizontal = 12.dp)
                                .testTag("pos_scan_customer_qr_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = "مسح QR الزبون",
                                    tint = GoldDarkInk,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "QR الزبون",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = GoldDarkInk
                                )
                            }
                        }
                    }

                    // Quick Custom Sale CTA + Category Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassFilterChip(
                            label = "بيع حر سريع",
                            selected = false,
                            leadingIcon = Icons.Rounded.AutoAwesome,
                            onClick = { showCustomSaleSheet = true },
                            modifier = Modifier.testTag("pos_quick_custom_sale_button")
                        )
                        GlassFilterChip(
                            label = "الكل",
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                        categories.forEach { cat ->
                            GlassFilterChip(
                                label = cat.label,
                                selected = selectedCategory == cat.label,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat.label) null else cat.label
                                }
                            )
                        }
                    }
                }
            }

            // Attached VIP Customer Stamp Preview Banner (when scanned)
            if (attachedCustomer != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AttachedCustomerBanner(
                        customer = attachedCustomer,
                        stampTarget = settings.stampTarget,
                        currencySymbol = settings.currencySymbol,
                        onChangeCustomer = onOpenCustomerQrScanner,
                        onDetach = onDetachCustomer
                    )
                }
            }

            // Section Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                        )
                        Text(
                            text = "العطور السريعة والتركيبات",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmCream
                        )
                    }
                    Text(
                        text = "${filteredProducts.size} منتج",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftSilver
                    )
                }
            }

            // Product Tiles
            if (filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GlassContainer(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                            Text("لا توجد عطور مطابقة للبحث", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                            Text("يمكنك استخدام زر \"بيع حر سريع\" لأي منتج غير مسجل", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    PosProductTile(
                        product = product,
                        inCart = product.id in cartProductIds,
                        currencySymbol = settings.currencySymbol,
                        onClick = { selectedProductForSheet = product }
                    )
                }
            }
        }

        // Floating Glass Cart Summary Bar (Detached above Bottom Nav)
        AnimatedVisibility(
            visible = cart.items.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 92.dp)
        ) {
            FloatingCartSummaryBar(
                cart = cart,
                attachedCustomer = attachedCustomer,
                currencySymbol = settings.currencySymbol,
                onPaymentToggle = onPaymentMethodChange,
                onOpenCartSheet = { showCartCheckoutSheet = true },
                onQuickCheckout = onCheckout
            )
        }
    }

    selectedProductForSheet?.let { product ->
        ProductSaleBottomSheet(
            product = product,
            quickSizes = quickSizes,
            currencySymbol = settings.currencySymbol,
            onAddToCart = onAddToCart,
            onDismiss = { selectedProductForSheet = null }
        )
    }

    if (showCustomSaleSheet) {
        QuickCustomSaleSheet(
            currencySymbol = settings.currencySymbol,
            onAddCustomSale = onAddToCart,
            onDismiss = { showCustomSaleSheet = false }
        )
    }

    if (showCartCheckoutSheet) {
        CartCheckoutSheet(
            cart = cart,
            attachedCustomer = attachedCustomer,
            stampTarget = settings.stampTarget,
            currencySymbol = settings.currencySymbol,
            onRemoveLine = onRemoveCartLine,
            onClearCart = {
                onClearCart()
                showCartCheckoutSheet = false
            },
            onPaymentMethodChange = onPaymentMethodChange,
            onCartDiscountChange = onCartDiscountChange,
            onScanCustomerClick = {
                showCartCheckoutSheet = false
                onOpenCustomerQrScanner()
            },
            onDetachCustomer = onDetachCustomer,
            onCheckout = onCheckout,
            onDismiss = { showCartCheckoutSheet = false }
        )
    }
}

@Composable
private fun PosProductTile(
    product: ProductEntity,
    inCart: Boolean,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isLowStock = product.currentStock <= product.minStockAlert

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_product_tile_${product.id}"),
        highlighted = inCart,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(10.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianDeep)
            ) {
                Image(
                    painter = painterResource(id = resolvePerfumeDrawable(product.imageKey)),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Category Pill (top-start)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ObsidianDeep.copy(alpha = 0.78f))
                        .border(0.7.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBright,
                        maxLines = 1
                    )
                }
                // In-Cart Check Badge (top-end)
                if (inCart) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "في السلة",
                            tint = GoldDarkInk,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                // Stock Badge (bottom-start)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isLowStock) AmberWarning.copy(alpha = 0.25f)
                            else ObsidianDeep.copy(alpha = 0.80f)
                        )
                        .border(
                            0.7.dp,
                            if (isLowStock) AmberWarning else EmeraldLuminous.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${formatDecimalQty(product.currentStock)} ${product.unitLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLowStock) AmberWarning else EmeraldLuminous
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = WarmCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!product.subtitle.isNullOrBlank()) {
                Text(
                    text = product.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatNumberPlain(product.sellPrice),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        color = GoldBright
                    )
                    Text(
                        text = "$currencySymbol / ${product.unitLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftSilver
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (inCart) GoldPrimary else GoldPrimary.copy(alpha = 0.18f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "إضافة ${product.name}",
                        tint = if (inCart) GoldDarkInk else GoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachedCustomerBanner(
    customer: CustomerEntity,
    stampTarget: Int,
    currencySymbol: String,
    onChangeCustomer: () -> Unit,
    onDetach: () -> Unit
) {
    val nextStamp = customer.stamps + 1
    val willUnlockGift = nextStamp >= stampTarget

    GlassContainer(
        modifier = Modifier.fillMaxWidth(),
        highlighted = true,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ChampagneGoldGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = GoldDarkInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
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
                                color = GoldBright
                            )
                        }
                        Text(
                            text = if (willUnlockGift) {
                                "🎁 هذا الشراء يكمل $stampTarget أختام ويؤهله لعطر هدية!"
                            } else {
                                "الأختام الحالية: ${customer.stamps}/$stampTarget • الكريدي: ${formatMoney(customer.creditBalance, currencySymbol)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (willUnlockGift) GoldBright else EmeraldLuminous
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "تغيير",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldPrimary,
                        modifier = Modifier
                            .clickable { onChangeCustomer() }
                            .padding(6.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إزالة الزبون",
                        tint = SoftSilver,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDetach() }
                    )
                }
            }

            // Mini 10-stamp tracker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianDeep.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val count = stampTarget.coerceAtMost(10)
                    repeat(count) { idx ->
                        val filled = idx < customer.stamps
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) ChampagneGoldGradient
                                    else androidx.compose.ui.graphics.SolidColor(ObsidianElevated)
                                )
                                .border(0.8.dp, GoldPrimary.copy(alpha = 0.55f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filled) "HP" else "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = if (filled) GoldDarkInk else SoftSilver
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CardGiftcard,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "+1 ختم",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldBright
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingCartSummaryBar(
    cart: PosCartState,
    attachedCustomer: CustomerEntity?,
    currencySymbol: String,
    onPaymentToggle: (String) -> Unit,
    onOpenCartSheet: () -> Unit,
    onQuickCheckout: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassDarkTint)
            .border(1.2.dp, ActiveGoldBorderBrush, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Cart Count & Total (Tap to open full checkout sheet)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenCartSheet() }
                    .testTag("floating_cart_expand_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldPrimary.copy(alpha = 0.2f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = formatMoney(cart.netTotal, currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        color = GoldBright
                    )
                    Text(
                        text = "${cart.itemCount} أصناف • " + (attachedCustomer?.name ?: "اضغط لتفاصيل السلة"),
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Cash vs Credit mini pill toggle
            val isCash = cart.paymentMethod == PaymentMethods.CASH
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianDeep)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isCash) GoldPrimary else Color.Transparent)
                        .clickable { onPaymentToggle(PaymentMethods.CASH) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "نقداً",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCash) GoldDarkInk else SoftSilver
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (!isCash) GoldPrimary else Color.Transparent)
                        .clickable { onPaymentToggle(PaymentMethods.CREDIT) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "كريدي",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (!isCash) GoldDarkInk else SoftSilver
                    )
                }
            }
        }

        GoldGradientButton(
            text = "إتمام البيع الآن",
            icon = Icons.Rounded.Bolt,
            height = 46.dp,
            modifier = Modifier.fillMaxWidth(),
            tag = "floating_cart_checkout_button",
            onClick = onQuickCheckout
        )
    }
}
