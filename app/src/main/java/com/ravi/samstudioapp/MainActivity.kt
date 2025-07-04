package com.ravi.samstudioapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.Telephony
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.room.Room
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction
import androidx.compose.material3.CircularProgressIndicator
import android.content.SharedPreferences

// Add DateRangeMode enum at the top level
enum class DateRangeMode(val days: Int) {
    DAILY(1), WEEKLY(7), MONTHLY(30)
}

// Define a single source of truth for categories
val categories = listOf(
    "Food" to (Icons.Filled.Fastfood to ComposeColor(0xFFEF6C00)),
    "Cigarette" to (Icons.Filled.LocalCafe to ComposeColor(0xFF6D4C41)),
    "Travel" to (Icons.Filled.DirectionsCar to ComposeColor(0xFF388E3C)),
    "Other" to (Icons.Filled.LocalDrink to ComposeColor(0xFF0288D1))
)

class MainActivity : ComponentActivity() {
    // Add a state to hold parsed transactions (for SMS parsing only)
    private val _transactions = mutableStateOf<List<ParsedSmsTransaction>>(emptyList())
    val transactions: List<ParsedSmsTransaction> get() = _transactions.value

    private lateinit var prefs: SharedPreferences
    private lateinit var requestSmsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    private fun setupSmsPermissionLauncher() {
        requestSmsPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    logFederalBankMessages()
                } else {
                    Toast.makeText(this, "Permission denied to read messages", Toast.LENGTH_SHORT).show()
                }
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = getSharedPreferences("samstudio_prefs", MODE_PRIVATE)
        setupSmsPermissionLauncher()
        setContent {
            SamStudioAppTheme {
                MainScreen()
            }
        }
    }

    private fun logFederalBankMessages() {
        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(0)
                    val body = it.getString(1)
                    if (body.contains("federal bank", ignoreCase = true)) {
                        Log.d("SamStudio", "Federal Bank SMS: $body")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SamStudio", "Error reading SMS: ${e.message}")
        }
    }

    // Data class for parsed SMS transaction
    data class ParsedSmsTransaction(
        val amount: Double,
        val bankName: String,
        val messageTime: Long,
        val rawMessage: String
    )

    // Function to read and parse SMS messages
    private suspend fun readAndParseSms(): List<ParsedSmsTransaction> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ParsedSmsTransaction>()
        try {
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
                null, null, null
            )
            cursor?.use {
                val amountRegex = Regex("([\\d,]+\\.?\\d*)")
                while (it.moveToNext()) {
                    val body = it.getString(0)
                    val dateMillis = it.getLong(1)
                    Log.d("SamStudio", "Processing SMS: $body")
                    if (body.contains("federal", ignoreCase = true)) {
                        val match = amountRegex.find(body)
                        val amount = match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                        if (amount != null && amount < 500) {
                            Log.d("SamStudio", "Matched Federal Transaction: amount=$amount, dateMillis=$dateMillis")
                            result.add(
                                ParsedSmsTransaction(
                                    amount = amount,
                                    bankName = "Federal Bank",
                                    messageTime = dateMillis,
                                    rawMessage = body
                                )
                            )
                        } else {
                            Log.d("SamStudio", "Federal SMS but amount not found or >= 500: $body")
                        }
                    } else {
                        Log.d("SamStudio", "No match for SMS: $body")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SamStudio", "Error reading SMS: ", e)
        }
        result
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        val coroutineScope = rememberCoroutineScope()
        val navController = rememberNavController()
        var smsTransactions by remember { mutableStateOf<List<ParsedSmsTransaction>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var roomTransactions by remember { mutableStateOf<List<BankTransaction>>(emptyList()) }
        var showEditSheet by remember { mutableStateOf(false) }
        var editId by remember { mutableStateOf<Int?>(null) }
        var editAmount by remember { mutableStateOf("") }
        var editType by remember { mutableStateOf("") }
        var mode by remember { mutableStateOf(DateRangeMode.DAILY) }
        var currentRange by remember {
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -(mode.days - 1))
            val start = cal.timeInMillis
            mutableStateOf<Pair<Long, Long>>(start to end)
        }
        var prevRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }
        LaunchedEffect(Unit) {
            val savedStart = prefs.getLong("date_range_start", -1L)
            val savedEnd = prefs.getLong("date_range_end", -1L)
            val savedMode = prefs.getString("date_range_mode", null)
            if (savedStart > 0 && savedEnd > 0 && savedMode != null) {
                mode = DateRangeMode.valueOf(savedMode)
                currentRange = savedStart to savedEnd
            }
        }
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_db"
        ).build()
        val dao = db.bankTransactionDao()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("main") {
                    Column {
                        CustomToolbarWithDateRange(
                            currentRange = currentRange,
                            mode = mode,
                            onPrevClick = {
                                prevRange = currentRange
                                val (start, end) = currentRange
                                val newEnd = start - 1
                                val newStart = newEnd - (mode.days - 1) * 24 * 60 * 60 * 1000L
                                currentRange = newStart to newEnd
                                prefs.edit()
                                    .putLong("date_range_start", newStart)
                                    .putLong("date_range_end", newEnd)
                                    .putString("date_range_mode", mode.name)
                                    .apply()
                            },
                            onNextClick = {
                                prevRange = currentRange
                                val (start, end) = currentRange
                                val newStart = end + 1
                                val newEnd = newStart + (mode.days - 1) * 24 * 60 * 60 * 1000L
                                currentRange = newStart to newEnd
                                prefs.edit()
                                    .putLong("date_range_start", newStart)
                                    .putLong("date_range_end", newEnd)
                                    .putString("date_range_mode", mode.name)
                                    .apply()
                            },
                            onModeChange = { newMode ->
                                mode = newMode
                                val cal = Calendar.getInstance()
                                val end = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
                                val start = cal.timeInMillis
                                currentRange = start to end
                                prefs.edit()
                                    .putLong("date_range_start", start)
                                    .putLong("date_range_end", end)
                                    .putString("date_range_mode", newMode.name)
                                    .apply()
                            },
                            onDatePickerChange = { start, end ->
                                prevRange = currentRange
                                currentRange = start to end
                                prefs.edit()
                                    .putLong("date_range_start", start)
                                    .putLong("date_range_end", end)
                                    .putString("date_range_mode", mode.name)
                                    .apply()
                            },
                            onRefreshClick = {
                                if (!isLoading) {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(
                                            this@MainActivity,
                                            Manifest.permission.READ_SMS
                                        ) -> {
                                            coroutineScope.launch {
                                                isLoading = true
                                                val parsed = readAndParseSms()
                                                parsed.forEach { sms ->
                                                    val txn = BankTransaction(
                                                        amount = sms.amount,
                                                        bankName = sms.bankName,
                                                        messageTime = sms.messageTime,
                                                        tags = "",
                                                        count = null
                                                    )
                                                    try {
                                                        dao.insert(txn)
                                                    } catch (e: Exception) {
                                                        Log.e("SamStudio", "Insert error: ${e.message}")
                                                    }
                                                }
                                                val allTxns = dao.getAll()
                                                smsTransactions = allTxns.map {
                                                    ParsedSmsTransaction(
                                                        amount = it.amount,
                                                        bankName = it.bankName,
                                                        messageTime = it.messageTime,
                                                        rawMessage = it.tags
                                                    )
                                                }
                                                isLoading = false
                                                navController.navigate("smsList")
                                            }
                                        }
                                        else -> {
                                            requestSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                        }
                                    }
                                }
                            },
                            isLoading = isLoading,
                            onAddDummyClick = {
                                coroutineScope.launch {
                                    val dummyList = listOf(
                                        BankTransaction(amount = 100.0, bankName = "HDFC", tags = "Food", messageTime = System.currentTimeMillis()),
                                        BankTransaction(amount = 200.0, bankName = "ICICI", tags = "Travel", messageTime = System.currentTimeMillis()),
                                        BankTransaction(amount = 50.0, bankName = "SBI", tags = "Cigarette", messageTime = System.currentTimeMillis()),
                                        BankTransaction(amount = 80.0, bankName = "Axis", tags = "Food", messageTime = System.currentTimeMillis()),
                                        BankTransaction(amount = 120.0, bankName = "Kotak", tags = "Other", messageTime = System.currentTimeMillis())
                                    )
                                    dummyList.forEach { dao.insert(it) }
                                    roomTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                                }
                            }
                        )
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LaunchedEffect(currentRange) {
                            val (start, end) = currentRange
                            roomTransactions = withContext(Dispatchers.IO) { dao.getByDateRange(start, end) }
                        }
                        val expenseCategories = roomTransactions.groupBy { it.tags.ifBlank { "Other" } }.map { (type, txns) ->
                            val (icon, color) = categories.find { it.first == type }?.second ?: (Icons.Filled.LocalDrink to ComposeColor(0xFF0288D1))
                            ExpenseCategory(
                                name = type,
                                total = txns.sumOf { it.amount },
                                icon = icon,
                                iconColor = color,
                                subTypes = txns.map { ExpenseSubType(it.id, "Txn ${it.id}", it.amount) }
                            )
                        }
                        FinancialDataComposable(
                            expenses = expenseCategories,
                            onEditClick = { categoryName, subType ->
                                editId = subType.id
                                editAmount = subType.amount.toString()
                                editType = categoryName
                                showEditSheet = true
                            }
                        )
                    }
                }
                composable("smsList") {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        SmsTransactionsByDateScreen(smsTransactions, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
        if (showEditSheet && editId != null) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showEditSheet = false }
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Edit Transaction", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
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
                            modifier = Modifier.width(180.dp).menuAnchor().clickable { expanded = true }
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
                                        val updatedTxn = txn.copy(amount = amt, tags = editType)
                                        dao.update(updatedTxn)
                                        roomTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                                    }
                                    showEditSheet = false
                                }
                            }
                        }) { Text("Save") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomToolbarWithDateRange(
    modifier: Modifier = Modifier,
    title: String = "Toolbar",
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
    onDatePickerChange: (Long, Long) -> Unit
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val context = LocalContext.current
    // Use globalDateRangeMode directly
    val formattedRange = currentRange.let { (start, end) ->
        if (start != null && end != null) {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
        } else dateRange
    } ?: dateRange
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
                IconButton(onClick = onIcon1Click) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(onClick = onRefreshClick, enabled = !isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = onAddDummyClick) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Dummy Data")
                }
                // Number toggle with circular background
                IconButton(onClick = {
                    val newMode = when (mode) {
                        DateRangeMode.DAILY -> DateRangeMode.WEEKLY
                        DateRangeMode.WEEKLY -> DateRangeMode.MONTHLY
                        DateRangeMode.MONTHLY -> DateRangeMode.DAILY
                    }
                    // Update globalDateRangeMode and globalDateRange
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
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = ComposeColor(0xFF1976D2),
                                shape = RoundedCornerShape(50)
                            )
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onPrevClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous")
            }
            // Neumorphic effect for date range text
            val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            val lightShadow = ComposeColor.White.copy(alpha = 0.10f)
            val darkShadow = ComposeColor.Black.copy(alpha = 0.40f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(darkShadow)
                        .blur(6.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = (-2).dp, y = (-2).dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(lightShadow)
                        .blur(6.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .clickable { showDateRangePicker = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = formattedRange,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            IconButton(onClick = onNextClick) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Next")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
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
            onAddDummyClick = {}
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
    transactions: List<MainActivity.ParsedSmsTransaction>,
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
                            Text("Amount: ₹${txn.amount}")
                            Text("Bank: ${txn.bankName}")
                            Text("Message: ${txn.rawMessage}", fontSize = 12.sp, color = ComposeColor.Gray)
                        }
                    }
                }
            }
        }
    }
}
