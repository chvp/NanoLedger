package be.chvp.nanoledger.data

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

    // TODO(chvp): Create function to append a transaction to the file

    suspend fun readFrom(fileUri: Uri, onFinish: suspend () -> Unit) {
        val inputStream = context.contentResolver.openInputStream(fileUri)
        val reader = inputStream?.let { BufferedReader(InputStreamReader(it)) }
        val contentBuilder = StringBuilder()
        reader?.lines()?.forEach { contentBuilder.append(it); contentBuilder.append('\n') }
        // TODO(chvp): Instead of just saving the "transactions", actually do some basic parsing of the file
        val result = contentBuilder.toString().trim().split("\n\n")
        inputStream?.close()
        _fileContents.postValue(result)
        onFinish()
    }
}
