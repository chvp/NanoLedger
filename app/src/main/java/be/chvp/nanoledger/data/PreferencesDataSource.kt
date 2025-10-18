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

class PreferencesDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val sharedPreferences =
            context.getSharedPreferences(
                "be.chvp.nanoledger.preferences",
                Context.MODE_PRIVATE,
            )

        private val fileUriData = sharedPreferences.stringLiveData(FILE_URI_KEY)

        val fileUri: LiveData<Uri?> = fileUriData.map { it?.toUri() }

        fun getFileUri(): Uri? = sharedPreferences.getString(FILE_URI_KEY, null)?.toUri()

        fun setFileUri(fileUri: Uri?) = sharedPreferences.edit { putString(FILE_URI_KEY, fileUri?.toString()) }

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

        val decimalSeparator: LiveData<String> = sharedPreferences.stringLiveData(DECIMAL_SEPARATOR_KEY, ".").map { it!! }

        fun getDecimalSeparator(): String = sharedPreferences.getString(DECIMAL_SEPARATOR_KEY, ".")!!

        fun setDecimalSeparator(separator: String) = sharedPreferences.edit { putString(DECIMAL_SEPARATOR_KEY, separator) }

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

        val postingWidth: LiveData<Int> = sharedPreferences.intLiveData(POSTING_WIDTH_KEY, 72).map { it }

        fun getPostingWidth(): Int = sharedPreferences.getInt(POSTING_WIDTH_KEY, 72)

        fun setPostingWidth(width: Int) = sharedPreferences.edit { putInt(POSTING_WIDTH_KEY, width) }

        val spacingBetweenCurrencyAndAmount: LiveData<Boolean> =
            sharedPreferences.booleanLiveData(
                SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY,
                true,
            )

        fun getCurrencyAmountSpacing(): Boolean = sharedPreferences.getBoolean(SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY, true)

        fun setCurrencyAmountSpacing(spacing: Boolean) =
            sharedPreferences.edit { putBoolean(SPACING_BETWEEN_CURRENCY_AND_AMOUNT_KEY, spacing) }
    }
