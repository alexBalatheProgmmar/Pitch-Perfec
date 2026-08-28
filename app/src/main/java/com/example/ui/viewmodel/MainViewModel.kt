package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.AIAnalysisResult
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.BillType
import com.example.data.model.CardItem
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemPriority
import com.example.data.model.ItemStatus
import com.example.data.model.ItemType
import com.example.data.model.PaymentRecord
import com.example.data.model.ReminderTiming
import com.example.data.model.UserItem
import com.example.data.remote.AIService
import com.example.data.repository.BillRepository
import com.example.data.repository.ItemRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class MainUiState(
    val activeItems: List<UserItem> = emptyList(),
    val inboxItems: List<UserItem> = emptyList(),
    val completedItems: List<UserItem> = emptyList(),
    val archivedItems: List<UserItem> = emptyList(),
    val allItems: List<UserItem> = emptyList(),
    val vaultItems: List<UserItem> = emptyList(),
    val searchResults: List<UserItem> = emptyList(),
    val calendarItems: List<UserItem> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val unpaidBills: List<Bill> = emptyList(),
    val paidBills: List<Bill> = emptyList(),
    val cards: List<CardItem> = emptyList(),
    val paymentRecords: List<PaymentRecord> = emptyList(),
    val totalAmountDue: Double = 0.0,
    val selectedBillTypeFilter: String = "ALL",
    val selectedBillTab: Int = 0,
    val selectedCategory: String = "ALL",
    val searchQuery: String = "",
    val selectedCalendarDate: String = DateTimeUtils.getTodayDateString(),
    val isAnalyzing: Boolean = false,
    val analysisStage: String = "",
    val pendingConfirmationItem: UserItem? = null,
    val duplicateWarningItem: UserItem? = null,
    val isCaptureSheetOpen: Boolean = false,
    val assistantQuery: String = "",
    val assistantResponse: String? = null,
    val isAssistantThinking: Boolean = false,
    val lastCompletedItem: UserItem? = null,
    val themeMode: String = "SYSTEM",
    val language: String = "en",
    val isOnboardingCompleted: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val errorMessage: String? = null
)

