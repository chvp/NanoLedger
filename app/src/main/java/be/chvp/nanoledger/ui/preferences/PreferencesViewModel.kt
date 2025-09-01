package be.chvp.nanoledger.ui.preferences

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import be.chvp.nanoledger.data.PreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel
    @Inject
    constructor(
        application: Application,
        private val preferencesDataSource: PreferencesDataSource,
    ) : AndroidViewModel(application) {
        val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri
        val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator
        val defaultCurrency: LiveData<String> = preferencesDataSource.defaultCurrency
        val defaultStatus: LiveData<String> = preferencesDataSource.defaultStatus
        val currencyBeforeAmount: LiveData<Boolean> = preferencesDataSource.currencyBeforeAmount
        val detailedPostings: LiveData<Boolean> = preferencesDataSource.detailedPostings
        val postingWidth: LiveData<Int> = preferencesDataSource.postingWidth

        fun storeFileUri(uri: Uri) = preferencesDataSource.setFileUri(uri)

        fun storeDecimalSeparator(separator: String) = preferencesDataSource.setDecimalSeparator(separator)

        fun storeDefaultCurrency(currency: String) = preferencesDataSource.setDefaultCurrency(currency)

        fun storeDefaultStatus(status: String) = preferencesDataSource.setDefaultStatus(status)

        fun storeCurrencyBeforeAmount(enable: Boolean) =
            preferencesDataSource.setCurrencyBeforeAmount(
                enable,
            )

        fun storeDetailedPostings(enable: Boolean) =
            preferencesDataSource.setDetailedPostings(
                enable,
            )

        fun storePostingWidth(width: Int) = preferencesDataSource.setPostingWidth(width)
    }
