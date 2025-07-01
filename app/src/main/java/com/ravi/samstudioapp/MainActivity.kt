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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.room.Room
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction
import androidx.compose.material3.CircularProgressIndicator

// Global variable to hold the current date range (start and end millis)
var globalDateRange by mutableStateOf<Pair<Long?, Long?>?>(null)

// Global variable for the current date range mode
enum class DateRangeMode(val days: Int) {
    DAILY(1), WEEKLY(7), MONTHLY(30)
}
var globalDateRangeMode by mutableStateOf(DateRangeMode.DAILY)

class MainActivity : ComponentActivity() {
    // Add a state to hold parsed transactions (for SMS parsing only)
    private val _transactions = mutableStateOf<List<ParsedSmsTransaction>>(emptyList())
    val transactions: List<ParsedSmsTransaction> get() = _transactions.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_db"
        ).build()
        val dao = db.bankTransactionDao()

        // Permission launcher
        val requestSmsPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    logFederalBankMessages()
                } else {
                    Toast.makeText(this, "Permission denied to read messages", Toast.LENGTH_SHORT).show()
                }
            }

        setContent {
            SamStudioAppTheme {
                val coroutineScope = rememberCoroutineScope()
                val navController = rememberNavController()
                var smsTransactions by remember { mutableStateOf<List<ParsedSmsTransaction>>(emptyList()) }
                var isLoading by remember { mutableStateOf(false) }
                var roomTransactions by remember { mutableStateOf<List<BankTransaction>>(emptyList()) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            Column {
                                CustomToolbarWithDateRange(
                                    title = "SamStudio",
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
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
                                                        // Save all as BankTransaction, ignore duplicates
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
                                // Fetch transactions from Room and map to ExpenseCategory
                                LaunchedEffect(Unit) {
                                    roomTransactions = withContext(Dispatchers.IO) { dao.getAll() }
                                }
                                val expenseCategories = roomTransactions.groupBy { it.tags.ifBlank { "Other" } }.map { (type, txns) ->
                                    val icon = when (type) {
                                        "Food" -> Icons.Filled.Fastfood
                                        "Travel" -> Icons.Filled.DirectionsCar
                                        "Cigarette" -> Icons.Filled.LocalCafe
                                        else -> Icons.Filled.LocalDrink
                                    }
                                    val color = when (type) {
                                        "Food" -> ComposeColor(0xFFEF6C00)
                                        "Travel" -> ComposeColor(0xFF388E3C)
                                        "Cigarette" -> ComposeColor(0xFF6D4C41)
                                        else -> ComposeColor(0xFF0288D1)
                                    }
                                    ExpenseCategory(
                                        name = type,
                                        total = txns.sumOf { it.amount },
                                        icon = icon,
                                        iconColor = color,
                                        subTypes = txns.map { ExpenseSubType("Txn ${it.id}", it.amount) }
                                    )
                                }
                                FinancialDataComposable(expenses = expenseCategories)
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
    onAddDummyClick: () -> Unit = {}
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val context = LocalContext.current
    var mode by remember { mutableStateOf(globalDateRangeMode) }

    // On mode change, update globalDateRange and globalDateRangeMode
    fun updateRangeForMode(newMode: DateRangeMode) {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
        val start = cal.timeInMillis
        globalDateRange = Pair(start, end)
        globalDateRangeMode = newMode
    }

    // On first composition or mode change, set initial range
    LaunchedEffect(mode) {
        updateRangeForMode(mode)
    }

    // Use globalDateRange if set, otherwise use picker state or default
    val formattedRange = globalDateRange?.let { (start, end) ->
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
                    mode = when (mode) {
                        DateRangeMode.DAILY -> DateRangeMode.WEEKLY
                        DateRangeMode.WEEKLY -> DateRangeMode.MONTHLY
                        DateRangeMode.MONTHLY -> DateRangeMode.DAILY
                    }
                    updateRangeForMode(mode)
                    Toast.makeText(context, "Range changed to ${mode.days} days", Toast.LENGTH_SHORT).show()
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
        if (showDateRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        globalDateRange = Pair(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
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
        CustomToolbarWithDateRange()
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