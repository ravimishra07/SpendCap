package com.ravi.samstudioapp.di

import android.app.Activity
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
import com.ravi.samstudioapp.domain.usecase.InsertIfNotVerifiedUseCase
import com.ravi.samstudioapp.domain.usecase.MarkBankTransactionAsDeletedUseCase
import com.ravi.samstudioapp.presentation.screens.expense.ExpenseViewModel
import com.ravi.samstudioapp.presentation.main.MainViewModelFactory
import com.ravi.samstudioapp.presentation.screens.daily.DailyViewModel
import com.ravi.samstudioapp.presentation.main.DailyViewModelFactory

object VmInjector {

    private var _viewModel: ExpenseViewModel? = null
    private var _dailyViewModel: DailyViewModel? = null

    fun getViewModel(context: Activity, owner: ViewModelStoreOwner): ExpenseViewModel {
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
            val insertIfNotVerifiedUseCase = InsertIfNotVerifiedUseCase(repo)
            val markAsDeleted = MarkBankTransactionAsDeletedUseCase(repo)


            _viewModel = ViewModelProvider(
                owner,
                MainViewModelFactory(getAll, getByRange, insert, update, findExact, getExistingMessageTimes,insertIfNotVerifiedUseCase,markAsDeleted)
            )[ExpenseViewModel::class.java]
        }
        return _viewModel!!
    }

    fun getDailyViewModel(context: Activity, owner: ViewModelStoreOwner): DailyViewModel {
        if (_dailyViewModel == null) {
            _dailyViewModel = ViewModelProvider(
                owner,
                DailyViewModelFactory()
            )[DailyViewModel::class.java]
        }
        return _dailyViewModel!!
    }

    fun clear() {
        _viewModel = null // Optional: call this when Activity is destroyed to avoid memory leaks
        _dailyViewModel = null
    }
}
