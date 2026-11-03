package com.you.music

import android.app.Application
import com.you.music.data.remote.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(
            NewPipeDownloader.create(),
            Localization("en", "IN"),
            ContentCountry("IN")
        )
    }
}
