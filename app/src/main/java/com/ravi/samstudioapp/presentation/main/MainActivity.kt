package com.ravi.samstudioapp.presentation.main

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ravi.samstudioapp.di.VmInjector
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.presentation.screens.daily.DailyScreen
import com.ravi.samstudioapp.presentation.screens.expense.ExpenseScreen
import com.ravi.samstudioapp.presentation.screens.expense.ExpenseViewModel
import com.ravi.samstudioapp.ui.AppPermissionHelper
import com.ravi.samstudioapp.ui.theme.SamStudioAppTheme
import com.ravi.samstudioapp.utils.PermissionManager

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var permissionHelper: AppPermissionHelper
    private var lastAutoSyncTime: Long = 0
    private val AUTO_SYNC_COOLDOWN = 30000L // 30 seconds cooldown between auto-syncs

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission helper
        permissionHelper = AppPermissionHelper(this)

        // Set up SMS permission launcher
        val smsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("MainActivity", "SMS permission result: $isGranted")
            permissionHelper.handleSmsPermissionResult(isGranted)
        }

        permissionHelper.setSmsPermissionLauncher(smsPermissionLauncher)

        // Register with central PermissionManager for compatibility
        PermissionManager.setSmsPermissionLauncher(smsPermissionLauncher)
        PermissionManager.setOnPermissionGrantedCallback {
            Log.d("MainActivity", "Permission granted callback triggered")
            // Auto-sync when permission is granted
            if (::viewModel.isInitialized) {
                viewModel.syncFromSms(this@MainActivity) { newTransactionCount ->
                    when (newTransactionCount) {
                        -1 -> Toast.makeText(this@MainActivity, "Auto-sync failed", Toast.LENGTH_SHORT).show()
                        0 -> Toast.makeText(this@MainActivity, "No new transactions found", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this@MainActivity, "Auto-sync completed: $newTransactionCount new transactions", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Request all permissions in sequence
        permissionHelper.requestAllPermissions {
            Log.d("MainActivity", "All permissions granted, starting app")
            startApp()
        }

        // Handle intent if app was launched from SMS receiver
        handleIncomingIntent(intent)
    }

    private fun startApp() {
        // Initialize ViewModel
        viewModel = VmInjector.getViewModel(this@MainActivity, this)

        // Set content
        setContent {
            SamStudioAppTheme {
                MainBottomNavTabs(viewModel)
            }
        }

        // Handle any pending SMS intent
        pendingSmsIntent?.let { intent ->
            Log.d("MainActivity", "🎯 Handling pending SMS intent in startApp")
            handleIncomingIntent(intent)
            pendingSmsIntent = null
        }

        // Auto-sync when app starts (with a small delay to ensure UI is loaded)
        Handler(Looper.getMainLooper()).postDelayed({
            performAutoSync()
        }, 1000) // 1 second delay
    }

    private fun performAutoSync() {
        Log.d("MainActivity", "Performing auto-sync on app start")

        val currentTime = System.currentTimeMillis()

        // Check if enough time has passed since last auto-sync
        if (currentTime - lastAutoSyncTime < AUTO_SYNC_COOLDOWN) {
            Log.d("MainActivity", "Auto-sync cooldown active, skipping auto-sync")
            return
        }

        // Check if ViewModel is initialized and not already loading
        if (!::viewModel.isInitialized) {
            Log.d("MainActivity", "ViewModel not initialized yet, skipping auto-sync")
            return
        }

        // Check if already syncing
        if (viewModel.isLoading.value) {
            Log.d("MainActivity", "Sync already in progress, skipping auto-sync")
            return
        }

        // Check if SMS permission is granted
        if (PermissionManager.isSmsPermissionGranted() ||
            checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {

            Log.d("MainActivity", "SMS permission granted, starting auto-sync")
            lastAutoSyncTime = currentTime
            Toast.makeText(this@MainActivity, "Auto-syncing SMS transactions...", Toast.LENGTH_SHORT).show()
            viewModel.syncFromSms(this@MainActivity) { newTransactionCount ->
                when (newTransactionCount) {
                    -1 -> Toast.makeText(this@MainActivity, "Auto-sync failed", Toast.LENGTH_SHORT).show()
                    0 -> Toast.makeText(this@MainActivity, "No new transactions found", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this@MainActivity, "Auto-sync completed: $newTransactionCount new transactions", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("MainActivity", "SMS permission not granted, skipping auto-sync")
        }
    }



    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart: App coming to foreground")

        // Auto-sync when app comes to foreground (but only if ViewModel is initialized)
        if (::viewModel.isInitialized) {
            performAutoSync()
        }
    }

    @Deprecated("Deprecated in favor of Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionHelper.handleActivityResult(requestCode, resultCode, data)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)

    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("NEW_SMS_DETECTED", false) == true) {
            Log.d("MainActivity", "🎯 Handling incoming SMS intent")

            val messageBody = intent.getStringExtra("MESSAGE_BODY") ?: return
            val timestamp = intent.getLongExtra("MESSAGE_TIMESTAMP", 0L)
            val bankTransaction = intent.getParcelableExtra<BankTransaction>("BANK_TRANSACTION")

            Log.d("MainActivity", "🎯 Message: ${messageBody.take(50)}...")
            Log.d("MainActivity", "🎯 Timestamp: $timestamp")
            Log.d("MainActivity", "🎯 Transaction: $bankTransaction")

            // Handle the new SMS in ViewModel
            if (::viewModel.isInitialized) {
                viewModel.handleNewSmsFromActivity(messageBody, timestamp)
                Log.d("MainActivity", "🎯 ViewModel handleNewSmsFromActivity called")
            } else {
                Log.d("MainActivity", "🎯 ViewModel not initialized yet, will handle in startApp")
                // Store the intent data to handle after ViewModel is initialized
                pendingSmsIntent = intent
            }
        }
    }

    private var pendingSmsIntent: Intent? = null

    override fun onDestroy() {
        super.onDestroy()
    }

    // Test method to verify SMS receiver is working
    private fun testSmsReceiver() {
        Log.d("MainActivity", "🧪 Testing SMS receiver...")

        // Create a test intent that simulates SMS received
        val testIntent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)

        // Send broadcast to test if receiver is working
        sendBroadcast(testIntent)
        Log.d("MainActivity", "🧪 Test SMS broadcast sent")
    }
}

sealed class BottomNavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Daily : BottomNavScreen("daily", "Daily", Icons.Default.Today)
    object Expense : BottomNavScreen("expense", "Expense", Icons.Default.AttachMoney)
    object Settings : BottomNavScreen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBottomNavTabs(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavScreen.Daily,
        BottomNavScreen.Expense,
        BottomNavScreen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = BottomNavScreen.Daily.route,
            Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Daily.route) { DailyScreen() }
            composable(BottomNavScreen.Expense.route) { ExpenseScreen(viewModel) }
            composable(BottomNavScreen.Settings.route) { SettingsTabContent() }
        }
    }
}

@Composable
fun SettingsTabContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Settings Tab Content")
    }
}