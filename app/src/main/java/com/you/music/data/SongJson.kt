package com.you.music.data

import org.json.JSONObject

fun Song.toJson(): JSONObject = JSONObject()
    .put("title", title)
    .put("artist", artist)
    .put("url", url)
    .put("thumb", thumb ?: "")

fun JSONObject.toSong(): Song = Song(
    title = getString("title"),
    artist = getString("artist"),
    url = getString("url"),
    thumb = optString("thumb").ifEmpty { null }
)
