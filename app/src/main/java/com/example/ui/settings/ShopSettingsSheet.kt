package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.LookupEntity
import com.example.data.local.LookupTypes
import com.example.data.local.LoyaltyModes
import com.example.data.local.ShopSettingsEntity
import com.example.data.repository.SyncState
import com.example.ui.components.GlassContainer
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GoldGradientButton
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShopSettingsSheet(
    settings: ShopSettingsEntity,
    userEmail: String?,
    syncState: SyncState,
    categories: List<LookupEntity>,
    units: List<LookupEntity>,
    quickSizes: List<LookupEntity>,
    onSaveSettings: (ShopSettingsEntity) -> Unit,
    onAddLookup: (String, String) -> Unit,
    onDeleteLookup: (String) -> Unit,
    onManualSync: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var shopName by remember(settings) { mutableStateOf(settings.shopName) }
    var currency by remember(settings) { mutableStateOf(settings.currencySymbol) }
    var loyaltyMode by remember(settings) { mutableStateOf(settings.defaultLoyaltyMode) }
    var stampTargetStr by remember(settings) { mutableStateOf(settings.stampTarget.toString()) }
    var rewardText by remember(settings) { mutableStateOf(settings.rewardText) }

    var newCategoryText by remember { mutableStateOf("") }
    var newUnitText by remember { mutableStateOf("") }
    var newQuickSizeText by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = GoldBright)
                    Text(
                        text = "إعدادات المحل والمرونة",
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmCream
                    )
                }
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "إغلاق",
                    onClick = onDismiss,
                    tint = SoftSilver
                )
            }

            // Cloud Account & Sync Card
            GlassContainer(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحساب السحابي والمزامنة",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldBright
                            )
                            Text(
                                text = userEmail ?: "مستخدم Google",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmCream
                            )
                            Text(
                                text = when (syncState) {
                                    is SyncState.Synced -> "متصل • محفوظ محلياً وسحابياً"
                                    is SyncState.Syncing -> "جارٍ المزامنة مع السحابة..."
                                    is SyncState.OfflineOrError -> syncState.message
                                    SyncState.Idle -> "جاهز للمزامنة"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (syncState is SyncState.Synced) EmeraldLuminous else SoftSilver
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldGradientButton(
                            text = "مزامنة الآن",
                            icon = Icons.Rounded.CloudSync,
                            height = 44.dp,
                            modifier = Modifier.weight(1f),
                            tag = "settings_sync_button",
                            onClick = onManualSync
                        )
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CrimsonContainer.copy(alpha = 0.7f))
                                .border(1.dp, CrimsonSoft.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .clickable { onSignOut() }
                                .padding(horizontal = 14.dp)
                                .testTag("settings_sign_out_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Logout,
                                    contentDescription = "تسجيل الخروج",
                                    tint = CrimsonSoft,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "خروج",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CrimsonSoft
                                )
                            }
                        }
                    }
                }
            }

            // Branding, Currency & Loyalty Defaults
            GlassContainer(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "الهوية والعملة ونظام الولاء",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBright
                    )
                    SettingsField(
                        label = "اسم المحل",
                        value = shopName,
                        onValueChange = { shopName = it }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsField(
                            label = "العملة (مثل د.ج / ر.س)",
                            value = currency,
                            onValueChange = { currency = it },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsField(
                            label = "عدد الأختام للهدية",
                            value = stampTargetStr,
                            onValueChange = { stampTargetStr = it.filter { ch -> ch.isDigit() } },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassFilterChip(
                            label = "نظام الأختام (10 أختام)",
                            selected = loyaltyMode == LoyaltyModes.STAMPS,
                            onClick = { loyaltyMode = LoyaltyModes.STAMPS },
                            modifier = Modifier.weight(1f)
                        )
                        GlassFilterChip(
                            label = "نظام النقاط المرن",
                            selected = loyaltyMode == LoyaltyModes.POINTS,
                            onClick = { loyaltyMode = LoyaltyModes.POINTS },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SettingsField(
                        label = "نص مكافأة البطاقة الرقمية",
                        value = rewardText,
                        onValueChange = { rewardText = it }
                    )
                    GoldGradientButton(
                        text = "حفظ إعدادات المحل",
                        icon = Icons.Rounded.Save,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "save_shop_settings_button",
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    shopName = shopName.trim().ifBlank { "حيدر للعطور" },
                                    currencySymbol = currency.trim().ifBlank { "د.ج" },
                                    defaultLoyaltyMode = loyaltyMode,
                                    stampTarget = (stampTargetStr.toIntOrNull() ?: 10).coerceIn(1, 20),
                                    rewardText = rewardText.trim()
                                        .ifBlank { "بعد إكمال 10 ختمات .. تحصل على عطر هدية" }
                                )
                            )
                        }
                    )
                }
            }

            // Dynamic Editable Lookup Tables
            EditableLookupSection(
                title = "تصنيفات العطور القابلة للتعديل",
                placeholder = "إضافة تصنيف (مثل: نيش فاخر)",
                items = categories,
                inputValue = newCategoryText,
                onInputChange = { newCategoryText = it },
                onAdd = {
                    if (newCategoryText.isNotBlank()) {
                        onAddLookup(LookupTypes.CATEGORY, newCategoryText)
                        newCategoryText = ""
                    }
                },
                onDelete = onDeleteLookup
            )

            EditableLookupSection(
                title = "وحدات القياس (مل، قارورة، تولة...)",
                placeholder = "إضافة وحدة قياس",
                items = units,
                inputValue = newUnitText,
                onInputChange = { newUnitText = it },
                onAdd = {
                    if (newUnitText.isNotBlank()) {
                        onAddLookup(LookupTypes.UNIT, newUnitText)
                        newUnitText = ""
                    }
                },
                onDelete = onDeleteLookup
            )

            EditableLookupSection(
                title = "أزرار الأحجام السريعة في نقطة البيع",
                placeholder = "مثل: 15 أو 75",
                items = quickSizes,
                inputValue = newQuickSizeText,
                onInputChange = { newQuickSizeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                onAdd = {
                    if (newQuickSizeText.isNotBlank()) {
                        onAddLookup(LookupTypes.QUICK_SIZE, newQuickSizeText)
                        newQuickSizeText = ""
                    }
                },
                onDelete = onDeleteLookup
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableLookupSection(
    title: String,
    placeholder: String,
    items: List<LookupEntity>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit
) {
    GlassContainer(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GoldBright
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ObsidianElevated)
                            .border(1.dp, Color(0x33FFD782), RoundedCornerShape(50))
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmCream
                        )
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "حذف ${entry.label}",
                            tint = SoftSilver,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDelete(entry.id) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(placeholder, color = SoftSilver) },
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
                GlassIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "إضافة",
                    highlighted = true,
                    onClick = onAdd
                )
            }
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
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
