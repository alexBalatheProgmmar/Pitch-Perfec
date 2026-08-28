package com.example.ui.screens.bills.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CardItem
import com.example.data.model.CardNetwork
import com.example.data.model.CardType
import java.util.UUID

val CardColorPalette = listOf(
    "#1E293B", // Navy slate
    "#0F766E", // Teal
    "#4338CA", // Indigo
    "#831843", // Rose
    "#047857", // Emerald
    "#7C2D12", // Amber / Copper
    "#18181B"  // Carbon Black
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardDialog(
    card: CardItem? = null,
    onDismiss: () -> Unit,
    onSave: (CardItem) -> Unit
) {
    var nickname by remember { mutableStateOf(card?.nickname ?: "") }
    var network by remember { mutableStateOf(card?.network?.let { runCatching { CardNetwork.valueOf(it) }.getOrNull() } ?: CardNetwork.VISA) }
    var cardType by remember { mutableStateOf(card?.cardType?.let { runCatching { CardType.valueOf(it) }.getOrNull() } ?: CardType.DEBIT) }
    var last4 by remember { mutableStateOf(card?.last4Digits ?: "") }
    var bankIssuer by remember { mutableStateOf(card?.bankIssuer ?: "") }
    var cardholderName by remember { mutableStateOf(card?.cardholderName ?: "") }
    var expiryMonth by remember { mutableStateOf(card?.expiryMonth?.toString() ?: "") }
    var expiryYear by remember { mutableStateOf(card?.expiryYear?.toString() ?: "") }
    var selectedColor by remember { mutableStateOf(card?.colorHex ?: CardColorPalette.first()) }
    var isDefault by remember { mutableStateOf(card?.isDefault ?: false) }

    var networkExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (card == null) "Add Payment Card" else "Edit Card",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Security Guarantee: Only nickname and last 4 digits are saved. Never enter your full card number, PIN, or CVV.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Nickname
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Card Nickname (e.g. Salary Account, Shopping Visa)") },
                    placeholder = { Text("Primary Debit Card") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_nickname_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Network & Type Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Network Dropdown
                    ExposedDropdownMenuBox(
                        expanded = networkExpanded,
                        onExpandedChange = { networkExpanded = !networkExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = network.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Network") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = networkExpanded,
                            onDismissRequest = { networkExpanded = false }
                        ) {
                            CardNetwork.values().forEach { net ->
                                DropdownMenuItem(
                                    text = { Text(net.displayName) },
                                    onClick = {
                                        network = net
                                        networkExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Card Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = cardType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            CardType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.displayName) },
                                    onClick = {
                                        cardType = t
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Last 4 Digits & Bank Issuer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) last4 = it },
                        label = { Text("Last 4 Digits") },
                        placeholder = { Text("4821") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(130.dp)
                            .testTag("card_last4_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = bankIssuer,
                        onValueChange = { bankIssuer = it },
                        label = { Text("Bank / Issuer") },
                        placeholder = { Text("City Bank / Chase") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cardholder Name (Optional)
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Cardholder Name (Optional)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Expiry Date (MM / YY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = expiryMonth,
                        onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) expiryMonth = it },
                        label = { Text("Exp Month") },
                        placeholder = { Text("08") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = expiryYear,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) expiryYear = it },
                        label = { Text("Exp Year") },
                        placeholder = { Text("2028") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Theme Picker
                Text(
                    text = "Card Theme Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardColorPalette.forEach { hex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.DarkGray)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set as default payment method",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val newCard = card?.copy(
                                cardName = nickname.ifBlank { "${network.displayName} Card" },
                                cardType = cardType.name,
                                network = network.name,
                                last4Digits = last4.takeLast(4),
                                cardholderName = cardholderName.ifBlank { null },
                                bankIssuer = bankIssuer.ifBlank { null },
                                expiryMonth = expiryMonth.toIntOrNull(),
                                expiryYear = expiryYear.toIntOrNull(),
                                colorHex = selectedColor,
                                isDefault = isDefault
                            ) ?: CardItem(
                                id = UUID.randomUUID().toString(),
                                cardName = nickname.ifBlank { "${network.displayName} Card" },
                                cardType = cardType.name,
                                network = network.name,
                                last4Digits = last4.takeLast(4),
                                cardholderName = cardholderName.ifBlank { null },
                                bankIssuer = bankIssuer.ifBlank { null },
                                expiryMonth = expiryMonth.toIntOrNull(),
                                expiryYear = expiryYear.toIntOrNull(),
                                colorHex = selectedColor,
                                isDefault = isDefault
                            )

                            onSave(newCard)
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("save_card_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save Card", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
