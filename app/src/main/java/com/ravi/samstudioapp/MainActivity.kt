package com.ravi.samstudioapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ravi.samstudioapp.di.VmInjector
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.AppPermissionHelper
import com.ravi.samstudioapp.ui.LoadMainScreen
import com.ravi.samstudioapp.utils.PermissionManager
import com.ravi.samstudioapp.utils.SmsReceiver

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: MainViewModel
    private lateinit var permissionHelper: AppPermissionHelper
    private lateinit var smsReceiver: SmsReceiver
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
    }
    
    private fun startApp() {
        // Register SMS receiver
        registerSmsReceiver()
        
        // Set up SMS listener
        setupSmsListener()
        
        // Initialize ViewModel
        viewModel = VmInjector.getViewModel(this@MainActivity, this)
        
        // Set content
        setContent {
            LoadMainScreen(viewModel)
        }
        
        // Auto-sync when app starts (with a small delay to ensure UI is loaded)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
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
            checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            
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
    
    private fun registerSmsReceiver() {
        Log.d("MainActivity", "Registering SMS receiver programmatically")
        smsReceiver = SmsReceiver()
        
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        filter.priority = 999
        
        try {
            registerReceiver(smsReceiver, filter)
            Log.d("MainActivity", "SMS receiver registered successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to register SMS receiver", e)
        }
    }
    
    private fun setupSmsListener() {
        Log.d("MainActivity", "Setting up SMS listener")
        SmsReceiver.setOnNewSmsListener { messageBody, timestamp ->
            Log.d("MainActivity", "New SMS received via BroadcastReceiver: $messageBody")
            viewModel.handleNewSmsFromActivity(messageBody, timestamp)
        }
        Log.d("MainActivity", "SMS listener set successfully")
    }
    
    private fun unregisterSmsReceiver() {
        try {
            unregisterReceiver(smsReceiver)
            Log.d("MainActivity", "SMS receiver unregistered")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to unregister SMS receiver", e)
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
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterSmsReceiver()
    }
}

