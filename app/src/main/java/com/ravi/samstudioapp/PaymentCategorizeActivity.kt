package com.ravi.samstudioapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.presentation.PaymentCategorizeViewModel
import com.ravi.samstudioapp.presentation.SaveState
import androidx.room.Room

class PaymentCategorizeActivity : ComponentActivity() {
    private lateinit var viewModel: PaymentCategorizeViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val amount = intent.getDoubleExtra("amount", 0.0)
        val bankName = intent.getStringExtra("bankName") ?: ""
        val messageTime = intent.getLongExtra("messageTime", 0L)
        val rawMessage = intent.getStringExtra("rawMessage") ?: ""

        // Manual DI for ViewModel and dependencies
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_db"
        ).build()
        val repo = BankTransactionRepository(db.bankTransactionDao())
        val insertUseCase = InsertBankTransactionUseCase(repo)
        viewModel = PaymentCategorizeViewModel(insertUseCase)

        setContent {
            MaterialTheme {
                val saveState by viewModel.saveState.collectAsState()
                PaymentCategorizeScreen(
                    amount = amount,
                    bankName = bankName,
                    messageTime = messageTime,
                    saveState = saveState,
                    onSave = { category, count ->
                        viewModel.saveTransaction(
                            BankTransaction(
                                amount = amount,
                                bankName = bankName,
                                tags = category,
                                messageTime = messageTime,
                                count = count
                            )
                        )
                    }
                )
                if (saveState is SaveState.Success) {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                } else if (saveState is SaveState.Error) {
                    Toast.makeText(this, (saveState as SaveState.Error).message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun PaymentCategorizeScreen(
    amount: Double,
    bankName: String,
    messageTime: Long,
    saveState: SaveState = SaveState.Idle,
    onSave: (String, Int?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("") }
    val categories = listOf("Cigarette", "Cold Drink", "Food", "Travel", "Other")
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Payment Detected", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Amount: ₹$amount")
        Text("Bank: $bankName")
        Text("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(messageTime))}")
        Spacer(modifier = Modifier.height(24.dp))
        Text("Select Category:")
        categories.forEach { cat ->
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
                Text(cat, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (selectedCategory == "Cigarette") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = count,
                onValueChange = { if (it.all { ch -> ch.isDigit() }) count = it },
                label = { Text("How many?") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (selectedCategory.isNotBlank()) {
                    onSave(selectedCategory, if (selectedCategory == "Cigarette") count.toIntOrNull() else null)
                }
            },
            enabled = selectedCategory.isNotBlank() && (selectedCategory != "Cigarette" || count.isNotBlank()) && saveState != SaveState.Loading
        ) {
            Text(if (saveState == SaveState.Loading) "Saving..." else "Save")
        }
    }
} 