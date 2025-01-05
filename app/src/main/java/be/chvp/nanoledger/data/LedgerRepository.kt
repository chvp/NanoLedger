package be.chvp.nanoledger.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.chvp.nanoledger.data.parser.extractTransactions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository
    @Inject
    constructor(
        private val context: Application,
        private val preferencesDataSource: PreferencesDataSource,
    ) {
        private val _fileContents = MutableLiveData<List<String>>(emptyList())
        val fileContents: LiveData<List<String>> = _fileContents
        private val _transactions = MutableLiveData<List<Transaction>>(emptyList())
        val transactions: LiveData<List<Transaction>> = _transactions
        val accounts: LiveData<Set<String>> =
            transactions.map {
                val result = HashSet<String>()
                it.forEach { result.addAll(it.postings.map { it.account ?: "" }) }
                result
            }
        val payees: LiveData<Set<String>> = transactions.map { HashSet(it.map { it.payee }) }
        val notes: LiveData<Set<String>> =
            transactions.map {
                HashSet(
                    it.map { it.note }.filter { it != null }.map { it!! },
                )
            }

        suspend fun matches(fileUri: Uri): Boolean {
            val result = ArrayList<String>()
            fileUri
                .let { context.contentResolver.openInputStream(it) }
                ?.let { BufferedReader(InputStreamReader(it)) }
                ?.use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        result.add(line)
                        line = reader.readLine()
                    }
                }
            return result.equals(fileContents.value)
        }

        suspend fun deleteTransaction(
            fileUri: Uri,
            transaction: Transaction,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    context.contentResolver.openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                if (i >= transaction.firstLine && i <= transaction.lastLine) {
                                    return@forEachIndexed
                                }
                                // If the line after the transaction is empty, consider it a
                                // divider for the next transaction and skip it as well
                                if (i == transaction.lastLine + 1 && line == "") {
                                    return@forEachIndexed
                                }
                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

        suspend fun replaceTransaction(
            fileUri: Uri,
            transaction: Transaction,
            text: String,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    context.contentResolver.openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                // If we encounter the first line of the transaction, write out the replacement
                                if (i == transaction.firstLine) {
                                    it.write(text)
                                    return@forEachIndexed
                                }

                                // Just skip all the next lines
                                if (i > transaction.firstLine && i <= transaction.lastLine) {
                                    return@forEachIndexed
                                }

                                // If the line after the transaction is empty, consider it a
                                // divider for the next transaction and skip it as well
                                if (i == transaction.lastLine + 1 && line == "") {
                                    return@forEachIndexed
                                }
                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

        suspend fun appendTo(
            fileUri: Uri,
            text: String,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    context.contentResolver.openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEach { line ->
                                it.write("${line}\n")
                            }
                            if (!fileContents.value!!.isEmpty() && fileContents.value!!.last() != "") {
                                it.write("\n")
                            }
                            it.write(text)
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

        suspend fun readFrom(
            fileUri: Uri,
            onFinish: suspend () -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            try {
                val result = ArrayList<String>()
                fileUri
                    .let { context.contentResolver.openInputStream(it) }
                    ?.let { BufferedReader(InputStreamReader(it)) }
                    ?.use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            result.add(line)
                            line = reader.readLine()
                        }
                    }
                val extracted = extractTransactions(result)
                _fileContents.postValue(result)
                _transactions.postValue(extracted)
                onFinish()
            } catch (e: IOException) {
                onReadError(e)
            }
        }
    }
