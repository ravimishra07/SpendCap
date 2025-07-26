package com.ravi.samstudioapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.ui.categoryDefs
import com.ravi.samstudioapp.ui.CategoryDef
import com.ravi.samstudioapp.ui.components.Black
import com.ravi.samstudioapp.ui.components.DarkGray
import com.ravi.samstudioapp.ui.components.LightGray
import com.ravi.samstudioapp.ui.components.NeumorphicBorderBox
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    txn: BankTransaction,
    bankTxn: BankTransaction?,
    dateTimeFormat: SimpleDateFormat,
    onEdit: (BankTransaction) -> Unit,
    onDelete: () -> Unit
) {
    NeumorphicBorderBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        cornerRadius = 12.dp,
        backgroundColor = DarkGray,
        borderColor = Color.White.copy(alpha = 0.10f),
        shadowElevation = 3.dp,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateTime = remember(txn.messageTime, dateTimeFormat) {
                dateTimeFormat.format(Date(txn.messageTime))
            }
            val catName = bankTxn?.category ?: "Other"
            val catDef = remember(catName) {
                categoryDefs.find { it.name.equals(catName, ignoreCase = true) }
                    ?: categoryDefs.last()
            }
            
            // Category icon in colored background (left)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(catDef.color, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    catDef.icon,
                    contentDescription = catDef.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            
            // Main info (center, expanded)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = catDef.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "₹${txn.amount}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = catDef.color
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = txn.bankName,
                        fontSize = 11.sp,
                        color = LightGray.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateTime,
                        fontSize = 10.sp,
                        color = LightGray.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Actions (right)
            TransactionActions(
                bankTxn = bankTxn,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
fun TransactionActions(
    bankTxn: BankTransaction?,
    onEdit: (BankTransaction) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bankTxn?.let { transaction ->
            IconButton(
                onClick = { onEdit(transaction) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit Transaction",
                    tint = LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete Transaction",
                    tint = Color.Red,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (bankTxn?.verified == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Verified",
                    tint = LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "✓",
                    fontSize = 10.sp,
                    color = LightGray.copy(alpha = 0.8f)
                )
            }
        }
    }
} 