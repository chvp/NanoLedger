package be.chvp.nanoledger.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.ui.util.Event
import be.chvp.nanoledger.ui.util.Quadruple
import java.io.IOException
import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date

val dateFormat = SimpleDateFormat("yyyy-MM-dd")

abstract class TransactionFormViewModel
    constructor(
        application: Application,
        protected val preferencesDataSource: PreferencesDataSource,
        protected val ledgerRepository: LedgerRepository,
    ) : AndroidViewModel(application) {
        private val _saving = MutableLiveData<Boolean>(false)
        val saving: LiveData<Boolean> = _saving

        protected fun setSaving(saving: Boolean) {
            _saving.value = saving
        }

        protected fun postSaving(saving: Boolean) {
            _saving.postValue(saving)
        }

        private val _date = MutableLiveData<Date>(Date())
        val date: LiveData<Date> = _date
        val formattedDate: LiveData<String> = _date.map { dateFormat.format(it) }

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
            MutableLiveData<List<Quadruple<String, String, String, String>>>(
                listOf(emptyPosting()),
            )
        val postings: LiveData<List<Quadruple<String, String, String, String>>> = _postings
        val accounts: LiveData<List<String>> = ledgerRepository.accounts.map { it.sorted() }
        val unbalancedAmount: LiveData<String> =
            postings.map {
                it
                    .map { it.third }
                    .filter { it != "" }
                    .map {
                        val cleaned =
                            it.replace(
                                Regex("[^-0-9${preferencesDataSource.getDecimalSeparator()}]"),
                                "",
                            ).replace(preferencesDataSource.getDecimalSeparator(), ".")
                        try {
                            BigDecimal(cleaned)
                        } catch (e: NumberFormatException) {
                            BigDecimal.ZERO
                        }
                    }
                    .fold(BigDecimal.ZERO) { l, r -> l + r }
                    .let { it.negate() }
                    .let {
                        if (it == BigDecimal.ZERO.setScale(it.scale())) {
                            ""
                        } else {
                            it.toString().replace(
                                ".",
                                preferencesDataSource.getDecimalSeparator(),
                            )
                        }
                    }
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
                        if (unbalancedAmount != "" &&
                            postings.dropLast(1).all {
                                it.third != "" ||
                                    (it.first == "" && it.third == "" && it.fourth != "") // its a note, allow empty amount
                            }
                        ) {
                            return@map false
                        }
                        if (
                            postings.dropLast(1).filter {
                                it.third == "" || (it.first == "" && it.third == "" && it.fourth != "") // its a note, allow empty amount
                            }.size > 1
                        ) {
                            return@map false
                        }
                        return@map true
                    }
                }
            }

        private val _latestError = MutableLiveData<Event<IOException>?>(null)
        val latestError: LiveData<Event<IOException>?> = _latestError

        protected fun postError(error: Event<IOException>) {
            _latestError.postValue(error)
        }

        private val _latestMismatch = MutableLiveData<Event<Int>?>(null)
        val latestMismatch: LiveData<Event<Int>?> = _latestMismatch

        protected fun postMismatch(mismatch: Event<Int>) {
            _latestMismatch.postValue(mismatch)
        }

        val currencyBeforeAmount: LiveData<Boolean> = preferencesDataSource.currencyBeforeAmount

        protected fun toTransactionString(): String {
            val transaction = StringBuilder()
            transaction.append(dateFormat.format(date.value!!))
            if (status.value!! != " ") {
                transaction.append(" ${status.value}")
            }
            transaction.append(" ${payee.value}")
            if (note.value!! != "") {
                transaction.append(" | ${note.value}")
            }
            transaction.append('\n')
            // Drop last element, it should always be an empty posting (and the only empty posting)
            for (posting in postings.value!!.dropLast(1)) {
                val usedLength =
                    7 +
                        posting.first.length +
                        posting.second.length +
                        posting.third.length

                val numberOfSpaces = preferencesDataSource.getPostingWidth() - usedLength
                val spaces = " ".repeat(maxOf(0, numberOfSpaces))

                if (posting.first == "") {
                    // this posting is a note
                    transaction.append("${posting.fourth}\n")
                } else if (posting.third == "") {
                    transaction.append(
                        "    ${posting.first}${posting.fourth}\n",
                    )
                } else if (preferencesDataSource.getCurrencyBeforeAmount()) {
                    transaction.append(
                        "    ${posting.first}  ${spaces}${posting.second} ${posting.third}${posting.fourth}\n",
                    )
                } else {
                    transaction.append(
                        "    ${posting.first}  ${spaces}${posting.third} ${posting.second}${posting.fourth}\n",
                    )
                }
            }
            transaction.append('\n')
            return transaction.toString()
        }

        abstract fun save(onFinish: suspend () -> Unit)

        fun setFromTransaction(transaction: Transaction) {
            setDate(transaction.date)
            setStatus(transaction.status ?: "")
            setPayee(transaction.payee)
            setNote(transaction.note ?: "")

            transaction.postings.forEachIndexed { i, posting ->
                setAccount(i, posting.account ?: "")
                setCurrency(i, posting.amount?.currency ?: "")
                setAmount(i, posting.amount?.quantity ?: "")
                setPostingNote(i, posting.note ?: "")
            }
        }

        fun setDate(dateMillis: Long) {
            _date.value = Date(dateMillis)
        }

        fun setDate(newDate: Date) {
            _date.value = newDate
        }

        fun setDate(newDate: String) {
            val parsed = dateFormat.parse(newDate, ParsePosition(0))
            if (parsed != null) {
                _date.value = parsed
            }
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
            result[index] = Quadruple(newAccount, result[index].second, result[index].third, result[index].fourth)
            val filteredResult = ArrayList<Quadruple<String, String, String, String>>()
            for (quadruple in result) {
                if (quadruple.first != "" || quadruple.third != "" || quadruple.fourth != "") {
                    filteredResult.add(quadruple)
                }
            }
            filteredResult.add(emptyPosting())
            _postings.value = filteredResult
        }

        fun setCurrency(
            index: Int,
            newCurrency: String,
        ) {
            val result = ArrayList(postings.value!!)
            result[index] = Quadruple(result[index].first, newCurrency, result[index].third, result[index].fourth)
            _postings.value = result
        }

        fun setAmount(
            index: Int,
            newAmount: String,
        ) {
            val result = ArrayList(postings.value!!)
            result[index] = Quadruple(result[index].first, result[index].second, newAmount, result[index].fourth)
            val filteredResult = ArrayList<Quadruple<String, String, String, String>>()
            for (quadruple in result) {
                if (quadruple.first != "" || quadruple.third != "" || quadruple.fourth != "") {
                    filteredResult.add(quadruple)
                }
            }
            filteredResult.add(emptyPosting())
            _postings.value = filteredResult
        }

        fun setPostingNote(
            index: Int,
            newPostingNote: String,
        ) {
            val result = ArrayList(postings.value!!)
            result[index] = Quadruple(result[index].first, result[index].second, result[index].third, newPostingNote)

            val filteredResult = ArrayList<Quadruple<String, String, String, String>>()
            for (quadruple in result) {
                if (quadruple.first != "" || quadruple.third != "" || quadruple.fourth != "") {
                    filteredResult.add(quadruple)
                }
            }
            filteredResult.add(emptyPosting())
            _postings.value = filteredResult
        }

        fun emptyPosting(): Quadruple<String, String, String, String> {
            return Quadruple("", preferencesDataSource.getDefaultCurrency(), "", "")
        }
    }
