package be.chvp.nanoledger.ui.add

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
class AddViewModel @Inject constructor(
    application: Application,
    private val preferencesDataSource: PreferencesDataSource,
    private val ledgerRepository: LedgerRepository
) : AndroidViewModel(application) {
    val _saving = MutableLiveData<Boolean>(false)
    val saving: LiveData<Boolean> = _saving
    val accounts = ledgerRepository.accounts

    fun append(onFinish: suspend () -> Unit) {
        val uri = preferencesDataSource.getFileUri()
        if (uri != null) {
            _saving.value = true
            viewModelScope.launch(IO) {
                ledgerRepository.appendTo(
                    uri,
                    "\n2023-09-02 * Test | Test\n  assets   € -10\n  expenses  € 10\n"
                ) {
                    _saving.postValue(false)
                    onFinish()
                }
            }
        }
    }
}
