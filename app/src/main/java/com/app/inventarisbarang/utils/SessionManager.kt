package com.app.inventarisbarang.utils

import android.content.Context
import android.content.SharedPreferences
import com.app.inventarisbarang.model.User

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "inventaris_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
        private const val KEY_DIVISI = "divisi"
        private const val KEY_AVATAR = "avatar"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }

    fun saveLoginSession(token: String, user: User) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_TOKEN, token)
        editor.putInt(KEY_USER_ID, user.id)
        editor.putString(KEY_NAME, user.name)
        editor.putString(KEY_USERNAME, user.username)
        editor.putString(KEY_EMAIL, user.email ?: "")
        editor.putString(KEY_ROLE, user.role.lowercase())
        editor.putString(KEY_DIVISI, user.divisi ?: "")
        editor.putString(KEY_AVATAR, user.avatar ?: "")
        editor.apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)

    fun getName(): String = prefs.getString(KEY_NAME, "") ?: ""

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun getRole(): String = prefs.getString(KEY_ROLE, "user") ?: "user"

    fun getDivisi(): String = prefs.getString(KEY_DIVISI, "Dipanegara Computer Club") ?: "Dipanegara Computer Club"

    fun getAvatar(): String = prefs.getString(KEY_AVATAR, "") ?: ""

    fun isAdmin(): Boolean = getRole().equals("admin", ignoreCase = true)

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
