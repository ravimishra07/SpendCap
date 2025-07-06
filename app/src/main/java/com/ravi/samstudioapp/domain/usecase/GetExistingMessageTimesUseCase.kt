package com.ravi.samstudioapp.domain.usecase

import android.util.Log
import com.ravi.samstudioapp.data.BankTransactionRepository

class GetExistingMessageTimesUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(messageTimes: List<Long>): List<Long> {
        Log.d("SamStudio", "GetExistingMessageTimesUseCase: Checking ${messageTimes.size} message times")
        try {
            val existingTimes = repository.getExistingMessageTimes(messageTimes)
            Log.d("SamStudio", "GetExistingMessageTimesUseCase: Found ${existingTimes.size} existing message times")
            return existingTimes
        } catch (e: Exception) {
            Log.e("SamStudio", "GetExistingMessageTimesUseCase: Error checking existing message times", e)
            throw e
        }
    }
} 