package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Bills : Screen("bills", R.string.nav_bills, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Inbox : Screen("inbox", R.string.nav_inbox, Icons.Filled.Inbox, Icons.Outlined.Inbox)
    object Calendar : Screen("calendar", R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Vault : Screen("vault", R.string.nav_vault, Icons.Filled.Folder, Icons.Outlined.Folder)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Bills,
    Screen.Inbox,
    Screen.Vault,
    Screen.Settings
)
