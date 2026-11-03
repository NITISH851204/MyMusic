package com.you.music

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.you.music.data.MusicRepository
import com.you.music.data.Song
import com.you.music.playback.PlaybackService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controllerState = mutableStateOf<MediaController?>(null)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            { controllerState.value = future.get() },
            ContextCompat.getMainExecutor(this)
        )

        setContent {
            MaterialTheme {
                MusicScreen(controllerState.value)
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@Composable
fun MusicScreen(controller: MediaController?) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: PlaybackException) {
                status = "Player error: ${error.errorCodeName}"
            }
        }
        controller?.addListener(listener)
        isPlaying = controller?.isPlaying ?: false
        onDispose { controller?.removeListener(listener) }
    }

    val doSearch: () -> Unit = {
        if (query.isNotBlank()) {
            scope.launch {
                status = "Searching..."
                try {
                    results = MusicRepository.search(query)
                    status = "${results.size} results"
                } catch (e: Exception) {
                    status = "Search error: ${e.message}"
                }
            }
        }
    }

    val play: (Song) -> Unit = { song ->
        scope.launch {
            status = "Loading: ${song.title}"
            try {
                val url = MusicRepository.audioUrl(song.url)
                controller?.let {
                    it.setMediaItem(
                        MediaItem.Builder()
                            .setUri(url)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setArtist(song.artist)
                                    .build()
                            )
                            .build()
                    )
                    it.prepare()
                    it.play()
                }
                status = "Now: ${song.title}"
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search songs") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() })
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = doSearch) { Text("Go") }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Button(
                enabled = controller != null,
                onClick = {
                    controller?.let { if (it.isPlaying) it.pause() else it.play() }
                }
            ) { Text(if (isPlaying) "Pause" else "Play") }
        }

        LazyColumn {
            items(results) { song ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { play(song) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(song.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                    Text(song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
