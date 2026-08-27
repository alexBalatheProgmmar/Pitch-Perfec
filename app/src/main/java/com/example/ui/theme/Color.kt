package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern, Friendly, Calming 2026 Consumer Color Palette
// Primary Sapphire / Royal Sky
val VaultBluePrimary = Color(0xFF2563EB)
val VaultBlueOnPrimary = Color(0xFFFFFFFF)
val VaultBluePrimaryContainer = Color(0xFFEFF6FF)
val VaultBlueOnPrimaryContainer = Color(0xFF1E40AF)

// Secondary Fresh Mint / Sage
val VaultTealSecondary = Color(0xFF0D9488)
val VaultTealOnSecondary = Color(0xFFFFFFFF)
val VaultTealSecondaryContainer = Color(0xFFF0FDF4)
val VaultTealOnSecondaryContainer = Color(0xFF115E59)

// Tertiary Warm Amber / Sunset
val VaultAmberTertiary = Color(0xFFD97706)
val VaultAmberOnTertiary = Color(0xFFFFFFFF)
val VaultAmberTertiaryContainer = Color(0xFFFEF3C7)
val VaultAmberOnTertiaryContainer = Color(0xFF78350F)

// Light Background & Surfaces (Soft warm pearl canvas)
val VaultBackgroundLight = Color(0xFFF8FAFC)
val VaultOnBackgroundLight = Color(0xFF0F172A)
val VaultSurfaceLight = Color(0xFFFFFFFF)
val VaultOnSurfaceLight = Color(0xFF0F172A)
val VaultSurfaceVariantLight = Color(0xFFF1F5F9)
val VaultOnSurfaceVariantLight = Color(0xFF64748B)
val VaultOutlineLight = Color(0xFFE2E8F0)
val VaultOutlineVariantLight = Color(0xFFF1F5F9)

// Dark Theme Colors (Deep Obsidian & Slate Card system)
val VaultBluePrimaryDark = Color(0xFF60A5FA)
val VaultBlueOnPrimaryDark = Color(0xFF0B192C)
val VaultBluePrimaryContainerDark = Color(0xFF1E3A8A)
val VaultBlueOnPrimaryContainerDark = Color(0xFFDBEAFE)

val VaultTealSecondaryDark = Color(0xFF34D399)
val VaultTealOnSecondaryDark = Color(0xFF064E3B)
val VaultTealSecondaryContainerDark = Color(0xFF065F46)
val VaultTealOnSecondaryContainerDark = Color(0xFFD1FAE5)

val VaultAmberTertiaryDark = Color(0xFFFBBF24)
val VaultAmberOnTertiaryDark = Color(0xFF451A03)
val VaultAmberTertiaryContainerDark = Color(0xFF78350F)
val VaultAmberOnTertiaryContainerDark = Color(0xFFFEF3C7)

val VaultBackgroundDark = Color(0xFF0B0F19)
val VaultOnBackgroundDark = Color(0xFFF8FAFC)
val VaultSurfaceDark = Color(0xFF151B28)
val VaultOnSurfaceDark = Color(0xFFF8FAFC)
val VaultSurfaceVariantDark = Color(0xFF1F293D)
val VaultOnSurfaceVariantDark = Color(0xFF94A3B8)
val VaultOutlineDark = Color(0xFF2E3A52)
val VaultOutlineVariantDark = Color(0xFF1E283C)

// Priority & Confidence Colors
val PriorityHigh = Color(0xFFEF4444)
val PriorityHighContainer = Color(0xFFFEE2E2)
val PriorityMedium = Color(0xFFF59E0B)
val PriorityMediumContainer = Color(0xFFFEF3C7)
val PriorityLow = Color(0xFF10B981)
val PriorityLowContainer = Color(0xFFD1FAE5)

val SuccessGreen = Color(0xFF10B981)
val ConfidenceClear = Color(0xFF059669)
val ConfidenceReview = Color(0xFFD97706)
val ConfidenceUnsure = Color(0xFFDC2626)

// Friendly Soft Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
)

val UrgentGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFEF4444), Color(0xFFF97316))
)

val CardGlowGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.08f), Color(0xFF4F46E5).copy(alpha = 0.02f))
)
