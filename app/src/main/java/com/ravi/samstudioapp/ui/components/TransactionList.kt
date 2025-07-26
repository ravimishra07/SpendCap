package com.ravi.samstudioapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.components.GlassTransactionCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    smsTransactions: List<BankTransaction>,
    bankTransactions: List<BankTransaction>,
    onEdit: (BankTransaction) -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var editingTxn by remember { mutableStateOf<BankTransaction?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editBankName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Other") }

    // Keep edit fields in sync with editingTxn
    LaunchedEffect(editingTxn) {
        editingTxn?.let {
            editAmount = it.amount.toString()
            editType = it.tags
            editBankName = it.bankName
            editCategory = it.category.ifBlank { "Other" }
        }
    }

    // Memoize expensive operations
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }
    
    val (filteredTxns, selectedCategory, onCategorySelected) = useTransactionFilter(smsTransactions)
    val grouped = remember(filteredTxns, dateFormat) {
        filteredTxns.groupBy { dateFormat.format(Date(it.messageTime)) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips row
        TransactionFilterChips(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
        
        // Total spend and cigarette count row
        TransactionSummaryRow(filteredTxns = filteredTxns)

        // Grouped LazyColumn for smsTransactions, filtered by selected chip
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (date, txns) ->
                    items(txns) { txn ->
                        val bankTxn = remember(txn, bankTransactions) {
                            bankTransactions.find { 
                                it.amount == txn.amount && 
                                it.bankName == txn.bankName && 
                                it.messageTime == txn.messageTime 
                            }
                        }
                        
                        GlassTransactionCard(
                            txn = txn,
                            bankTxn = bankTxn,
                            dateTimeFormat = dateTimeFormat,
                            onEdit = { editingTxn = it },
                            onDelete = { viewModel.markTransactionAsDeleted(txn.messageTime) }
                        )
                    }
                }
            }

            // Edit dialog
            if (editingTxn != null) {
                TransactionEditDialog(
                    editingTxn = editingTxn,
                    editAmount = editAmount,
                    editType = editType,
                    editBankName = editBankName,
                    editCategory = editCategory,
                    onAmountChange = { editAmount = it },
                    onTypeChange = { editType = it },
                    onBankNameChange = { editBankName = it },
                    onCategoryChange = { editCategory = it },
                    onSave = {
                        editingTxn?.let { txn ->
                            val updatedTxn = txn.copy(
                                amount = editAmount.toDoubleOrNull() ?: txn.amount,
                                tags = editType,
                                bankName = editBankName,
                                category = editCategory,
                                deleted = false // Always restore on edit
                            )
                            onEdit(updatedTxn)
                        }
                        editingTxn = null
                    },
                    onDismiss = { editingTxn = null }
                )
            }
        }
    }
} 