class MainViewModel(
    private val repository: ItemRepository,
    private val billRepository: BillRepository,
    private val preferencesManager: PreferencesManager,
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect DB flows for items
        viewModelScope.launch {
            repository.activeItems.collectLatest { items ->
                _uiState.value = _uiState.value.copy(activeItems = items)
            }
        }

        viewModelScope.launch {
            repository.inboxItems.collectLatest { items ->
                _uiState.value = _uiState.value.copy(inboxItems = items)
            }
        }

        viewModelScope.launch {
            repository.completedItems.collectLatest { items ->
                _uiState.value = _uiState.value.copy(completedItems = items)
            }
        }

        viewModelScope.launch {
            repository.archivedItems.collectLatest { items ->
                _uiState.value = _uiState.value.copy(archivedItems = items)
            }
        }

        viewModelScope.launch {
            repository.allItems.collectLatest { items ->
                _uiState.value = _uiState.value.copy(allItems = items)
                updateVaultFilter(_uiState.value.selectedCategory, items)
                updateCalendarFilter(_uiState.value.selectedCalendarDate, items)
            }
        }

        // Collect DB flows for bills & cards
        viewModelScope.launch {
            billRepository.allBills.collectLatest { allBillsList ->
                val unpaid = allBillsList.filter { it.status != BillStatus.PAID.name && it.status != BillStatus.CANCELLED.name }
                val paid = allBillsList.filter { it.status == BillStatus.PAID.name }
                val totalDue = unpaid.sumOf { it.amountDue }

                _uiState.value = _uiState.value.copy(
                    bills = allBillsList,
                    unpaidBills = unpaid,
                    paidBills = paid,
                    totalAmountDue = totalDue
                )
            }
        }

        viewModelScope.launch {
            billRepository.allCards.collectLatest { cardList ->
                _uiState.value = _uiState.value.copy(cards = cardList)
            }
        }

        viewModelScope.launch {
            billRepository.allPayments.collectLatest { paymentsList ->
                _uiState.value = _uiState.value.copy(paymentRecords = paymentsList)
            }
        }

        // Collect preferences
        viewModelScope.launch {
            preferencesManager.isOnboardingCompleted.collectLatest { completed ->
                _uiState.value = _uiState.value.copy(isOnboardingCompleted = completed)
            }
        }

        viewModelScope.launch {
            preferencesManager.themeMode.collectLatest { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }

        viewModelScope.launch {
            preferencesManager.language.collectLatest { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }

        viewModelScope.launch {
            preferencesManager.isNotificationsEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(isNotificationsEnabled = enabled)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    fun openCaptureSheet() {
        _uiState.value = _uiState.value.copy(isCaptureSheetOpen = true)
    }

    fun closeCaptureSheet() {
        _uiState.value = _uiState.value.copy(isCaptureSheetOpen = false)
    }

    fun setSelectedCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        updateVaultFilter(category, _uiState.value.allItems)
    }

    private fun updateVaultFilter(category: String, all: List<UserItem>) {
        val filtered = when (category) {
            "ALL" -> all.filter { it.status != ItemStatus.DISMISSED.name }
            "ARCHIVED" -> all.filter { it.status == ItemStatus.ARCHIVED.name }
            else -> all.filter { it.category.equals(category, ignoreCase = true) || it.type.equals(category, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(vaultItems = filtered)
    }

    fun setSelectedCalendarDate(dateStr: String) {
        _uiState.value = _uiState.value.copy(selectedCalendarDate = dateStr)
        updateCalendarFilter(dateStr, _uiState.value.allItems)
    }

    private fun updateCalendarFilter(dateStr: String, all: List<UserItem>) {
        val filtered = all.filter { item ->
            val due = item.dueDate
            due != null && (due == dateStr || due.contains(dateStr))
        }
        _uiState.value = _uiState.value.copy(calendarItems = filtered)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        } else {
            viewModelScope.launch {
                val q = query.lowercase().trim()
                val results = _uiState.value.allItems.filter { item ->
                    item.title.lowercase().contains(q) ||
                    item.description.lowercase().contains(q) ||
                    item.action.lowercase().contains(q) ||
                    item.category.lowercase().contains(q) ||
                    item.type.lowercase().contains(q) ||
                    (item.organization?.lowercase()?.contains(q) == true) ||
                    (item.person?.lowercase()?.contains(q) == true) ||
                    (item.amount != null && item.amount.toString().contains(q))
                }
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }
    }

    fun setSelectedBillTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedBillTab = index)
    }

    fun setSelectedBillTypeFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedBillTypeFilter = filter)
    }

    // --- Bill Management ---

    fun saveBill(bill: Bill) {
        viewModelScope.launch {
            billRepository.insertBill(bill)
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            billRepository.updateBill(bill)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            billRepository.deleteBill(bill)
        }
    }

    fun markBillAsPaid(
        bill: Bill,
        paymentMethod: String? = null,
        cardId: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            billRepository.markBillAsPaid(bill, paymentMethod, cardId, notes)
            // Also mark corresponding UserItem completed if linked
            val userItem = repository.checkForDuplicate(bill.provider ?: bill.billType, bill.amountDue, bill.dueDate)
            if (userItem != null) {
                repository.markCompleted(userItem)
            }
        }
    }

    // --- Card Management ---

    fun saveCard(card: CardItem) {
        viewModelScope.launch {
            billRepository.insertCard(card)
        }
    }

    fun updateCard(card: CardItem) {
        viewModelScope.launch {
            billRepository.updateCard(card)
        }
    }

    fun deleteCard(card: CardItem) {
        viewModelScope.launch {
            billRepository.deleteCard(card)
        }
    }

    // --- AI Processing ---

    fun analyzeAndProcessText(text: String, source: String = "Text") {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisStage = "Reading content…",
                isCaptureSheetOpen = false
            )

            delay(300)
            _uiState.value = _uiState.value.copy(analysisStage = "Finding dates and amounts…")

            delay(300)
            _uiState.value = _uiState.value.copy(analysisStage = "Organizing into actionable records…")

            val result = aiService.analyzeText(text)

            handleAnalysisResult(result, originalContent = text, source = source)
        }
    }

    fun analyzeAndProcessImage(bitmap: Bitmap, promptHint: String? = null, source: String = "Image") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisStage = "Reading document/image…",
                isCaptureSheetOpen = false
            )

            val result = aiService.analyzeImage(bitmap, promptHint)
            handleAnalysisResult(result, originalContent = promptHint ?: "Image OCR Capture", source = source)
        }
    }

    private suspend fun handleAnalysisResult(
        result: AIAnalysisResult,
        originalContent: String,
        source: String
    ) {
        val dueTimestamp = DateTimeUtils.calculateTimestamp(result.date, result.time)
        val reminderTiming = if (dueTimestamp != null) ReminderTiming.ONE_DAY_BEFORE else ReminderTiming.NONE
        val reminderTimestamp = DateTimeUtils.calculateReminderTimestamp(dueTimestamp, reminderTiming)

        val candidateItem = UserItem(
            id = UUID.randomUUID().toString(),
            title = result.title,
            description = result.description,
            type = result.type,
            category = result.category,
            contentType = result.contentType,
            actionability = result.actionability,
            action = result.action,
            dueDate = result.date,
            dueTime = result.time,
            dueTimestamp = dueTimestamp,
            amount = result.amount,
            currency = result.currency,
            person = result.person,
            organization = result.organization ?: result.billProvider,
            location = result.location,
            source = source,
            originalContent = originalContent,
            priority = result.priority,
            status = ItemStatus.ACTIVE.name,
            confidence = result.confidence,
            contentTypeConfidence = result.contentTypeConfidence,
            actionabilityConfidence = result.actionabilityConfidence,
            extractionConfidence = result.extractionConfidence,
            evidence = result.evidence,
            reason = result.reason,
            explanation = result.explanation,
            reminderTiming = reminderTiming.name,
            reminderTimestamp = reminderTimestamp,
            returnWindowDays = result.returnWindowDays,
            warrantyExpiryDate = result.warrantyExpiryDate,
            subscriptionInterval = result.subscriptionInterval
        )

        // Check for duplicate
        val existing = repository.checkForDuplicate(candidateItem.title, candidateItem.amount, candidateItem.dueDate)

        _uiState.value = _uiState.value.copy(
            isAnalyzing = false,
            analysisStage = "",
            pendingConfirmationItem = candidateItem,
            duplicateWarningItem = existing
        )
    }

    fun confirmPendingItem(item: UserItem) {
        viewModelScope.launch {
            repository.insertItem(item)

            // If this is a bill with an amount, automatically stage/insert in Bills table as well!
            if (item.contentType == ContentType.BILL.name || item.type == ItemType.PAYMENT.name || item.category == ItemCategory.FINANCE.name) {
                if (item.amount != null && item.amount > 0) {
                    val billType = mapToBillType(item.title)
                    val bill = Bill(
                        id = item.id,
                        billType = billType.name,
                        provider = item.organization ?: extractProviderFromTitle(item.title),
                        amountDue = item.amount,
                        currency = item.currency ?: "BDT",
                        dueDate = item.dueDate,
                        dueTimestamp = item.dueTimestamp,
                        billingPeriod = "Current Cycle",
                        status = BillStatus.UNPAID.name,
                        source = item.source,
                        notes = item.description,
                        reminderTimestamp = item.reminderTimestamp
                    )
                    billRepository.insertBill(bill)
                }
            }

            _uiState.value = _uiState.value.copy(
                pendingConfirmationItem = null,
                duplicateWarningItem = null
            )
        }
    }

    fun dismissPendingItem() {
        _uiState.value = _uiState.value.copy(
            pendingConfirmationItem = null,
            duplicateWarningItem = null
        )
    }

    fun saveDirectItem(item: UserItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }

    fun updateItem(item: UserItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun markItemComplete(item: UserItem) {
        viewModelScope.launch {
            repository.markCompleted(item)
            _uiState.value = _uiState.value.copy(lastCompletedItem = item)
        }
    }

    fun undoLastComplete() {
        val last = _uiState.value.lastCompletedItem ?: return
        viewModelScope.launch {
            repository.markIncomplete(last)
            _uiState.value = _uiState.value.copy(lastCompletedItem = null)
        }
    }

    fun snoozeItem(item: UserItem, hours: Int) {
        viewModelScope.launch {
            repository.snoozeItem(item, hours)
        }
    }

    fun archiveItem(item: UserItem) {
        viewModelScope.launch {
            repository.archiveItem(item)
        }
    }

    fun dismissInboxItem(item: UserItem) {
        viewModelScope.launch {
            repository.dismissItem(item)
        }
    }

    fun deleteItem(item: UserItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun askAssistant(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                assistantQuery = query,
                isAssistantThinking = true,
                assistantResponse = null
            )
            val answer = aiService.answerQuestion(query, _uiState.value.allItems)
            _uiState.value = _uiState.value.copy(
                isAssistantThinking = false,
                assistantResponse = answer
            )
        }
    }

    fun clearAssistantResponse() {
        _uiState.value = _uiState.value.copy(
            assistantQuery = "",
            assistantResponse = null,
            isAssistantThinking = false
        )
    }

    fun setTheme(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            billRepository.deleteAll()
        }
    }

    suspend fun getExportJson(): String {
        return repository.exportDataAsJson(_uiState.value.allItems)
    }

    private fun mapToBillType(title: String): BillType {
        val t = title.lowercase()
        return when {
            t.contains("gas") || t.contains("গ্যাস") -> BillType.GAS
            t.contains("electricity") || t.contains("electric") || t.contains("বিদ্যুৎ") -> BillType.ELECTRICITY
            t.contains("water") || t.contains("পানি") -> BillType.WATER
            t.contains("internet") || t.contains("wifi") -> BillType.INTERNET
            t.contains("mobile") || t.contains("phone") -> BillType.MOBILE
            t.contains("rent") -> BillType.RENT
            t.contains("tuition") || t.contains("fee") -> BillType.TUITION
            t.contains("insurance") -> BillType.INSURANCE
            t.contains("card") -> BillType.CREDIT_CARD
            t.contains("loan") -> BillType.LOAN
            t.contains("subscription") -> BillType.SUBSCRIPTION
            else -> BillType.OTHER
        }
    }

    private fun extractProviderFromTitle(title: String): String? {
        val lower = title.lowercase()
        val providers = listOf("desco", "dpdc", "reb", "nesco", "titas", "wasa", "btcl", "link3", "carnival")
        return providers.firstOrNull { lower.contains(it) }?.uppercase()
    }
}

class ViewModelFactory(
    private val repository: ItemRepository,
    private val billRepository: BillRepository,
    private val preferencesManager: PreferencesManager,
    private val aiService: AIService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, billRepository, preferencesManager, aiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
