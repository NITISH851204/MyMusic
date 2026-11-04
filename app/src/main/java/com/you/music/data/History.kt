package com.you.music.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

object History {

    private lateinit var prefs: SharedPreferences

    private val _recent = MutableStateFlow<List<Song>>(emptyList())
    val recent: StateFlow<List<Song>> = _recent

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("history", Context.MODE_PRIVATE)
        _recent.value = load()
    }

    fun add(song: Song) {
        _recent.value = (listOf(song) + _recent.value.filter { it.url != song.url }).take(50)
        val arr = JSONArray()
        _recent.value.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    private fun load(): List<Song> = try {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it).toSong() }
    } catch (e: Throwable) {
        emptyList()
    }
}
