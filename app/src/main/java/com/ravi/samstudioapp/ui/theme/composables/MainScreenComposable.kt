package com.ravi.samstudioapp.ui.theme.composables

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.data.BankTransactionDao
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.DateRangeMode
import com.ravi.samstudioapp.ui.ExpenseSubType
import com.ravi.samstudioapp.ui.MainTabContent
import com.ravi.samstudioapp.ui.MainTabs
import com.ravi.samstudioapp.ui.ToolbarWithDateRange
import com.ravi.samstudioapp.ui.categories
import com.ravi.samstudioapp.ui.shiftDateRange
import com.ravi.samstudioapp.utils.readAndParseSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


// Refactored MainActivity.kt — Modular Breakdown Starting Point
// Extract reusable Composables and clean state management begins here

// Final step: Refactor LoadMainScreen to use extracted modular components.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadMainScreen1(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("samstudio_prefs", Context.MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()

    val db = AppDatabase.getInstance(context)
    val dao = db.bankTransactionDao()

    var smsTransactions by remember { mutableStateOf<List<ParsedSmsTransaction>>(emptyList()) }
    var roomTransactions by remember { mutableStateOf<List<BankTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<Int?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf(DateRangeMode.DAILY) }
    var currentRange by remember {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(mode.days - 1))
        val start = cal.timeInMillis
        mutableStateOf(start to end)
    }

    LaunchedEffect(Unit) {
        LoadInitialPreferences(prefs, dao) { parsed, savedMode, savedRange ->
            coroutineScope.launch {
                smsTransactions = parsed
                roomTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                mode = savedMode
                currentRange = savedRange
            }
        }
    }

    if (showEditSheet && editId != null) {
        TransactionEditBottomSheet(
            editAmount = editAmount,
            onAmountChange = { editAmount = it },
            editType = editType,
            onTypeChange = { editType = it },
            typeOptions = categories.map { it.first },
            onCancelClick = { showEditSheet = false },
            onSaveClick = {
                val amt = editAmount.toDoubleOrNull()
                if (amt != null && editType.isNotBlank()) {
                    coroutineScope.launch {
                        val txn = roomTransactions.find { it.id == editId }
                        txn?.let {
                            val updatedTxn = it.copy(amount = amt, tags = editType)
                            viewModel.updateTransaction(updatedTxn)
                            roomTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                        }
                        showEditSheet = false
                    }
                }
            }
        )
    }

    ToolbarWithDateRange(
        currentRange = currentRange,
        mode = mode,
        prevRange = null,
        prefs = prefs,
        onPrevClick = {
            val newRange = shiftDateRange(currentRange, mode, forward = false)
            currentRange = newRange
            prefs.edit {
                putLong("date_range_start", newRange.first)
                putLong("date_range_end", newRange.second)
                putString("date_range_mode", mode.name)
            }
        },
        onNextClick = {
            val newRange = shiftDateRange(currentRange, mode, forward = true)
            currentRange = newRange
            prefs.edit {
                putLong("date_range_start", newRange.first)
                putLong("date_range_end", newRange.second)
                putString("date_range_mode", mode.name)
            }
        },
        onModeChange = { newMode ->
            mode = newMode
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
            val start = cal.timeInMillis
            currentRange = start to end
            prefs.edit {
                putLong("date_range_start", start)
                putLong("date_range_end", end)
                putString("date_range_mode", newMode.name)
            }
        },
        onDatePickerChange = { start, end ->
            currentRange = start to end
            prefs.edit {
                putLong("date_range_start", start)
                putLong("date_range_end", end)
                putString("date_range_mode", mode.name)
            }
        },
        onRefreshClick = {
            coroutineScope.launch {
                HandleRefreshSms(
                    context = context,
                    dao = dao,
                    onStart = { isLoading = true },
                    onComplete = {
                        coroutineScope.launch {
                            smsTransactions = it
                            roomTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                            isLoading = false
                        }
                    },
                    onError = {
                        Log.e("SamStudio", "Error refreshing SMS", it)
                        isLoading = false
                    }
                )
            }
        },
        isLoading = isLoading,
        onAddDummyClick = {},
        smsTransactions = smsTransactions,
        bankTransactions = roomTransactions,
        onEdit = { editingTxn ->
            editId = editingTxn.id
            editAmount = editingTxn.amount.toString()
            editType = editingTxn.tags
            showEditSheet = true
        }
    )

    SpendMirrorMainContent(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
        smsTransactions = smsTransactions,
        roomTransactions = roomTransactions,
        currentRange = currentRange,
        mode = mode,
        categories = categories,
        onEditClick = { categoryName, subType ->
            editId = subType.id
            editAmount = subType.amount.toString()
            editType = categoryName
            showEditSheet = true
        }
    )
}

