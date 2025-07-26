package com.ravi.samstudioapp.presentation.screens.expense

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.presentation.insights.InsightsActivity
import com.ravi.samstudioapp.presentation.main.BaseViewModel
import com.ravi.samstudioapp.presentation.main.EditTransactionDialog
import com.ravi.samstudioapp.presentation.screens.expense.ExpenseViewModel
import com.ravi.samstudioapp.ui.DetailedInsightsTab
import com.ravi.samstudioapp.ui.MainTabs
import com.ravi.samstudioapp.ui.ToolbarWithDateRange
import com.ravi.samstudioapp.ui.components.DarkGray
import com.ravi.samstudioapp.ui.components.LightGray
import com.ravi.samstudioapp.ui.components.NewMessagePopup
import com.ravi.samstudioapp.ui.components.TransactionList
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import com.ravi.samstudioapp.utils.PermissionManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current

    // UI state only
    var editingTransaction by remember { mutableStateOf<BankTransaction?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Get state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val smsTransactions by viewModel.smsTransactions.collectAsState()
    val filteredSmsTransactions by viewModel.filteredSmsTransactions.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currentRange by viewModel.dateRange.collectAsState()
    val mode by viewModel.dateRangeMode.collectAsState()
    val prevRange by viewModel.prevRange.collectAsState()

    // Real-time message detection state
    val newMessageDetected by viewModel.newMessageDetected.collectAsState()

    val prefs = context.getSharedPreferences(BaseViewModel.CORE_NAME, Context.MODE_PRIVATE)
    viewModel.loadInitialPreferences(prefs)

    // Helper function to save preferences
    fun savePreferences() {
        val (start, end, modeName) = viewModel.getDateRangeForPreferences()
        prefs.edit {
            putLong(BaseViewModel.RANGE_START, start)
            putLong(BaseViewModel.RANGE_END, end)
            putString(BaseViewModel.RANGE_MODE, modeName)
        }
    }

    SamStudioAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // .statusBarsPadding()
                    //.background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 0.dp)
            ) {
                ToolbarWithDateRange(
                    currentRange = currentRange,
                    mode = mode,
                    prevRange = prevRange,
                    prefs = prefs,
                    onPrevClick = {
                        viewModel.shiftDateRange(forward = false)
                        savePreferences()
                    },
                    onNextClick = {
                        viewModel.shiftDateRange(forward = true)
                        savePreferences()
                    },
                    onModeChange = { newMode ->
                        viewModel.changeDateRangeMode(newMode)
                        savePreferences()
                    },
                    onDatePickerChange = { start, end ->
                        viewModel.setDateRangeFromPicker(start, end)
                        savePreferences()
                    },
                    onRefreshClick = {
                        Log.d("SamStudio", "UI: Refresh button clicked, isLoading: $isLoading")
                        Log.d("SamStudio", "UI: About to check SMS permission")
                        if (!isLoading) {
                            // Check cached permission state first
                            if (PermissionManager.isSmsPermissionGranted()) {
                                Log.d("SamStudio", "UI: Permission already granted, starting sync")
                                viewModel.syncFromSms(context) { newTransactionCount ->
                                    when (newTransactionCount) {
                                        -1 -> Toast.makeText(context, "Sync failed", Toast.LENGTH_SHORT).show()
                                        0 -> Toast.makeText(context, "No new transactions found", Toast.LENGTH_SHORT).show()
                                        else -> Toast.makeText(context, "Sync completed: $newTransactionCount new transactions", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Check actual permission status
                                val permissionGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_SMS
                                ) == PackageManager.PERMISSION_GRANTED

                                Log.d("SamStudio", "UI: SMS permission granted: $permissionGranted")

                                if (permissionGranted) {
                                    Log.d("SamStudio", "UI: Permission granted, calling viewModel.syncFromSms")
                                    viewModel.syncFromSms(context) { newTransactionCount ->
                                        when (newTransactionCount) {
                                            -1 -> Toast.makeText(context, "Sync failed", Toast.LENGTH_SHORT).show()
                                            0 -> Toast.makeText(context, "No new transactions found", Toast.LENGTH_SHORT).show()
                                            else -> Toast.makeText(context, "Sync completed: $newTransactionCount new transactions", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    Log.d("SamStudio", "UI: viewModel.syncFromSms called successfully")
                                } else {
                                    Log.d("SamStudio", "UI: Permission not granted, requesting SMS permission...")
                                    PermissionManager.requestSmsPermission()
                                    Log.d("SamStudio", "UI: Permission request launched")
                                }
                            }
                        } else {
                            Log.d("SamStudio", "UI: Already loading, ignoring refresh click")
                        }
                    },
                    onInsightsClick = {
                        // Navigate to insights activity
                        val intent = Intent(context, InsightsActivity::class.java)
                        context.startActivity(intent)
                    },
                    isLoading = isLoading,
                    smsTransactions = filteredSmsTransactions,
                    bankTransactions = transactions,
                    onEdit = { editingTransaction = it; showDialog = true }
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
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val rotationAnimation by rememberInfiniteTransition().animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        )
                                    )
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Syncing...",
                                        tint = LightGray,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .graphicsLayer(rotationZ = rotationAnimation)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Syncing SMS transactions...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            TransactionList(
                                smsTransactions = filteredSmsTransactions,
                                bankTransactions = transactions,
                                onEdit = { transaction ->
                                    viewModel.findAndOverwriteTransaction(transaction)
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                    1 -> {
                        // Insights Tab with detailed charts
                        DetailedInsightsTab(
                            transactions = filteredSmsTransactions,
                            dateRange = currentRange,
                            mode = mode,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            }



            if (showDialog) {
                EditTransactionDialog(
                    transaction = editingTransaction,
                    onDismiss = { showDialog = false },
                    onSave = {
                        //  viewModel.saveTransaction(it)
                        val updatedTxn = editingTransaction?.copy(
                            amount = it.amount,
                            tags = it.tags,
                            bankName = it.bankName,
                            category = it.category,
                            deleted = false // Always restore on edit
                        )
                        viewModel.findAndOverwriteTransaction(updatedTxn ?: it)
                        showDialog = false
                    }
                )
            }

            // New message popup
            newMessageDetected?.let { transaction ->
                NewMessagePopup(
                    transaction = transaction,
                    onDismiss = { viewModel.dismissNewMessagePopup() },
                    onSave = { bankTransaction ->
                        viewModel.findAndOverwriteTransaction(bankTransaction)
                        viewModel.dismissNewMessagePopup()
                    }
                )
            }


        }
    }
}