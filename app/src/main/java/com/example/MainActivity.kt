package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.LifeVaultDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.UserItem
import com.example.data.remote.AIServiceImpl
import com.example.data.repository.ItemRepository
import com.example.ui.components.capture.CaptureBottomSheet
import com.example.ui.components.confirm.AnalyzingOverlay
import com.example.ui.components.confirm.ConfirmationDialog
import com.example.ui.components.navigation.FloatingNavBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.assistant.AssistantDialog
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.detail.ItemDetailScreen
import com.example.ui.screens.edit.ItemEditScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.inbox.InboxScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.vault.VaultScreen
import com.example.ui.theme.LifeVaultTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val db = LifeVaultDatabase.getDatabase(applicationContext)
        val repository = ItemRepository(applicationContext, db.userItemDao())
        val preferences = PreferencesManager(applicationContext)
        val aiService = AIServiceImpl()
        ViewModelFactory(repository, preferences, aiService)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Handle Share Sheet intent or notification intent
        handleIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            val isDark = when (uiState.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            LifeVaultTheme(darkTheme = isDark) {
                LifeVaultApp(
                    viewModel = viewModel,
                    initialItemId = intent?.getStringExtra("OPEN_ITEM_ID")
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.analyzeAndProcessText(sharedText, source = "ShareSheet")
                    }
                } else if (intent.type?.startsWith("image/") == true) {
                    val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                    if (imageUri != null) {
                        try {
                            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                            }
                            viewModel.analyzeAndProcessImage(bitmap, "Shared Image: $imageUri", source = "ShareSheet")
                        } catch (e: Exception) {
                            viewModel.analyzeAndProcessText("Shared image URI: $imageUri", source = "ShareSheet")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LifeVaultApp(
    viewModel: MainViewModel,
    initialItemId: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    var selectedDetailItem by remember { mutableStateOf<UserItem?>(null) }
    var selectedEditItem by remember { mutableStateOf<UserItem?>(null) }
    var isAssistantOpen by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(initialItemId, uiState.allItems) {
        if (!initialItemId.isNullOrBlank() && uiState.allItems.isNotEmpty()) {
            val item = uiState.allItems.find { it.id == initialItemId }
            if (item != null) {
                selectedDetailItem = item
            }
        }
    }

    if (showSplash) {
        com.example.ui.components.brand.LifeVaultSplashScreen(
            onFinish = { showSplash = false }
        )
    } else if (!uiState.isOnboardingCompleted) {
        OnboardingScreen(
            onFinish = { viewModel.completeOnboarding() }
        )
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                if (selectedDetailItem == null && selectedEditItem == null) {
                    FloatingNavBar(
                        currentRoute = currentRoute,
                        inboxBadgeCount = uiState.inboxItems.size,
                        onNavigate = { screen ->
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            uiState = uiState,
                            onOpenCapture = { viewModel.openCaptureSheet() },
                            onOpenAssistant = { isAssistantOpen = true },
                            onItemClick = { selectedDetailItem = it },
                            onToggleComplete = { viewModel.markItemComplete(it) },
                            onSnoozeItem = { item, hours -> viewModel.snoozeItem(item, hours) },
                            onUndoComplete = { viewModel.undoLastComplete() }
                        )
                    }

                    composable(Screen.Inbox.route) {
                        InboxScreen(
                            uiState = uiState,
                            onItemClick = { selectedDetailItem = it },
                            onConfirmItem = { viewModel.confirmPendingItem(it) },
                            onEditItem = { selectedEditItem = it },
                            onDeleteItem = { viewModel.dismissInboxItem(it) }
                        )
                    }

                    composable(Screen.Calendar.route) {
                        CalendarScreen(
                            uiState = uiState,
                            onItemClick = { selectedDetailItem = it },
                            onToggleComplete = { viewModel.markItemComplete(it) }
                        )
                    }

                    composable(Screen.Vault.route) {
                        VaultScreen(
                            uiState = uiState,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onSelectCategory = { viewModel.setSelectedCategory(it) },
                            onItemClick = { selectedDetailItem = it },
                            onToggleComplete = { viewModel.markItemComplete(it) }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            uiState = uiState,
                            onSetTheme = { viewModel.setTheme(it) },
                            onSetLanguage = { viewModel.setLanguage(it) },
                            onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                            onExportJson = { viewModel.getExportJson() },
                            onDeleteAllData = { viewModel.deleteAllData() }
                        )
                    }
                }

                // Detail View Modal/Overlay
                if (selectedDetailItem != null) {
                    ItemDetailScreen(
                        item = selectedDetailItem!!,
                        onBack = { selectedDetailItem = null },
                        onEdit = {
                            val item = selectedDetailItem
                            selectedDetailItem = null
                            selectedEditItem = item
                        },
                        onToggleComplete = {
                            viewModel.markItemComplete(it)
                            selectedDetailItem = null
                        },
                        onSnooze = { item, hours ->
                            viewModel.snoozeItem(item, hours)
                            selectedDetailItem = null
                        },
                        onArchive = {
                            viewModel.archiveItem(it)
                            selectedDetailItem = null
                        },
                        onDelete = {
                            viewModel.deleteItem(it)
                            selectedDetailItem = null
                        }
                    )
                }

                // Edit View Modal/Overlay
                if (selectedEditItem != null) {
                    ItemEditScreen(
                        item = selectedEditItem!!,
                        onSave = { updated ->
                            viewModel.updateItem(updated)
                            selectedEditItem = null
                        },
                        onCancel = { selectedEditItem = null }
                    )
                }

                // Capture Bottom Sheet
                if (uiState.isCaptureSheetOpen) {
                    CaptureBottomSheet(
                        onDismiss = { viewModel.closeCaptureSheet() },
                        onAnalyzeText = { text, source ->
                            viewModel.analyzeAndProcessText(text, source)
                        },
                        onAnalyzeImage = { bitmap, prompt ->
                            viewModel.analyzeAndProcessImage(bitmap, prompt)
                        }
                    )
                }

                // Analyzing Overlay (Loading AI stages)
                if (uiState.isAnalyzing) {
                    AnalyzingOverlay(stage = uiState.analysisStage)
                }

                // Confirmation Dialog
                if (uiState.pendingConfirmationItem != null) {
                    ConfirmationDialog(
                        item = uiState.pendingConfirmationItem!!,
                        duplicateItem = uiState.duplicateWarningItem,
                        onConfirm = { confirmedItem ->
                            viewModel.confirmPendingItem(confirmedItem)
                        },
                        onEdit = { candidateItem ->
                            viewModel.dismissPendingItem()
                            selectedEditItem = candidateItem
                        },
                        onDismiss = { viewModel.dismissPendingItem() },
                        onOpenExisting = { existingItem ->
                            viewModel.dismissPendingItem()
                            selectedDetailItem = existingItem
                        }
                    )
                }

                // Assistant Dialog
                if (isAssistantOpen) {
                    AssistantDialog(
                        queryText = uiState.assistantQuery,
                        answerText = uiState.assistantResponse,
                        isThinking = uiState.isAssistantThinking,
                        onAsk = { query -> viewModel.askAssistant(query) },
                        onDismiss = {
                            isAssistantOpen = false
                            viewModel.clearAssistantResponse()
                        }
                    )
                }
            }
        }
    }
}
