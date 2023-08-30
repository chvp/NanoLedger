package be.chvp.nanoledger

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(application: Application) : AndroidViewModel(
    application
) {
    fun storeFileUri(uri: Uri) {
        Log.d("nanoledger", "$uri")
    }
}
