package com.ravi.samstudioapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Data class for expense category
data class ExpenseCategory(
    val name: String,
    val total: Double,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val subTypes: List<ExpenseSubType>
)

// Data class for expense sub-type
data class ExpenseSubType(
    val id: Int,
    val name: String,
    val amount: Double
)

@Composable
fun FinancialDataComposable(
    expenses: List<ExpenseCategory>,
    onEditClick: (String, ExpenseSubType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        expenses.forEach { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Icon(
                                category.icon,
                                contentDescription = category.name,
                                tint = category.iconColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("₹${category.total}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    category.subTypes.forEach { subType ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subType.name, style = MaterialTheme.typography.bodyMedium)
                            Row {
                                Text("₹${subType.amount}", style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onEditClick(category.name, subType) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 