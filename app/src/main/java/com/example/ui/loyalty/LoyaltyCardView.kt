package com.example.ui.loyalty

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CustomerEntity
import com.example.data.local.LoyaltyModes
import com.example.data.local.ShopSettingsEntity
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.ChampagneGoldGradient
import com.example.ui.theme.EmeraldLuminous
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDarkInk
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream
import com.example.util.generateQrBitmap

/**
 * Interactive Haider Parfums Black & Gold Glass Digital Loyalty Card
 * inspired directly by the boutique's black-and-gold physical card.
 */
@Composable
fun HaiderDigitalLoyaltyCard(
    customer: CustomerEntity,
    settings: ShopSettingsEntity,
    onStampTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(customer.loyaltyCode) {
        generateQrBitmap(customer.loyaltyCode, sizePx = 360)
    }
    val target = settings.stampTarget.coerceIn(1, 20)
    val rewardReady = customer.loyaltyMode == LoyaltyModes.STAMPS && customer.stamps >= target
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A0A0E),
                        Color(0xFF17140F),
                        Color(0xFF0B0B11)
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.15f),
                        radius = size.minDimension * 0.9f
                    ),
                    radius = size.minDimension * 0.9f,
                    center = Offset(size.width * 0.8f, size.height * 0.15f)
                )
            }
            .border(1.8.dp, ActiveGoldBorderBrush, cardShape)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Haider Crest + Thank You Copy + Crisp QR Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Logo & Customer Name
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.haider_app_icon),
                            contentDescription = "شعار حيدر للعطور",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GoldPrimary, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "شكراً لك",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldBright
                            )
                            Text(
                                text = settings.shopName,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary
                            )
                        }
                    }

                    Text(
                        text = "لثقتك بنا واختيارك لعطورنا الفاخرة",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmCream.copy(alpha = 0.85f)
                    )

                    Spacer(Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldPrimary.copy(alpha = 0.16f))
                            .border(0.8.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${customer.name} • ${customer.loyaltyCode}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = WarmCream
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Scannable QR Code on Warm Cream Frame
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(WarmCream)
                            .border(1.5.dp, GoldPrimary, RoundedCornerShape(14.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "رمز QR الخاص بـ ${customer.name}",
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = customer.loyaltyCode,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = GoldBright
                    )
                }
            }

            // Middle: 10-Stamp Visual Grid OR Flexible Points Counter
            if (customer.loyaltyMode == LoyaltyModes.STAMPS) {
                val cols = 5
                val rows = (target + cols - 1) / cols
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianDeep.copy(alpha = 0.65f))
                        .border(0.8.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مع كل عملية شراء عطر .. تحصل على ختم واحد (${customer.stamps}/$target)",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldBright,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (c in 0 until cols) {
                                val index = r * cols + c
                                if (index < target) {
                                    val earned = index < customer.stamps
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (earned) ChampagneGoldGradient
                                                else Brush.linearGradient(
                                                    listOf(Color(0xFFF6EFE0), Color(0xFFE7DEC9))
                                                )
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = if (earned) GoldBright else GoldDeep,
                                                shape = CircleShape
                                            )
                                            .clickable { onStampTapped() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (earned) "HP" else "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (earned) GoldDarkInk else Color(0xFF8C826E)
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.size(44.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                // Points Counter Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldPrimary.copy(alpha = 0.14f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Stars, contentDescription = null, tint = GoldBright, modifier = Modifier.size(28.dp))
                            Text(
                                text = "${customer.points}",
                                style = MaterialTheme.typography.displayLarge,
                                color = GoldBright
                            )
                        }
                        Text(
                            text = "نقطة ولاء ذهبية • VIP",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmCream
                        )
                    }
                }
            }

            // Bottom Reward Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (rewardReady) EmeraldLuminous.copy(alpha = 0.18f)
                        else ObsidianDeep.copy(alpha = 0.85f)
                    )
                    .border(
                        1.dp,
                        if (rewardReady) EmeraldLuminous else GoldPrimary,
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CardGiftcard,
                        contentDescription = null,
                        tint = if (rewardReady) EmeraldLuminous else GoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (rewardReady) "🎁 مبروك! الزبون مؤهل الآن لاستلام عطر هدية" else settings.rewardText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (rewardReady) EmeraldLuminous else GoldBright,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
