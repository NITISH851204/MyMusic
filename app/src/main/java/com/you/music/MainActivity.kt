package com.you.music

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.you.music.playback.PlaybackService
import com.you.music.ui.MusicApp
import com.you.music.ui.rememberPlayerState
import com.you.music.ui.theme.AppTheme
import com.you.music.ui.theme.ThemeManager
import com.you.music.ui.theme.ThemeSettings
import java.io.File

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controllerState = mutableStateOf<MediaController?>(null)
    private val themeManager by lazy { ThemeManager(applicationContext) }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val crashFile = File(filesDir, "crash.txt")
        val lastCrash: String? = if (crashFile.exists()) {
            val text = crashFile.readText()
            crashFile.delete()
            text
        } else null

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            { controllerState.value = future.get() },
            ContextCompat.getMainExecutor(this)
        )

        setContent {
            val settings by themeManager.settings.collectAsState(initial = ThemeSettings())
            val controller = controllerState.value
            val state = rememberPlayerState(controller)

            AppTheme(settings, state.artwork) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MusicApp(controller, state, lastCrash, settings, themeManager)
                }
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}
