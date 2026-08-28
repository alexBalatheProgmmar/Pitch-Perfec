package com.example.ui.screens.bills.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.BillType

@Composable
fun BillCardItem(
    bill: Bill,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val billType = runCatching { BillType.valueOf(bill.billType) }.getOrDefault(BillType.OTHER)
    val isPaid = bill.status == BillStatus.PAID.name
    val currSymbol = if (bill.currency == "BDT" || bill.currency == "৳") "৳" else "${bill.currency} "

    val isOverdue = !isPaid && bill.dueDate != null && isDatePast(bill.dueDate)
    val isDueSoon = !isPaid && !isOverdue && bill.dueDate != null

    val (badgeBg, badgeFg, badgeText) = when {
        isPaid -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "PAID")
        isOverdue -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "OVERDUE")
        isDueSoon -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "DUE SOON")
        bill.dueDate != null -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "UPCOMING")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant, "NO DUE DATE")
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 0.dp else 3.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("bill_item_${bill.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Icon + Provider/Title + Options Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(getBillTypeColor(billType).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getBillTypeIcon(billType),
                            contentDescription = billType.displayName,
                            tint = getBillTypeColor(billType),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = bill.provider ?: billType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (bill.provider != null) billType.displayName else (bill.billingPeriod ?: "Utility Bill"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Due status badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Due and Due Date Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isPaid) "Amount Paid" else "Amount Due",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currSymbol${if (bill.amountDue % 1.0 == 0.0) bill.amountDue.toInt().toString() else bill.amountDue.toString()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPaid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                }

                if (bill.dueDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = bill.dueDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!bill.accountNumber.isNullOrBlank() || !bill.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = listOfNotNull(bill.accountNumber?.let { "Acct: $it" }, bill.notes).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }

            if (!isPaid) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onMarkPaid,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mark_paid_button_${bill.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Paid", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun getBillTypeIcon(billType: BillType): ImageVector {
    return when (billType) {
        BillType.ELECTRICITY -> Icons.Default.Bolt
        BillType.GAS -> Icons.Default.LocalFireDepartment
        BillType.WATER -> Icons.Default.WaterDrop
        BillType.INTERNET -> Icons.Default.Wifi
        BillType.MOBILE -> Icons.Default.PhoneAndroid
        BillType.RENT -> Icons.Default.Home
        BillType.TUITION -> Icons.Default.School
        BillType.TV_CABLE -> Icons.Default.Tv
        BillType.CREDIT_CARD -> Icons.Default.CreditCard
        else -> Icons.Default.Receipt
    }
}

fun getBillTypeColor(billType: BillType): Color {
    return when (billType) {
        BillType.ELECTRICITY -> Color(0xFFF59E0B) // Amber
        BillType.GAS -> Color(0xFFEF4444)         // Red/Orange
        BillType.WATER -> Color(0xFF0EA5E9)       // Blue
        BillType.INTERNET -> Color(0xFF8B5CF6)    // Purple
        BillType.MOBILE -> Color(0xFF10B981)      // Emerald
        BillType.RENT -> Color(0xFF6366F1)        // Indigo
        BillType.TUITION -> Color(0xFFEC4899)     // Pink
        BillType.CREDIT_CARD -> Color(0xFF0284C7) // Sky
        else -> Color(0xFF64748B)                 // Slate
    }
}

private fun isDatePast(dateStr: String): Boolean {
    // Conservative check
    val lower = dateStr.lowercase()
    if (lower.contains("yesterday") || lower.contains("last week")) return true
    return false
}
