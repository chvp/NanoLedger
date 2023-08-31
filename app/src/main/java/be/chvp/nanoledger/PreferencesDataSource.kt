package be.chvp.nanoledger

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val FILE_URI_KEY = "file_uri"

class PreferencesDataSource @Inject constructor(@ApplicationContext private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "be.chvp.nanoledger.preferences",
        Context.MODE_PRIVATE
    )

    private val fileUriData = sharedPreferences.stringLiveData(FILE_URI_KEY)

    val fileUri: LiveData<Uri?> = fileUriData.map { it?.let { Uri.parse(it) } }

    fun setFileUri(fileUri: Uri?) = sharedPreferences.edit().putString(
        FILE_URI_KEY,
        fileUri?.toString()
    ).apply()
}
