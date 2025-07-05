package com.ravi.samstudioapp.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import kotlin.collections.component1
import kotlin.collections.component2

private val Black = androidx.compose.ui.graphics.Color(0xFF121212)
private val DarkGray = androidx.compose.ui.graphics.Color(0xFF232323)
private val LightGray = androidx.compose.ui.graphics.Color(0xFFE0E0E0)

@Composable
fun MainTabIndicator(tabPositions: List<TabPosition>, selectedTabIndex: Int) {
    TabRowDefaults.Indicator(
        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
        height = 2.dp,
        color = LightGray
    )
}

@Composable
fun MainTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Transactions", "Insights")
    TabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = { tabPositions ->
            MainTabIndicator(tabPositions, selectedTabIndex)
        },
        containerColor = Black,
        contentColor = LightGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(48.dp),
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(40.dp),
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedTabIndex == index)
                            LightGray
                        else
                            LightGray.copy(alpha = 0.5f)
                    )
                }
            )
        }
    }
}

@Composable
fun ToolbarWithDateRange(
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    prevRange: Pair<Long, Long>?,
    prefs: SharedPreferences,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onModeChange: (DateRangeMode) -> Unit,
    onDatePickerChange: (Long, Long) -> Unit,
    onRefreshClick: () -> Unit,
    onInsightsClick: () -> Unit,
    isLoading: Boolean,
    onAddDummyClick: () -> Unit,
    smsTransactions: List<ParsedSmsTransaction>,
    bankTransactions: List<BankTransaction>,
    onEdit: (BankTransaction) -> Unit
) {
    CustomToolbarWithDateRange(
        currentRange = currentRange,
        mode = mode,
        onPrevClick = onPrevClick,
        onNextClick = onNextClick,
        onModeChange = onModeChange,
        onDatePickerChange = onDatePickerChange,
        onRefreshClick = onRefreshClick,
        onInsightsClick = onInsightsClick,
        isLoading = isLoading,
        onAddDummyClick = onAddDummyClick,
        smsTransactions = smsTransactions,
        bankTransactions = bankTransactions,
        onEdit = onEdit
    )
}

@Composable
fun MainTabContent(
    selectedTabIndex: Int,
    roomTransactions: List<BankTransaction>,
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode,
    categories: List<Pair<String, Pair<Any, Any>>>,
    onEditClick: (String, ExpenseSubType) -> Unit
) {
    when (selectedTabIndex) {
        0 -> TransactionTabContent(roomTransactions, currentRange, categories, onEditClick)
        1 -> InsightsTabContent(roomTransactions, currentRange, mode)
    }
}

@Composable
fun TransactionTabContent(
    roomTransactions: List<BankTransaction>,
    currentRange: Pair<Long, Long>,
    categories: List<Pair<String, Pair<Any, Any>>>,
    onEditClick: (String, ExpenseSubType) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    val expenseCategories =
        roomTransactions.groupBy { it.tags.ifBlank { "Other" } }
            .map { (type, txns) ->
                val (icon, color) = categories.find { it.first == type }?.second
                    ?: (Icons.Filled.LocalDrink to Color.Red)
                ExpenseCategory(
                    name = type,
                    total = txns.sumOf { it.amount },
                    icon = icon as androidx.compose.ui.graphics.vector.ImageVector,
                    iconColor = color as Color,
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
        onEditClick = onEditClick
    )
}

@Composable
fun InsightsTabContent(
    roomTransactions: List<BankTransaction>,
    currentRange: Pair<Long, Long>,
    mode: DateRangeMode
) {
    Spacer(modifier = Modifier.height(8.dp))
    SpendBarGraph(
        transactions = roomTransactions,
        dateRange = currentRange,
        mode = mode,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
    )
}