package be.chvp.nanoledger.ui.preferences

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.PreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    application: Application,
    private val preferencesDataSource: PreferencesDataSource,
) : AndroidViewModel(application) {
    val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri
    val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator
    val defaultCurrency: LiveData<String> = preferencesDataSource.defaultCurrency
    val defaultStatus: LiveData<String> = preferencesDataSource.defaultStatus
    val currencyBeforeAmount: LiveData<Boolean> = preferencesDataSource.currencyBeforeAmount
    val postingWidth: LiveData<Int> = preferencesDataSource.postingWidth
    val spacingBetweenCurrencyAndAmount: LiveData<Boolean> = preferencesDataSource.spacingBetweenCurrencyAndAmount
    val transactionStatusPresentByDefault: LiveData<Boolean> = preferencesDataSource.transactionStatusPresentByDefault
    val transactionCodePresentByDefault: LiveData<Boolean> = preferencesDataSource.transactionCodePresentByDefault
    val transactionPayeePresentByDefault: LiveData<Boolean> = preferencesDataSource.transactionPayeePresentByDefault
    val transactionNotePresentByDefault: LiveData<Boolean> = preferencesDataSource.transactionNotePresentByDefault
    val transactionCurrenciesPresentByDefault: LiveData<Boolean> = preferencesDataSource.transactionCurrenciesPresentByDefault
    val transactionDefaultElements: LiveData<List<Int>> = transactionStatusPresentByDefault.switchMap { status ->
        transactionCodePresentByDefault.switchMap { code ->
            transactionPayeePresentByDefault.switchMap { payee ->
                transactionNotePresentByDefault.switchMap { note ->
                    transactionCurrenciesPresentByDefault.map { currencies ->
                        listOf(
                            if(status) R.string.status else null,
                            if(code) R.string.code else null,
                            if(payee) R.string.payee else null,
                            if(note) R.string.note else null,
                            if(currencies) R.string.currencies else null,
                        ).filter { res -> res != null }.map { res -> res!! }
                    }
                }
            }
        }

    }
    val postingAmountPresentByDefault: LiveData<Boolean> = preferencesDataSource.postingAmountPresentByDefault
    val postingCostPresentByDefault: LiveData<Boolean> = preferencesDataSource.postingCostPresentByDefault
    val postingAssertionPresentByDefault: LiveData<Boolean> = preferencesDataSource.postingAssertionPresentByDefault
    val postingAssertionCostPresentByDefault: LiveData<Boolean> = preferencesDataSource.postingAssertionCostPresentByDefault
    val postingCommentPresentByDefault: LiveData<Boolean> = preferencesDataSource.postingCommentPresentByDefault
    val postingDefaultElements: LiveData<List<Int>> = postingAmountPresentByDefault.switchMap { amount ->
        postingCostPresentByDefault.switchMap { cost ->
            postingAssertionPresentByDefault.switchMap { assertion ->
                postingAssertionCostPresentByDefault.switchMap { assertionCost ->
                    postingCommentPresentByDefault.map { comment ->
                        listOf(
                            if (amount) R.string.amount else null,
                            if (cost) R.string.cost else null,
                            if (assertion) R.string.assertion else null,
                            if (assertionCost) R.string.assertion_cost else null,
                            if (comment) R.string.comment else null,
                        ).filter { res -> res != null }.map { res -> res!! }
                    }
                }
            }
        }
    }

    fun storeFileUri(uri: Uri) = preferencesDataSource.setFileUri(uri)

    fun storeDecimalSeparator(separator: String) = preferencesDataSource.setDecimalSeparator(separator)

    fun storeDefaultCurrency(currency: String) = preferencesDataSource.setDefaultCurrency(currency)

    fun storeDefaultStatus(status: String) = preferencesDataSource.setDefaultStatus(status)

    fun storeCurrencyBeforeAmount(enable: Boolean) =
        preferencesDataSource.setCurrencyBeforeAmount(
            enable,
        )

    fun storePostingWidth(width: Int) = preferencesDataSource.setPostingWidth(width)

    fun storeCurrencyAmountSpacing(spacing: Boolean) = preferencesDataSource.setCurrencyAmountSpacing(spacing)

    fun storeTransactionStatusPresentByDefault(status: Boolean) = preferencesDataSource.setTransactionStatusPresentByDefault(status)

    fun storeTransactionCodePresentByDefault(code: Boolean) = preferencesDataSource.setTransactionCodePresentByDefault(code)

    fun storeTransactionPayeePresentByDefault(payee: Boolean) = preferencesDataSource.setTransactionPayeePresentByDefault(payee)

    fun storeTransactionNotePresentByDefault(note: Boolean) = preferencesDataSource.setTransactionNotePresentByDefault(note)

    fun storeTransactionCurrenciesPresentByDefault(currencies: Boolean) = preferencesDataSource.setTransactionCurrenciesPresentByDefault(currencies)

    fun storePostingAmountPresentByDefault(amount: Boolean) = preferencesDataSource.setPostingAmountPresentByDefault(amount)

    fun storePostingCostPresentByDefault(cost: Boolean) = preferencesDataSource.setPostingCostPresentByDefault(cost)

    fun storePostingAssertionPresentByDefault(assertion: Boolean) = preferencesDataSource.setPostingAssertionPresentByDefault(assertion)

    fun storePostingAssertionCostPresentByDefault(assertionCost: Boolean) = preferencesDataSource.setPostingAssertionCostPresentByDefault(assertionCost)

    fun storePostingCommentPresentByDefault(comment: Boolean) = preferencesDataSource.setPostingCommentPresentByDefault(comment)
}
