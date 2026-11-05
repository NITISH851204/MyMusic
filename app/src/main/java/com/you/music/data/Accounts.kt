package com.you.music.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Phone ke andar ke accounts. Guest = null. Har account ka alag likes/playlists/history/offline list. */
object Accounts {

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    private val _current = MutableStateFlow<String?>(null)
    val current: StateFlow<String?> = _current

    private val nameRule = Regex("^[a-z0-9_]{3,20}$")

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("accounts", Context.MODE_PRIVATE)
        val saved = prefs.getString("session", null)
        _current.value = if (saved != null && prefs.contains("u_$saved")) saved else null
    }

    /** Guest ke liye purana naam (data safe), account ke liye naam ke saath */
    fun storeName(base: String): String {
        val user = _current.value ?: return base
        return base + "_" + user
    }

    /** Error message deta hai, sab theek ho to null */
    fun register(username: String, password: String): String? {
        val u = username.trim().lowercase()
        if (!nameRule.matches(u)) return "Username 3 se 20 akshar ka ho: a-z, 0-9 ya _"
        if (password.length < 6) return "Password kam se kam 6 akshar ka rakho"
        if (prefs.contains("u_$u")) return "Ye username pehle se maujood hai"

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(password, salt)
        prefs.edit()
            .putString("u_$u", enc(salt) + ":" + enc(hash))
            .putString("session", u)
            .apply()
        switchTo(u)
        return null
    }

    fun login(username: String, password: String): String? {
        val u = username.trim().lowercase()
        val wrong = "Username ya password galat hai"
        val saved = prefs.getString("u_$u", null) ?: return wrong
        val parts = saved.split(":")
        if (parts.size != 2) return wrong

        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        if (!MessageDigest.isEqual(hash(password, salt), expected)) return wrong

        prefs.edit().putString("session", u).apply()
        switchTo(u)
        return null
    }

    fun logout() {
        prefs.edit().remove("session").apply()
        switchTo(null)
    }

    private fun switchTo(user: String?) {
        _current.value = user
        Likes.init(appContext)
        Playlists.init(appContext)
        History.init(appContext)
        Downloads.init(appContext)
    }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 50000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun enc(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
}
