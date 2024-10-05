package be.chvp.nanoledger.ui.edit

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.ui.common.TransactionFormViewModel
import be.chvp.nanoledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditViewModel
    @Inject
    constructor(
        application: Application,
        preferencesDataSource: PreferencesDataSource,
        ledgerRepository: LedgerRepository,
    ) : TransactionFormViewModel(application, preferencesDataSource, ledgerRepository) {
        private lateinit var sourceTransaction: Transaction

        private val _loading = MutableLiveData<Boolean>(true)
        val loading: LiveData<Boolean> = _loading

        fun setFromIndex(index: Int) {
            sourceTransaction = ledgerRepository.transactions.value!![index]

            setDate(sourceTransaction.date)
            setStatus(sourceTransaction.status ?: "")
            setPayee(sourceTransaction.payee)
            setNote(sourceTransaction.note ?: "")

            sourceTransaction.postings.forEachIndexed { i, posting ->
                setAccount(i, posting.account)
                setCurrency(i, posting.amount?.currency ?: "")
                setAmount(i, posting.amount?.quantity ?: "")
            }

            _loading.value = false
        }

        override fun save(onFinish: suspend () -> Unit) {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                setSaving(true)
                viewModelScope.launch(IO) {
                    ledgerRepository.replaceTransaction(
                        uri,
                        sourceTransaction,
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
