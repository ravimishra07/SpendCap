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
import androidx.compose.ui.tooling.preview.Preview
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import androidx.compose.foundation.clickable
const val SamDateFormat = "MM/dd/yy, h:mm a"
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
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF23272A)) // Simple solid background
            .border(
                width = 1.dp,
                color = Color(0xFF444950),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    // Subtle gray circular background for icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)), // semi-transparent white/gray
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = catDef.icon,
                            contentDescription = catDef.name,
                            tint = Color(0xFFBABED2), // muted gray
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Category Title
                    Column {
                        Text(
                            text = catDef.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = txn.bankName,
                            fontSize = 14.sp,
                            color = Color(0xFFBABED2)
                        )
                    }
                }

                // Date and Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Date",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = dateTime,
                        fontSize = 12.sp,
                        color = Color(0xFFBABED2)
                    )
                    Text(
                        text = "I",
                        fontSize = 20.sp,
                        color = Color(0xFF4489C0)
                    )
                    // Bank Information Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Business,
                            contentDescription = "Bank",
                            tint = Color(0xFF00BCD4),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = txn.bankName,
                            fontSize = 12.sp,
                            color = Color(0xFFBABED2)
                        )
                    }
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                // Action Icons (now clickable)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Edit Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { bankTxn?.let { onEdit(it) } }
                            .background(
                                color = Color(0xFF444950),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF5A5F66),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Delete Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onDelete() }
                            .background(
                                color = Color(0xFF444950),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF5A5F66),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
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

@Preview(showBackground = true)
@Composable
fun GlassTransactionCardPreview_Food() {
    SamStudioAppTheme {
        GlassTransactionCard(
            txn = BankTransaction(
                messageTime = 1718000000000,
                amount = 250.0,
                bankName = "HDFC Bank",
                tags = "food, lunch",
                category = "Food",
                verified = true
            ),
            bankTxn = BankTransaction(
                messageTime = 1718000000000,
                amount = 250.0,
                bankName = "HDFC Bank",
                tags = "food, lunch",
                category = "Food",
                verified = true
            ),
            dateTimeFormat = SimpleDateFormat(SamDateFormat, Locale.getDefault()),
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GlassTransactionCardPreview_Transport() {
    SamStudioAppTheme {
        GlassTransactionCard(
            txn = BankTransaction(
                messageTime = 1718100000000,
                amount = 120.0,
                bankName = "SBI",
                tags = "travel, cab",
                category = "Transport",
                verified = false
            ),
            bankTxn = BankTransaction(
                messageTime = 1718100000000,
                amount = 120.0,
                bankName = "SBI",
                tags = "travel, cab",
                category = "Transport",
                verified = false
            ),
            dateTimeFormat = SimpleDateFormat(SamDateFormat, Locale.getDefault()),
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GlassTransactionCardPreview_Other() {
    SamStudioAppTheme {
        GlassTransactionCard(
            txn = BankTransaction(
                messageTime = 1718200000000,
                amount = 9999.0,
                bankName = "ICICI",
                tags = "misc",
                category = "Other",
                verified = false
            ),
            bankTxn = null,
            dateTimeFormat = SimpleDateFormat(SamDateFormat, Locale.getDefault()),
            onEdit = {},
            onDelete = {}
        )
    }
} 