package be.chvp.nanoledger

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val preferencesDataSource: PreferencesDataSource
) : AndroidViewModel(application) {
    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri
    private val _fileContents = MutableLiveData<List<String>>(emptyList())
    val fileContents: LiveData<List<String>> = _fileContents

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch(IO) {
            val application: Application = getApplication()
            val inputStream = fileUri.value?.let { application.contentResolver.openInputStream(it) }
            val reader = inputStream?.let { BufferedReader(InputStreamReader(it)) }
            val result = ArrayList<String>()
            reader?.lines()?.forEach { result.add(it) }
            inputStream?.close()
            withContext(Main) {
                _fileContents.value = result
                _isRefreshing.value = false
            }
        }
    }
}
