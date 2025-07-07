package com.ravi.samstudioapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.dimensions.MutableDimensions
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.presentation.insights.InsightsActivity
import com.ravi.samstudioapp.presentation.main.EditTransactionDialog
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import com.ravi.samstudioapp.ui.components.NewMessagePopup
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.RepeatMode
import com.ravi.samstudioapp.utils.PermissionManager
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
    "Travel" to (Icons.Filled.DirectionsCar to ComposeColor(0xFF388E3C)),
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
        ComposeColor(0xFFEF6C00)
    ) { txn ->
        txn.tags.contains("food", ignoreCase = true) || txn.bankName.contains(
            "food",
            ignoreCase = true
        )
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
        "Travel",
        Icons.Filled.DirectionsCar,
        ComposeColor(0xFF388E3C)
    ) { txn -> txn.tags.contains("travel", ignoreCase = true) },
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
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

//@Preview(showBackground = true)
//@Composable
//fun PreviewSpendBarGraph() {
//    val now = System.currentTimeMillis()
//    val dummyTransactions = listOf(
//        BankTransaction(
//            1,
//            100.0,
//            "HDFC",
//            "Food",
//            now - 6 * 24 * 60 * 60 * 1000,
//            null,
//            "Food",
//            false
//        ),
//        BankTransaction(
//            2,
//            200.0,
//            "ICICI",
//            "Travel",
//            now - 5 * 24 * 60 * 60 * 1000,
//            null,
//            "Travel",
//            false
//        ),
//        BankTransaction(
//            3,
//            50.0,
//            "SBI",
//            "Cigarette",
//            now - 4 * 24 * 60 * 60 * 1000,
//            null,
//            "Cigarette",
//            false
//        ),
//        BankTransaction(
//            4,
//            80.0,
//            "Axis",
//            "Food",
//            now - 3 * 24 * 60 * 60 * 1000,
//            null,
//            "Food",
//            false
//        ),
//        BankTransaction(
//            5,
//            120.0,
//            "Kotak",
//            "Other",
//            now - 2 * 24 * 60 * 60 * 1000,
//            null,
//            "Other",
//            false
//        ),
//        BankTransaction(
//            6,
//            60.0,
//            "HDFC",
//            "Food",
//            now - 1 * 24 * 60 * 60 * 1000,
//            null,
//            "Food",
//            false
//        ),
//        BankTransaction(7, 90.0, "ICICI", "Travel", now, null, "Travel", false)
//    )
//    val dateRange = Pair(now - 6 * 24 * 60 * 60 * 1000, now)
//    SamStudioAppTheme {
//        SpendBarGraph(
//            transactions = dummyTransactions,
//            dateRange = dateRange,
//            mode = DateRangeMode.WEEKLY,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        )
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadMainScreen(viewModel: MainViewModel) {
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

    val prefs = context.getSharedPreferences(MainViewModel.CORE_NAME, Context.MODE_PRIVATE)
    viewModel.loadInitialPreferences(prefs)

    // Helper function to save preferences
    fun savePreferences() {
        val (start, end, modeName) = viewModel.getDateRangeForPreferences()
        prefs.edit {
            putLong(MainViewModel.RANGE_START, start)
            putLong(MainViewModel.RANGE_END, end)
            putString(MainViewModel.RANGE_MODE, modeName)
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
                    .statusBarsPadding()
                    //.background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
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
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            TransactionList(
                                smsTransactions = filteredSmsTransactions,
                                bankTransactions = transactions,
                                onEdit = { transaction ->
                                    viewModel.findAndOverwriteTransaction(transaction)
                                }
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
                        viewModel.findAndOverwriteTransaction(it)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionList(
    smsTransactions: List<BankTransaction>,
    bankTransactions: List<BankTransaction>,
    onEdit: (BankTransaction) -> Unit
) {
    val context = LocalContext.current
    var editingTxn by remember { mutableStateOf<BankTransaction?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editBankName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Other") }

    // Keep edit fields in sync with editingTxn
    LaunchedEffect(editingTxn) {
        editingTxn?.let {
            editAmount = it.amount.toString()
            editType = it.tags
            editBankName = it.bankName
            editCategory = it.category.ifBlank { "Other" }
        }
    }

    // Filter chips row
    var selectedCategory by remember { mutableStateOf<CategoryDef?>(null) }
    val allCategory = CategoryDef(
        name = "All",
        icon = Icons.Filled.List,
        color = ComposeColor(0xFF0288D1)
    ) { true }
    val filterCategories = listOf(allCategory) + categoryDefs
    
    // Memoize expensive operations
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }
    
    val filteredTxns = remember(smsTransactions, selectedCategory) {
        selectedCategory?.let { cat ->
            if (cat.name == "All") smsTransactions else smsTransactions.filter { it.category.equals(cat.name, ignoreCase = true) }
        } ?: smsTransactions
    }
    
    val grouped = remember(filteredTxns, dateFormat) {
        filteredTxns.groupBy { dateFormat.format(Date(it.messageTime)) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            filterCategories.forEach { category ->
                val isSelected = selectedCategory?.name == category.name || (selectedCategory == null && category.name == "All")
                val catColor = if (category.name == "All") ComposeColor(0xFF0288D1) else (category as? CategoryDef)?.color ?: ComposeColor(0xFF0288D1)
                NeumorphicBorderBox(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    cornerRadius = 4.dp,
                    backgroundColor = if (isSelected) catColor.copy(alpha = 0.18f) else Black,
                    borderColor = if (isSelected) catColor else Color.White.copy(alpha = 0.10f),
                    shadowElevation = if (isSelected) 4.dp else 2.dp,
                    contentPadding = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedCategory = if (category.name == "All") null else category
                        }
                    ) {
                        Icon(
                            category.icon, 
                            contentDescription = category.name, 
                            tint = catColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            category.name, 
                            color = if (isSelected) catColor else LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Grouped LazyColumn for smsTransactions, filtered by selected chip
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (date, txns) ->
                    items(txns) { txn ->
                        val bankTxn = remember(txn, bankTransactions) {
                            bankTransactions.find { 
                                it.amount == txn.amount && 
                                it.bankName == txn.bankName && 
                                it.messageTime == txn.messageTime 
                            }
                        }
                        
                        NeumorphicBorderBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            cornerRadius = 12.dp,
                            backgroundColor = DarkGray,
                            borderColor = Color.White.copy(alpha = 0.10f),
                            shadowElevation = 3.dp,
                            contentPadding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateTime = remember(txn.messageTime, dateTimeFormat) {
                                    dateTimeFormat.format(Date(txn.messageTime))
                                }
                                val catName = bankTxn?.category ?: "Other"
                                val catDef = remember(catName) {
                                    categoryDefs.find { it.name.equals(catName, ignoreCase = true) }
                                        ?: categoryDefs.last()
                                }
                                // Category icon in colored background (left)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(catDef.color, shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        catDef.icon,
                                        contentDescription = catDef.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                // Main info (center, expanded)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = catDef.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "₹${txn.amount}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = catDef.color
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = txn.bankName,
                                            fontSize = 11.sp,
                                            color = LightGray.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = dateTime,
                                            fontSize = 10.sp,
                                            color = LightGray.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                // Actions (right)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    bankTxn?.let { transaction ->
                                        IconButton(
                                            onClick = { editingTxn = transaction },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit Transaction",
                                                tint = LightGray.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (bankTxn?.verified == true) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Verified",
                                                tint = LightGray.copy(alpha = 0.8f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "✓",
                                                fontSize = 10.sp,
                                                color = LightGray.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                                label = { Text("Bank") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            // Simplified dropdown to avoid coroutine issues
                            var showCategoryDropdown by remember { mutableStateOf(false) }
                            
                            OutlinedTextField(
                                value = editCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { 
                                    Icon(
                                        Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Select Category",
                                        modifier = Modifier.clickable { showCategoryDropdown = !showCategoryDropdown }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategoryDropdown = !showCategoryDropdown }
                            )
                            
                            if (showCategoryDropdown) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column {
                                        categoryDefs.forEach { category ->
                                            Text(
                                                text = category.name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        editCategory = category.name
                                                        showCategoryDropdown = false
                                                    }
                                                    .padding(16.dp),
                                                color = if (editCategory == category.name) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            if (category != categoryDefs.last()) {
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            editingTxn?.let { txn ->
                                val updatedTxn = txn.copy(
                                    amount = editAmount.toDoubleOrNull() ?: txn.amount,
                                    tags = editType,
                                    bankName = editBankName,
                                    category = editCategory
                                )
                                onEdit(updatedTxn)
                            }
                            editingTxn = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingTxn = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryBarChart(
    transactions: List<BankTransaction>,
    modifier: Modifier = Modifier
) {
    // Function to normalize category names
    fun normalizeCategory(category: String): String {
        return when {
            category.equals("ciggret", ignoreCase = true) -> "Cigarette"
            category.equals("cigarette", ignoreCase = true) -> "Cigarette"
            category.equals("food", ignoreCase = true) -> "Food"
            category.equals("travel", ignoreCase = true) -> "Travel"
            else -> "Other"
        }
    }
    
    val categoryData = transactions
        .groupBy { normalizeCategory(it.category) }
        .mapValues { (_, txns) -> txns.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Debug logging
    LaunchedEffect(categoryData) {
        println("CategoryBarChart - All categories found: ${categoryData.map { it.first }}")
        println("CategoryBarChart - Original transactions: ${transactions.map { "${it.category}: ₹${it.amount}" }}")
        println("CategoryBarChart - Normalized transactions: ${transactions.map { "${normalizeCategory(it.category)}: ₹${it.amount}" }}")
    }

    if (categoryData.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No category data available",
                color = Color.Gray
            )
        }
        return
    }

    val chartEntries = categoryData.mapIndexed { index, (_, amount) ->
        entryOf(index.toFloat(), amount.toFloat())
    }
    val chartEntryModel = entryModelOf(chartEntries)
    val categoryLabels = categoryData.map { it.first }
    val barColors = listOf(
        Color(0xFFEF6C00), // Food
        Color(0xFF6D4C41), // Cigarette
        Color(0xFF388E3C), // Travel
        Color(0xFF0288D1), // Other
        Color(0xFF1976D2), // Extra
        Color(0xFF8E24AA)  // Extra
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Spending by Category",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Chart(
            chart = columnChart(),
            model = chartEntryModel,
            startAxis = startAxis(),
            bottomAxis = bottomAxis(
                valueFormatter = { value, _ ->
                    categoryLabels.getOrNull(value.toInt()) ?: ""
                },
                guideline = null
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height((60 * categoryData.size).dp.coerceAtLeast(180.dp))
        )
        // Category legend
        Column(
            modifier = Modifier.padding(top = 8.dp)
        ) {
            categoryData.forEachIndexed { idx, (category, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(barColors[idx % barColors.size], shape = RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "₹${String.format("%.2f", amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
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