suspend fun HandleRefreshSms(
    context: Context,
    dao: BankTransactionDao,
    onStart: () -> Unit,
    onComplete: (List<ParsedSmsTransaction>) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        onStart()
        val parsed = withContext(Dispatchers.IO) { readAndParseSms(context) }
        parsed.forEach { sms ->
            val txn = BankTransaction(
                amount = sms.amount,
                bankName = sms.bankName,
                messageTime = sms.messageTime,
                tags = sms.rawMessage,
                count = null
            )
            try {
                withContext(Dispatchers.IO) { dao.insert(txn) }
            } catch (e: Exception) {
                Log.e("SamStudio", "Insert error: ${'$'}{e.message}")
            }
        }
        val allTxns = withContext(Dispatchers.IO) { dao.getAll() }
        val parsedResult = allTxns.map {
            ParsedSmsTransaction(
                amount = it.amount,
                bankName = it.bankName,
                messageTime = it.messageTime,
                rawMessage = it.tags
            )
        }
        onComplete(parsedResult)
    } catch (e: Exception) {
        onError(e)
    }
}

// Composables to extract:
// - SpendMirrorMainContent

@Composable
fun SpendMirrorMainContent(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    smsTransactions: List<ParsedSmsTransaction>,
    roomTransactions: List<BankTransaction>,
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    categories: List<Pair<String, Pair<Any, Any>>>,
    onEditClick: (String, ExpenseSubType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
        val filteredSmsTransactions = smsTransactions.filter {
            it.messageTime in currentRange.first..currentRange.second
        }

        MainTabs(selectedTabIndex = selectedTabIndex, onTabSelected = onTabSelected)

        MainTabContent(
            selectedTabIndex = selectedTabIndex,
            roomTransactions = roomTransactions,
            currentRange = currentRange,
            mode = mode,
            categories = categories,
            onEditClick = onEditClick
        )
    }
}
// - TransactionEditBottomSheet
// - LoadInitialPreferences
// - HandleRefreshSms
// - SpendMirrorMainContent

// Proceed with first extraction: TransactionEditBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditBottomSheet(
    editAmount: String,
    onAmountChange: (String) -> Unit,
    editType: String,
    onTypeChange: (String) -> Unit,
    typeOptions: List<String>,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onCancelClick) {
        Column(Modifier.padding(24.dp)) {
            Text("Edit Transaction", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = editAmount,
                onValueChange = { onAmountChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = editType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                    modifier = Modifier
                        .width(180.dp)
                        .menuAnchor()
                        .clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    typeOptions.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                onTypeChange(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancelClick) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSaveClick) { Text("Save") }
            }
        }
    }
}

// Proceed with next extraction: SpendMirrorMainContent

suspend fun LoadInitialPreferences(
    prefs: SharedPreferences,
    dao: BankTransactionDao,
    onLoad: (List<ParsedSmsTransaction>, DateRangeMode, Pair<Long, Long>) -> Unit
) {
    val savedStart = prefs.getLong("date_range_start", -1L)
    val savedEnd = prefs.getLong("date_range_end", -1L)
    val savedMode = prefs.getString("date_range_mode", null)

    val mode = if (savedMode != null) DateRangeMode.valueOf(savedMode) else DateRangeMode.DAILY
    val range = if (savedStart > 0 && savedEnd > 0) savedStart to savedEnd else {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(mode.days - 1))
        cal.timeInMillis to end
    }

    val allTxns = withContext(Dispatchers.IO) { dao.getAll() }
    val parsed = allTxns.map {
        ParsedSmsTransaction(
            amount = it.amount,
            bankName = it.bankName,
            messageTime = it.messageTime,
            rawMessage = it.tags
        )
    }
    onLoad(parsed, mode, range)
}