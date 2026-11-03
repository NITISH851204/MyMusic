package com.you.music

import android.app.Application
import android.util.Log
import com.you.music.data.remote.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.File

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                File(filesDir, "crash.txt").writeText(Log.getStackTraceString(error))
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }

        NewPipe.init(
            NewPipeDownloader.create(),
            Localization("en", "IN"),
            ContentCountry("IN")
        )
    }
}
