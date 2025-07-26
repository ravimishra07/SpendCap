package com.ravi.samstudioapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition


// Add DateRangeMode enum at the top level
enum class DateRangeMode(val days: Int) {
    DAILY(1), WEEKLY(7), MONTHLY(30)
}

private val Black = ComposeColor(0xFF121212)
private val DarkGray = ComposeColor(0xFF080809)
private val LightGray = ComposeColor(0xFFE0E0E0)

// Define a single source of truth for categories
val categories = listOf(
    "Food" to (Icons.Filled.Fastfood to ComposeColor(0xFFEF6C00)),
    "Cigarette" to (Icons.Filled.LocalCafe to ComposeColor(0xFF6D4C41)),
    "Transport" to (Icons.Filled.DirectionsCar to ComposeColor(0xFF388E3C)),
    "Impulse" to (Icons.Filled.LocalDrink to ComposeColor(0xFF0288D1)),
    "Family" to (Icons.Filled.Message to ComposeColor(0xFF7B1FA2)),
    "Work" to (Icons.Filled.Analytics to ComposeColor(0xFF1976D2)),
    "Home" to (Icons.Filled.List to ComposeColor(0xFF6A1B9A)),
    "Health" to (Icons.Filled.Check to ComposeColor(0xFF2E7D32)),
    "Subscriptions" to (Icons.Filled.Refresh to ComposeColor(0xFF0097A7)),
    "Other" to (Icons.Filled.LocalDrink to ComposeColor(0xFF0288D1))
)

// Central source for categories
// Each category: name, icon, color, and a matcher function

data class CategoryDef(
    val name: String,
    val icon: ImageVector,
    val color: ComposeColor,
    val matcher: (BankTransaction) -> Boolean
)


