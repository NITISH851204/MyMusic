package com.you.music.data

import android.content.Context

object Prefs {
    private fun p(c: Context) =
        c.applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun sponsorBlock(c: Context): Boolean = p(c).getBoolean("sponsorblock", true)

    fun setSponsorBlock(c: Context, value: Boolean) {
        p(c).edit().putBoolean("sponsorblock", value).apply()
    }
}
