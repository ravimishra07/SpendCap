package com.ravi.samstudioapp.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

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

data class CategoryDef(val name: String, val icon: ImageVector, val color: ComposeColor, val matcher: (ParsedSmsTransaction) -> Boolean)

val categoryDefs = listOf(
    CategoryDef("Food", Icons.Filled.Fastfood, ComposeColor(0xFFEF6C00)) { txn -> txn.rawMessage.contains("food", ignoreCase = true) || txn.bankName.contains("food", ignoreCase = true) },
    CategoryDef("Cigarette", Icons.Filled.LocalCafe, ComposeColor(0xFF6D4C41)) { txn -> txn.rawMessage.contains("cigarette", ignoreCase = true) },
    CategoryDef("Travel", Icons.Filled.DirectionsCar, ComposeColor(0xFF388E3C)) { txn -> txn.rawMessage.contains("travel", ignoreCase = true) },
    CategoryDef("Other", Icons.Filled.LocalDrink, ComposeColor(0xFF0288D1)) { txn -> true } // fallback
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
            Text(text = title, style = MaterialTheme.typography.titleLarge)
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
                    Toast.makeText(context, "Range changed to ${newMode.days} days", Toast.LENGTH_SHORT).show()
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
                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous")
            }
            Box(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = formattedRange,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { showDateRangePicker = true }
                )
            }
            IconButton(onClick = onNextClick) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
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
                    onClick = { selectedCategory = if (selectedCategory == category) null else category },
                    label = { Text(category.name) },
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
        val filteredTxns = selectedCategory?.let { cat -> smsTransactions.filter { cat.matcher(it) } } ?: smsTransactions
        val grouped = filteredTxns.groupBy { dateFormat.format(Date(it.messageTime)) }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (date, txns) ->
                    item {
                        Text(date, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp))
                    }
                    items(txns) { txn ->
                        val bankTxn = bankTransactions.find { it.amount == txn.amount && it.bankName == txn.bankName && it.messageTime == txn.messageTime }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }
                                val dateTime = dateTimeFormat.format(Date(txn.messageTime))
                                Text("Amount: ₹${txn.amount}")
                                Text("Bank: ${txn.bankName}")
                                Text("Message: ${txn.rawMessage}", fontSize = 12.sp, color = ComposeColor.Gray)
                                // Show category as a chip
                                val catName = bankTxn?.category ?: "Other"
                                val catDef = categoryDefs.find { it.name.equals(catName, ignoreCase = true) } ?: categoryDefs.last()
                                Row(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(catDef.name) },
                                        leadingIcon = { Icon(catDef.icon, contentDescription = catDef.name) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = catDef.color,
                                            labelColor = ComposeColor.White
                                        )
                                    )
                                    if (bankTxn?.verified == true) {
                                        Spacer(Modifier.width(8.dp))
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Verified") },
                                            leadingIcon = { Icon(Icons.Filled.Check, contentDescription = "Verified") },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = ComposeColor(0xFF388E3C),
                                                labelColor = ComposeColor.White
                                            )
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
                                        Toast.makeText(context, "Edit clicked", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                }
                                // Date & Time at the bottom, bold and larger
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = dateTime,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ComposeColor.White,
                                        modifier = Modifier.background(ComposeColor(0xFF222222)).padding(horizontal = 8.dp, vertical = 4.dp)
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
        val context =LocalContext.current
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                    Text(date, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
                items(txns) { txn ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }
                            val dateTime = dateTimeFormat.format(Date(txn.messageTime))
                            Text("Amount: ₹${txn.amount}")
                            Text("Bank: ${txn.bankName}")
                            Text("Date & Time: $dateTime", fontSize = 12.sp, color = ComposeColor.Gray)
                            Text("Message: ${txn.rawMessage}", fontSize = 12.sp, color = ComposeColor.Gray)
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
