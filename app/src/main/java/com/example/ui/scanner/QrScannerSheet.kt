package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.CustomerEntity
import com.example.ui.components.GlassContainer
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GoldGradientButton
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.example.util.QrBarcodeAnalyzer
import java.util.concurrent.Executors

enum class ScannerPurpose {
    CUSTOMER_QR,
    PRODUCT_BARCODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerSheet(
    purpose: ScannerPurpose,
    customers: List<CustomerEntity>,
    stampTarget: Int,
    onCodeScanned: (String) -> Unit,
    onCustomerPicked: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var manualCode by remember { mutableStateOf("") }
    var hasDispatchedScan by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val filteredCustomers = remember(manualCode, customers) {
        if (manualCode.isBlank()) customers.take(5)
        else customers.filter {
            it.name.contains(manualCode, ignoreCase = true) ||
                it.loyaltyCode.contains(manualCode, ignoreCase = true) ||
                (it.phone?.contains(manualCode, ignoreCase = true) == true)
        }
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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (purpose == ScannerPurpose.CUSTOMER_QR) {
                                "مسح بطاقة ولاء الزبون (QR)"
                            } else {
                                "مسح باركود العطر"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmCream
                        )
                        Text(
                            text = if (purpose == ScannerPurpose.CUSTOMER_QR) {
                                "امسح الكود مع كل عملية شراء لإضافة الختم تلقائياً"
                            } else {
                                "وجّه الكاميرا نحو الباركود أو أدخله يدوياً"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftSilver
                        )
                    }
                }
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "إغلاق الماسح",
                    onClick = onDismiss,
                    tint = SoftSilver
                )
            }

            // Live CameraX Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ObsidianDeep)
                    .border(1.5.dp, ActiveGoldBorderBrush, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    CameraQrPreview(
                        onCodeDetected = { raw ->
                            if (!hasDispatchedScan) {
                                hasDispatchedScan = true
                                onCodeScanned(raw)
                            }
                        }
                    )
                    // Gold target reticle overlay
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .border(
                                width = 2.dp,
                                brush = ActiveGoldBorderBrush,
                                shape = RoundedCornerShape(20.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ObsidianDeep.copy(alpha = 0.78f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ضع رمز QR داخل الإطار الذهبي",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldBright
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "يلزم إذن الكاميرا للمسح المباشر، أو استخدم البحث اليدوي أدناه",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftSilver
                        )
                        GoldGradientButton(
                            text = "تفعيل الكاميرا",
                            height = 44.dp,
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                }
            }

            // Manual Code Fallback Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scanner_manual_input"),
                    placeholder = {
                        Text(
                            text = if (purpose == ScannerPurpose.CUSTOMER_QR) {
                                "أدخل PRF-1042 أو اسم الزبون..."
                            } else {
                                "أدخل الباركود أو اسم العطر..."
                            },
                            color = SoftSilver
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = GoldPrimary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedContainerColor = ObsidianElevated,
                        unfocusedContainerColor = ObsidianElevated,
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream
                    )
                )
                GoldGradientButton(
                    text = "تأكيد",
                    height = 54.dp,
                    enabled = manualCode.isNotBlank(),
                    tag = "scanner_confirm_manual",
                    onClick = { onCodeScanned(manualCode.trim()) }
                )
            }

            // Quick Customer Picker for instant selection in Customer QR mode
            if (purpose == ScannerPurpose.CUSTOMER_QR && filteredCustomers.isNotEmpty()) {
                Text(
                    text = "اختيار سريع للزبون",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldBright
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        GlassContainer(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            onClick = { onCustomerPicked(customer) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = GoldBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
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
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(EmeraldLuminous.copy(alpha = 0.14f))
                                        .border(0.8.dp, EmeraldLuminous.copy(alpha = 0.4f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${customer.stamps}/$stampTarget أختام",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldLuminous
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CameraQrPreview(
    onCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            runCatching {
                provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(
                            analysisExecutor,
                            QrBarcodeAnalyzer { code ->
                                mainExecutor.execute { onCodeDetected(code) }
                            }
                        )
                    }
                provider?.unbindAll()
                provider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
        }, mainExecutor)

        onDispose {
            runCatching { provider?.unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
