package com.ravi.samstudioapp.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.ui.TransactionList
import com.ravi.samstudioapp.domain.model.BankTransaction
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onEdit: (BankTransaction) -> Unit,
    onAdd: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val dateRange by viewModel.dateRange.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedRange = "${formatter.format(Date(dateRange.first))} - ${formatter.format(Date(dateRange.second))}"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions: $formattedRange") },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date Range")
                    }
                    IconButton(onClick = { viewModel.syncFromSms(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync SMS")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TransactionList(
                transactions = transactions,
                onEdit = onEdit,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
            // Optionally show loading indicator
            // if (isLoading) { ... }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = dateRange.first,
            initialSelectedEndDateMillis = dateRange.second
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.setDateRange(start, end)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = pickerState)
        }
    }
} 