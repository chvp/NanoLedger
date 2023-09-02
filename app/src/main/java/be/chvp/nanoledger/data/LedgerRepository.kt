package be.chvp.nanoledger.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.chvp.nanoledger.data.parser.extractTransactions
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class LedgerRepository @Inject constructor(
    private val context: Application,
    private val preferencesDataSource: PreferencesDataSource
) {
    private val _fileContents = MutableLiveData<List<String>>(emptyList())
    val fileContents: LiveData<List<String>> = _fileContents
    private val _transactions = MutableLiveData<List<Transaction>>(emptyList())
    val transactions: LiveData<List<Transaction>> = _transactions

    // TODO(chvp): Create function to append a transaction to the file

    suspend fun readFrom(fileUri: Uri?, onFinish: suspend () -> Unit) {
        val result = ArrayList<String>()
        fileUri
            ?.let { context.contentResolver.openInputStream(it) }
            ?.let { BufferedReader(InputStreamReader(it)) }
            ?.use { it.lines().forEach { result.add(it) } }
        val extracted = extractTransactions(result)
        _fileContents.postValue(result)
        _transactions.postValue(extracted)
        onFinish()
    }
}
