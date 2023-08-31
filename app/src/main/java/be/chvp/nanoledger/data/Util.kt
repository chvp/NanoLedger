package be.chvp.nanoledger.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class SharedPreferenceLiveData<T>(
    protected val sharedPrefs: SharedPreferences,
    private val key: String
) : LiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {
    abstract fun getValueFromPreferences(key: String): T?

    override fun onActive() {
        super.onActive()
        value = getValueFromPreferences(key)
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onInactive()
    }

    override fun onSharedPreferenceChanged(_sp: SharedPreferences?, key: String?) {
        if (key == this.key) {
            value = getValueFromPreferences(this.key)
        }
    }
}

class SharedPreferenceStringLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    private val default: String?
) :
    SharedPreferenceLiveData<String?>(sharedPrefs, key) {
    init {
        value = this.getValueFromPreferences(key)
    }

    override fun getValueFromPreferences(
        key: String
    ): String? =
        if (sharedPrefs.contains(key)) sharedPrefs.getString(key, default) else default
}

fun SharedPreferences.stringLiveData(
    key: String,
    default: String? = null
): SharedPreferenceLiveData<String?> {
    return SharedPreferenceStringLiveData(this, key, default)
}
