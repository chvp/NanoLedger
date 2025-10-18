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
        private val _isRefreshing = MutableLiveData(false)
        val isRefreshing: LiveData<Boolean> = _isRefreshing

        private val _searching = MutableLiveData(false)
        val searching: LiveData<Boolean> = _searching

        private val _query = MutableLiveData("")
        val query: LiveData<String> = _query

        val fileUri = preferencesDataSource.fileUri
        val transactions = ledgerRepository.transactions
        val filteredTransactions =
            transactions.switchMap { ts ->
                _query.map { query ->
                    val indexedTs = ts.mapIndexed { index, t -> index to t }
                    if (query.equals("")) {
                        indexedTs
                    } else {
                        indexedTs.filter { (i, t) -> t.contains(query) }
                    }
                }
            }

        private val _selectedIndex = MutableLiveData<Int?>(null)
        val selectedIndex: LiveData<Int?> = _selectedIndex

        private val _latestReadError = MutableLiveData<Event<IOException>?>(null)
        val latestReadError: LiveData<Event<IOException>?> = _latestReadError

        private val _latestWriteError = MutableLiveData<Event<IOException>?>(null)
        val latestWriteError: LiveData<Event<IOException>?> = _latestWriteError

        private val _latestMismatch = MutableLiveData<Event<Int>?>(null)
        val latestMismatch: LiveData<Event<Int>?> = _latestMismatch

        fun refresh() {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                _isRefreshing.value = true
                viewModelScope.launch(IO) {
                    ledgerRepository.readFrom(
                        uri,
                        {
                            _selectedIndex.postValue(null)
                            _isRefreshing.postValue(false)
                        },
                        {
                            _isRefreshing.postValue(false)
                            _latestReadError.postValue(Event(it))
                        },
                    )
                }
            }
        }

        fun toggleSelect(index: Int) {
            if (selectedIndex.value == index) {
                _selectedIndex.postValue(null)
            } else {
                _selectedIndex.postValue(index)
            }
        }

        fun deleteSelected() {
            val transaction = transactions.value!![selectedIndex.value!!]
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                _isRefreshing.value = true
                viewModelScope.launch(IO) {
                    ledgerRepository.deleteTransaction(
                        uri,
                        transaction,
                        {
                            _selectedIndex.postValue(null)
                            _isRefreshing.postValue(false)
                        },
                        {
                            _isRefreshing.postValue(false)
                            _latestMismatch.postValue(Event(1))
                        },
                        {
                            _isRefreshing.postValue(false)
                            _latestWriteError.postValue(Event(it))
                        },
                        {
                            _isRefreshing.postValue(false)
                            _latestReadError.postValue(Event(it))
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
