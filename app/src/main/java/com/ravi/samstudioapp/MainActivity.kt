package com.ravi.samstudioapp

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ravi.samstudioapp.di.VmInjector
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.ui.LoadMainScreen

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var viewModel: MainViewModel
    private lateinit var requestSmsPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSMSPermission()
        viewModel = VmInjector.getViewModel(this@MainActivity, this)
        setContent {
            LoadMainScreen(viewModel)
        }
    }
    fun requestSMSPermission() {
        requestSmsPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) {
        }
    }
}

