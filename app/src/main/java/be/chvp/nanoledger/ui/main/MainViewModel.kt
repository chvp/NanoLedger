package be.chvp.nanoledger.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val preferencesDataSource: PreferencesDataSource,
    private val ledgerRepository: LedgerRepository
) : AndroidViewModel(application) {
    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    val fileUri = preferencesDataSource.fileUri
    val transactions = ledgerRepository.transactions

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch(IO) {
            ledgerRepository.readFrom(fileUri.value) { _isRefreshing.postValue(false) }
        }
    }
}
