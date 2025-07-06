package com.ravi.samstudioapp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Pre-Marshmallow, no battery optimization
        }
    }
    
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                Log.d("BatteryOptimizationHelper", "Requested battery optimization bypass")
            } catch (e: Exception) {
                Log.e("BatteryOptimizationHelper", "Failed to request battery optimization bypass", e)
            }
        }
    }
    
    fun showBatteryOptimizationDialog(context: Context) {
        // You can implement a custom dialog here to explain why battery optimization bypass is needed
        Log.d("BatteryOptimizationHelper", "Showing battery optimization dialog")
    }
} 