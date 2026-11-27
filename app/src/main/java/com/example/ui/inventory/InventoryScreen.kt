package com.example.ui.inventory

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LookupEntity
import com.example.data.local.LookupTypes
import com.example.data.local.ProductEntity
import com.example.ui.components.GlassContainer
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GoldGradientButton
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.example.util.PerfumeArtworkOptions
import com.example.util.formatDecimalQty
import com.example.util.formatMoney
import com.example.util.resolvePerfumeDrawable

@Composable
fun InventoryScreen(
    products: List<ProductEntity>,
    categories: List<LookupEntity>,
    units: List<LookupEntity>,
    currencySymbol: String,
    onSaveProduct: (
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
    ) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAddLookup: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showCreateProductSheet by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryLabel by remember { mutableStateOf("") }

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

    val lowStockCount = remember(products) {
        products.count { it.currentStock <= it.minStockAlert }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search & Add Product Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input"),
                        placeholder = { Text("ابحث في المخزون...", color = SoftSilver) },
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
                    GoldGradientButton(
                        text = "عطر جديد",
                        icon = Icons.Rounded.Add,
                        height = 54.dp,
                        tag = "inventory_add_product_button",
                        onClick = { showCreateProductSheet = true }
                    )
                }
            }

            // Editable/Addable Category Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassFilterChip(
                        label = "الكل (${products.size})",
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
                    GlassFilterChip(
                        label = "تصنيف جديد",
                        selected = false,
                        leadingIcon = Icons.Rounded.Add,
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.testTag("inventory_add_category_chip")
                    )
                }
            }

            // Low-Stock Alert Strip if any items are below threshold
            if (lowStockCount > 0) {
                item {
                    GlassContainer(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        fillColor = AmberWarning.copy(alpha = 0.12f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AmberWarning)
                            Text(
                                text = "تنبيه: يوجد $lowStockCount أصناف تحت الحد الأدنى للمخزون",
                                style = MaterialTheme.typography.titleSmall,
                                color = AmberWarning
                            )
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
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
                            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                            Text("المخزون فارغ في هذا التصنيف", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                            Text("اضغط على \"عطر جديد\" لإضافة منتجاتك بالمل أو القارورة", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    InventoryProductCard(
                        product = product,
                        currencySymbol = currencySymbol,
                        onClick = { editingProduct = product }
                    )
                }
            }
        }
    }

    if (showCreateProductSheet || editingProduct != null) {
        ProductEditorBottomSheet(
            initial = editingProduct,
            categories = categories,
            units = units,
            currencySymbol = currencySymbol,
            onSave = { name, subtitle, barcode, category, unitLabel, buyPrice, sellPrice, stock, minAlert, imgKey ->
                onSaveProduct(
                    editingProduct?.id,
                    name,
                    subtitle,
                    barcode,
                    category,
                    unitLabel,
                    buyPrice,
                    sellPrice,
                    stock,
                    minAlert,
                    imgKey
                )
                showCreateProductSheet = false
                editingProduct = null
            },
            onDelete = { id ->
                onDeleteProduct(id)
                editingProduct = null
            },
            onAddLookup = onAddLookup,
            onDismiss = {
                showCreateProductSheet = false
                editingProduct = null
            }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = GoldBright,
            textContentColor = WarmCream,
            title = { Text("إضافة تصنيف عطور جديد") },
            text = {
                OutlinedTextField(
                    value = newCategoryLabel,
                    onValueChange = { newCategoryLabel = it },
                    placeholder = { Text("مثلاً: نيش فاخر، بخور، دهن عود", color = SoftSilver) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryLabel.isNotBlank()) {
                            onAddLookup(LookupTypes.CATEGORY, newCategoryLabel)
                            newCategoryLabel = ""
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("إضافة", color = GoldBright, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("إلغاء", color = SoftSilver)
                }
            }
        )
    }
}

@Composable
private fun InventoryProductCard(
    product: ProductEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isLow = product.currentStock <= product.minStockAlert
    val targetReference = (product.minStockAlert * 3.0).coerceAtLeast(10.0)
    val stockRatio = (product.currentStock / targetReference).toFloat().coerceIn(0.05f, 1f)
    val barColor = when {
        product.currentStock <= product.minStockAlert * 0.5 -> CrimsonSoft
        isLow -> AmberWarning
        else -> EmeraldLuminous
    }
    val marginPerUnit = (product.sellPrice - product.buyPrice).coerceAtLeast(0.0)

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_card_${product.id}"),
        highlighted = isLow,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = resolvePerfumeDrawable(product.imageKey)),
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ActiveGoldBorderBrush, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmCream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GoldPrimary.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(product.category, style = MaterialTheme.typography.labelSmall, color = GoldBright)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "الشراء: ${formatMoney(product.buyPrice, currencySymbol)}  •  البيع: ${formatMoney(product.sellPrice, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftSilver
                    )
                    Text(
                        text = "هامش الربح: +${formatMoney(marginPerUnit, currencySymbol)} / ${product.unitLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLuminous
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "تعديل ${product.name}",
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Stock Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المخزون الحالي: ${formatDecimalQty(product.currentStock)} ${product.unitLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = barColor
                    )
                    Text(
                        text = "حد التنبيه: ${formatDecimalQty(product.minStockAlert)} ${product.unitLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftSilver
                    )
                }
                LinearProgressIndicator(
                    progress = { stockRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = barColor,
                    trackColor = ObsidianDeep
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorBottomSheet(
    initial: ProductEntity?,
    categories: List<LookupEntity>,
    units: List<LookupEntity>,
    currencySymbol: String,
    onSave: (
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
    ) -> Unit,
    onDelete: (String) -> Unit,
    onAddLookup: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var subtitle by remember { mutableStateOf(initial?.subtitle ?: "") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: categories.firstOrNull()?.label ?: "شرقي") }
    var unitLabel by remember { mutableStateOf(initial?.unitLabel ?: units.firstOrNull()?.label ?: "مل") }
    var buyPriceStr by remember { mutableStateOf(initial?.let { formatDecimalQty(it.buyPrice) } ?: "") }
    var sellPriceStr by remember { mutableStateOf(initial?.let { formatDecimalQty(it.sellPrice) } ?: "") }
    var stockStr by remember { mutableStateOf(initial?.let { formatDecimalQty(it.currentStock) } ?: "") }
    var minAlertStr by remember { mutableStateOf(initial?.let { formatDecimalQty(it.minStockAlert) } ?: "10") }
    var imageKey by remember { mutableStateOf(initial?.imageKey ?: "royal_oud") }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initial == null) "إضافة عطر جديد للمخزون" else "تعديل بيانات العطر",
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmCream
                )
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "إغلاق",
                    onClick = onDismiss,
                    tint = SoftSilver
                )
            }

            // Bottle Artwork Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PerfumeArtworkOptions.forEach { option ->
                    val selected = imageKey == option.key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) GoldBright else Color(0x28FFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { imageKey = option.key }
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = option.resId),
                            contentDescription = option.label,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) GoldBright else SoftSilver
                        )
                    }
                }
            }

            EditorField(
                label = "اسم العطر أو التركيبة *",
                value = name,
                onValueChange = { name = it },
                tag = "product_name_input"
            )
            EditorField(
                label = "وصف مختصر / النوتات العطرية",
                value = subtitle,
                onValueChange = { subtitle = it }
            )
            EditorField(
                label = "الباركود أو الكود المختصر (اختياري)",
                value = barcode,
                onValueChange = { barcode = it }
            )

            // Dynamic Category Selector + Free Text
            Text("التصنيف (اختر أو اكتب تصنيفاً حراً)", style = MaterialTheme.typography.labelMedium, color = GoldBright)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    GlassFilterChip(
                        label = cat.label,
                        selected = category == cat.label,
                        onClick = { category = cat.label }
                    )
                }
            }
            EditorField(label = "اسم التصنيف", value = category, onValueChange = { category = it })

            // Dynamic Unit Selector + Free Text
            Text("وحدة القياس (مل، قارورة، تولة...)", style = MaterialTheme.typography.labelMedium, color = GoldBright)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                units.forEach { u ->
                    GlassFilterChip(
                        label = u.label,
                        selected = unitLabel == u.label,
                        onClick = { unitLabel = u.label }
                    )
                }
            }
            EditorField(label = "الوحدة", value = unitLabel, onValueChange = { unitLabel = it })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorField(
                    label = "سعر الشراء ($currencySymbol)",
                    value = buyPriceStr,
                    onValueChange = { buyPriceStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                EditorField(
                    label = "سعر البيع ($currencySymbol)",
                    value = sellPriceStr,
                    onValueChange = { sellPriceStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    tag = "product_sell_price_input"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorField(
                    label = "المخزون الحالي (عشري)",
                    value = stockStr,
                    onValueChange = { stockStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                    tag = "product_stock_input"
                )
                EditorField(
                    label = "حد تنبيه النقص",
                    value = minAlertStr,
                    onValueChange = { minAlertStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initial != null) {
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CrimsonContainer.copy(alpha = 0.7f))
                            .border(1.dp, CrimsonSoft.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onDelete(initial.id) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف العطر", tint = CrimsonSoft)
                    }
                }
                GoldGradientButton(
                    text = if (initial == null) "إضافة للمخزون" else "حفظ التعديلات",
                    icon = Icons.Rounded.Save,
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                    tag = "save_product_button",
                    onClick = {
                        val cleanCat = category.trim().ifBlank { "شرقي" }
                        val cleanUnit = unitLabel.trim().ifBlank { "مل" }
                        if (categories.none { it.label == cleanCat }) {
                            onAddLookup(LookupTypes.CATEGORY, cleanCat)
                        }
                        if (units.none { it.label == cleanUnit }) {
                            onAddLookup(LookupTypes.UNIT, cleanUnit)
                        }
                        onSave(
                            name,
                            subtitle.takeIf { it.isNotBlank() },
                            barcode.takeIf { it.isNotBlank() },
                            cleanCat,
                            cleanUnit,
                            buyPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            sellPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            stockStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            minAlertStr.replace(",", ".").toDoubleOrNull() ?: 5.0,
                            imageKey
                        )
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    tag: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        label = { Text(label, color = SoftSilver) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
