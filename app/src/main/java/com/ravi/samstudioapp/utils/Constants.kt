package com.ravi.samstudioapp.utils

import androidx.activity.result.ActivityResultLauncher
import android.Manifest
import android.util.Log

object Constants {
    const val DB_NAME ="SAM_DB"
}
object SharedPreference {
    const val  CORE_NAME ="samstudio_prefs"
    const val  RANGE_START ="date_range_start"
    const val  RANGE_END ="date_range_end"
    const val  RANGE_MODE ="date_range_mode"
}

object PermissionManager {
    private var smsPermissionLauncher: ActivityResultLauncher<String>? = null
    private var smsPermissionGranted: Boolean = false
    private var onPermissionGrantedCallback: (() -> Unit)? = null
    
    fun setSmsPermissionLauncher(launcher: ActivityResultLauncher<String>) {
        Log.d("SamStudio", "PermissionManager: Setting SMS permission launcher")
        smsPermissionLauncher = launcher
    }
    
    fun getSmsPermissionLauncher(): ActivityResultLauncher<String>? {
        return smsPermissionLauncher
    }
    
    fun isSmsPermissionGranted(): Boolean {
        return smsPermissionGranted
    }
    
    fun setOnPermissionGrantedCallback(callback: () -> Unit) {
        Log.d("SamStudio", "PermissionManager: Setting permission granted callback")
        onPermissionGrantedCallback = callback
    }
    
    fun handlePermissionResult(isGranted: Boolean) {
        Log.d("SamStudio", "PermissionManager: Handling permission result: $isGranted")
        smsPermissionGranted = isGranted
        if (isGranted) {
            Log.d("SamStudio", "PermissionManager: Permission granted, executing callback")
            onPermissionGrantedCallback?.invoke()
        }
    }
    
    fun requestSmsPermission() {
        Log.d("SamStudio", "PermissionManager: Requesting SMS permission")
        if (smsPermissionLauncher != null) {
            Log.d("SamStudio", "PermissionManager: Launching permission request")
            smsPermissionLauncher?.launch(Manifest.permission.READ_SMS)
        } else {
            Log.e("SamStudio", "PermissionManager: SMS permission launcher is null!")
        }
    }
}
