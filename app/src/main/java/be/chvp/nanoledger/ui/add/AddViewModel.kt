package be.chvp.nanoledger.ui.add

import android.app.Application
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.ui.common.TransactionFormViewModel
import be.chvp.nanoledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddViewModel
    @Inject
    constructor(
        application: Application,
        preferencesDataSource: PreferencesDataSource,
        ledgerRepository: LedgerRepository,
    ) : TransactionFormViewModel(application, preferencesDataSource, ledgerRepository) {
        fun loadTransactionFromIndex(index: Int) {
            setFromTransaction(ledgerRepository.transactions.value!![index])
            // When copying, set the date to today
            setDate(Date())
        }

        override fun save(onFinish: suspend () -> Unit) {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                setSaving(true)
                viewModelScope.launch(IO) {
                    ledgerRepository.appendTo(
                        uri,
                        toTransactionString(),
                        {
                            postSaving(false)
                            onFinish()
                        },
                        {
                            postSaving(false)
                            postMismatch(Event(1))
                        },
                        {
                            postSaving(false)
                            postError(Event(it))
                        },
                        {
                            // We ignore a read error, the write went through so the
                            // only thing the user will experience is the
                            // transaction not being in the transaction
                            // overview. Which isn't optimal, but not a big problem
                            // either.
                            postSaving(false)
                            onFinish()
                        },
                    )
                }
            }
        }
    }
