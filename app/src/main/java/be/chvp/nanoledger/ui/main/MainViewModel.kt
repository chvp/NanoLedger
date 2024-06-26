package be.chvp.nanoledger.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        application: Application,
        private val preferencesDataSource: PreferencesDataSource,
        private val ledgerRepository: LedgerRepository,
    ) : AndroidViewModel(application) {
        private val _isRefreshing = MutableLiveData<Boolean>(false)
        val isRefreshing: LiveData<Boolean> = _isRefreshing

        private val _searching = MutableLiveData<Boolean>(false)
        val searching: LiveData<Boolean> = _searching

        private val _query = MutableLiveData<String>("")
        val query: LiveData<String> = _query

        val fileUri = preferencesDataSource.fileUri
        val transactions = ledgerRepository.transactions
        val filteredTransactions =
            transactions.switchMap { ts ->
                _query.map { query ->
                    if (query.equals("")) {
                        ts
                    } else {
                        ts.filter { t -> t.contains(query) }
                    }
                }
            }

        private val _latestError = MutableLiveData<Event<IOException>?>(null)
        val latestError: LiveData<Event<IOException>?> = _latestError

        fun refresh() {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                _isRefreshing.value = true
                viewModelScope.launch(IO) {
                    ledgerRepository.readFrom(
                        uri,
                        { _isRefreshing.postValue(false) },
                        {
                            _isRefreshing.postValue(false)
                            _latestError.postValue(Event(it))
                        },
                    )
                }
            }
        }

        fun deleteTransaction(transaction: Transaction) {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                _isRefreshing.value = true
                viewModelScope.launch(IO) {
                    ledgerRepository.deleteTransaction(
                        uri,
                        transaction,
                        {},
                        { _isRefreshing.postValue(false) },
                        {
                            _isRefreshing.postValue(false)
                            _latestError.postValue(Event(it))
                        },
                        {
                            _isRefreshing.postValue(false)
                            _latestError.postValue(Event(it))
                        },
                    )
                }
            }
        }

        fun setSearching(value: Boolean) {
            _searching.value = value
        }

        fun setQuery(value: String) {
            _query.value = value
        }
    }
