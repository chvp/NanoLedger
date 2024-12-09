package be.chvp.nanoledger.ui.stats

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import be.chvp.nanoledger.data.PreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatsViewModel
@Inject
constructor(
    application: Application,
    private val preferencesDataSource: PreferencesDataSource,
) : AndroidViewModel(application) {
    val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri
    val priceFileUri: LiveData<Uri?> = preferencesDataSource.priceFileUri
    val ledgerCommand: LiveData<String> = preferencesDataSource.ledgerCommand

    fun storeLedgerCommand(command: String) = preferencesDataSource.setLedgerCommand(command)
}
