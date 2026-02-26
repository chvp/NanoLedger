package be.chvp.nanoledger.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.Cost
import be.chvp.nanoledger.data.CostType
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

    private val _status = MutableLiveData(if (preferencesDataSource.getTransactionStatusPresentByDefault()) preferencesDataSource.getDefaultStatus() else null)
    val status: LiveData<String?> = _status

    private val _code = MutableLiveData(if (preferencesDataSource.getTransactionCodePresentByDefault()) "" else null)
    val code: LiveData<String?> = _code

    private val _payee = MutableLiveData(if (preferencesDataSource.getTransactionPayeePresentByDefault()) "" else null)
    val payee: LiveData<String?> = _payee
    val possiblePayees: LiveData<List<String>> =
        ledgerRepository.payees.switchMap { payees ->
            payee.map { search ->
                payees.filter { it.contains((search ?: ""), ignoreCase = true) }.sorted()
            }
        }

    private val _note = MutableLiveData(if (preferencesDataSource.getTransactionNotePresentByDefault()) "" else null)
    val note: LiveData<String?> = _note
    val possibleNotes: LiveData<List<String>> =
        ledgerRepository.notes.switchMap { notes ->
            note.map { search ->
                notes.filter { it.contains((search ?: ""), ignoreCase = true) }.sorted()
            }
        }

    private val _currencyEnabled = MutableLiveData<Boolean>(preferencesDataSource.getTransactionCurrenciesPresentByDefault())
    val currencyEnabled: LiveData<Boolean> = _currencyEnabled

    private val _postings = MutableLiveData(listOf(newPosting()))
    val postings: LiveData<List<Posting>> = _postings
    val accounts: LiveData<List<String>> = ledgerRepository.accounts.map { it.sorted() }
    val unbalancedAmount: LiveData<String> =
        postings.map { ps ->
            val relevantPostings = ps.filter { p -> !p.isVirtual() && !p.isComment() }
            if (relevantPostings.any { it.assertion != null && it.amount == null }) return@map ""
            if (relevantPostings.any { it.cost != null }) return@map ""
            if (relevantPostings.mapNotNull { p -> p.amount }.map { it.currency }.distinct().count() > 1) return@map ""

            relevantPostings
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
        postings.switchMap { postings ->
            unbalancedAmount.map { unbalancedAmount ->
                if (postings.filter { !it.isVirtual() && !it.isComment() }.size < 2) {
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
                // If there are multiple postings with an empty amount and no assertions, it's invalid
                if (postings
                        .dropLast(1)
                        .filter { !it.isComment() }
                        .filter { (it.amount?.quantity ?: "") == "" && (it.assertion?.quantity ?: "") == "" }
                        .size > 1
                ) {
                    return@map false
                }
                return@map true
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
        val postingWidth = preferencesDataSource.getPostingWidth()
        val currencyBeforeAmount = preferencesDataSource.getCurrencyBeforeAmount()
        val currencyAmountSpacing = preferencesDataSource.getCurrencyAmountSpacing()

        val transaction = Transaction(
            0,
            0,
            dateFormat.format(date.value!!),
            status.value,
            code.value,
            payee.value,
            note.value,
            postings.value!!.dropLast(1)
        )
        return transaction.format(postingWidth, currencyBeforeAmount, currencyAmountSpacing, currencyEnabled.value ?: true)
    }

    abstract fun save(onFinish: suspend () -> Unit)

    fun setFromTransaction(transaction: Transaction) {
        setDate(transaction.date)
        setStatus(transaction.status)
        if (transaction.status == null && preferencesDataSource.getTransactionStatusPresentByDefault())
            setStatus(" ")
        setCode(transaction.code)
        setPayee(transaction.payee)
        setNote(transaction.note)
        if (
            (preferencesDataSource.getTransactionPayeePresentByDefault() && !preferencesDataSource.getTransactionNotePresentByDefault() && transaction.payee == null) ||
            (preferencesDataSource.getTransactionNotePresentByDefault() && !preferencesDataSource.getTransactionPayeePresentByDefault() && transaction.note == null)
        ) {
            setPayee(transaction.note)
            setNote(transaction.payee)
        }
        setPostings(transaction.postings)
        _currencyEnabled.value = transaction.postings.map {
            (it.amount?.currency ?: "") != ""
                    || (it.cost?.amount?.currency ?: "") != ""
                    || (it.assertion?.currency ?: "") != ""
                    || (it.assertionCost?.amount?.currency ?: "") != ""
        }.reduce { acc, bool -> acc || bool }
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

    fun setStatus(newStatus: String?) {
        _status.value = newStatus
    }

    fun setCode(newCode: String?) {
        _code.value = newCode
    }

    fun setPayee(newPayee: String?) {
        _payee.value = newPayee
    }

    fun setNote(newNote: String?) {
        _note.value = newNote
    }

    fun toggleStatus() {
        _status.value = if (status.value == null) " " else null
    }

    fun toggleCode() {
        _code.value = if (code.value == null) "" else null
    }

    fun togglePayee() {
        _payee.value = if (payee.value == null) "" else null
    }

    fun toggleNote() {
        _note.value = if (note.value == null) "" else null
    }

    fun toggleCurrency() {
        _currencyEnabled.value = !currencyEnabled.value!!
    }

    fun setPostings(newPostings: List<Posting>) {
        _postings.value = filterPostings(newPostings)
    }

    fun setAccount(
        index: Int,
        newAccount: String,
    ) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withAccount(newAccount)
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

        result[index] = result[index].withAmount(newAmount)
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

        result[index] = result[index].withAmount(newAmount)
        _postings.value = filterPostings(result)
    }

    fun setCostType(
        index: Int,
        newCostType: CostType,
    ) {
        val result = ArrayList(postings.value!!)
        val quantity = result[index].cost?.amount?.quantity ?: ""
        val currency = result[index].cost?.amount?.currency ?: ""
        val newCost = Cost(Amount(quantity, currency, ""), newCostType)
        result[index] = result[index].withCost(newCost)
        _postings.value = filterPostings(result)
    }

    fun setCostCurrency(
        index: Int,
        newCostCurrency: String,
    ) {
        val result = ArrayList(postings.value!!)
        val costType = result[index].cost?.type ?: CostType.UNIT
        val quantity = result[index].cost?.amount?.quantity ?: ""
        val newCost = Cost(Amount(quantity, newCostCurrency, ""), costType)
        result[index] = result[index].withCost(newCost)
        _postings.value = filterPostings(result)
    }

    fun setCostAmount(
        index: Int,
        newCostAmount: String,
    ) {
        val result = ArrayList(postings.value!!)
        val costType = result[index].cost?.type ?: CostType.UNIT
        val currency = result[index].cost?.amount?.currency ?: ""
        val newCost = Cost(Amount(newCostAmount, currency, ""), costType)
        result[index] = result[index].withCost(newCost)
        _postings.value = filterPostings(result)
    }

    fun setAssertionCurrency(
        index: Int,
        newCurrency: String,
    ) {
        val result = ArrayList(postings.value!!)

        val quantity = result[index].assertion?.quantity ?: ""
        val newAssertion = Amount(quantity, newCurrency, "")

        result[index] = result[index].withAssertion(newAssertion)
        _postings.value = filterPostings(result)
    }

    fun setAssertionAmount(
        index: Int,
        newAmountString: String,
    ) {
        val result = ArrayList(postings.value!!)
        val currency = result[index].assertion?.currency ?: ""
        val newAmount = Amount(newAmountString, currency, "")

        result[index] = result[index].withAssertion(newAmount)
        _postings.value = filterPostings(result)
    }

    fun setAssertionCostType(
        index: Int,
        newCostType: CostType,
    ) {
        val result = ArrayList(postings.value!!)
        val quantity = result[index].assertionCost?.amount?.quantity ?: ""
        val currency = result[index].assertionCost?.amount?.currency ?: ""
        val newCost = Cost(Amount(quantity, currency, ""), newCostType)
        result[index] = result[index].withAssertionCost(newCost)
        _postings.value = filterPostings(result)
    }

    fun setAssertionCostCurrency(
        index: Int,
        newCostCurrency: String,
    ) {
        val result = ArrayList(postings.value!!)
        val costType = result[index].assertionCost?.type ?: CostType.UNIT
        val quantity = result[index].assertionCost?.amount?.quantity ?: ""
        val newCost = Cost(Amount(quantity, newCostCurrency, ""), costType)
        result[index] = result[index].withAssertionCost(newCost)
        _postings.value = filterPostings(result)
    }

    fun setAssertionCostAmount(
        index: Int,
        newCostAmount: String,
    ) {
        val result = ArrayList(postings.value!!)
        val costType = result[index].assertionCost?.type ?: CostType.UNIT
        val currency = result[index].assertionCost?.amount?.currency ?: ""
        val newCost = Cost(Amount(newCostAmount, currency, ""), costType)
        result[index] = result[index].withAssertionCost(newCost)
        _postings.value = filterPostings(result)
    }

    fun setComment(index: Int, newComment: String?) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withComment(newComment)
        _postings.value = filterPostings(result)
    }

    fun removePosting(index: Int) {
        val result = ArrayList(postings.value!!)
        result.removeAt(index)
        _postings.value = filterPostings(result)
    }

    fun toggleAccount(index: Int, on: Boolean) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withAccount(if(on) "" else null)
        if (!on) {
            result[index] = result[index].withAmount(null).withCost(null).withAssertion(null).withAssertionCost(null)
        }
        _postings.value = filterPostings(result)
    }

    fun toggleAmount(index: Int, on: Boolean) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withAmount(if(on) defaultAmount() else null)
        _postings.value = filterPostings(result)
    }

    fun toggleCost(index: Int, on: Boolean) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withCost(if(on) Cost( defaultAmount(), CostType.UNIT) else null)
        _postings.value = filterPostings(result)
    }

    fun toggleAssertion(index: Int, on: Boolean) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withAssertion(if(on) defaultAmount() else null)
        _postings.value = filterPostings(result)
    }

    fun toggleAssertionCost(index: Int, on: Boolean) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withAssertionCost(if(on) Cost( defaultAmount(), CostType.UNIT) else null)
        _postings.value = filterPostings(result)
    }

    fun toggleComment(index: Int, on: Boolean) {
        val result = ArrayList(postings.value!!)
        result[index] = result[index].withComment(if(on) "" else null)
        _postings.value = filterPostings(result)
    }

    fun filterPostings(postings: List<Posting>): ArrayList<Posting> {
        val filteredResult = ArrayList<Posting>()
        for (posting in postings) {
            filteredResult.add(posting)
        }

        if (filteredResult.isNotEmpty() && filteredResult.last() != newPosting())
            filteredResult.add(newPosting())
        return filteredResult
    }

    fun defaultAmount() = Amount("", preferencesDataSource.getDefaultCurrency(), "")

    fun newPosting(): Posting {
        return Posting(
            "",
            if (preferencesDataSource.getPostingAmountPresentByDefault()) defaultAmount() else null,
            if (preferencesDataSource.getPostingCostPresentByDefault()) Cost(defaultAmount(), CostType.UNIT) else null,
            if (preferencesDataSource.getPostingAssertionPresentByDefault()) defaultAmount() else null,
            if (preferencesDataSource.getPostingAssertionCostPresentByDefault()) Cost(defaultAmount(), CostType.UNIT) else null,
            if (preferencesDataSource.getPostingCommentPresentByDefault()) "" else null,
        )
    }

}
