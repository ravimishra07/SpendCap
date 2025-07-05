package com.ravi.samstudioapp.ui

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.domain.model.BankTransaction
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.vector.ImageVector
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.room.Room
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.presentation.main.EditTransactionDialog
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.utils.Constants
import com.ravi.samstudioapp.utils.SharedPreference.CORE_NAME
import com.ravi.samstudioapp.utils.SharedPreference.RANGE_END
import com.ravi.samstudioapp.utils.SharedPreference.RANGE_MODE
import com.ravi.samstudioapp.utils.SharedPreference.RANGE_START
import com.ravi.samstudioapp.utils.readAndParseSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Add DateRangeMode enum at the top level
enum class DateRangeMode(val days: Int) {
    DAILY(1), WEEKLY(7), MONTHLY(30)
}

val darkShadow = ComposeColor.Black.copy(alpha = 0.40f)
val lightShadow = ComposeColor.White.copy(alpha = 0.10f)
val backgroundColor = ComposeColor.Black.copy(alpha = 0.9f)

// Define a single source of truth for categories
val categories = listOf(
    "Food" to (Icons.Filled.Fastfood to ComposeColor(0xFFEF6C00)),
    "Cigarette" to (Icons.Filled.LocalCafe to ComposeColor(0xFF6D4C41)),
    "Travel" to (Icons.Filled.DirectionsCar to ComposeColor(0xFF388E3C)),
    "Other" to (Icons.Filled.LocalDrink to ComposeColor(0xFF0288D1))
)

// Central source for categories
// Each category: name, icon, color, and a matcher function

data class CategoryDef(
    val name: String,
    val icon: ImageVector,
    val color: ComposeColor,
    val matcher: (ParsedSmsTransaction) -> Boolean
)

