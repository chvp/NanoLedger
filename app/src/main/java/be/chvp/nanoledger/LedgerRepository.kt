package be.chvp.nanoledger

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class LedgerRepository @Inject constructor(
    private val context: Application,
    private val preferencesDataSource: PreferencesDataSource
) {
    private val _fileContents = MutableLiveData<List<String>>(emptyList())
    val fileContents: LiveData<List<String>> = _fileContents

    suspend fun readFrom(fileUri: Uri, onFinish: suspend () -> Unit) {
        val inputStream = context.contentResolver.openInputStream(fileUri)
        val reader = inputStream?.let { BufferedReader(InputStreamReader(it)) }
        val result = ArrayList<String>()
        reader?.lines()?.forEach { result.add(it) }
        inputStream?.close()
        _fileContents.postValue(result)
        onFinish()
    }
}
