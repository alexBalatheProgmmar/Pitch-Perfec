package com.example.ui.screens.bills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.BillType
import com.example.data.model.CardItem
import com.example.ui.screens.bills.components.BillCardItem
import com.example.ui.screens.bills.components.PaymentHistoryItem
import com.example.ui.screens.bills.components.VisualCreditCard
import com.example.ui.screens.bills.dialogs.AddEditBillDialog
import com.example.ui.screens.bills.dialogs.AddEditCardDialog
import com.example.ui.screens.bills.dialogs.MarkBillPaidDialog
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    uiState: MainUiState,
    onSaveBill: (Bill) -> Unit,
    onUpdateBill: (Bill) -> Unit,
    onDeleteBill: (Bill) -> Unit,
    onMarkBillPaid: (Bill, String?, String?, String?) -> Unit,
    onSaveCard: (CardItem) -> Unit,
    onUpdateCard: (CardItem) -> Unit,
    onDeleteCard: (CardItem) -> Unit,
    onTabSelected: (Int) -> Unit,
    onFilterTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingBill by remember { mutableStateOf<Bill?>(null) }
    var isAddBillOpen by remember { mutableStateOf(false) }

    var editingCard by remember { mutableStateOf<CardItem?>(null) }
    var isAddCardOpen by remember { mutableStateOf(false) }

    var payingBill by remember { mutableStateOf<Bill?>(null) }

    val tabs = listOf("Bills", "My Cards", "Payment History")

    Scaffold(
        floatingActionButton = {
            if (uiState.selectedBillTab == 0) {
                FloatingActionButton(
                    onClick = { isAddBillOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_bill_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Bill"
                    )
                }
            } else if (uiState.selectedBillTab == 1) {
                FloatingActionButton(
                    onClick = { isAddCardOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_card_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Card"
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.nav_bills),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track utility bills, dues & manage payment methods safely",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Top Segmented Tabs
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedBillTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedBillTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontWeight = if (uiState.selectedBillTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (index == 0 && uiState.unpaidBills.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (uiState.selectedBillTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = uiState.unpaidBills.size.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.selectedBillTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else if (index == 1 && uiState.cards.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = uiState.cards.size.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            when (uiState.selectedBillTab) {
                0 -> BillsTabContent(
                    uiState = uiState,
                    onFilterTypeSelected = onFilterTypeSelected,
                    onMarkPaid = { payingBill = it },
                    onEdit = { editingBill = it },
                    onDelete = onDeleteBill
                )
                1 -> CardsTabContent(
                    uiState = uiState,
                    onSetDefault = { card ->
                        val updated = card.copy(isDefault = true)
                        onUpdateCard(updated)
                    },
                    onEdit = { editingCard = it },
                    onDelete = onDeleteCard
                )
                2 -> HistoryTabContent(uiState = uiState)
            }
        }

        // Dialogs
        if (isAddBillOpen) {
            AddEditBillDialog(
                bill = null,
                onDismiss = { isAddBillOpen = false },
                onSave = {
                    onSaveBill(it)
                    isAddBillOpen = false
                }
            )
        }

        if (editingBill != null) {
            AddEditBillDialog(
                bill = editingBill,
                onDismiss = { editingBill = null },
                onSave = {
                    onUpdateBill(it)
                    editingBill = null
                }
            )
        }

        if (isAddCardOpen) {
            AddEditCardDialog(
                card = null,
                onDismiss = { isAddCardOpen = false },
                onSave = {
                    onSaveCard(it)
                    isAddCardOpen = false
                }
            )
        }

        if (editingCard != null) {
            AddEditCardDialog(
                card = editingCard,
                onDismiss = { editingCard = null },
                onSave = {
                    onUpdateCard(it)
                    editingCard = null
                }
            )
        }

        if (payingBill != null) {
            MarkBillPaidDialog(
                bill = payingBill!!,
                cards = uiState.cards,
                onDismiss = { payingBill = null },
                onConfirm = { method, cardId, notes ->
                    onMarkBillPaid(payingBill!!, method, cardId, notes)
                    payingBill = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillsTabContent(
    uiState: MainUiState,
    onFilterTypeSelected: (String) -> Unit,
    onMarkPaid: (Bill) -> Unit,
    onEdit: (Bill) -> Unit,
    onDelete: (Bill) -> Unit
) {
    val filterTypes = listOf("ALL") + BillType.values().map { it.name }

    val filteredBills = if (uiState.selectedBillTypeFilter == "ALL") {
        uiState.bills
    } else {
        uiState.bills.filter { it.billType.equals(uiState.selectedBillTypeFilter, ignoreCase = true) }
    }

    val unpaidFiltered = filteredBills.filter { it.status != BillStatus.PAID.name }
    val paidFiltered = filteredBills.filter { it.status == BillStatus.PAID.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Total Due Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bills_total_due_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL AMOUNT DUE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "${uiState.unpaidBills.size} Unpaid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "৳${if (uiState.totalAmountDue % 1.0 == 0.0) uiState.totalAmountDue.toInt().toString() else uiState.totalAmountDue.toString()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (uiState.unpaidBills.isEmpty()) "All caught up! No unpaid bills." else "Separated from balances and charges for clear tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Horizontal Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterTypes) { typeKey ->
                    val isSelected = uiState.selectedBillTypeFilter.equals(typeKey, ignoreCase = true)
                    val label = if (typeKey == "ALL") "All Bills" else {
                        runCatching { BillType.valueOf(typeKey).displayName }.getOrDefault(typeKey)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterTypeSelected(typeKey) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Unpaid Bills Section
        if (unpaidFiltered.isNotEmpty()) {
            item {
                Text(
                    text = "Pending Bills",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(unpaidFiltered, key = { it.id }) { bill ->
                BillCardItem(
                    bill = bill,
                    onMarkPaid = { onMarkPaid(bill) },
                    onEdit = { onEdit(bill) },
                    onDelete = { onDelete(bill) }
                )
            }
        }

        // Paid Bills Section
        if (paidFiltered.isNotEmpty()) {
            item {
                Text(
                    text = "Paid / Settled (${paidFiltered.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(paidFiltered, key = { it.id }) { bill ->
                BillCardItem(
                    bill = bill,
                    onMarkPaid = {},
                    onEdit = { onEdit(bill) },
                    onDelete = { onDelete(bill) }
                )
            }
        }

        if (filteredBills.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No bills found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add a bill manually or share bill text (e.g., 'Gas bill ৳850 due Sep 5')",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsTabContent(
    uiState: MainUiState,
    onSetDefault: (CardItem) -> Unit,
    onEdit: (CardItem) -> Unit,
    onDelete: (CardItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Safe Wallet Notice
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Safe Wallet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Safe & Private Card Wallet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.card_wallet_security_note),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Cards List
        if (uiState.cards.isNotEmpty()) {
            items(uiState.cards, key = { it.id }) { card ->
                VisualCreditCard(
                    card = card,
                    onSetDefault = { onSetDefault(card) },
                    onEdit = { onEdit(card) },
                    onDelete = { onDelete(card) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No cards added yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your debit and credit cards for quick reference during bill payments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    uiState: MainUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.paymentRecords.isNotEmpty()) {
            items(uiState.paymentRecords, key = { it.id }) { record ->
                PaymentHistoryItem(payment = record)
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No payment records yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When you mark a bill as paid, it will appear in this ledger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
