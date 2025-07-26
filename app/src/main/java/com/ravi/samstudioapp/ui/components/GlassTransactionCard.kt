package com.ravi.samstudioapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.ui.categoryDefs
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlassTransactionCard(
    txn: BankTransaction,
    bankTxn: BankTransaction?,
    dateTimeFormat: SimpleDateFormat,
    onEdit: (BankTransaction) -> Unit,
    onDelete: () -> Unit
) {
    val dateTime = remember(txn.messageTime, dateTimeFormat) {
        dateTimeFormat.format(Date(txn.messageTime))
    }
    val catName = bankTxn?.category ?: "Other"
    val catDef = remember(catName) {
        categoryDefs.find { it.name.equals(catName, ignoreCase = true) }
            ?: categoryDefs.last()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x30FFFFFF), // More visible glass effect
                        Color(0x15FFFFFF)  // Subtle bottom
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x50FFFFFF), // Brighter top border
                        Color(0x20FFFFFF), // Subtle middle
                        Color(0x10FFFFFF)  // Very subtle bottom
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Section - Category and Bank Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Icon and Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Circular Category Icon
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                color = catDef.color.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = catDef.icon,
                            contentDescription = catDef.name,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Category Title
                    Text(
                        text = catDef.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Bank Information Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Business,
                        contentDescription = "Bank",
                        tint = Color(0xFFB0B0B0),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = txn.bankName,
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }
            
            // Right Section - Amount, Date, and Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Amount
                Text(
                    text = "₹${txn.amount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Date and Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Date",
                        tint = Color(0xFFB0B0B0),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = dateTime,
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }
                
                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Edit Button
                    IconButton(
                        onClick = { bankTxn?.let { onEdit(it) } },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x40FFFFFF),
                                        Color(0x20FFFFFF)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x60FFFFFF),
                                        Color(0x30FFFFFF)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.1f),
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x40FFFFFF),
                                        Color(0x20FFFFFF)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x60FFFFFF),
                                        Color(0x30FFFFFF)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.1f),
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
} 