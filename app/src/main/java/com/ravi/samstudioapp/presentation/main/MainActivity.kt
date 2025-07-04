package com.ravi.samstudioapp

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.presentation.main.EditTransactionDialog
import com.ravi.samstudioapp.presentation.main.MainScreen
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.presentation.main.MainViewModelFactory
import com.ravi.samstudioapp.ui.CustomToolbarWithDateRange
import com.ravi.samstudioapp.ui.DateRangeMode
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.ui.SmsTransactionsByDateScreen
import com.ravi.samstudioapp.ui.categories
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import com.ravi.samstudioapp.utils.readAndParseSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.Long
import kotlin.Pair
import androidx.core.content.edit
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.ui.ExpenseCategory
import com.ravi.samstudioapp.ui.ExpenseSubType
import com.ravi.samstudioapp.ui.FinancialDataComposable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var requestSmsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission launcher
        requestSmsPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("SamStudio", "SMS permission granted by user")
            } else {
                Log.d("SamStudio", "SMS permission denied by user")
            }
        }

        // Create dependencies
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.bankTransactionDao()
        val repo = BankTransactionRepository(dao)
        val getAll = GetAllBankTransactionsUseCase(repo)
        val getByRange = GetBankTransactionsByDateRangeUseCase(repo)
        val insert = InsertBankTransactionUseCase(repo)
        val update = UpdateBankTransactionUseCase(repo)

        // Create ViewModel using factory
        val viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(getAll, getByRange, insert, update)
        )[MainViewModel::class.java]

        setContent {
            LoadMainScreen(viewModel)
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoadMainScreen(viewModel: MainViewModel) {
        prefs = getSharedPreferences("samstudio_prefs", MODE_PRIVATE)

        val context = LocalContext.current
        SamStudioAppTheme {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            var smsTransactions by remember {
                mutableStateOf<List<ParsedSmsTransaction>>(
                    emptyList()
                )
            }
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
            var editingTransaction by remember { mutableStateOf<BankTransaction?>(null) }
            var showDialog by remember { mutableStateOf(false) }


//            MainScreen(
//                viewModel = viewModel,
//                onEdit = {
//                    editingTransaction = it
//                    showDialog = true
//                },
//                onAdd = {
//                    editingTransaction = null
//                    showDialog = true
//                }
//            )

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
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp)) {
                            CustomToolbarWithDateRange(
                                currentRange = currentRange,
                                mode = mode,
                                onPrevClick = {
                                    prevRange = currentRange
                                    val (start, _) = currentRange
                                    val newEnd = start - 1
                                    val newStart = newEnd - (mode.days - 1) * 24 * 60 * 60 * 1000L
                                    currentRange = newStart to newEnd
                                    prefs.edit {
                                        putLong("date_range_start", newStart)
                                        putLong("date_range_end", newEnd)
                                        putString("date_range_mode", mode.name)
                                    }
                                },
                                onNextClick = {
                                    prevRange = currentRange
                                    val (_, end) = currentRange
                                    val newStart = end + 1
                                    val newEnd = newStart + (mode.days - 1) * 24 * 60 * 60 * 1000L
                                    currentRange = newStart to newEnd
                                    prefs.edit {
                                        putLong("date_range_start", newStart)
                                        putLong("date_range_end", newEnd)
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
                                    currentRange = start to end
                                    prefs.edit {
                                        putLong("date_range_start", start)
                                        putLong("date_range_end", end)
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
                                                    Log.d("SamStudio", "Retrieved ${allTxns.size} transactions from DB")
                                                    
                                                    smsTransactions = allTxns.map {
                                                        ParsedSmsTransaction(
                                                            amount = it.amount,
                                                            bankName = it.bankName,
                                                            messageTime = it.messageTime,
                                                            rawMessage = it.tags
                                                        )
                                                    }
                                                    Log.d("SamStudio", "Updated smsTransactions list with ${smsTransactions.size} items")
                                                    isLoading = false
                                                } catch (e: Exception) {
                                                    Log.e("SamStudio", "Error during refresh: ${e.message}", e)
                                                    isLoading = false
                                                }
                                            }
                                        } else {
                                            Log.d("SamStudio", "Requesting SMS permission...")
                                            requestSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
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
                                smsTransactions = smsTransactions
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Fetch transactions from Room and map to ExpenseCategory
                            LaunchedEffect(currentRange) {
                                val (start, end) = currentRange
                                roomTransactions = withContext(Dispatchers.IO) {
                                    dao.getByDateRange(
                                        start,
                                        end
                                    )
                                }
                            }
                            val expenseCategories =
                                roomTransactions.groupBy { it.tags.ifBlank { "Other" } }
                                    .map { (type, txns) ->
                                        val (icon, color) = categories.find { it.first == type }?.second
                                            ?: (Icons.Filled.LocalDrink to Color.Red)
                                        ExpenseCategory(
                                            name = type,
                                            total = txns.sumOf { it.amount },
                                            icon = icon,
                                            iconColor = color,
                                            subTypes = txns.map {
                                                ExpenseSubType(
                                                    it.id,
                                                    "Txn ${it.id}",
                                                    it.amount
                                                )
                                            }
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            SmsTransactionsByDateScreen(
                                smsTransactions,
                                onBack = { navController.popBackStack() })
                        }
                    }
                }
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
        }
    }
    private fun syncBanksTransactions(){
       // 1. get room database for records
       // 2. find the latest timestamp in room database
        //3. now pass read the message only newer ones. stop execution when older message start looping
    }
}