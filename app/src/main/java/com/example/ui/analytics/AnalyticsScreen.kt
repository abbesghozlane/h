package com.example.ui.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PaymentMethods
import com.example.data.local.SaleItemEntity
import com.example.data.local.SaleWithItems
import com.example.ui.AnalyticsSummary
import com.example.ui.DaySalesPoint
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
import com.example.util.formatDateTimeArabic
import com.example.util.formatDecimalQty
import com.example.util.formatMoney
import com.example.util.formatNumberPlain
import kotlin.math.max

@Composable
fun AnalyticsScreen(
    analytics: AnalyticsSummary,
    salesWithItems: List<SaleWithItems>,
    currencySymbol: String,
    onUpdateInvoice: (String, List<SaleItemEntity>, String, Double) -> Unit,
    onReturnInvoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSaleForEdit by remember { mutableStateOf<SaleWithItems?>(null) }
    var selectedChartIndex by remember { mutableStateOf(6) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 2x2 Glass KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiGlassCard(
                        title = "مبيعات اليوم",
                        value = formatMoney(analytics.todaySales, currencySymbol),
                        icon = Icons.Rounded.TrendingUp,
                        accent = GoldBright,
                        modifier = Modifier.weight(1f)
                    )
                    KpiGlassCard(
                        title = "صافي الربح التقديري",
                        value = formatMoney(analytics.estimatedNetProfit, currencySymbol),
                        icon = Icons.Rounded.EmojiEvents,
                        accent = EmeraldLuminous,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiGlassCard(
                        title = "إجمالي الكريدي",
                        value = formatMoney(analytics.totalCreditOwed, currencySymbol),
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accent = CrimsonSoft,
                        modifier = Modifier.weight(1f)
                    )
                    KpiGlassCard(
                        title = "نواقص المخزون",
                        value = "${analytics.lowStockCount} أصناف",
                        icon = Icons.Rounded.WarningAmber,
                        accent = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Weekly Sales Glowing Gold Spline Chart
        item {
            val points = analytics.weeklySales
            val safeIdx = selectedChartIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
            val highlightedDay = points.getOrNull(safeIdx)

            GlassContainer(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مبيعات الأسبوع (7 أيام)", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                            if (highlightedDay != null) {
                                Text(
                                    text = "${highlightedDay.dayLabel}: ${formatMoney(highlightedDay.totalSales, currencySymbol)} (${highlightedDay.invoiceCount} فواتير)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldBright
                                )
                            }
                        }
                        Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = GoldBright)
                    }

                    WeeklyGoldSplineChart(
                        points = points,
                        selectedIndex = safeIdx,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                    )

                    if (points.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            points.forEachIndexed { idx, pt ->
                                val selected = idx == safeIdx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) GoldPrimary.copy(alpha = 0.22f) else Color.Transparent)
                                        .clickable { selectedChartIndex = idx }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = pt.dayLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) GoldBright else SoftSilver
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top 5 Selling Perfumes Leaderboard
        item {
            GlassContainer(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = GoldBright)
                        Text("أكثر 5 عطور مبيعاً", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                    }
                    if (analytics.topPerfumes.isEmpty()) {
                        Text("ستظهر أفضل العطور مبيعاً هنا فور تسجيل أول فاتورة", style = MaterialTheme.typography.bodySmall, color = SoftSilver)
                    } else {
                        val maxRev = analytics.topPerfumes.maxOf { it.totalRevenue }.coerceAtLeast(1.0)
                        analytics.topPerfumes.forEachIndexed { index, stat ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (index == 0) ChampagneGoldGradient else Brush.linearGradient(listOf(ObsidianElevated, ObsidianElevated)))
                                                .border(0.8.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (index == 0) GoldDarkInk else GoldBright
                                            )
                                        }
                                        Text(
                                            text = stat.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = WarmCream,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${formatDecimalQty(stat.totalQty)} ${stat.unitLabel} • ${formatMoney(stat.totalRevenue, currencySymbol)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldBright
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (stat.totalRevenue / maxRev).toFloat().coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = GoldPrimary,
                                    trackColor = ObsidianDeep
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Editable Invoices Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = GoldBright)
                    Text("الفواتير الأخيرة (قابلة للتعديل والمرتجع)", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                }
                Text("${salesWithItems.size} فاتورة", style = MaterialTheme.typography.labelSmall, color = SoftSilver)
            }
        }

        if (salesWithItems.isEmpty()) {
            item {
                GlassContainer(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = "لا توجد فواتير مسجلة بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSilver
                    )
                }
            }
        } else {
            items(salesWithItems, key = { it.sale.id }) { saleWithItems ->
                InvoiceRowCard(
                    saleWithItems = saleWithItems,
                    currencySymbol = currencySymbol,
                    onClick = { selectedSaleForEdit = saleWithItems }
                )
            }
        }
    }

    selectedSaleForEdit?.let { saleWithItems ->
        EditableInvoiceSheet(
            saleWithItems = saleWithItems,
            currencySymbol = currencySymbol,
            onSaveEdits = { updatedItems, method, discount ->
                onUpdateInvoice(saleWithItems.sale.id, updatedItems, method, discount)
                selectedSaleForEdit = null
            },
            onProcessReturn = {
                onReturnInvoice(saleWithItems.sale.id)
                selectedSaleForEdit = null
            },
            onDismiss = { selectedSaleForEdit = null }
        )
    }
}

