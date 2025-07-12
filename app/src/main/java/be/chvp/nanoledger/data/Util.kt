package be.chvp.nanoledger.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class SharedPreferenceLiveData<T>(
    protected val sharedPrefs: SharedPreferences,
    private val key: String,
) : LiveData<T>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
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

    override fun onSharedPreferenceChanged(
        _sp: SharedPreferences?,
        key: String?,
    ) {
        if (key == this.key) {
            value = getValueFromPreferences(this.key)
        }
    }
}

class SharedPreferenceStringLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    private val default: String?,
) : SharedPreferenceLiveData<String?>(sharedPrefs, key) {
    init {
        value = this.getValueFromPreferences(key)
    }

    override fun getValueFromPreferences(key: String): String? =
        if (sharedPrefs.contains(key)) sharedPrefs.getString(key, default) else default
}

fun SharedPreferences.stringLiveData(
    key: String,
    default: String? = null,
): SharedPreferenceLiveData<String?> = SharedPreferenceStringLiveData(this, key, default)

class SharedPreferenceBooleanLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    private val default: Boolean,
) : SharedPreferenceLiveData<Boolean>(sharedPrefs, key) {
    init {
        value = this.getValueFromPreferences(key)
    }

    override fun getValueFromPreferences(key: String): Boolean =
        if (sharedPrefs.contains(key)) sharedPrefs.getBoolean(key, default) else default
}

fun SharedPreferences.booleanLiveData(
    key: String,
    default: Boolean = false,
): SharedPreferenceLiveData<Boolean> = SharedPreferenceBooleanLiveData(this, key, default)

class SharedPreferenceIntLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    private val default: Int,
) : SharedPreferenceLiveData<Int>(sharedPrefs, key) {
    init {
        value = this.getValueFromPreferences(key)
    }

    override fun getValueFromPreferences(key: String): Int = if (sharedPrefs.contains(key)) sharedPrefs.getInt(key, default) else default
}

fun SharedPreferences.intLiveData(
    key: String,
    default: Int = 0,
): SharedPreferenceLiveData<Int> = SharedPreferenceIntLiveData(this, key, default)
