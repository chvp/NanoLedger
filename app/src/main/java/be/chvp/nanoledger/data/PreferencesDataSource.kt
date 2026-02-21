package be.chvp.nanoledger.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val FILE_URI_KEY = "file_uri"
const val DECIMAL_SEPARATOR_KEY = "decimal_separator"
const val DEFAULT_CURRENCY_KEY = "default_currency"
const val DEFAULT_STATUS_KEY = "default_status"
const val CURRENCY_BEFORE_AMOUNT_KEY = "currency_before_amount"
const val POSTING_WIDTH_KEY = "posting_width"
const val SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY = "spacing_between_currency_and_amount"
const val TRANSACTION_STATUS_PRESENT_BY_DEFAULT_KEY = "transaction_status_present_by_default"
const val TRANSACTION_CODE_PRESENT_BY_DEFAULT_KEY = "transaction_code_present_by_default"
const val TRANSACTION_PAYEE_PRESENT_BY_DEFAULT_KEY = "transaction_payee_present_by_default"
const val TRANSACTION_NOTE_PRESENT_BY_DEFAULT_KEY = "transaction_note_present_by_default"
const val TRANSACTION_CURRENCIES_PRESENT_BY_DEFAULT_KEY = "transaction_currencies_present_by_default"
const val POSTING_AMOUNT_PRESENT_BY_DEFAULT_KEY = "posting_amount_present_by_default"
const val POSTING_COST_PRESENT_BY_DEFAULT_KEY = "posting_cost_present_by_default"
const val POSTING_ASSERTION_PRESENT_BY_DEFAULT_KEY = "posting_assertion_present_by_default"
const val POSTING_ASSERTION_COST_PRESENT_BY_DEFAULT_KEY = "posting_assertion_cost_present_by_default"
const val POSTING_COMMENT_PRESENT_BY_DEFAULT_KEY = "posting_comment_present_by_default"

