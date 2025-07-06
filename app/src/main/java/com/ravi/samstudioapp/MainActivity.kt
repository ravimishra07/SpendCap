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
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ravi.samstudioapp.di.VmInjector
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.LoadMainScreen
import com.ravi.samstudioapp.ui.OverlayPermissionHelper
import com.ravi.samstudioapp.utils.PermissionManager
import com.ravi.samstudioapp.utils.SmsReceiver

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: MainViewModel
    private lateinit var requestSmsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    private lateinit var smsReceiver: SmsReceiver

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check overlay permission first
        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            OverlayPermissionHelper.requestOverlayPermission(this)
            return // Don't proceed until permission is granted
        }
        
        setupSmsPermissionLauncher()
        registerSmsReceiver()
        setupSmsListener()
        viewModel = VmInjector.getViewModel(this@MainActivity, this)
        setContent {
            LoadMainScreen(viewModel)
        }
    }
    
    private fun setupSmsPermissionLauncher() {
        Log.d("SamStudio", "MainActivity: Setting up SMS permission launcher")
        
        // Set up callback for when permission is granted
        PermissionManager.setOnPermissionGrantedCallback {
            Log.d("SamStudio", "MainActivity: Permission granted callback triggered, starting sync")
            viewModel.syncFromSms(this@MainActivity)
        }
        
        requestSmsPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("SamStudio", "MainActivity: SMS permission result: $isGranted")
            PermissionManager.handlePermissionResult(isGranted)
        }
        
        // Register with central PermissionManager
        PermissionManager.setSmsPermissionLauncher(requestSmsPermissionLauncher)
        Log.d("SamStudio", "MainActivity: SMS permission launcher registered with PermissionManager")
    }
    
    private fun registerSmsReceiver() {
        Log.d("SamStudio", "MainActivity: Registering SMS receiver programmatically")
        smsReceiver = SmsReceiver()
        
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        filter.priority = 999
        
        try {
            registerReceiver(smsReceiver, filter)
            Log.d("SamStudio", "MainActivity: SMS receiver registered successfully")
        } catch (e: Exception) {
            Log.e("SamStudio", "MainActivity: Failed to register SMS receiver", e)
        }
    }
    
    private fun setupSmsListener() {
        Log.d("SamStudio", "MainActivity: Setting up SMS listener")
        SmsReceiver.setOnNewSmsListener { messageBody, timestamp ->
            Log.d("SamStudio", "MainActivity: New SMS received via BroadcastReceiver: $messageBody")
            // Notify ViewModel about new SMS
            viewModel.handleNewSmsFromActivity(messageBody, timestamp)
        }
        Log.d("SamStudio", "MainActivity: SMS listener set successfully")
    }
    
    private fun unregisterSmsReceiver() {
        try {
            unregisterReceiver(smsReceiver)
            Log.d("SamStudio", "MainActivity: SMS receiver unregistered")
        } catch (e: Exception) {
            Log.e("SamStudio", "MainActivity: Failed to unregister SMS receiver", e)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (OverlayPermissionHelper.hasOverlayPermission(this)) {
                // Permission granted, restart the app
                recreate()
            } else {
                // Permission denied, show message and exit
                android.widget.Toast.makeText(this, "Overlay permission is required for this app to work!", android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterSmsReceiver()
    }
    

}

