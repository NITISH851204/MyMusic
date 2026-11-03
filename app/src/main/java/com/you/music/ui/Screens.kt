package com.you.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.you.music.data.MusicRepository
import com.you.music.data.Song
import com.you.music.data.toMediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UiPlayerState {
    var title by mutableStateOf("")
    var artist by mutableStateOf("")
    var artwork by mutableStateOf<String?>(null)
    var isPlaying by mutableStateOf(false)
    var buffering by mutableStateOf(false)
    var hasMedia by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var error by mutableStateOf<String?>(null)
}

@Composable
fun rememberPlayerState(controller: MediaController?): UiPlayerState {
    val state = remember { UiPlayerState() }

    DisposableEffect(controller) {
        if (controller == null) {
            return@DisposableEffect onDispose { }
        }
        val sync = {
            state.hasMedia = controller.mediaItemCount > 0
            state.title = controller.mediaMetadata.title?.toString() ?: ""
            state.artist = controller.mediaMetadata.artist?.toString() ?: ""
            state.artwork = controller.mediaMetadata.artworkUri?.toString()
            state.isPlaying = controller.isPlaying
            state.buffering = controller.playbackState == Player.STATE_BUFFERING
            state.shuffle = controller.shuffleModeEnabled
            state.repeat = controller.repeatMode
            if (controller.isPlaying) state.error = null
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                sync()
            }

            override fun onPlayerError(error: PlaybackException) {
                state.error = error.errorCodeName
            }
        }
        controller.addListener(listener)
        sync()
        onDispose { controller.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        while (true) {
            controller?.let {
                state.position = it.currentPosition.coerceAtLeast(0L)
                state.duration = it.duration.coerceAtLeast(0L)
            }
            delay(400)
        }
    }
    return state
}

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** YouTube Music ki chhoti thumbnail ko badi size me badalta hai (player screen ke liye). */
private fun String.hiRes(): String =
    replace(Regex("=w\\d+-h\\d+"), "=w800-h800")

@Composable
fun MusicApp(controller: MediaController?, lastCrash: String?) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val state = rememberPlayerState(controller)

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var showPlayer by remember { mutableStateOf(false) }
    var crash by remember { mutableStateOf(lastCrash) }

    BackHandler(enabled = showPlayer) { showPlayer = false }

    val doSearch: () -> Unit = {
        if (query.isNotBlank()) {
            focus.clearFocus()
            scope.launch {
                status = "Searching..."
                try {
                    results = MusicRepository.search(query)
                    status = if (results.isEmpty()) "Kuch nahi mila" else ""
                } catch (e: Throwable) {
                    status = "Search error: ${e.javaClass.simpleName}: ${e.message}"
                }
            }
        }
    }

    val playAt: (Int) -> Unit = { index ->
        controller?.let { c ->
            c.setMediaItems(results.map { it.toMediaItem() }, index, 0L)
            c.prepare()
            c.play()
        }
    }

    val playNext: (Song) -> Unit = { song ->
        controller?.let { c ->
            if (c.mediaItemCount == 0) {
                c.setMediaItem(song.toMediaItem())
                c.prepare()
            } else {
                c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
            }
        }
    }

    val addToQueue: (Song) -> Unit = { song ->
        controller?.let { c ->
            if (c.mediaItemCount == 0) {
                c.setMediaItem(song.toMediaItem())
                c.prepare()
            } else {
                c.addMediaItem(song.toMediaItem())
            }
        }
    }

    val toggle: () -> Unit = {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    val infoLine = state.error?.let { "Player error: $it" } ?: status

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Tracks",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )

                val c = crash
                if (c != null) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Last crash (screenshot bhejo):", style = MaterialTheme.typography.labelMedium)
                        SelectionContainer {
                            Text(
                                c.take(2500),
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(onClick = { crash = null }) { Text("Close") }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = { Text("Search songs") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() })
                )

                if (infoLine.isNotBlank()) {
                    Text(
                        infoLine,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(results) { index, song ->
                        TrackRow(
                            song = song,
                            onClick = { playAt(index) },
                            onPlayNext = { playNext(song) },
                            onAddToQueue = { addToQueue(song) }
                        )
                    }
                }
            }

            if (state.hasMedia) {
                MiniPlayer(
                    state = state,
                    onOpen = { showPlayer = true },
                    onToggle = toggle,
                    onNext = { controller?.seekToNextMediaItem() }
                )
            }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            PlayerScreen(state, controller, onClose = { showPlayer = false })
        }
    }
}

@Composable
fun PlayerScreen(state: UiPlayerState, controller: MediaController?, onClose: () -> Unit) {
    val progress =
        if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val active = MaterialTheme.colorScheme.primary

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
            }
            Spacer(Modifier.weight(1f))
            Text("Now playing", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.size(16.dp))

        Art(
            state.artwork?.hiRes(),
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(28.dp)
                ),
            28.dp
        )

        Spacer(Modifier.size(28.dp))

        Text(
            state.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            state.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = inactive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.size(20.dp))

        WavySeekbar(
            progress = progress,
            isPlaying = state.isPlaying,
            onSeek = { f ->
                if (state.duration > 0) {
                    controller?.seekTo((f * state.duration).toLong())
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(state.position), style = MaterialTheme.typography.bodySmall)
            Text(fmt(state.duration), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.size(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
            }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffle) active else inactive
                )
            }
            IconButton(onClick = { controller?.seekToPreviousMediaItem() }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }
            FilledIconButton(
                onClick = {
                    controller?.let { if (it.isPlaying) it.pause() else it.play() }
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = { controller?.seekToNextMediaItem() }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = {
                controller?.let {
                    it.repeatMode = when (it.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                }
            }) {
                Icon(
                    if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                    else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeat == Player.REPEAT_MODE_OFF) inactive else active
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.size(12.dp))
            Text(
                "Player error: ${state.error}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
        }
    }
}
