package com.ravi.samstudioapp.ui

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.ui.DateRangeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun MPChartSpendBarGraph(
    transactions: List<BankTransaction>,
    dateRange: Pair<Long, Long>,
    mode: DateRangeMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    
    // Capture theme colors for use in AndroidView
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    // Group transactions by day
    val txnsByDay = transactions.groupBy { transaction ->
        Instant.ofEpochMilli(transaction.messageTime).atZone(zone).toLocalDate()
    }
    
    // Generate date range
    val startDate = Instant.ofEpochMilli(dateRange.first).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(dateRange.second).atZone(zone).toLocalDate()
    
    val days = mutableListOf<LocalDate>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        days.add(currentDate)
        currentDate = currentDate.plusDays(1)
    }
    
    val amounts = days.map { day ->
        txnsByDay[day]?.sumOf { transaction -> transaction.amount } ?: 0.0
    }
    
    // Debug output
    LaunchedEffect(amounts) {
        println("MPChartSpendBarGraph days: $days")
        println("MPChartSpendBarGraph amounts: $amounts")
        println("MPChartSpendBarGraph transactions: ${transactions.size}")
        println("MPChartSpendBarGraph dateRange: $dateRange")
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Spending (${mode.days} days)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (amounts.all { it == 0.0 }) {
            Text(
                "No spending data in this range",
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            AndroidView(
                factory = { context ->
                    BarChart(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            400
                        )
                        description.isEnabled = false
                        legend.isEnabled = false
                        setDrawGridBackground(false)
                        setDrawBarShadow(false)
                        setDrawValueAboveBar(true)
                        
                        // X-axis configuration
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            setDrawAxisLine(true)
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    val index = value.toInt()
                                    return if (index in days.indices) {
                                        days[index].format(dateFormatter)
                                    } else ""
                                }
                            }
                            textSize = 10f
                            textColor = onSurfaceColor
                        }
                        
                        // Y-axis configuration
                        axisLeft.apply {
                            setDrawGridLines(true)
                            setDrawAxisLine(true)
                            textSize = 10f
                            textColor = onSurfaceColor
                        }
                        axisRight.isEnabled = false
                        
                        // Animation
                        animateY(1000)
                        animateX(1000)
                    }
                },
                update = { chart ->
                    val entries = amounts.mapIndexed { index, amount ->
                        BarEntry(index.toFloat(), amount.toFloat())
                    }
                    
                    val dataSet = BarDataSet(entries, "Spending").apply {
                        color = primaryColor
                        valueTextColor = onSurfaceColor
                        valueTextSize = 10f
                        setDrawValues(true)
                    }
                    
                    val barData = BarData(dataSet).apply {
                        barWidth = 0.6f
                    }
                    
                    chart.data = barData
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}

@Composable
fun MPChartCategoryBarChart(
    transactions: List<BankTransaction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Capture theme colors for use in AndroidView
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
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
        .groupBy { transaction -> normalizeCategory(transaction.category) }
        .mapValues { (_, txns) -> txns.sumOf { transaction -> transaction.amount } }
        .toList()
        .sortedByDescending { (_, amount) -> amount }
    
    // Debug logging
    LaunchedEffect(categoryData) {
        println("MPChartCategoryBarChart - All categories found: ${categoryData.map { (category, _) -> category }}")
        println("MPChartCategoryBarChart - Original transactions: ${transactions.map { transaction -> "${transaction.category}: ₹${transaction.amount}" }}")
        println("MPChartCategoryBarChart - Normalized transactions: ${transactions.map { transaction -> "${normalizeCategory(transaction.category)}: ₹${transaction.amount}" }}")
    }
    
    if (categoryData.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No category data available",
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
        return
    }
    
    val barColors = listOf(
        Color.rgb(239, 108, 0),   // Food - Orange
        Color.rgb(109, 76, 65),   // Cigarette - Brown
        Color.rgb(56, 142, 60),   // Travel - Green
        Color.rgb(2, 136, 209),   // Other - Blue
        Color.rgb(25, 118, 210),  // Extra - Dark Blue
        Color.rgb(142, 36, 170)   // Extra - Purple
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Spending by Category",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (60 * categoryData.size).coerceAtLeast(180)
                    )
                    description.isEnabled = false
                    legend.isEnabled = false
                    setDrawGridBackground(false)
                    setDrawBarShadow(false)
                    setDrawValueAboveBar(true)
                    
                    // X-axis configuration
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        setDrawAxisLine(true)
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val index = value.toInt()
                                return if (index in categoryData.indices) {
                                    categoryData[index].first
                                } else ""
                            }
                        }
                        textSize = 10f
                        textColor = onSurfaceColor
                        labelRotationAngle = -45f
                    }
                    
                    // Y-axis configuration
                    axisLeft.apply {
                        setDrawGridLines(true)
                        setDrawAxisLine(true)
                        textSize = 10f
                        textColor = onSurfaceColor
                    }
                    axisRight.isEnabled = false
                    
                    // Animation
                    animateY(1000)
                    animateX(1000)
                }
            },
            update = { chart ->
                val entries = categoryData.mapIndexed { index, (_, amount) ->
                    BarEntry(index.toFloat(), amount.toFloat())
                }
                
                val dataSet = BarDataSet(entries, "Categories").apply {
                    colors = barColors.take(categoryData.size)
                    valueTextColor = onSurfaceColor
                    valueTextSize = 10f
                    setDrawValues(true)
                }
                
                val barData = BarData(dataSet).apply {
                    barWidth = 0.6f
                }
                
                chart.data = barData
                chart.invalidate()
            },
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
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = androidx.compose.ui.graphics.Color(
                                        barColors[idx % barColors.size]
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
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