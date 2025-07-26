package com.ravi.samstudioapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ravi.samstudioapp.presentation.screens.daily.DailyViewModel

class DailyViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DailyViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 