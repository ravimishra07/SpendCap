package com.ravi.samstudioapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.ui.categoryDefs
import com.ravi.samstudioapp.ui.CategoryDef
import com.ravi.samstudioapp.ui.components.Black
import com.ravi.samstudioapp.ui.components.DarkGray
import com.ravi.samstudioapp.ui.components.LightGray
import com.ravi.samstudioapp.ui.components.NeumorphicBorderBox

@Composable
fun useTransactionFilter(smsTransactions: List<BankTransaction>): Triple<List<BankTransaction>, CategoryDef?, (CategoryDef?) -> Unit> {
    var selectedCategory by remember { mutableStateOf<CategoryDef?>(null) }
    val allCategory = CategoryDef(
        name = "All",
        icon = Icons.Filled.List,
        color = ComposeColor(0xFF0288D1)
    ) { true }
    val filterCategories = listOf(allCategory) + categoryDefs
    
    val filteredTxns = remember(smsTransactions, selectedCategory) {
        val nonDeleted = smsTransactions.filter { !it.deleted }
        selectedCategory?.let { cat ->
            if (cat.name == "All") nonDeleted else nonDeleted.filter { it.category.equals(cat.name, ignoreCase = true) }
        } ?: nonDeleted
    }
    
    val onCategorySelected = { category: CategoryDef? ->
        selectedCategory = if (category?.name == "All") null else category
    }
    
    return Triple(filteredTxns, selectedCategory, onCategorySelected)
}

@Composable
fun TransactionFilterChips(
    selectedCategory: CategoryDef?,
    onCategorySelected: (CategoryDef?) -> Unit
) {
    val allCategory = CategoryDef(
        name = "All",
        icon = Icons.Filled.List,
        color = ComposeColor(0xFF0288D1)
    ) { true }
    val filterCategories = listOf(allCategory) + categoryDefs
    
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
                        onCategorySelected(if (category.name == "All") null else category)
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
}

@Composable
fun TransactionSummaryRow(filteredTxns: List<BankTransaction>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val totalSpend = filteredTxns.sumOf { it.amount }
        val cigaretteCount = filteredTxns.count { it.category.equals("Cigarette", ignoreCase = true) }
        Text(
            text = "Total Spend: ₹${"%.2f".format(totalSpend)}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = "Cigarette Count: $cigaretteCount",
            color = ComposeColor(0xFF6D4C41),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
} 