package be.chvp.nanoledger.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.ui.util.Event
import java.io.IOException
import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

val dateFormat = SimpleDateFormat("yyyy-MM-dd")

abstract class TransactionFormViewModel(
    application: Application,
    protected val preferencesDataSource: PreferencesDataSource,
    protected val ledgerRepository: LedgerRepository,
) : AndroidViewModel(application) {
    private val _saving = MutableLiveData(false)
    val saving: LiveData<Boolean> = _saving

    protected fun setSaving(saving: Boolean) {
        _saving.value = saving
    }

    protected fun postSaving(saving: Boolean) {
        _saving.postValue(saving)
    }

    private val _date = MutableLiveData(Date())
    val date: LiveData<Date> = _date
    val formattedDate: LiveData<String> = _date.map { dateFormat.format(it) }

    private val _status = MutableLiveData(preferencesDataSource.getDefaultStatus())
    val status: LiveData<String> = _status

    private val _code = MutableLiveData("")
    val code: LiveData<String> = _code

    private val _payee = MutableLiveData("")
    val payee: LiveData<String> = _payee
    val possiblePayees: LiveData<List<String>> =
        ledgerRepository.payees.switchMap { payees ->
            payee.map { search ->
                payees.filter { it.contains(search, ignoreCase = true) }.sorted()
            }
        }

    private val _note = MutableLiveData("")
    val note: LiveData<String> = _note
    val possibleNotes: LiveData<List<String>> =
        ledgerRepository.notes.switchMap { notes ->
            note.map { search ->
                notes.filter { it.contains(search, ignoreCase = true) }.sorted()
            }
        }

    private val _postings =
        MutableLiveData(
            listOf(Posting(preferencesDataSource.getDefaultCurrency())),
        )
    val postings: LiveData<List<Posting>> = _postings
    val accounts: LiveData<List<String>> = ledgerRepository.accounts.map { it.sorted() }
    val unbalancedAmount: LiveData<String> =
        postings.map { ps ->
            ps
                .filter { p -> !p.isVirtual() && !p.isComment() }
                .mapNotNull { p -> p.amount }
                .map { p -> p.quantity }
                .map { quantity ->
                    val cleaned =
                        quantity
                            .replace(
                                Regex("[^-0-9${preferencesDataSource.getDecimalSeparator()}]"),
                                "",
                            ).replace(preferencesDataSource.getDecimalSeparator(), ".")
                    try {
                        BigDecimal(cleaned)
                    } catch (_: NumberFormatException) {
                        BigDecimal.ZERO
                    }
                }.fold(BigDecimal.ZERO) { l, r -> l + r }
                .negate()
                .let { num ->
                    if (num == BigDecimal.ZERO.setScale(num.scale())) {
                        ""
                    } else {
                        num.toString().replace(
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
                    if (postings.filter { !it.isVirtual() && !it.isComment() }.size < 2) {
                        return@map false
                    }
                    if (payee == "") {
                        return@map false
                    }
                    // If there is an unbalanced amount, and there are no postings with an empty amount, it's invalid
                    if (unbalancedAmount != "" &&
                        postings
                            .dropLast(1)
                            .filter {
                                !it.isComment()
                            }.all {
                                (it.amount?.quantity ?: "") != ""
                            }
                    ) {
                        return@map false
                    }
                    // If there are multiple postings with an empty amount, it's invalid
                    if (postings
                            .dropLast(1)
                            .filter { !it.isComment() }
                            .filter { (it.amount?.quantity ?: "") == "" }
                            .size > 1
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
        if (code.value!! != "") {
            transaction.append(" (${code.value})")
        }
        transaction.append(" ${payee.value}")
        if (note.value!! != "") {
            transaction.append(" | ${note.value}")
        }
        transaction.append('\n')
        // Drop last element, it should always be an empty posting (and the only empty posting)
        for (posting in postings.value!!.dropLast(1)) {
            val account = posting.account ?: ""
            val currency = posting.amount?.currency ?: ""
            val quantity = posting.amount?.quantity ?: ""
            val note = posting.comment ?: ""

            val spacer = if (preferencesDataSource.getCurrencyAmountSpacing()) " " else ""
            val usedLength = 6 + account.length + currency.length + quantity.length + spacer.length

            val numberOfSpaces = preferencesDataSource.getPostingWidth() - usedLength
            val spaces = " ".repeat(maxOf(0, numberOfSpaces))

            if (posting.isComment()) {
                transaction.append("${note}\n")
            } else if (quantity == "") {
                transaction.append(
                    "    ${account}${note}\n",
                )
            } else if (currency == "") {
                transaction.append(
                    "    $account  $spaces $quantity$note\n",
                )
            } else if (preferencesDataSource.getCurrencyBeforeAmount()) {
                transaction.append(
                    "    $account  $spaces$currency$spacer$quantity$note\n",
                )
            } else {
                transaction.append(
                    "    $account  $spaces$quantity$spacer$currency$note\n",
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
        setCode(transaction.code ?: "")
        setNote(transaction.note ?: "")
        setPostings(transaction.postings)
    }

    fun setDate(dateMillis: Long) {
        _date.value = Date(dateMillis - TimeZone.getDefault().getOffset(dateMillis))
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

    fun setCode(newCode: String) {
        _code.value = newCode
    }

    fun setNote(newNote: String) {
        _note.value = newNote
    }

    fun setPostings(newPostings: List<Posting>) {
        _postings.value = filterPostings(newPostings)
    }

    fun setAccount(
        index: Int,
        newAccount: String,
    ) {
        val result = ArrayList(postings.value!!)
        result[index] = Posting(newAccount, result[index].amount, null, null, null, result[index].comment)
        _postings.value = filterPostings(result)
    }

    fun setCurrency(
        index: Int,
        newCurrency: String,
    ) {
        val result = ArrayList(postings.value!!)

        val quantity = result[index].amount?.quantity ?: ""
        val original = result[index].amount?.original ?: ""
        var newAmount: Amount? = null

        if (quantity != "" || original != "" || newCurrency != "") {
            newAmount = Amount(quantity, newCurrency, original)
        }

        result[index] = Posting(result[index].account, newAmount, null, null, null, result[index].comment)
        _postings.value = filterPostings(result)
    }

    fun setAmount(
        index: Int,
        newAmountString: String,
    ) {
        val result = ArrayList(postings.value!!)
        val currency = result[index].amount?.currency ?: ""
        val original = result[index].amount?.original ?: ""
        var newAmount: Amount? = null

        if (newAmountString != "" || original != "" || currency != "") {
            newAmount = Amount(newAmountString, currency, original)
        }

        result[index] = Posting(result[index].account, newAmount, null, null, null, result[index].comment)
        _postings.value = filterPostings(result)
    }

    fun filterPostings(postings: List<Posting>): ArrayList<Posting> {
        val filteredResult = ArrayList<Posting>()
        for (posting in postings) {
            if (!posting.isEmpty()) {
                filteredResult.add(posting)
            }
        }

        filteredResult.add(Posting(preferencesDataSource.getDefaultCurrency()))
        return filteredResult
    }
}
