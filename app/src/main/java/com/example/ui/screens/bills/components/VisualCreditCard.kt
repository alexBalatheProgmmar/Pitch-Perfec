package com.example.ui.screens.bills.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CardItem
import com.example.data.model.CardNetwork

@Composable
fun VisualCreditCard(
    card: CardItem,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val baseColor = runCatching { Color(android.graphics.Color.parseColor(card.colorHex)) }.getOrDefault(Color(0xFF1E293B))
    val lighterGradient = baseColor.copy(alpha = 0.85f)
    val darkerGradient = Color(
        red = (baseColor.red * 0.6f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.6f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.6f).coerceIn(0f, 1f)
    )

    val cardNetwork = runCatching { CardNetwork.valueOf(card.network) }.getOrDefault(CardNetwork.VISA)

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = baseColor.copy(alpha = 0.5f)
            )
            .testTag("visual_card_${card.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(lighterGradient, baseColor, darkerGradient)
                    )
                )
                .padding(22.dp)
        ) {
            // Subtle decorative background circles
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Row: Bank/Nickname + Default Badge + Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = card.nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!card.bankIssuer.isNullOrBlank()) {
                            Text(
                                text = card.bankIssuer,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (card.isDefault) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Default",
                                        tint = Color(0xFFFBBF24), // Gold
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DEFAULT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Card options",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (!card.isDefault) {
                                    DropdownMenuItem(
                                        text = { Text("Set as Default") },
                                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onSetDefault()
                                        }
                                    )
                                }
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
                }

                Spacer(modifier = Modifier.height(24.dp))

                // EMV Chip Simulation & Contactless Waves
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD4AF37)) // Gold chip
                            .border(1.dp, Color(0xFFB8860B), RoundedCornerShape(6.dp))
                    )

                    // Card Type (DEBIT / CREDIT)
                    Text(
                        text = card.cardType.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Masked Number: •••• •••• •••• 4821
                Text(
                    text = "••••  ••••  ••••  ${card.last4Digits}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Row: Cardholder & Expiry & Network
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        if (!card.cardholderName.isNullOrBlank()) {
                            Text(
                                text = "CARDHOLDER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = card.cardholderName.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (card.expiryMonth != null && card.expiryYear != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "EXPIRES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            val mStr = card.expiryMonth.toString().padStart(2, '0')
                            val yStr = card.expiryYear.toString().takeLast(2)
                            Text(
                                text = "$mStr/$yStr",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    // Network Logo / Name
                    Text(
                        text = cardNetwork.displayName.uppercase(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
