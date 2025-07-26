package com.ravi.samstudioapp.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class BottomSheetTabsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //BottomSheetTabsScreen()
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun BottomSheetTabsScreen() {
//    val sheetState = rememberModalBottomSheetState()
//    val scope = rememberCoroutineScope()
//    var selectedTab by remember { mutableStateOf(0) }
//    val tabItems = listOf(
//        TabItem("Daily", Icons.Default.Today),
//        TabItem("Expense", Icons.Default.AttachMoney),
//        TabItem("Settings", Icons.Default.Settings)
//    )
//    ModalBottomSheet(
//        onDismissRequest = {},
//        sheetState = sheetState,
//        dragHandle = null
//    ) {
//        Column(Modifier.fillMaxWidth().padding(16.dp)) {
//            TabRow(selectedTabIndex = selectedTab) {
//                tabItems.forEachIndexed { index, item ->
//                    Tab(
//                        selected = selectedTab == index,
//                        onClick = { selectedTab = index },
//                        text = { Text(item.title) },
//                        icon = { Icon(item.icon, contentDescription = item.title) }
//                    )
//                }
//            }
//            Spacer(Modifier.height(16.dp))
//            when (selectedTab) {
//                0 -> DailyTabContent()
//                1 -> ExpenseTabContent()
//                2 -> SettingsTabContent()
//            }
//        }
//    }
//}

//data class TabItem(val title: String, val icon: ImageVector)
