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
        val priceFileUri: LiveData<Uri?> = preferencesDataSource.priceFileUri
        val defaultCurrency: LiveData<String> = preferencesDataSource.defaultCurrency
        val defaultStatus: LiveData<String> = preferencesDataSource.defaultStatus
        val currencyBeforeAmount: LiveData<Boolean> = preferencesDataSource.currencyBeforeAmount

        fun storeFileUri(uri: Uri) = preferencesDataSource.setFileUri(uri)

        fun storePriceFileUri(uri: Uri) = preferencesDataSource.setPriceFileUri(uri)

        fun storeDefaultCurrency(currency: String) = preferencesDataSource.setDefaultCurrency(currency)

        fun storeDefaultStatus(status: String) = preferencesDataSource.setDefaultStatus(status)

        fun storeCurrencyBeforeAmount(enable: Boolean) =
            preferencesDataSource.setCurrencyBeforeAmount(
                enable,
            )
    }