@Composable
private fun KpiGlassCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassContainer(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = SoftSilver)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyGoldSplineChart(
    points: List<DaySalesPoint>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val maxVal = points.maxOf { it.totalSales }.coerceAtLeast(1000.0)
        val padH = 16.dp.toPx()
        val padTop = 16.dp.toPx()
        val padBottom = 14.dp.toPx()
        val chartW = (size.width - padH * 2).coerceAtLeast(1f)
        val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

        // Subtle horizontal guide lines
        for (i in 0..2) {
            val y = padTop + chartH * (i / 2f)
            drawLine(
                color = Color(0x1FFFFFFF),
                start = Offset(padH, y),
                end = Offset(size.width - padH, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val stepX = if (points.size > 1) chartW / (points.size - 1) else 0f
        val coords = points.mapIndexed { idx, pt ->
            val x = padH + idx * stepX
            val normalized = (pt.totalSales / maxVal).toFloat().coerceIn(0f, 1f)
            val y = padTop + chartH * (1f - normalized)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, padTop + chartH)
            lineTo(coords.first().x, padTop + chartH)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    GoldPrimary.copy(alpha = 0.42f),
                    GoldPrimary.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                startY = padTop,
                endY = padTop + chartH
            )
        )

        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(listOf(GoldPrimary, GoldBright, GoldPrimary)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        coords.forEachIndexed { idx, pt ->
            val isSel = idx == selectedIndex
            if (isSel) {
                drawCircle(
                    color = GoldBright.copy(alpha = 0.25f),
                    radius = 12.dp.toPx(),
                    center = pt
                )
            }
            drawCircle(
                color = if (isSel) GoldBright else ObsidianDeep,
                radius = if (isSel) 5.5.dp.toPx() else 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = GoldPrimary,
                radius = if (isSel) 5.5.dp.toPx() else 4.dp.toPx(),
                center = pt,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun InvoiceRowCard(
    saleWithItems: SaleWithItems,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val sale = saleWithItems.sale
    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_card_${sale.id}"),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(sale.invoiceNumber, style = MaterialTheme.typography.titleMedium, color = WarmCream)
                    if (sale.returned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CrimsonContainer)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("مرتجع", style = MaterialTheme.typography.labelSmall, color = CrimsonSoft)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (sale.paymentMethod == PaymentMethods.CREDIT) AmberWarning.copy(alpha = 0.18f)
                                    else EmeraldLuminous.copy(alpha = 0.16f)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (sale.paymentMethod == PaymentMethods.CREDIT) "كريدي" else "نقداً",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sale.paymentMethod == PaymentMethods.CREDIT) AmberWarning else EmeraldLuminous
                            )
                        }
                    }
                }
                Text(
                    text = (sale.customerNameSnapshot ?: "زبون عابر") + " • ${saleWithItems.items.size} أصناف • ${formatDateTimeArabic(sale.createdAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSilver
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(sale.totalAmount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (sale.returned) SoftSilver else GoldBright
                )
                if (!sale.returned) {
                    Text(
                        text = "ربح: +${formatNumberPlain(sale.estimatedProfit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLuminous
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableInvoiceSheet(
    saleWithItems: SaleWithItems,
    currencySymbol: String,
    onSaveEdits: (List<SaleItemEntity>, String, Double) -> Unit,
    onProcessReturn: () -> Unit,
    onDismiss: () -> Unit
) {
    val sale = saleWithItems.sale
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableItems = remember(saleWithItems) {
        mutableStateListOf<SaleItemEntity>().apply { addAll(saleWithItems.items) }
    }
    var paymentMethod by remember(sale) { mutableStateOf(sale.paymentMethod) }
    var discountStr by remember(sale) { mutableStateOf(formatDecimalQty(sale.discount)) }

    val subtotal = editableItems.sumOf { it.totalPrice }
    val parsedDiscount = (discountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val newTotal = max(0.0, subtotal - parsedDiscount)

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
                    Text("تعديل الفاتورة ${sale.invoiceNumber}", style = MaterialTheme.typography.titleLarge, color = WarmCream)
                    Text(
                        text = (sale.customerNameSnapshot ?: "زبون عابر") + " • " + formatDateTimeArabic(sale.createdAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftSilver
                    )
                }
                GlassIconButton(icon = Icons.Rounded.Close, contentDescription = "إغلاق", onClick = onDismiss, tint = SoftSilver)
            }

            if (sale.returned) {
                GlassContainer(
                    modifier = Modifier.fillMaxWidth(),
                    fillColor = CrimsonContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "هذه الفاتورة مسجلة كمرتجع وتمت استعادة المخزون وتحديث الرصيد مسبقاً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonSoft
                    )
                }
            }

            // Editable Invoice Line Items
            editableItems.forEachIndexed { idx, line ->
                GlassContainer(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(line.itemName, style = MaterialTheme.typography.titleMedium, color = WarmCream)
                            Text(formatMoney(line.totalPrice, currencySymbol), style = MaterialTheme.typography.titleSmall, color = GoldBright)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = formatDecimalQty(line.quantity),
                                onValueChange = { raw ->
                                    if (!sale.returned) {
                                        val q = raw.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        val nextTotal = max(0.0, q * line.actualUnitPrice - line.lineDiscount)
                                        editableItems[idx] = line.copy(quantity = q, totalPrice = nextTotal)
                                    }
                                },
                                enabled = !sale.returned,
                                modifier = Modifier.weight(1f),
                                label = { Text("الكمية (${line.unitLabel})", color = SoftSilver) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
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
                                value = formatDecimalQty(line.actualUnitPrice),
                                onValueChange = { raw ->
                                    if (!sale.returned) {
                                        val p = raw.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        val nextTotal = max(0.0, line.quantity * p - line.lineDiscount)
                                        editableItems[idx] = line.copy(actualUnitPrice = p, totalPrice = nextTotal)
                                    }
                                },
                                enabled = !sale.returned,
                                modifier = Modifier.weight(1f),
                                label = { Text("سعر الوحدة", color = SoftSilver) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
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
                    }
                }
            }

            if (!sale.returned) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassFilterChip(
                        label = "نقداً (Cash)",
                        selected = paymentMethod == PaymentMethods.CASH,
                        onClick = { paymentMethod = PaymentMethods.CASH },
                        modifier = Modifier.weight(1f)
                    )
                    GlassFilterChip(
                        label = "آجل / كريدي",
                        selected = paymentMethod == PaymentMethods.CREDIT,
                        onClick = { paymentMethod = PaymentMethods.CREDIT },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("خصم الفاتورة ($currencySymbol)", color = SoftSilver) },
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

                GlassContainer(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجمالي بعد التعديل", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                        Text(formatMoney(newTotal, currencySymbol), style = MaterialTheme.typography.headlineMedium, color = GoldBright)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CrimsonContainer.copy(alpha = 0.78f))
                            .border(1.dp, CrimsonSoft.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .clickable { onProcessReturn() }
                            .padding(horizontal = 14.dp)
                            .testTag("invoice_return_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = "مرتجع", tint = CrimsonSoft, modifier = Modifier.size(18.dp))
                            Text("مرتجع كامل", style = MaterialTheme.typography.labelLarge, color = CrimsonSoft)
                        }
                    }
                    GoldGradientButton(
                        text = "حفظ تعديلات الفاتورة",
                        icon = Icons.Rounded.Save,
                        modifier = Modifier.weight(1f),
                        tag = "invoice_save_edits_button",
                        onClick = { onSaveEdits(editableItems.toList(), paymentMethod, parsedDiscount) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
