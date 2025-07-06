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
            viewModel.syncFromSms(this@MainActivity)
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
        
        // Start persistent overlay service
        startPersistentOverlayService()
        
        // Initialize ViewModel
        viewModel = VmInjector.getViewModel(this@MainActivity, this)
        
        // Set content
        setContent {
            LoadMainScreen(viewModel)
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
    
    private fun startPersistentOverlayService() {
        try {
            Log.d("MainActivity", "Starting persistent overlay service...")
            
            val serviceIntent = Intent(this, com.ravi.samstudioapp.ui.OverlayPopupService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("MainActivity", "✅ Persistent overlay service started successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Failed to start persistent overlay service", e)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionHelper.handleActivityResult(requestCode, resultCode, data)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterSmsReceiver()
    }
}

