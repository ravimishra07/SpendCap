package com.ravi.samstudioapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.presentation.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedMainScreen(
    viewModel: MainViewModel,
    filteredSmsTransactions: List<ParsedSmsTransaction>,
    transactions: List<BankTransaction>,
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    prevRange: Pair<Long, Long>?,
    prefs: android.content.SharedPreferences,
    onSavePreferences: () -> Unit,
    onRefreshClick: () -> Unit,
    onInsightsClick: () -> Unit,
    isLoading: Boolean
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
        ToolbarWithDateRange(
            currentRange = currentRange,
            mode = mode,
            prevRange = prevRange,
            prefs = prefs,
            onPrevClick = {
                viewModel.shiftDateRange(forward = false)
                onSavePreferences()
            },
            onNextClick = {
                viewModel.shiftDateRange(forward = true)
                onSavePreferences()
            },
            onModeChange = { newMode ->
                viewModel.changeDateRangeMode(newMode)
                onSavePreferences()
            },
            onDatePickerChange = { start, end ->
                viewModel.setDateRangeFromPicker(start, end)
                onSavePreferences()
            },
            onRefreshClick = onRefreshClick,
            onInsightsClick = onInsightsClick,
            isLoading = isLoading,
            onAddDummyClick = {},
            smsTransactions = filteredSmsTransactions,
            bankTransactions = transactions,
            onEdit = { transaction ->
                viewModel.updateTransaction(transaction)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        MainTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // Transactions Tab
                TransactionList(
                    smsTransactions = filteredSmsTransactions,
                    bankTransactions = transactions,
                    onEdit = { transaction ->
                        viewModel.updateTransaction(transaction)
                    }
                )
            }
            1 -> {
                // Insights Tab - Empty for now
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Analytics,
                            contentDescription = "Insights",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Insights Coming Soon",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analytics and charts will be displayed here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
} 