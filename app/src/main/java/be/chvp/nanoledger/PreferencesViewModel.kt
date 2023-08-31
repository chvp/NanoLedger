package be.chvp.nanoledger

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    application: Application,
    private val preferencesDataSource: PreferencesDataSource
) : AndroidViewModel(application) {
    val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri

    fun storeFileUri(uri: Uri) {
        preferencesDataSource.setFileUri(uri)
    }
}
