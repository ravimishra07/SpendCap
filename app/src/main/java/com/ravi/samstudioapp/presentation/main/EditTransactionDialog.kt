package com.ravi.samstudioapp.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.domain.model.BankTransaction

@Composable
fun EditTransactionDialog(
    transaction: BankTransaction?,
    onDismiss: () -> Unit,
    onSave: (BankTransaction) -> Unit
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(transaction?.tags ?: "") }
    var bankName by remember { mutableStateOf(transaction?.bankName ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val txn = BankTransaction(
                    id = transaction?.id ?: 0,
                    amount = amt,
                    tags = type,
                    bankName = bankName,
                    messageTime = transaction?.messageTime ?: System.currentTimeMillis(),
                    count = transaction?.count
                )
                onSave(txn)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
} 