package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.GlassContainer
import com.example.ui.components.GoldGradientButton
import com.example.ui.components.ObsidianAmbientBackground
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingSession by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSession = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSession = false
            },
            scope = scope
        )
    }

    ObsidianAmbientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Haider Parfums Emblem Ring
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(2.dp, ActiveGoldBorderBrush, CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.haider_app_icon),
                        contentDescription = "شعار حيدر للعطور",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "حيدر للعطور",
                    style = MaterialTheme.typography.displayLarge,
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "HAIDER PARFUMS • نظام الإدارة الذكي",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftSilver,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(26.dp))

                GlassContainer(
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = true,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Rounded.Storefront,
                            title = "نقطة بيع فائقة المرونة",
                            subtitle = "بيع بالمليلتر والفواصل العشرية أو بالمبلغ مباشرة"
                        )
                        FeatureRow(
                            icon = Icons.Rounded.QrCodeScanner,
                            title = "ولاء رقمي بالـ QR وختم فوري",
                            subtitle = "مسح بطاقة الزبون مع كل شراء وتصدير PNG للواتساب"
                        )
                        FeatureRow(
                            icon = Icons.Rounded.CloudDone,
                            title = "عمل فوري بدون إنترنت + نسخ سحابي",
                            subtitle = "قاعدة SQLite محلية سريعة مع مزامنة آمنة لحسابك"
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CrimsonContainer.copy(alpha = 0.75f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonSoft,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (isCheckingSession || isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = GoldBright,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (isCheckingSession) "جارٍ التحقق من الجلسة..." else "جارٍ تسجيل الدخول...",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                        }
                    }
                } else {
                    GoldGradientButton(
                        text = "تسجيل الدخول بحساب Google",
                        icon = Icons.Rounded.AutoAwesome,
                        modifier = Modifier.fillMaxWidth(),
                        height = 56.dp,
                        tag = "google_sign_in_button",
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "يتم حفظ بيانات المحل محلياً ومزامنتها بشكل خاص وآمن مع حساب Google الخاص بك",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftSilver.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GoldPrimary.copy(alpha = 0.16f))
                .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = WarmCream
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SoftSilver
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(EmeraldLuminous)
        )
    }
}
