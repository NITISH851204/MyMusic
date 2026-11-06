package com.you.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** null = user ne abhi bhasha nahi chuni (pehli baar app khuli hai) */
object AppLanguage {

    private val _current = MutableStateFlow<Lang?>(null)
    val current: StateFlow<Lang?> = _current

    private var loaded = false

    fun ensureInit(context: Context) {
        if (loaded) return
        loaded = true
        _current.value = when (Prefs.languageOrNull(context)) {
            "english" -> Lang.ENGLISH
            "hindi" -> Lang.HINDI
            else -> null
        }
    }

    fun set(context: Context, lang: Lang) {
        loaded = true
        Prefs.setLanguage(context, lang.code)
        _current.value = lang
    }
}
