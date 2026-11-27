package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActiveGoldBorderBrush
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.ChampagneGoldGradient
import com.example.ui.theme.GlassBorderBrush
import com.example.ui.theme.GlassDarkTint
import com.example.ui.theme.GlassFillMedium
import com.example.ui.theme.GlassFillSubtle
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDarkInk
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianCanvasGradient
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SoftSilver
import com.example.ui.theme.WarmCream

/**
 * Full-screen deep obsidian canvas with subtle ambient warm-gold and deep-amber radial glow orbs
 * floating behind the translucent glass layers.
 */
@Composable
fun ObsidianAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvasGradient)
            .drawBehind {
                // Top-right warm champagne gold ambient glow (12% opacity)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.14f),
                            GoldPrimary.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.minDimension * 0.72f
                    ),
                    radius = size.minDimension * 0.72f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f)
                )
                // Bottom-left deep amber glow orb (15% opacity)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AmberGlow.copy(alpha = 0.22f),
                            AmberGlow.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.78f),
                        radius = size.minDimension * 0.78f
                    ),
                    radius = size.minDimension * 0.78f,
                    center = Offset(size.width * 0.15f, size.height * 0.78f)
                )
                // Subtle mid-screen specular warmth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoldBright.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.45f),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.5f, size.height * 0.45f)
                )
            },
        content = content
    )
}

/**
 * Reusable Luxury Glassmorphism Container with translucent surface fill,
 * ultra-thin 1.dp refracting gradient border, and optional spring-tap interaction.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    highlighted: Boolean = false,
    fillColor: Color = if (highlighted) GlassFillMedium else GlassFillSubtle,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_scale"
    )
    val borderBrush = if (highlighted) ActiveGoldBorderBrush else GlassBorderBrush

    val interactiveModifier = if (onClick != null) {
        modifier
            .scale(scale)
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.62f))
            .background(fillColor)
            .border(1.dp, borderBrush, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GoldPrimary.copy(alpha = 0.25f)),
                role = Role.Button,
                onClick = onClick
            )
    } else {
        modifier
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.62f))
            .background(fillColor)
            .border(1.dp, borderBrush, shape)
    }

    Box(
        modifier = interactiveModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Primary Champagne Gold gradient button with spring tap feedback and 48dp+ touch target.
 */
@Composable
fun GoldGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    tag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "gold_cta_scale"
    )
    val shape = RoundedCornerShape(16.dp)
    val baseMod = modifier
        .then(if (tag != null) Modifier.testTag(tag) else Modifier)
        .scale(scale)
        .height(height)
        .clip(shape)
        .background(
            brush = if (enabled) ChampagneGoldGradient
            else Brush.linearGradient(listOf(Color(0xFF4A4330), Color(0xFF332E22)))
        )
        .border(1.dp, GoldBright.copy(alpha = if (enabled) 0.55f else 0.15f), shape)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = ripple(color = Color.White.copy(alpha = 0.35f)),
            role = Role.Button,
            onClick = onClick
        )

    Box(
        modifier = baseMod.padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) GoldDarkInk else SoftSilver,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) GoldDarkInk else SoftSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Frosted glass filter chip with metallic gold active highlight.
 */
@Composable
fun GlassFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(50)
    val bgColor by animateColorAsState(
        targetValue = if (selected) GoldPrimary else GlassFillSubtle,
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) GoldDarkInk else WarmCream.copy(alpha = 0.85f),
        label = "chip_fg"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = shape,
        color = bgColor,
        border = BorderStroke(
            width = 1.dp,
            brush = if (selected) ChampagneGoldGradient else GlassBorderBrush
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1
            )
        }
    }
}

data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val testTag: String
)

/**
 * Detached pill-shaped Floating Glass Bottom Navigation Bar with spring active glow.
 * Respects WindowInsets.navigationBars so system gestures never clip icons.
 */
@Composable
fun FloatingGlassBottomBar(
    items: List<NavTabItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val pillShape = RoundedCornerShape(30.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(pillShape)
                .background(GlassDarkTint)
                .background(GlassFillSubtle)
                .border(1.dp, GlassBorderBrush, pillShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.route == selectedRoute
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.04f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "nav_scale_${item.route}"
                )
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                    label = "nav_pill_${item.route}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(item.testTag)
                        .scale(scale)
                        .clip(RoundedCornerShape(22.dp))
                        .background(pillBg)
                        .then(
                            if (isSelected) Modifier.border(
                                0.8.dp,
                                GoldPrimary.copy(alpha = 0.55f),
                                RoundedCornerShape(22.dp)
                            ) else Modifier
                        )
                        .clickable(
                            role = Role.Tab,
                            onClick = { onSelect(item.route) }
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) GoldBright else SoftSilver,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) GoldBright else SoftSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Glass Navigation Rail for Medium/Expanded tablets & foldables.
 */
@Composable
fun GlassNavigationRail(
    items: List<NavTabItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val railShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .padding(12.dp)
            .width(88.dp)
            .clip(railShape)
            .background(GlassDarkTint)
            .border(1.dp, GlassBorderBrush, railShape)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            val isSelected = item.route == selectedRoute
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(item.testTag)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) GoldPrimary.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onSelect(item.route) })
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isSelected) item.activeIcon else item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) GoldBright else SoftSilver,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) GoldBright else SoftSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact circular glass icon button enforcing minimum 48dp touch target.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GoldBright,
    highlighted: Boolean = false
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(if (highlighted) GoldPrimary.copy(alpha = 0.18f) else GlassFillSubtle)
            .border(1.dp, if (highlighted) ActiveGoldBorderBrush else GlassBorderBrush, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
