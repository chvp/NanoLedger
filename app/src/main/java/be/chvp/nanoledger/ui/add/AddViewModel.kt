package be.chvp.nanoledger.ui.add

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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddViewModel
    @Inject
    constructor(
        application: Application,
        private val preferencesDataSource: PreferencesDataSource,
        private val ledgerRepository: LedgerRepository,
    ) : AndroidViewModel(application) {
        private val _saving = MutableLiveData<Boolean>(false)
        val saving: LiveData<Boolean> = _saving

        private val _date = MutableLiveData<LocalDate>(LocalDate.now())
        val date: LiveData<LocalDate> = _date

        private val _status = MutableLiveData<String>(preferencesDataSource.getDefaultStatus())
        val status: LiveData<String> = _status

        private val _payee = MutableLiveData<String>("")
        val payee: LiveData<String> = _payee
        val possiblePayees: LiveData<List<String>> =
            ledgerRepository.payees.switchMap { payees ->
                payee.map { search ->
                    payees.filter { it.contains(search, ignoreCase = true) }.sorted()
                }
            }

        private val _note = MutableLiveData<String>("")
        val note: LiveData<String> = _note
        val possibleNotes: LiveData<List<String>> =
            ledgerRepository.notes.switchMap { notes ->
                note.map { search ->
                    notes.filter { it.contains(search, ignoreCase = true) }.sorted()
                }
            }

        private val _postings =
            MutableLiveData<List<Triple<String, String, String>>>(
                listOf(emptyPosting()),
            )
        val postings: LiveData<List<Triple<String, String, String>>> = _postings
        val accounts: LiveData<List<String>> = ledgerRepository.accounts.map { it.sorted() }
        val unbalancedAmount: LiveData<String> =
            postings.map {
                it
                    .map { it.third }
                    .filter { it != "" }
                    .map {
                        try {
                            BigDecimal(it)
                        } catch (e: NumberFormatException) {
                            BigDecimal.ZERO
                        }
                    }
                    .fold(BigDecimal.ZERO) { l, r -> l + r }
                    .let { it.negate() }
                    .let { if (it == BigDecimal.ZERO.setScale(it.scale())) "" else it.toString() }
            }

        val valid: LiveData<Boolean> =
            payee.switchMap { payee ->
                postings.switchMap { postings ->
                    unbalancedAmount.map { unbalancedAmount ->
                        if (postings.size < 2) {
                            return@map false
                        }
                        if (payee == "") {
                            return@map false
                        }
                        if (postings.dropLast(1).any { it.first == "" }) {
                            return@map false
                        }
                        if (unbalancedAmount != "" && postings.dropLast(1).all { it.third != "" }) {
                            return@map false
                        }
                        if (postings.dropLast(1).filter { it.third == "" }.size > 1) {
                            return@map false
                        }
                        return@map true
                    }
                }
            }

        private val _latestError = MutableLiveData<Event<IOException>?>(null)
        val latestError: LiveData<Event<IOException>?> = _latestError

        val currencyBeforeAmount: LiveData<Boolean> = preferencesDataSource.currencyBeforeAmount

        fun append(onFinish: suspend () -> Unit) {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                _saving.value = true
                viewModelScope.launch(IO) {
                    val transaction = StringBuilder()
                    transaction.append(date.value!!.format(DateTimeFormatter.ISO_DATE))
                    if (status.value!! != " ") {
                        transaction.append(" ${status.value}")
                    }
                    transaction.append(" ${payee.value}")
                    if (note.value!! != "") {
                        transaction.append(" | ${note.value}")
                    }
                    transaction.append('\n')
                    // Drop last element, it should always be an empty posting
                    for (posting in postings.value!!.dropLast(1)) {
                        if (posting.third == "") {
                            transaction.append(
                                "    ${posting.first}\n",
                            )
                        } else if (preferencesDataSource.getCurrencyBeforeAmount()) {
                            transaction.append(
                                "    ${posting.first}      ${posting.second} ${posting.third}\n",
                            )
                        } else {
                            transaction.append(
                                "    ${posting.first}      ${posting.third} ${posting.second}\n",
                            )
                        }
                    }
                    transaction.append('\n')
                    ledgerRepository.appendTo(
                        uri,
                        transaction.toString(),
                        {
                            _saving.postValue(false)
                            onFinish()
                        },
                        {
                            _saving.postValue(false)
                            _latestError.postValue(Event(it))
                        },
                        {
                            // We ignore a read error, the write went through so the
                            // only thing the user will experience is the
                            // transaction not being in the transaction
                            // overview. Which isn't optimal, but not a big problem
                            // either.
                            _saving.postValue(false)
                            onFinish()
                        },
                    )
                }
            }
        }

        fun setDate(dateMillis: Long) {
            _date.value = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.of("UTC")).toLocalDate()
        }

        fun setStatus(newStatus: String) {
            _status.value = newStatus
        }

        fun setPayee(newPayee: String) {
            _payee.value = newPayee
        }

        fun setNote(newNote: String) {
            _note.value = newNote
        }

        fun setAccount(
            index: Int,
            newAccount: String,
        ) {
            val result = ArrayList(postings.value!!)
            result[index] = Triple(newAccount, result[index].second, result[index].third)
            result.removeIf { it.first == "" && it.third == "" }
            result.add(emptyPosting())
            _postings.value = result
        }

        fun setCurrency(
            index: Int,
            newCurrency: String,
        ) {
            val result = ArrayList(postings.value!!)
            result[index] = Triple(result[index].first, newCurrency, result[index].third)
            _postings.value = result
        }

        fun setAmount(
            index: Int,
            newAmount: String,
        ) {
            val result = ArrayList(postings.value!!)
            result[index] = Triple(result[index].first, result[index].second, newAmount)
            result.removeIf { it.first == "" && it.third == "" }
            result.add(emptyPosting())
            _postings.value = result
        }

        fun emptyPosting(): Triple<String, String, String> {
            return Triple("", preferencesDataSource.getDefaultCurrency(), "")
        }
    }
