package com.ravi.samstudioapp.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class AppPermissionHelper(private val activity: Activity) {
    
    companion object {
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        const val BATTERY_OPTIMIZATION_REQUEST_CODE = 1002
        const val SMS_PERMISSION_REQUEST_CODE = 1003
        
        fun hasAllRequiredPermissions(context: Context): Boolean {
            return hasSmsPermission(context) && 
                   hasOverlayPermission(context) && 
                   isIgnoringBatteryOptimizations(context)
        }
        
        fun hasSmsPermission(context: Context): Boolean {
            val readSmsGranted = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
            val receiveSmsGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
            return readSmsGranted && receiveSmsGranted
        }
        
        fun hasOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
        
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        }
    }
    
    private var onAllPermissionsGranted: (() -> Unit)? = null
    private var smsPermissionLauncher: ActivityResultLauncher<String>? = null
    
    fun setSmsPermissionLauncher(launcher: ActivityResultLauncher<String>) {
        smsPermissionLauncher = launcher
    }
    
    fun requestAllPermissions(onComplete: () -> Unit) {
        Log.d("AppPermissionHelper", "Starting permission request flow")
        onAllPermissionsGranted = onComplete
        
        // Check permissions in order: SMS -> Overlay -> Battery Optimization
        when {
            !hasSmsPermission(activity) -> {
                Log.d("AppPermissionHelper", "Requesting SMS permission")
                requestSmsPermission()
            }
            !hasOverlayPermission(activity) -> {
                Log.d("AppPermissionHelper", "Requesting overlay permission")
                requestOverlayPermission()
            }
            !isIgnoringBatteryOptimizations(activity) -> {
                Log.d("AppPermissionHelper", "Requesting battery optimization bypass")
                requestBatteryOptimizationBypass()
            }
            else -> {
                Log.d("AppPermissionHelper", "All permissions already granted")
                onAllPermissionsGranted?.invoke()
            }
        }
    }
    
    private fun requestSmsPermission() {
        smsPermissionLauncher?.let { launcher ->
            // Request both permissions if not granted
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_SMS)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
            }
            if (permissionsToRequest.isNotEmpty()) {
                if (permissionsToRequest.size == 1) {
                    launcher.launch(permissionsToRequest[0])
                } else {
                    // If only one permission can be requested at a time, request READ_SMS first, then RECEIVE_SMS
                    launcher.launch(Manifest.permission.READ_SMS)
                    // The next permission will be requested in handleSmsPermissionResult if needed
                }
            }
        } ?: run {
            Log.e("AppPermissionHelper", "SMS permission launcher not set")
            Toast.makeText(activity, "SMS permission launcher not configured", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            // Pre-Marshmallow, overlay permission not needed
            checkNextPermission()
        }
    }
    
    private fun requestBatteryOptimizationBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
            } catch (e: Exception) {
                Log.e("AppPermissionHelper", "Failed to request battery optimization bypass", e)
                // Continue anyway, this is not critical
                checkNextPermission()
            }
        } else {
            // Pre-Marshmallow, battery optimization not available
            checkNextPermission()
        }
    }
    
    fun handleSmsPermissionResult(isGranted: Boolean) {
        Log.d("AppPermissionHelper", "SMS permission result: $isGranted")
        if (isGranted) {
            // Check if both permissions are now granted
            if (!hasSmsPermission(activity)) {
                // If not, request the missing one
                requestSmsPermission()
            } else {
                checkNextPermission()
            }
        } else {
            Log.e("AppPermissionHelper", "SMS permission denied")
            Toast.makeText(activity, "SMS permission is required for transaction detection!", Toast.LENGTH_LONG).show()
            activity.finish()
        }
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("AppPermissionHelper", "Activity result: requestCode=$requestCode, resultCode=$resultCode")
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (hasOverlayPermission(activity)) {
                    Log.d("AppPermissionHelper", "Overlay permission granted")
                    checkNextPermission()
                } else {
                    Log.e("AppPermissionHelper", "Overlay permission denied")
                    Toast.makeText(activity, "Overlay permission is required for transaction alerts!", Toast.LENGTH_LONG).show()
                    activity.finish()
                }
            }
            BATTERY_OPTIMIZATION_REQUEST_CODE -> {
                if (isIgnoringBatteryOptimizations(activity)) {
                    Log.d("AppPermissionHelper", "Battery optimization bypassed")
                    checkNextPermission()
                } else {
                    Log.w("AppPermissionHelper", "Battery optimization not bypassed, but continuing")
                    // Don't fail here, just show a warning
                    Toast.makeText(activity, "Battery optimization bypass recommended for reliable SMS detection", Toast.LENGTH_LONG).show()
                    checkNextPermission()
                }
            }
        }
    }
    
    private fun checkNextPermission() {
        Log.d("AppPermissionHelper", "Checking next permission...")
        
        when {
            !hasSmsPermission(activity) -> {
                Log.d("AppPermissionHelper", "Still need SMS permission")
                requestSmsPermission()
            }
            !hasOverlayPermission(activity) -> {
                Log.d("AppPermissionHelper", "Still need overlay permission")
                requestOverlayPermission()
            }
            !isIgnoringBatteryOptimizations(activity) -> {
                Log.d("AppPermissionHelper", "Still need battery optimization bypass")
                requestBatteryOptimizationBypass()
            }
            else -> {
                Log.d("AppPermissionHelper", "All permissions granted! Starting app...")
                onAllPermissionsGranted?.invoke()
            }
        }
    }
    
    fun showPermissionExplanationDialog() {
        // You can implement a custom dialog here to explain why each permission is needed
        val message = """
            This app needs the following permissions:
            
            1. SMS Permission: To read transaction messages from your bank
            2. Overlay Permission: To show transaction alerts over other apps
            3. Battery Optimization Bypass: To ensure reliable SMS detection
            
            Please grant all permissions for the best experience.
        """.trimIndent()
        
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }
} 