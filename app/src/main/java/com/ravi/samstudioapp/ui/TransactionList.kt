package com.ravi.samstudioapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.domain.model.BankTransaction

@Composable
fun TransactionList(
    transactions: List<BankTransaction>,
    onEdit: (BankTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(transactions) { transaction ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(transaction.bankName, style = MaterialTheme.typography.titleMedium)
                        Text("₹${transaction.amount}", style = MaterialTheme.typography.bodyLarge)
                        Text("${transaction.tags}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onEdit(transaction) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }
        }
    }
} 