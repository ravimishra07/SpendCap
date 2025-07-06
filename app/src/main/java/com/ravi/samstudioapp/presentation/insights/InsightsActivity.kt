package com.ravi.samstudioapp.presentation.insights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import com.ravi.samstudioapp.ui.MainTabs
import com.ravi.samstudioapp.ui.MainTabContent
import com.ravi.samstudioapp.ui.categories
import com.ravi.samstudioapp.ui.DateRangeMode
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.ui.ExpenseSubType
import java.util.Calendar

class InsightsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SamStudioAppTheme {
                InsightsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Sample data for insights - in a real app, this would come from a ViewModel
    val sampleTransactions = remember {
        listOf(
            BankTransaction(
                messageTime = System.currentTimeMillis() - 6 * 24 * 60 * 60 * 1000,
                amount = 100.0,
                bankName = "HDFC",
                tags = "Food",
                count = null,
                category = "Food",
                verified = false
            ),
            BankTransaction(
                messageTime = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
                amount = 200.0,
                bankName = "ICICI",
                tags = "Travel",
                count = null,
                category = "Travel",
                verified = false
            ),
            BankTransaction(
                messageTime = System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000,
                amount = 50.0,
                bankName = "SBI",
                tags = "Cigarette",
                count = null,
                category = "Cigarette",
                verified = false
            ),
            BankTransaction(
                messageTime = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
                amount = 80.0,
                bankName = "Axis",
                tags = "Food",
                count = null,
                category = "Food",
                verified = false
            ),
            BankTransaction(
                messageTime = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                amount = 120.0,
                bankName = "Kotak",
                tags = "Other",
                count = null,
                category = "Other",
                verified = false
            )
        )
    }
    
    val currentRange = remember {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val start = cal.timeInMillis
        start to end
    }
    
    val mode = remember { DateRangeMode.WEEKLY }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Analytics") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 24.dp)
        ) {
            MainTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MainTabContent(
                selectedTabIndex = selectedTabIndex,
                roomTransactions = sampleTransactions,
                currentRange = currentRange,
                mode = mode,
                categories = categories,
                onEditClick = { categoryName, subType ->
                    // Handle edit click if needed
                }
            )
        }
    }
} 