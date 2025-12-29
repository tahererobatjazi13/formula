package ir.kitgroup.formulaNew.core

import android.content.Context

object SharedPrefManager {
    private const val PREF_NAME = "my_app_prefs"
    private const val KEY_IS_REGISTERED = "is_registered"

    fun isUserRegistered(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
    }

    fun setUserRegistered(context: Context, registered: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_REGISTERED, registered).apply()
    }
}
