package com.you.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.you.music.data.LyricLine
import com.you.music.data.LyricsRepository
import com.you.music.data.LyricsResult

@Composable
fun LyricsView(
    state: UiPlayerState,
    controller: MediaController?,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<LyricsResult?>(null) }

    LaunchedEffect(state.title, state.artist) {
        if (state.title.isBlank()) return@LaunchedEffect
        loading = true
        result = null
        result = LyricsRepository.fetch(ctx, state.title, state.artist, state.duration / 1000)
        loading = false
    }

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        val r = result
        when {
            loading -> CircularProgressIndicator()
            r == null -> Text(
                "Lyrics nahi mile.\nYa to is gaane ke lyrics available nahi, ya internet band hai.",
                modifier = Modifier.padding(24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            r.synced.isNotEmpty() -> SyncedLyrics(r.synced, state, controller)
            else -> Text(
                r.plain,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun SyncedLyrics(
    lines: List<LyricLine>,
    state: UiPlayerState,
    controller: MediaController?
) {
    val listState = rememberLazyListState()
    val active = lines.indexOfLast { it.timeMs <= state.position + 250 }

    LaunchedEffect(active) {
        if (active >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((active - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        itemsIndexed(lines) { i, line ->
            val isActive = i == active
            Text(
                line.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { controller?.seekTo(line.timeMs) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = if (isActive) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.titleMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}