val categoryDefs = listOf(
    CategoryDef(
        "Food",
        Icons.Filled.Fastfood,
        ComposeColor(0xFFEF6C00)
    ) { txn ->
        txn.rawMessage.contains("food", ignoreCase = true) || txn.bankName.contains(
            "food",
            ignoreCase = true
        )
    },
    CategoryDef(
        "Cigarette",
        Icons.Filled.LocalCafe,
        ComposeColor(0xFF6D4C41)
    ) { txn -> txn.rawMessage.contains("cigarette", ignoreCase = true) },
    CategoryDef(
        "Travel",
        Icons.Filled.DirectionsCar,
        ComposeColor(0xFF388E3C)
    ) { txn -> txn.rawMessage.contains("travel", ignoreCase = true) },
    CategoryDef(
        "Other",
        Icons.Filled.LocalDrink,
        ComposeColor(0xFF0288D1)
    ) { txn -> true } // fallback
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomToolbarWithDateRange(
    modifier: Modifier = Modifier,
    title: String = "SpendMirror",
    onIcon1Click: () -> Unit = {},
    dateRange: String = "Date Range",
    onPrevClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    isLoading: Boolean = false,
    onAddDummyClick: () -> Unit = {},
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    onModeChange: (DateRangeMode) -> Unit,
    onDatePickerChange: (Long, Long) -> Unit,
    smsTransactions: List<ParsedSmsTransaction> = emptyList(),
    bankTransactions: List<BankTransaction> = emptyList(),
    onEdit: (BankTransaction) -> Unit = {}
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val context = LocalContext.current
    val formattedRange = currentRange.let { (start, end) ->
        run {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
        }
    }
    var editingTxn by remember { mutableStateOf<BankTransaction?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editBankName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Other") }
    var expanded by remember { mutableStateOf(false) }

    // Keep edit fields in sync with editingTxn
    LaunchedEffect(editingTxn) {
        editingTxn?.let {
            editAmount = it.amount.toString()
            editType = it.tags
            editBankName = it.bankName
            editCategory = it.category.ifBlank { "Other" }
        }
    }
    Column(modifier = modifier) {
        // Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ComposeColor.White
            )
            Row {
                IconButton(onClick = onRefreshClick, enabled = !isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
                // Number toggle with circular background
                IconButton(onClick = {
                    val newMode = when (mode) {
                        DateRangeMode.DAILY -> DateRangeMode.WEEKLY
                        DateRangeMode.WEEKLY -> DateRangeMode.MONTHLY
                        DateRangeMode.MONTHLY -> DateRangeMode.DAILY
                    }
                    onModeChange(newMode)
                    val cal = Calendar.getInstance()
                    val end = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
                    val start = cal.timeInMillis
                    onDatePickerChange(start, end)
                    Toast.makeText(
                        context,
                        "Range changed to ${newMode.days} days",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = mode.days.toString(),
                            color = ComposeColor.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Date Range Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onPrevClick) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Previous",
                    tint = ComposeColor.White
                )
            }
            Box(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = formattedRange,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ComposeColor.White,
                    modifier = Modifier.clickable { showDateRangePicker = true }
                )
            }
            IconButton(onClick = onNextClick) {
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = ComposeColor.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Filter chips row
        var selectedCategory by remember { mutableStateOf<CategoryDef?>(null) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            categoryDefs.forEach { category ->
                AssistChip(
                    onClick = {
                        selectedCategory = if (selectedCategory == category) null else category
                    },
                    label = { Text(category.name, color = ComposeColor.White) },
                    leadingIcon = { Icon(category.icon, contentDescription = category.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedCategory == category) category.color else ComposeColor.LightGray,
                        labelColor = if (selectedCategory == category) ComposeColor.White else ComposeColor.Black
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Grouped LazyColumn for smsTransactions, filtered by selected chip
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
        val filteredTxns =
            selectedCategory?.let { cat -> smsTransactions.filter { cat.matcher(it) } }
                ?: smsTransactions
        val grouped = filteredTxns.groupBy { dateFormat.format(Date(it.messageTime)) }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (date, txns) ->
                    item {
                        Text(
                            date,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                            color = ComposeColor.White
                        )
                    }
                    items(txns) { txn ->
                        val bankTxn =
                            bankTransactions.find { it.amount == txn.amount && it.bankName == txn.bankName && it.messageTime == txn.messageTime }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val dateTimeFormat = remember {
                                    SimpleDateFormat(
                                        "MMM dd, yyyy, hh:mm a",
                                        Locale.getDefault()
                                    )
                                }
                                val dateTime = dateTimeFormat.format(Date(txn.messageTime))
                                Text("Amount: ₹${txn.amount}", color = ComposeColor.White)
                                Text("Bank: ${txn.bankName}", color = ComposeColor.White)
                                Text(
                                    "Message: ${txn.rawMessage}",
                                    fontSize = 12.sp,
                                    color = ComposeColor.Gray
                                )
                                // Show category as a chip
                                val catName = bankTxn?.category ?: "Other"
                                val catDef =
                                    categoryDefs.find { it.name.equals(catName, ignoreCase = true) }
                                        ?: categoryDefs.last()
                                Row(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(catDef.name, color = ComposeColor.White) },
                                        leadingIcon = {
                                            Icon(
                                                catDef.icon,
                                                contentDescription = catDef.name
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = catDef.color,
                                            labelColor = ComposeColor.White
                                        )
                                    )
                                    if (bankTxn?.verified == true) {
                                        Spacer(Modifier.width(8.dp))
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    "Verified",
                                                    color = ComposeColor.White,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Verified"
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = ComposeColor(0xFF388E3C),
                                                labelColor = ComposeColor.White
                                            )
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        val txnToEdit = bankTxn ?: BankTransaction(
                                            amount = txn.amount,
                                            bankName = txn.bankName,
                                            tags = "",
                                            messageTime = txn.messageTime,
                                            count = null,
                                            category = "Other"
                                        )
                                        editingTxn = txnToEdit
                                        Toast.makeText(context, "Edit clicked", Toast.LENGTH_SHORT)
                                            .show()
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                }
                                // Date & Time at the bottom, bold and larger
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = dateTime,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ComposeColor.White,
                                        modifier = Modifier
                                            .background(ComposeColor(0xFF222222))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Edit dialog
            if (editingTxn != null) {
                AlertDialog(
                    onDismissRequest = { editingTxn = null },
                    title = { Text("Edit Transaction") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Amount") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editType,
                                onValueChange = { editType = it },
                                label = { Text("Type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editBankName,
                                onValueChange = { editBankName = it },
                                label = { Text("Bank Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            // Category dropdown using ExposedDropdownMenuBox
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = editCategory,
                                    onValueChange = {},
                                    label = { Text("Category") },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    categoryDefs.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                editCategory = cat.name
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val amt = editAmount.toDoubleOrNull() ?: 0.0
                            val updated = editingTxn!!.copy(
                                amount = amt,
                                tags = editType,
                                bankName = editBankName,
                                category = editCategory.ifBlank { "Other" },
                                verified = true
                            )
                            onEdit(updated)
                            editingTxn = null
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingTxn = null }) { Text("Cancel") }
                    }
                )
            }
        }
        val context = LocalContext.current
        if (showDateRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onDatePickerChange(
                            dateRangePickerState.selectedStartDateMillis ?: -1L,
                            dateRangePickerState.selectedEndDateMillis ?: -1L
                        )
                        showDateRangePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(state = dateRangePickerState)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SamStudioAppTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SamStudioAppTheme {
        // Note: DateRangePicker dialog will only show on click in a real device/emulator.
        CustomToolbarWithDateRange(
            currentRange = 0L to 0L,
            mode = DateRangeMode.DAILY,
            onPrevClick = {},
            onNextClick = {},
            onModeChange = {},
            onDatePickerChange = { _, _ -> },
            onRefreshClick = {},
            isLoading = false,
            onAddDummyClick = {},
            bankTransactions = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DateRangePickerDialogPreview() {
    SamStudioAppTheme {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = {},
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {}) { Text("OK") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {}) { Text("Cancel") }
            }
        ) {
            val state = rememberDateRangePickerState()
            DateRangePicker(state = state)
        }
    }
}

// New screen composable for SMS transactions
@Composable
fun SmsTransactionsByDateScreen(
    transactions: List<ParsedSmsTransaction>,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val grouped = transactions.groupBy { dateFormat.format(Date(it.messageTime)) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("SMS Transactions (Grouped by Date)", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (date, txns) ->
                item {
                    Text(
                        date,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(txns) { txn ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val dateTimeFormat = remember {
                                SimpleDateFormat(
                                    "MMM dd, yyyy, hh:mm a",
                                    Locale.getDefault()
                                )
                            }
                            val dateTime = dateTimeFormat.format(Date(txn.messageTime))
                            Text("Amount: ₹${txn.amount}")
                            Text("Bank: ${txn.bankName}")
                            Text(
                                "Date & Time: $dateTime",
                                fontSize = 12.sp,
                                color = ComposeColor.Gray
                            )
                            Text(
                                "Message: ${txn.rawMessage}",
                                fontSize = 12.sp,
                                color = ComposeColor.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// Utility function for robust date range navigation
fun shiftDateRange(
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    forward: Boolean,
    preventFuture: Boolean = true
): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val days = mode.days

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    val cal = Calendar.getInstance()
    val currentStart = startOfDay(currentRange.first)
    val currentEnd = currentRange.second

    if (days == 1) {
        cal.timeInMillis = currentStart
        if (forward) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val newStart = startOfDay(cal.timeInMillis)
        val isToday = Calendar.getInstance().apply { timeInMillis = newStart }
            .get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR) &&
                Calendar.getInstance().apply { timeInMillis = newStart }
                    .get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)

        val newEnd = if (isToday && preventFuture) now else endOfDay(newStart)
        return if (preventFuture && newEnd > now) {
            Pair(newStart, now)
        } else {
            Pair(newStart, newEnd)
        }
    }

    val shift = if (forward) days else -days
    cal.timeInMillis = currentStart
    cal.add(Calendar.DAY_OF_YEAR, shift)
    val newStart = startOfDay(cal.timeInMillis)
    cal.add(Calendar.DAY_OF_YEAR, days - 1)
    val newEnd = endOfDay(cal.timeInMillis)

    val cappedEnd = if (preventFuture && newEnd > now) now else newEnd
    val cappedStart = if (preventFuture && cappedEnd < newStart) newStart else newStart

    return Pair(cappedStart, cappedEnd)
}

@Composable
fun SpendBarGraph(
    transactions: List<BankTransaction>,
    dateRange: Pair<Long, Long>,
    mode: DateRangeMode,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 24.dp,
    barSpacing: Dp = 16.dp,
    graphHeight: Dp = 180.dp
) {
    val zone = ZoneId.systemDefault()
    val startDate = Instant.ofEpochMilli(dateRange.first).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(dateRange.second).atZone(zone).toLocalDate()
    val days = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(endDate) }
        .toList()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val txnsByDay = transactions.groupBy {
        Instant.ofEpochMilli(it.messageTime).atZone(zone).toLocalDate()
    }
    val amounts = days.map { day ->
        txnsByDay[day]?.sumOf { it.amount } ?: 0.0
    }

    // Debug output
    LaunchedEffect(amounts) {
        println("SpendBarGraph days: $days")
        println("SpendBarGraph amounts: $amounts")
        println("SpendBarGraph transactions: $transactions")
        println("SpendBarGraph dateRange: $dateRange")
    }

    // Create chart entries for Vico
    val chartEntries = days.mapIndexed { index, day ->
        entryOf(index.toFloat(), amounts[index].toFloat())
    }

    val chartEntryModel = entryModelOf(chartEntries)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Spending (${mode.days} days)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (amounts.all { it == 0.0 }) {
            Text(
                "No spending data in this range",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = startAxis(),
                bottomAxis = bottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(graphHeight)
            )

            // Date labels below chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { day ->
                    Text(
                        text = day.format(dateFormatter),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSpendBarGraph() {
    val now = System.currentTimeMillis()
    val dummyTransactions = listOf(
        BankTransaction(
            1,
            100.0,
            "HDFC",
            "Food",
            now - 6 * 24 * 60 * 60 * 1000,
            null,
            "Food",
            false
        ),
        BankTransaction(
            2,
            200.0,
            "ICICI",
            "Travel",
            now - 5 * 24 * 60 * 60 * 1000,
            null,
            "Travel",
            false
        ),
        BankTransaction(
            3,
            50.0,
            "SBI",
            "Cigarette",
            now - 4 * 24 * 60 * 60 * 1000,
            null,
            "Cigarette",
            false
        ),
        BankTransaction(
            4,
            80.0,
            "Axis",
            "Food",
            now - 3 * 24 * 60 * 60 * 1000,
            null,
            "Food",
            false
        ),
        BankTransaction(
            5,
            120.0,
            "Kotak",
            "Other",
            now - 2 * 24 * 60 * 60 * 1000,
            null,
            "Other",
            false
        ),
        BankTransaction(
            6,
            60.0,
            "HDFC",
            "Food",
            now - 1 * 24 * 60 * 60 * 1000,
            null,
            "Food",
            false
        ),
        BankTransaction(7, 90.0, "ICICI", "Travel", now, null, "Travel", false)
    )
    val dateRange = Pair(now - 6 * 24 * 60 * 60 * 1000, now)
    SamStudioAppTheme {
        SpendBarGraph(
            transactions = dummyTransactions,
            dateRange = dateRange,
            mode = DateRangeMode.WEEKLY,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var roomTransactions by remember { mutableStateOf<List<BankTransaction>>(emptyList()) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<Int?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(DateRangeMode.DAILY) }
    val prefs = context.getSharedPreferences(CORE_NAME, MODE_PRIVATE)
    var editingTransaction by remember { mutableStateOf<BankTransaction?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var prevRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    var currentRange by remember {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(mode.days - 1))
        val start = cal.timeInMillis
        mutableStateOf<Pair<Long, Long>>(start to end)
    }
    var smsTransactions by remember {
        mutableStateOf<List<ParsedSmsTransaction>>(emptyList())
    }

    SamStudioAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val db = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                Constants.DB_NAME
            ).build()
            val dao = db.bankTransactionDao()

            LaunchedEffect(Unit) {
                val savedStart = prefs.getLong(RANGE_START, -1L)
                val savedEnd = prefs.getLong(RANGE_END, -1L)
                val savedMode = prefs.getString(RANGE_MODE, null)
                if (savedStart > 0 && savedEnd > 0 && savedMode != null) {
                    mode = DateRangeMode.valueOf(savedMode)
                    currentRange = savedStart to savedEnd
                }
                // Todo: move to vm
                val allTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                smsTransactions = allTransactions.map {
                    ParsedSmsTransaction(
                        amount = it.amount,
                        bankName = it.bankName,
                        messageTime = it.messageTime,
                        rawMessage = it.tags
                    )
                }
            }

            // Move this outside the Column:
            var selectedTabIndex by remember { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 24.dp)
            ) {
                // Filter smsTransactions by currentRange
                val filteredSmsTransactions = smsTransactions.filter {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                    Log.d(
                        "SamStudio",
                        "currentRange: ${sdf.format(currentRange.first)} to ${
                            sdf.format(currentRange.second)
                        }"
                    )
                    Log.d("SamStudio", "txn: ${it.bankName} @ ${sdf.format(it.messageTime)}")
                    it.messageTime in currentRange.first..currentRange.second
                }

                ToolbarWithDateRange(
                    currentRange = currentRange,
                    mode = mode,
                    prevRange = prevRange,
                    prefs = prefs,
                    onPrevClick = {
                        prevRange = currentRange
                        val newRange = shiftDateRange(currentRange, mode, forward = false)
                        currentRange = newRange
                        prefs.edit {
                            putLong("date_range_start", newRange.first)
                            putLong("date_range_end", newRange.second)
                            putString("date_range_mode", mode.name)
                        }
                    },
                    onNextClick = {
                        prevRange = currentRange
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
                        val cal = java.util.Calendar.getInstance()
                        val end = cal.timeInMillis
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -(newMode.days - 1))
                        val start = cal.timeInMillis
                        currentRange = start to end
                        prefs.edit {
                            putLong("date_range_start", start)
                            putLong("date_range_end", end)
                            putString("date_range_mode", newMode.name)
                        }
                    },
                    onDatePickerChange = { start, end ->
                        prevRange = currentRange
                        // Patch: expand single-day range to full day
                        val calStart =
                            java.util.Calendar.getInstance().apply { timeInMillis = start }
                        val calEnd =
                            java.util.Calendar.getInstance().apply { timeInMillis = end }
                        val isSameDay =
                            calStart.get(java.util.Calendar.YEAR) == calEnd.get(java.util.Calendar.YEAR) &&
                                    calStart.get(java.util.Calendar.DAY_OF_YEAR) == calEnd.get(
                                java.util.Calendar.DAY_OF_YEAR
                            )
                        if (isSameDay) {
                            calStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            calStart.set(java.util.Calendar.MINUTE, 0)
                            calStart.set(java.util.Calendar.SECOND, 0)
                            calStart.set(java.util.Calendar.MILLISECOND, 0)
                            calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            calEnd.set(java.util.Calendar.MINUTE, 59)
                            calEnd.set(java.util.Calendar.SECOND, 59)
                            calEnd.set(java.util.Calendar.MILLISECOND, 999)
                        }
                        val newStart = calStart.timeInMillis
                        val newEnd = calEnd.timeInMillis
                        currentRange = newStart to newEnd
                        prefs.edit {
                            putLong("date_range_start", newStart)
                            putLong("date_range_end", newEnd)
                            putString("date_range_mode", mode.name)
                        }
                    },
                    onRefreshClick = {
                        Log.d("SamStudio", "Refresh button clicked, isLoading: $isLoading")
                        if (!isLoading) {
                            val permissionGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                            Log.d("SamStudio", "SMS permission granted: $permissionGranted")

                            if (permissionGranted) {
                                coroutineScope.launch {
                                    try {
                                        Log.d("SamStudio", "Starting SMS parsing...")
                                        isLoading = true
                                        val parsed = withContext(Dispatchers.IO) {
                                            readAndParseSms(context)
                                        }
                                        Log.d("SamStudio", "Parsed ${parsed.size} SMS messages")

                                        // Save all as BankTransaction, ignore duplicates
                                        parsed.forEach { sms ->
                                            val txn = BankTransaction(
                                                amount = sms.amount,
                                                bankName = sms.bankName,
                                                messageTime = sms.messageTime,
                                                tags = sms.rawMessage,
                                                count = null
                                            )
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    dao.insert(txn)
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "SamStudio",
                                                    "Insert error: ${e.message}"
                                                )
                                            }
                                        }

                                        val allTxns = withContext(Dispatchers.IO) {
                                            dao.getAll()
                                        }
                                        Log.d(
                                            "SamStudio",
                                            "Retrieved ${allTxns.size} transactions from DB"
                                        )

                                        smsTransactions = allTxns.map {
                                            ParsedSmsTransaction(
                                                amount = it.amount,
                                                bankName = it.bankName,
                                                messageTime = it.messageTime,
                                                rawMessage = it.tags
                                            )
                                        }
                                        Log.d(
                                            "SamStudio",
                                            "Updated smsTransactions list with ${smsTransactions.size} items"
                                        )
                                        isLoading = false
                                    } catch (e: Exception) {
                                        Log.e(
                                            "SamStudio",
                                            "Error during refresh: ${e.message}",
                                            e
                                        )
                                        isLoading = false
                                    }
                                }
                            } else {
                                Log.d("SamStudio", "Requesting SMS permission...")
                              //  requestSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                            }
                        } else {
                            Log.d("SamStudio", "Already loading, ignoring refresh click")
                        }
                    },
                    isLoading = isLoading,
                    onAddDummyClick = {
                        coroutineScope.launch {
                            val dummyList = listOf(
                                BankTransaction(
                                    amount = 100.0,
                                    bankName = "HDFC",
                                    tags = "Food",
                                    messageTime = System.currentTimeMillis()
                                ),
                                BankTransaction(
                                    amount = 200.0,
                                    bankName = "ICICI",
                                    tags = "Travel",
                                    messageTime = System.currentTimeMillis()
                                ),
                                BankTransaction(
                                    amount = 50.0,
                                    bankName = "SBI",
                                    tags = "Cigarette",
                                    messageTime = System.currentTimeMillis()
                                ),
                                BankTransaction(
                                    amount = 80.0,
                                    bankName = "Axis",
                                    tags = "Food",
                                    messageTime = System.currentTimeMillis()
                                ),
                                BankTransaction(
                                    amount = 120.0,
                                    bankName = "Kotak",
                                    tags = "Other",
                                    messageTime = System.currentTimeMillis()
                                )
                            )
                            dummyList.forEach { dao.insert(it) }
                            roomTransactions =
                                withContext(Dispatchers.IO) { dao.getAll() }
                        }
                    },
                    smsTransactions = smsTransactions,
                    bankTransactions = roomTransactions,
                    onEdit = { editingTransaction = it; showDialog = true }
                )

                MainTabs(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it })

                // Tab content
                MainTabContent(
                    selectedTabIndex = selectedTabIndex,
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
            // Show bottom sheet for editing (must be inside setContent)
            if (showEditSheet && editId != null) {
                ModalBottomSheet(
                    onDismissRequest = { showEditSheet = false }
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Edit Transaction", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = editAmount,
                            onValueChange = {
                                editAmount = it.filter { ch -> ch.isDigit() || ch == '.' }
                            },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        val typeOptions = categories.map { it.first }
                        var expanded by remember { mutableStateOf(false) }
                        androidx.compose.material3.ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = editType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type") },
                                trailingIcon = { Icons.Filled.KeyboardArrowDown },
                                modifier = Modifier
                                    .width(180.dp)
                                    .menuAnchor()
                                    .clickable { expanded = true }
                            )
                            androidx.compose.material3.DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                typeOptions.forEach { type ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            editType = type
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showEditSheet = false }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                val amt = editAmount.toDoubleOrNull()
                                if (amt != null && editType.isNotBlank() && editId != null) {
                                    coroutineScope.launch {
                                        val txn = roomTransactions.find { it.id == editId }
                                        if (txn != null) {
                                            val updatedTxn =
                                                txn.copy(amount = amt, tags = editType)
                                            viewModel.updateTransaction(updatedTxn)
                                            roomTransactions =
                                                withContext(Dispatchers.IO) { dao.getAll() }
                                        }
                                        showEditSheet = false
                                    }
                                }
                            }) { Text("Save") }
                        }
                    }
                }
            }

            if (showDialog) {
                EditTransactionDialog(
                    transaction = editingTransaction,
                    onDismiss = { showDialog = false },
                    onSave = {
                        if (it.id == 0) viewModel.addTransaction(it) else viewModel.updateTransaction(
                            it
                        )
                        showDialog = false
                    }
                )
            }
        }
    }
}