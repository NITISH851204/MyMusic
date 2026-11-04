package com.you.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.you.music.data.HomeData
import com.you.music.data.HomeRepository
import com.you.music.data.Song

private fun String.sized(px: Int): String =
    replace(Regex("=w\\d+-h\\d+"), "=w$px-h$px")

@Composable
fun HomeScreen(
    onPlay: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var data by remember { mutableStateOf<HomeData?>(HomeRepository.cachedData()) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        if (reloadKey > 0) data = null
        if (data == null) {
            data = HomeRepository.load(force = reloadKey > 0)
        }
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 16.dp)) {
        val d = data
        when {
            d == null -> item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            d.isEmpty -> item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Home load nahi hua. Internet check karo.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { reloadKey++ },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Retry") }
                }
            }

            else -> {
                if (d.forYou.isNotEmpty()) {
                    item { SectionTitle("For You") }
                    item { SongRow(d.forYou, onPlay) }
                }
                if (d.hits.isNotEmpty()) {
                    item { SectionTitle("Hit Song") }
                    item { HeroCard(d.hits[0]) { onPlay(d.hits, 0) } }
                }
                if (d.fresh.isNotEmpty()) {
                    item { SectionTitle("New Song") }
                    item { SongRow(d.fresh, onPlay) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 10.dp)
    )
}

@Composable
private fun SongRow(songs: List<Song>, onPlay: (List<Song>, Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(songs) { index, song ->
            SongCard(song) { onPlay(songs, index) }
        }
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Art(song.thumb?.sized(400), Modifier.size(140.dp), 16.dp)
        Text(
            song.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 2.dp, top = 6.dp, end = 2.dp)
        )
        Text(
            song.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun HeroCard(song: Song, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Art(song.thumb?.sized(800), Modifier.fillMaxSize(), 24.dp)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            FilledIconButton(onClick = onClick) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
