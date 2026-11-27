package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.signOutUser
import com.example.data.repository.SyncState
import com.example.ui.ShopViewModel
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.components.FloatingGlassBottomBar
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassNavigationRail
import com.example.ui.components.NavTabItem
import com.example.ui.components.ObsidianAmbientBackground
import com.example.ui.inventory.InventoryScreen
import com.example.ui.loyalty.CustomersLoyaltyScreen
import com.example.ui.pos.PosScreen
import com.example.ui.scanner.QrScannerSheet
import com.example.ui.scanner.ScannerPurpose
import com.example.ui.settings.ShopSettingsSheet
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GlassDarkTint
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.HaiderParfumsTheme
import com.example.ui.theme.ObsidianHighest
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ROUTE_POS = "pos"
private const val ROUTE_INVENTORY = "inventory"
private const val ROUTE_LOYALTY = "loyalty"
private const val ROUTE_ANALYTICS = "analytics"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            HaiderParfumsTheme {
                HaiderParfumsRoot()
            }
        }
    }
}

@Composable
private fun HaiderParfumsRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = currentUser!!.uid
        val email = currentUser!!.email
        key(uid) {
            HaiderShopWorkspace(
                currentUserId = uid,
                userEmail = email
            )
        }
    }
}

@Composable
private fun HaiderShopWorkspace(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ShopViewModel = viewModel(
        key = currentUserId,
        factory = ShopViewModel.factory(context)
    )

    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val quickSizes by viewModel.quickSizes.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val creditLogs by viewModel.creditLogs.collectAsStateWithLifecycle()
    val salesWithItems by viewModel.salesWithItems.collectAsStateWithLifecycle()
    val settings by viewModel.shopSettings.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val attachedCustomer by viewModel.attachedCustomer.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(ROUTE_POS) }
    var scannerPurpose by remember { mutableStateOf<ScannerPurpose?>(null) }
    var scannerLaunchedFromLoyalty by remember { mutableStateOf(false) }
    var focusedCustomerId by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Support system back gesture returning to POS Home from secondary tabs
    BackHandler(enabled = selectedTab != ROUTE_POS) {
        selectedTab = ROUTE_POS
    }

    // Auto-dismiss glass toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800L)
            viewModel.clearToast()
        }
    }

    val navTabs = remember {
        listOf(
            NavTabItem(ROUTE_POS, "نقطة البيع", Icons.Outlined.PointOfSale, Icons.Rounded.PointOfSale, "nav_pos"),
            NavTabItem(ROUTE_INVENTORY, "المخزون", Icons.Outlined.Inventory2, Icons.Rounded.Inventory2, "nav_inventory"),
            NavTabItem(ROUTE_LOYALTY, "الزبائن والولاء", Icons.Outlined.Badge, Icons.Rounded.Badge, "nav_loyalty"),
            NavTabItem(ROUTE_ANALYTICS, "التقارير", Icons.Outlined.BarChart, Icons.Rounded.BarChart, "nav_analytics")
        )
    }

    ObsidianAmbientBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 680.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    GlassNavigationRail(
                        items = navTabs,
                        selectedRoute = selectedTab,
                        onSelect = { selectedTab = it },
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxHeight()
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Luxury Floating Glass Top Header
                        ShopTopHeader(
                            shopName = settings.shopName,
                            syncState = syncState,
                            onSyncClick = { viewModel.triggerManualSync() },
                            onSettingsClick = { showSettingsSheet = true }
                        )

                        // Active Tab Content
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedTab) {
                                ROUTE_POS -> PosScreen(
                                    products = products,
                                    categories = categories,
                                    quickSizes = quickSizes,
                                    cart = cart,
                                    attachedCustomer = attachedCustomer,
                                    settings = settings,
                                    onAddToCart = viewModel::addToCart,
                                    onRemoveCartLine = viewModel::removeCartItem,
                                    onClearCart = viewModel::clearCart,
                                    onPaymentMethodChange = viewModel::setPaymentMethod,
                                    onCartDiscountChange = viewModel::setCartDiscount,
                                    onDetachCustomer = { viewModel.attachCustomerToCart(null) },
                                    onOpenCustomerQrScanner = {
                                        scannerLaunchedFromLoyalty = false
                                        scannerPurpose = ScannerPurpose.CUSTOMER_QR
                                    },
                                    onOpenBarcodeScanner = {
                                        scannerLaunchedFromLoyalty = false
                                        scannerPurpose = ScannerPurpose.PRODUCT_BARCODE
                                    },
                                    onCheckout = viewModel::checkoutCart
                                )

                                ROUTE_INVENTORY -> InventoryScreen(
                                    products = products,
                                    categories = categories,
                                    units = units,
                                    currencySymbol = settings.currencySymbol,
                                    onSaveProduct = viewModel::saveProduct,
                                    onDeleteProduct = viewModel::deleteProduct,
                                    onAddLookup = viewModel::addLookup
                                )

                                ROUTE_LOYALTY -> CustomersLoyaltyScreen(
                                    customers = customers,
                                    creditLogs = creditLogs,
                                    settings = settings,
                                    focusedCustomerId = focusedCustomerId,
                                    onClearFocusedCustomer = { focusedCustomerId = null },
                                    onOpenQrScanner = {
                                        scannerLaunchedFromLoyalty = true
                                        scannerPurpose = ScannerPurpose.CUSTOMER_QR
                                    },
                                    onLinkCustomerToPos = { customer ->
                                        viewModel.attachCustomerToCart(customer.id)
                                        selectedTab = ROUTE_POS
                                    },
                                    onSaveCustomer = viewModel::saveCustomer,
                                    onAdjustStamps = viewModel::adjustCustomerStamps,
                                    onAdjustPoints = viewModel::adjustCustomerPoints,
                                    onRedeemReward = viewModel::redeemCustomerReward,
                                    onToggleLoyaltyMode = viewModel::toggleCustomerLoyaltyMode,
                                    onRecordCreditAdjustment = viewModel::recordCreditPaymentOrAdjustment,
                                    onDeleteCustomer = viewModel::deleteCustomer,
                                    onShowMessage = viewModel::showToast
                                )

                                ROUTE_ANALYTICS -> AnalyticsScreen(
                                    analytics = analytics,
                                    salesWithItems = salesWithItems,
                                    currencySymbol = settings.currencySymbol,
                                    onUpdateInvoice = viewModel::updateInvoice,
                                    onReturnInvoice = viewModel::returnInvoice
                                )
                            }
                        }
                    }

                    // Detached Pill-Shaped Floating Glass Bottom Navigation Bar (Handhelds)
                    if (!useRail) {
                        FloatingGlassBottomBar(
                            items = navTabs,
                            selectedRoute = selectedTab,
                            onSelect = { selectedTab = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // Frosted Glass Toast Notification Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = toastMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 74.dp, start = 20.dp, end = 20.dp)
                    ) {
                        toastMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ObsidianHighest.copy(alpha = 0.95f))
                                    .border(1.dp, ActiveGoldBorderBrush, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WarmCream
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Live CameraX QR & Barcode Scanner Bottom Sheet
    scannerPurpose?.let { purpose ->
        QrScannerSheet(
            purpose = purpose,
            customers = customers,
            stampTarget = settings.stampTarget,
            onCodeScanned = { code ->
                scannerPurpose = null
                if (purpose == ScannerPurpose.CUSTOMER_QR) {
                    scope.launch {
                        val found = viewModel.resolveScannedCustomer(code)
                        if (found != null) {
                            viewModel.attachCustomerToCart(found.id)
                            if (scannerLaunchedFromLoyalty) {
                                focusedCustomerId = found.id
                            }
                        } else {
                            viewModel.showToast("لم يتم العثور على زبون بالكود: $code")
                        }
                    }
                } else {
                    val prod = viewModel.findProductByBarcode(code)
                    if (prod != null) {
                        viewModel.showToast("تم العثور على ${prod.name} (${prod.barcode ?: ""})")
                    } else {
                        viewModel.showToast("لم يتم العثور على عطر بالباركود: $code")
                    }
                }
            },
            onCustomerPicked = { customer ->
                scannerPurpose = null
                viewModel.attachCustomerToCart(customer.id)
                if (scannerLaunchedFromLoyalty) {
                    focusedCustomerId = customer.id
                }
            },
            onDismiss = { scannerPurpose = null }
        )
    }

    // Shop Settings & Cloud Sync Sheet
    if (showSettingsSheet) {
        ShopSettingsSheet(
            settings = settings,
            userEmail = userEmail,
            syncState = syncState,
            categories = categories,
            units = units,
            quickSizes = quickSizes,
            onSaveSettings = viewModel::saveShopSettings,
            onAddLookup = viewModel::addLookup,
            onDeleteLookup = viewModel::deleteLookup,
            onManualSync = viewModel::triggerManualSync,
            onSignOut = {
                showSettingsSheet = false
                signOutUser(credentialManager = credentialManager, scope = scope)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun ShopTopHeader(
    shopName: String,
    syncState: SyncState,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassDarkTint.copy(alpha = 0.72f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.4.dp, ActiveGoldBorderBrush, CircleShape)
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.haider_app_icon),
                    contentDescription = shopName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "HAIDER PARFUMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        maxLines = 1
                    )
                }
                val (statusText, statusColor, statusIcon) = when (syncState) {
                    is SyncState.Synced -> Triple("محلي + سحابي نشط", EmeraldLuminous, Icons.Rounded.CloudDone)
                    is SyncState.Syncing -> Triple("جارٍ المزامنة السحابية...", GoldBright, Icons.Rounded.CloudSync)
                    is SyncState.OfflineOrError -> Triple("عمل محلي فوري (Offline)", AmberWarning, Icons.Rounded.CloudOff)
                    SyncState.Idle -> Triple("نقطة البيع نشطة", EmeraldLuminous, Icons.Rounded.CloudDone)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable { onSyncClick() }
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
        }

        GlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "إعدادات المحل والمزامنة",
            onClick = onSettingsClick,
            tint = GoldBright,
            modifier = Modifier.testTag("header_settings_button")
        )
    }
}
