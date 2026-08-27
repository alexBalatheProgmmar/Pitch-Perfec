package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VaultBluePrimaryDark,
    onPrimary = VaultBlueOnPrimaryDark,
    primaryContainer = VaultBluePrimaryContainerDark,
    onPrimaryContainer = VaultBlueOnPrimaryContainerDark,
    secondary = VaultTealSecondaryDark,
    onSecondary = VaultTealOnSecondaryDark,
    secondaryContainer = VaultTealSecondaryContainerDark,
    onSecondaryContainer = VaultTealOnSecondaryContainerDark,
    tertiary = VaultAmberTertiaryDark,
    onTertiary = VaultAmberOnTertiaryDark,
    tertiaryContainer = VaultAmberTertiaryContainerDark,
    onTertiaryContainer = VaultAmberOnTertiaryContainerDark,
    background = VaultBackgroundDark,
    onBackground = VaultOnBackgroundDark,
    surface = VaultSurfaceDark,
    onSurface = VaultOnSurfaceDark,
    surfaceVariant = VaultSurfaceVariantDark,
    onSurfaceVariant = VaultOnSurfaceVariantDark,
    outline = VaultOutlineDark,
    outlineVariant = VaultOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = VaultBluePrimary,
    onPrimary = VaultBlueOnPrimary,
    primaryContainer = VaultBluePrimaryContainer,
    onPrimaryContainer = VaultBlueOnPrimaryContainer,
    secondary = VaultTealSecondary,
    onSecondary = VaultTealOnSecondary,
    secondaryContainer = VaultTealSecondaryContainer,
    onSecondaryContainer = VaultTealOnSecondaryContainer,
    tertiary = VaultAmberTertiary,
    onTertiary = VaultAmberOnTertiary,
    tertiaryContainer = VaultAmberTertiaryContainer,
    onTertiaryContainer = VaultAmberOnTertiaryContainer,
    background = VaultBackgroundLight,
    onBackground = VaultOnBackgroundLight,
    surface = VaultSurfaceLight,
    onSurface = VaultOnSurfaceLight,
    surfaceVariant = VaultSurfaceVariantLight,
    onSurfaceVariant = VaultOnSurfaceVariantLight,
    outline = VaultOutlineLight,
    outlineVariant = VaultOutlineVariantLight
)

@Composable
fun LifeVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = LifeVaultShapes,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compat alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    LifeVaultTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