class PreferencesDataSource @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(
            "be.chvp.nanoledger.preferences",
            Context.MODE_PRIVATE,
        )

    private val fileUriData = sharedPreferences.stringLiveData(FILE_URI_KEY)

    val fileUri: LiveData<Uri?> = fileUriData.map { it?.toUri() }

    fun getFileUri(): Uri? = sharedPreferences.getString(FILE_URI_KEY, null)?.toUri()

    fun setFileUri(fileUri: Uri?) =
        sharedPreferences.edit { putString(FILE_URI_KEY, fileUri?.toString()) }

    val defaultCurrency: LiveData<String> =
        sharedPreferences
            .stringLiveData(
                DEFAULT_CURRENCY_KEY,
                "€",
            ).map { it!! }

    fun getDefaultCurrency(): String = sharedPreferences.getString(DEFAULT_CURRENCY_KEY, "€")!!

    fun setDefaultCurrency(currency: String) =
        sharedPreferences.edit {
            putString(
                DEFAULT_CURRENCY_KEY,
                currency,
            )
        }

    val defaultStatus: LiveData<String> =
        sharedPreferences
            .stringLiveData(
                DEFAULT_STATUS_KEY,
                " ",
            ).map { it!! }

    fun getDefaultStatus(): String = sharedPreferences.getString(DEFAULT_STATUS_KEY, " ")!!

    fun setDefaultStatus(status: String) =
        sharedPreferences.edit {
            putString(
                DEFAULT_STATUS_KEY,
                status,
            )
        }

    val decimalSeparator: LiveData<String> =
        sharedPreferences.stringLiveData(DECIMAL_SEPARATOR_KEY, ".").map { it!! }

    fun getDecimalSeparator(): String =
        sharedPreferences.getString(DECIMAL_SEPARATOR_KEY, ".")!!

    fun setDecimalSeparator(separator: String) =
        sharedPreferences.edit { putString(DECIMAL_SEPARATOR_KEY, separator) }

    val currencyBeforeAmount: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(
            CURRENCY_BEFORE_AMOUNT_KEY,
            true,
        )

    fun getCurrencyBeforeAmount(): Boolean =
        sharedPreferences.getBoolean(
            CURRENCY_BEFORE_AMOUNT_KEY,
            true,
        )

    fun setCurrencyBeforeAmount(currencyBeforeAmount: Boolean) =
        sharedPreferences.edit {
            putBoolean(
                CURRENCY_BEFORE_AMOUNT_KEY,
                currencyBeforeAmount,
            )
        }

    val postingWidth: LiveData<Int> =
        sharedPreferences.intLiveData(POSTING_WIDTH_KEY, 72).map { it }

    fun getPostingWidth(): Int = sharedPreferences.getInt(POSTING_WIDTH_KEY, 72)

    fun setPostingWidth(width: Int) =
        sharedPreferences.edit { putInt(POSTING_WIDTH_KEY, width) }

    val spacingBetweenCurrencyAndAmount: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(
            SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY,
            true,
        )

    fun getCurrencyAmountSpacing(): Boolean =
        sharedPreferences.getBoolean(SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY, true)

    fun setCurrencyAmountSpacing(spacing: Boolean) =
        sharedPreferences.edit { putBoolean(SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY, spacing) }

    val transactionStatusPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(TRANSACTION_STATUS_PRESENT_BY_DEFAULT_KEY, true)

    fun getTransactionStatusPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(TRANSACTION_STATUS_PRESENT_BY_DEFAULT_KEY, true)

    fun setTransactionStatusPresentByDefault(status: Boolean) =
        sharedPreferences.edit { putBoolean(TRANSACTION_STATUS_PRESENT_BY_DEFAULT_KEY, status) }

    val transactionCodePresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(TRANSACTION_CODE_PRESENT_BY_DEFAULT_KEY, false)

    fun getTransactionCodePresentByDefault(): Boolean =
        sharedPreferences.getBoolean(TRANSACTION_CODE_PRESENT_BY_DEFAULT_KEY, false)

    fun setTransactionCodePresentByDefault(code: Boolean) =
        sharedPreferences.edit { putBoolean(TRANSACTION_CODE_PRESENT_BY_DEFAULT_KEY, code) }

    val transactionPayeePresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(TRANSACTION_PAYEE_PRESENT_BY_DEFAULT_KEY, true)

    fun getTransactionPayeePresentByDefault(): Boolean =
        sharedPreferences.getBoolean(TRANSACTION_PAYEE_PRESENT_BY_DEFAULT_KEY, true)

    fun setTransactionPayeePresentByDefault(payee: Boolean) =
        sharedPreferences.edit { putBoolean(TRANSACTION_PAYEE_PRESENT_BY_DEFAULT_KEY, payee) }

    val transactionNotePresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(TRANSACTION_NOTE_PRESENT_BY_DEFAULT_KEY, true)

    fun getTransactionNotePresentByDefault(): Boolean =
        sharedPreferences.getBoolean(TRANSACTION_NOTE_PRESENT_BY_DEFAULT_KEY, true)

    fun setTransactionNotePresentByDefault(note: Boolean) =
        sharedPreferences.edit { putBoolean(TRANSACTION_NOTE_PRESENT_BY_DEFAULT_KEY, note) }

    val transactionCurrenciesPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(TRANSACTION_CURRENCIES_PRESENT_BY_DEFAULT_KEY, true)

    fun getTransactionCurrenciesPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(TRANSACTION_CURRENCIES_PRESENT_BY_DEFAULT_KEY, true)

    fun setTransactionCurrenciesPresentByDefault(currencies: Boolean) =
        sharedPreferences.edit { putBoolean(TRANSACTION_CURRENCIES_PRESENT_BY_DEFAULT_KEY, currencies) }

    val postingAmountPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(POSTING_AMOUNT_PRESENT_BY_DEFAULT_KEY, true)

    fun getPostingAmountPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(POSTING_AMOUNT_PRESENT_BY_DEFAULT_KEY, true)

    fun setPostingAmountPresentByDefault(amount: Boolean) =
        sharedPreferences.edit { putBoolean(POSTING_AMOUNT_PRESENT_BY_DEFAULT_KEY, amount) }

    val postingCostPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(POSTING_COST_PRESENT_BY_DEFAULT_KEY, false)

    fun getPostingCostPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(POSTING_COST_PRESENT_BY_DEFAULT_KEY, false)

    fun setPostingCostPresentByDefault(cost: Boolean) =
        sharedPreferences.edit { putBoolean(POSTING_COST_PRESENT_BY_DEFAULT_KEY, cost) }

    val postingAssertionPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(POSTING_ASSERTION_PRESENT_BY_DEFAULT_KEY, false)

    fun getPostingAssertionPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(POSTING_ASSERTION_PRESENT_BY_DEFAULT_KEY, false)

    fun setPostingAssertionPresentByDefault(assertion: Boolean) =
        sharedPreferences.edit { putBoolean(POSTING_ASSERTION_PRESENT_BY_DEFAULT_KEY, assertion) }

    val postingAssertionCostPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(POSTING_ASSERTION_COST_PRESENT_BY_DEFAULT_KEY, false)

    fun getPostingAssertionCostPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(POSTING_ASSERTION_COST_PRESENT_BY_DEFAULT_KEY, false)

    fun setPostingAssertionCostPresentByDefault(assertionCost: Boolean) =
        sharedPreferences.edit { putBoolean(POSTING_ASSERTION_COST_PRESENT_BY_DEFAULT_KEY, assertionCost) }

    val postingCommentPresentByDefault: LiveData<Boolean> =
        sharedPreferences.booleanLiveData(POSTING_COMMENT_PRESENT_BY_DEFAULT_KEY, false)

    fun getPostingCommentPresentByDefault(): Boolean =
        sharedPreferences.getBoolean(POSTING_COMMENT_PRESENT_BY_DEFAULT_KEY, false)

    fun setPostingCommentPresentByDefault(comment: Boolean) =
        sharedPreferences.edit { putBoolean(POSTING_COMMENT_PRESENT_BY_DEFAULT_KEY, comment) }
}
