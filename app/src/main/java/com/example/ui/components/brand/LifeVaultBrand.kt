package com.example.ui.components.brand

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.VaultAmberTertiary
import com.example.ui.theme.VaultBluePrimary
import kotlinx.coroutines.delay

/**
 * High-definition standalone LifeVault Logo Icon.
 * Displays the custom generated master logo image inside a polished rounded squircle
 * with subtle ambient elevation and gradient border.
 */
@Composable
fun LifeVaultLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    shapeRadius: Dp = size * 0.28f,
    elevation: Dp = 8.dp,
    showGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("lifevault_app_logo"),
        contentAlignment = Alignment.Center
    ) {
        // Outer subtle glow layer
        if (showGlow) {
            Box(
                modifier = Modifier
                    .size(size * 1.08f)
                    .clip(RoundedCornerShape(shapeRadius * 1.15f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VaultBluePrimary.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main Icon Surface
        Surface(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(shapeRadius),
                    spotColor = Color(0xFF1E1B4B).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(shapeRadius))
                .border(
                    width = (size.value * 0.02f).coerceAtLeast(1f).dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            VaultBluePrimary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(shapeRadius)
                ),
            shape = RoundedCornerShape(shapeRadius),
            color = Color(0xFF0D1322)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_lifevault_logo),
                contentDescription = "LifeVault Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Clean typography LifeVault Wordmark.
 * Precision-styled "Life" and "Vault" with balanced optical hierarchy and letter spacing.
 */
@Composable
fun LifeVaultWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showAccent: Boolean = true
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.85f),
                    letterSpacing = 0.5.sp
                )
            ) {
                append("Life")
            }
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Black,
                    color = if (showAccent) MaterialTheme.colorScheme.primary else textColor,
                    letterSpacing = (-0.5).sp
                )
            ) {
                append("Vault")
            }
        },
        fontSize = fontSize,
        lineHeight = fontSize * 1.15f,
        modifier = modifier.testTag("lifevault_wordmark")
    )
}

/**
 * Full Brand Lockup: Logo Symbol + Wordmark in horizontal or vertical alignment.
 */
@Composable
fun LifeVaultBrandLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    textSize: TextUnit = 22.sp,
    isVertical: Boolean = false,
    showTagline: Boolean = false
) {
    if (isVertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LifeVaultLogoIcon(size = iconSize)
            Spacer(modifier = Modifier.height(12.dp))
            LifeVaultWordmark(fontSize = textSize)
            if (showTagline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Smart Memory & Life Organizer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.2.sp
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LifeVaultLogoIcon(size = iconSize)
            Column {
                LifeVaultWordmark(fontSize = textSize)
                if (showTagline) {
                    Text(
                        text = "Smart Life Organizer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Minimalist, animated branded Splash Screen.
 * Renders the brand logo and wordmark with a smooth entrance scale & fade.
 */
@Composable
fun LifeVaultSplashScreen(
    onFinish: () -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 400, easing = LinearEasing)
        )
        delay(850)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D16),
                        Color(0xFF0E1626),
                        Color(0xFF0B101D)
                    )
                )
            )
            .testTag("lifevault_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scale.value)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Master Logo Icon
            LifeVaultLogoIcon(
                size = 100.dp,
                shapeRadius = 28.dp,
                elevation = 16.dp,
                showGlow = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // LifeVault Wordmark
            LifeVaultWordmark(
                fontSize = 32.sp,
                textColor = Color.White,
                showAccent = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Memory • Protection • Action",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
