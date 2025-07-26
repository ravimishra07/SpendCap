package com.ravi.samstudioapp.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var category by remember { mutableStateOf(transaction?.category ?: "Other") }
    var verified by remember { mutableStateOf(transaction?.verified ?: false) }

    val categories = listOf(
        "Food", "Cigarette", "Transport", "Impulse", "Family",
        "Work", "Home", "Health", "Subscriptions", "Other"
    )

    var showCategoryDropdown by remember { mutableStateOf(false) }

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
                    label = { Text("Raw message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = !showCategoryDropdown }
                )
                if (showCategoryDropdown) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        categories.forEach { cat ->
                            Text(
                                text = cat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable {
                                        category = cat
                                        showCategoryDropdown = false
                                    }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = verified,
                        onCheckedChange = { verified = it }
                    )
                    Text("Verified")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val txn = BankTransaction(
                    messageTime = transaction?.messageTime ?: System.currentTimeMillis(),
                    amount = amt,
                    tags = type,
                    bankName = bankName,
                    count = transaction?.count,
                    category = category,
                    verified = verified
                )
                onSave(txn)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}