package com.ravi.samstudioapp

import android.Manifest
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ravi.samstudioapp.di.VmInjector
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.LoadMainScreen
import com.ravi.samstudioapp.utils.PermissionManager

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: MainViewModel
    private lateinit var requestSmsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSmsPermissionLauncher()
        viewModel = VmInjector.getViewModel(this@MainActivity, this)
        setContent {
            LoadMainScreen(viewModel)
        }
    }
    
    private fun setupSmsPermissionLauncher() {
        Log.d("SamStudio", "MainActivity: Setting up SMS permission launcher")
        requestSmsPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("SamStudio", "MainActivity: SMS permission result: $isGranted")
            if (isGranted) {
                Log.d("SamStudio", "MainActivity: SMS permission granted, can now sync")
                // Optionally trigger sync automatically when permission is granted
                // viewModel.syncFromSms(this@MainActivity)
            } else {
                Log.d("SamStudio", "MainActivity: SMS permission denied")
            }
        }
        // Register with central PermissionManager
        PermissionManager.setSmsPermissionLauncher(requestSmsPermissionLauncher)
        Log.d("SamStudio", "MainActivity: SMS permission launcher registered with PermissionManager")
    }
}