val categoryDefs = listOf(
    CategoryDef(
        "Food",
        Icons.Filled.Fastfood,
        ComposeColor(0xFFA07050) // Warm brown/orange color to match screenshot
    ) { txn ->
        txn.tags.contains("food", ignoreCase = true) || txn.bankName.contains("food", ignoreCase = true)
    },
    CategoryDef(
        "Cigarette",
        Icons.Filled.LocalCafe,
        ComposeColor(0xFF6D4C41)
    ) { txn ->
        txn.tags.contains("cigarette", ignoreCase = true) ||
                txn.tags.contains("ciggret", ignoreCase = true) ||
                txn.category.equals("ciggret", ignoreCase = true)
    },
    CategoryDef(
        "Transport",
        Icons.Filled.DirectionsCar,
        ComposeColor(0xFF388E3C)
    ) { txn -> txn.tags.contains("travel", ignoreCase = true) },
    CategoryDef(
        "Impulse",
        Icons.Filled.LocalDrink,
        ComposeColor(0xFF0288D1)
    ) { txn -> txn.tags.contains("impulse", ignoreCase = true) },
    CategoryDef(
        "Family",
        Icons.Filled.Message,
        ComposeColor(0xFF7B1FA2)
    ) { txn -> txn.tags.contains("family", ignoreCase = true) },
    CategoryDef(
        "Work",
        Icons.Filled.Analytics,
        ComposeColor(0xFF1976D2)
    ) { txn -> txn.tags.contains("work", ignoreCase = true) },
    CategoryDef(
        "Home",
        Icons.Filled.List,
        ComposeColor(0xFF6A1B9A)
    ) { txn -> txn.tags.contains("home", ignoreCase = true) },
    CategoryDef(
        "Health",
        Icons.Filled.Check,
        ComposeColor(0xFF2E7D32)
    ) { txn -> txn.tags.contains("health", ignoreCase = true) },
    CategoryDef(
        "Subscriptions",
        Icons.Filled.Refresh,
        ComposeColor(0xFF0097A7)
    ) { txn -> txn.tags.contains("subscription", ignoreCase = true) },
    CategoryDef(
        "Other",
        Icons.Filled.LocalDrink,
        ComposeColor(0xFF0288D1)
    ) { txn -> true } // fallback
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeumorphicBorderBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Black,
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    borderWidth: Dp = 1.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.25f),
    shadowElevation: Dp = 8.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

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
    onInsightsClick: () -> Unit = {},
    isLoading: Boolean = false,
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    onModeChange: (DateRangeMode) -> Unit,
    onDatePickerChange: (Long, Long) -> Unit,
    smsTransactions: List<BankTransaction> = emptyList(),
    bankTransactions: List<BankTransaction> = emptyList(),
    onEdit: (BankTransaction) -> Unit = {}
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val context = LocalContext.current
    
    // Memoize formatted range to prevent recalculation
    val formattedRange = remember(currentRange) {
        val (start, end) = currentRange
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
    }

    // Memoize animation state to prevent recreation
    val rotationAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Toolbar Row
        NeumorphicBorderBox(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 8.dp,
            backgroundColor = DarkGray,
            borderColor = Color.White.copy(alpha = 0.10f),
            shadowElevation = 2.dp,
            contentPadding = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Row - Title and Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = LightGray
                    )

                    // Action Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Insights Button
                        NeumorphicBorderBox(
                            modifier = Modifier.size(40.dp),
                            cornerRadius = 4.dp,
                            backgroundColor = DarkGray,
                            borderColor = Color.White.copy(alpha = 0.10f),
                            shadowElevation = 2.dp,
                            contentPadding = 0.dp
                        ) {
                            IconButton(
                                onClick = onInsightsClick,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Filled.Analytics,
                                    contentDescription = "Insights",
                                    tint = LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Refresh Button
                        NeumorphicBorderBox(
                            modifier = Modifier.size(40.dp),
                            cornerRadius = 4.dp,
                            backgroundColor = DarkGray,
                            borderColor = Color.White.copy(alpha = 0.10f),
                            shadowElevation = 2.dp,
                            contentPadding = 0.dp
                        ) {
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = if (isLoading) "Syncing..." else "Refresh",
                                    tint = LightGray,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(
                                            rotationZ = if (isLoading) rotationAnimation else 0f
                                        )
                                )
                            }
                        }
                        
                        // Mode Toggle Button
                        NeumorphicBorderBox(
                            modifier = Modifier.size(40.dp),
                            cornerRadius = 4.dp,
                            backgroundColor = DarkGray,
                            borderColor = Color.White.copy(alpha = 0.10f),
                            shadowElevation = 2.dp,
                            contentPadding = 0.dp
                        ) {
                            IconButton(
                                onClick = {
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
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = mode.days.toString(),
                                    color = LightGray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Bottom Row - Date Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous Button
                    NeumorphicBorderBox(
                        modifier = Modifier.size(36.dp),
                        cornerRadius = 4.dp,
                        backgroundColor = Black,
                        borderColor = Color.White.copy(alpha = 0.10f),
                        shadowElevation = 2.dp,
                        contentPadding = 0.dp
                    ) {
                        IconButton(
                            onClick = onPrevClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Previous",
                                tint = LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Date Range Display
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = DarkGray
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDateRangePicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formattedRange,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = LightGray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    // Next Button
                    NeumorphicBorderBox(
                        modifier = Modifier.size(36.dp),
                        cornerRadius = 4.dp,
                        backgroundColor = Black,
                        borderColor = Color.White.copy(alpha = 0.10f),
                        shadowElevation = 2.dp,
                        contentPadding = 0.dp
                    ) {
                        IconButton(
                            onClick = onNextClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Date Range Picker Dialog
    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onDatePickerChange(start, end)
                    }
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
    transactions: List<BankTransaction>,
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
    graphHeight: Dp = 250.dp
) {
    MPChartSpendBarGraph(
        transactions = transactions,
        dateRange = dateRange,
        mode = mode,
        modifier = modifier
    )
}



// TransactionList moved to components/TransactionList.kt

// useTransactionFilter moved to components/TransactionFilter.kt

// TransactionFilterChips moved to components/TransactionFilter.kt

// TransactionSummaryRow moved to components/TransactionFilter.kt

// TransactionItem moved to components/TransactionItem.kt

// TransactionActions moved to components/TransactionItem.kt

// TransactionEditDialog moved to components/TransactionEditDialog.kt

@Composable
fun CategoryBarChart(
    transactions: List<BankTransaction>,
    modifier: Modifier = Modifier
) {
    MPChartCategoryBarChart(
        transactions = transactions,
        modifier = modifier
    )
}

@Composable
fun DetailedInsightsTab(
    transactions: List<BankTransaction>,
    dateRange: Pair<Long, Long>,
    mode: DateRangeMode,
    modifier: Modifier = Modifier
) {
    // Debug logging for the entire insights tab
    LaunchedEffect(transactions) {
        println("=== DETAILED INSIGHTS TAB DEBUG ===")
        println("Total transactions received: ${transactions.size}")
        println("All transaction categories: ${transactions.map { it.category }}")
        println("Unique categories: ${transactions.map { it.category }.distinct()}")
        println("Date range: $dateRange")
        println("Mode: $mode")
        println("==================================")
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SpendingSummary(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            CategoryBarChart(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            SpendBarGraph(
                transactions = transactions,
                dateRange = dateRange,
                mode = mode,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SpendingSummary(
    transactions: List<BankTransaction>,
    modifier: Modifier = Modifier
) {
    val totalSpent = transactions.sumOf { it.amount }
    val avgSpent = if (transactions.isNotEmpty()) totalSpent / transactions.size else 0.0
    val maxSpent = transactions.maxOfOrNull { it.amount } ?: 0.0
    val categoryCount = transactions.map { it.category }.distinct().size

    // Debug logging for spending summary
    LaunchedEffect(transactions) {
        println("=== SPENDING SUMMARY DEBUG ===")
        println("Total spent: $totalSpent")
        println("Average spent: $avgSpent")
        println("Max spent: $maxSpent")
        println("Category count: $categoryCount")
        println("All categories: ${transactions.map { it.category }.distinct()}")
        println("==============================")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Spending Summary",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "₹${String.format("%.2f", totalSpent)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "₹${String.format("%.2f", avgSpent)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Average",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "₹${String.format("%.2f", maxSpent)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Highest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = transactions.size.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = categoryCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}