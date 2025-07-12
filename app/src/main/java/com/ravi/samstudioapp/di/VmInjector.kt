package com.ravi.samstudioapp.di

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.ravi.samstudioapp.data.AppDatabase
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.FindExactBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.GetExistingMessageTimesUseCase
import com.ravi.samstudioapp.domain.usecase.MarkBankTransactionAsDeletedUseCase
import com.ravi.samstudioapp.presentation.main.MainViewModel
import com.ravi.samstudioapp.presentation.main.MainViewModelFactory

object VmInjector {

    private var _viewModel: MainViewModel? = null

    fun getViewModel(context: Activity, owner: ViewModelStoreOwner): MainViewModel {
        if (_viewModel == null) {
            // Create dependencies
            val db = AppDatabase.getInstance(context.applicationContext)
            val dao = db.bankTransactionDao()
            val repo = BankTransactionRepository(dao)
            val getAll = GetAllBankTransactionsUseCase(repo)
            val getByRange = GetBankTransactionsByDateRangeUseCase(repo)
            val insert = InsertBankTransactionUseCase(repo)
            val update = UpdateBankTransactionUseCase(repo)
            val findExact = FindExactBankTransactionUseCase(repo)
            val getExistingMessageTimes = GetExistingMessageTimesUseCase(repo)
            val markAsDeleted = MarkBankTransactionAsDeletedUseCase(repo)

            _viewModel = ViewModelProvider(
                owner,
                MainViewModelFactory(getAll, getByRange, insert, update, findExact, getExistingMessageTimes, markAsDeleted)
            )[MainViewModel::class.java]
        }
        return _viewModel!!
    }

    fun clear() {
        _viewModel = null // Optional: call this when Activity is destroyed to avoid memory leaks
    }
}
