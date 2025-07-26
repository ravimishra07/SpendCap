package com.ravi.samstudioapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.ui.categoryDefs

@Composable
fun TransactionEditDialog(
    editingTxn: BankTransaction?,
    editAmount: String,
    editType: String,
    editBankName: String,
    editCategory: String,
    onAmountChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onBankNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editType,
                    onValueChange = onTypeChange,
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editBankName,
                    onValueChange = onBankNameChange,
                    label = { Text("Bank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                
                // Category dropdown
                var showCategoryDropdown by remember { mutableStateOf(false) }
                
                OutlinedTextField(
                    value = editCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { 
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Select Category",
                            modifier = Modifier.clickable { showCategoryDropdown = !showCategoryDropdown }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = !showCategoryDropdown }
                )
                
                if (showCategoryDropdown) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column {
                            categoryDefs.forEach { category ->
                                Text(
                                    text = category.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCategoryChange(category.name)
                                            showCategoryDropdown = false
                                        }
                                        .padding(16.dp),
                                    color = if (editCategory == category.name) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                                if (category != categoryDefs.last()) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 