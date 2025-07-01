package com.ravi.samstudioapp

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton

// Data model for expense category and subtypes
data class ExpenseCategory(
    val name: String,
    val total: Double,
    val icon: ImageVector,
    val iconColor: Color,
    val subTypes: List<ExpenseSubType> = emptyList()
)

data class ExpenseSubType(
    val id: Int,
    val name: String,
    val amount: Double
)

@Composable
fun FinancialDataComposable(
    expenses: List<ExpenseCategory>,
    modifier: Modifier = Modifier,
    onEditClick: (categoryName: String, subType: ExpenseSubType) -> Unit = { _, _ -> }
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All") + expenses.map { it.name }
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Expenses",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
        )
        // Chip filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                androidx.compose.material3.FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }
        val filteredExpenses = if (selectedFilter == "All") expenses else expenses.filter { it.name == selectedFilter }
        filteredExpenses.forEach { category ->
            ExpandableExpenseCategory(category, onEditClick)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ExpandableExpenseCategory(category: ExpenseCategory, onEditClick: (String, ExpenseSubType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Neumorphic effect colors
    val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    val lightShadow = Color.White.copy(alpha = 0.10f)
    val darkShadow = Color.Black.copy(alpha = 0.40f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        // Neumorphic shadow layers
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(darkShadow)
                .blur(8.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-4).dp, y = (-4).dp)
                .clip(RoundedCornerShape(24.dp))
                .background(lightShadow)
                .blur(8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(category.iconColor.copy(alpha = 0.7f), category.iconColor)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.name,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    )
                    Text(
                        text = "₹${category.total}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = category.iconColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded && category.subTypes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                        .padding(start = 24.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                ) {
                    category.subTypes.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = sub.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text(text = "₹${sub.amount}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            IconButton(onClick = { onEditClick(category.name, sub) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinancialDataComposablePreview() {
    val sampleData = listOf(
        ExpenseCategory(
            name = "Food",
            total = 1200.0,
            icon = Icons.Filled.Fastfood,
            iconColor = Color(0xFFEF6C00),
            subTypes = listOf(
                ExpenseSubType(1, "Breakfast", 300.0),
                ExpenseSubType(2, "Lunch", 500.0),
                ExpenseSubType(3, "Dinner", 400.0)
            )
        ),
        ExpenseCategory(
            name = "Cigarettes",
            total = 600.0,
            icon = Icons.Filled.LocalCafe,
            iconColor = Color(0xFF6D4C41),
            subTypes = listOf(
                ExpenseSubType(4, "Classic", 400.0),
                ExpenseSubType(5, "Gold Flake", 200.0)
            )
        ),
        ExpenseCategory(
            name = "Cold Drinks",
            total = 350.0,
            icon = Icons.Filled.LocalDrink,
            iconColor = Color(0xFF0288D1),
            subTypes = listOf(
                ExpenseSubType(6, "Coke", 200.0),
                ExpenseSubType(7, "Pepsi", 150.0)
            )
        ),
        ExpenseCategory(
            name = "Travel",
            total = 900.0,
            icon = Icons.Filled.DirectionsCar,
            iconColor = Color(0xFF388E3C),
            subTypes = listOf(
                ExpenseSubType(8, "Bus", 300.0),
                ExpenseSubType(9, "Auto", 400.0),
                ExpenseSubType(10, "Cab", 200.0)
            )
        )
    )
    FinancialDataComposable(expenses = sampleData)
} 