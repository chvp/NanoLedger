package be.chvp.nanoledger.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val FILE_URI_KEY = "file_uri"
const val DEFAULT_CURRENCY_KEY = "default_currency"
const val DEFAULT_STATUS_KEY = "default_status"

class PreferencesDataSource @Inject constructor(@ApplicationContext private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "be.chvp.nanoledger.preferences",
        Context.MODE_PRIVATE
    )

    private val fileUriData = sharedPreferences.stringLiveData(FILE_URI_KEY)

    val fileUri: LiveData<Uri?> = fileUriData.map { it?.let { Uri.parse(it) } }

    fun getFileUri(): Uri? = sharedPreferences.getString(FILE_URI_KEY, null)?.let { Uri.parse(it) }

    fun setFileUri(fileUri: Uri?) = sharedPreferences.edit().putString(
        FILE_URI_KEY,
        fileUri?.toString()
    ).apply()

    val defaultCurrency: LiveData<String> = sharedPreferences.stringLiveData(
        DEFAULT_CURRENCY_KEY,
        "€"
    ).map { it!! }

    fun getDefaultCurrency(): String = sharedPreferences.getString(DEFAULT_CURRENCY_KEY, "€")!!

    fun setDefaultCurrency(currency: String) = sharedPreferences.edit().putString(
        DEFAULT_CURRENCY_KEY,
        currency
    ).apply()

    val defaultStatus: LiveData<String> = sharedPreferences.stringLiveData(
        DEFAULT_STATUS_KEY,
        " "
    ).map { it!! }

    fun getDefaultStatus(): String = sharedPreferences.getString(DEFAULT_STATUS_KEY, "€")!!

    fun setDefaultStatus(status: String) = sharedPreferences.edit().putString(
        DEFAULT_STATUS_KEY,
        status
    ).apply()
}
