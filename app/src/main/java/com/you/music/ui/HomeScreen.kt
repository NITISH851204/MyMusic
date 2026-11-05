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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.you.music.data.ArtistEntry
import com.you.music.data.Category
import com.you.music.data.HomeData
import com.you.music.data.HomeRepository
import com.you.music.data.Lang
import com.you.music.data.Prefs
import com.you.music.data.Song
import kotlinx.coroutines.launch

private fun String.sized(px: Int): String =
    replace(Regex("=w\\d+-h\\d+"), "=w$px-h$px")

private fun String.avatar(): String =
    replace(Regex("=s\\d+"), "=s300")

@Composable
fun HomeScreen(
    onPlay: (List<Song>, Int) -> Unit,
    onOpenCollection: (String, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var lang by remember {
        mutableStateOf(if (Prefs.language(ctx) == "english") Lang.ENGLISH else Lang.HINDI)
    }
    var data by remember(lang) { mutableStateOf(HomeRepository.cachedData(lang)) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(lang, reloadKey) {
        if (reloadKey > 0) data = null
        if (data == null) {
            data = HomeRepository.load(lang, force = reloadKey > 0)
        }
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            LanguageSwitch(lang) { picked ->
                Prefs.setLanguage(ctx, picked.code)
                lang = picked
            }
        }

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
                if (d.recommended.isNotEmpty()) {
                    item { SectionTitle("Recommended") }
                    item { SongRow(d.recommended, onPlay) }
                }
                if (d.fresh.isNotEmpty()) {
                    item { SectionTitle("New Song") }
                    item { SongRow(d.fresh, onPlay) }
                }
                if (d.popular.isNotEmpty()) {
                    item { SectionTitle("Today Popular Song") }
                    item { SongRow(d.popular, onPlay) }
                }
                if (d.hits.isNotEmpty()) {
                    item { SectionTitle("Hit Song") }
                    item { HeroCard(d.hits[0]) { onPlay(d.hits, 0) } }
                }

                val cfg = HomeRepository.config(lang)
                cfg.rows.forEach { row ->
                    item { CategoryRow(row, onOpenCollection) }
                }
                item { SectionTitle("Artists") }
                item { ArtistRow(cfg.artists, onOpenCollection) }
            }
        }
    }
}

@Composable
private fun LanguageSwitch(current: Lang, onPick: (Lang) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Lang.values().forEach { l ->
            if (l == current) {
                Button(onClick = { onPick(l) }, modifier = Modifier.weight(1f)) {
                    Text(l.label)
                }
            } else {
                OutlinedButton(onClick = { onPick(l) }, modifier = Modifier.weight(1f)) {
                    Text(l.label)
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

@Composable
private fun CategoryRow(
    items: List<Category>,
    onOpen: (String, List<Song>) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        itemsIndexed(items) { index, category ->
            CategoryCard(category, big = index == 0, onOpen = onOpen)
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    big: Boolean,
    onOpen: (String, List<Song>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var songs by remember(category.query) {
        mutableStateOf(HomeRepository.peekCollection(category.query))
    }
    LaunchedEffect(category.query) {
        if (songs == null) songs = HomeRepository.collection(category.query)
    }

    val cover = songs?.firstOrNull()?.thumb?.sized(500)
    val w = if (big) 230.dp else 130.dp
    val h = if (big) 150.dp else 130.dp

    Box(
        Modifier
            .width(w)
            .height(h)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                scope.launch {
                    val list = songs?.takeIf { it.isNotEmpty() }
                        ?: HomeRepository.collection(category.query)
                    if (list.isNotEmpty()) onOpen(category.title, list)
                }
            }
    ) {
        if (cover != null) {
            Art(cover, Modifier.fillMaxSize(), 20.dp)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        Text(
            category.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            style = if (big) MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

@Composable
private fun ArtistRow(
    artists: List<ArtistEntry>,
    onOpen: (String, List<Song>) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(artists) { artist -> ArtistItem(artist, onOpen) }
    }
}

@Composable
private fun ArtistItem(
    entry: ArtistEntry,
    onOpen: (String, List<Song>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var image by remember(entry.name) {
        mutableStateOf(HomeRepository.peekArtistImage(entry.name))
    }
    LaunchedEffect(entry.name) {
        if (image == null) image = HomeRepository.artistImage(entry)
    }

    Column(
        Modifier
            .width(92.dp)
            .clickable {
                scope.launch {
                    val songs = HomeRepository.collection("${entry.query} songs")
                    if (songs.isNotEmpty()) onOpen(entry.name, songs)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val img = image
            if (img != null) {
                Art(img.avatar(), Modifier.fillMaxSize(), 42.dp)
            } else {
                Text(entry.name.take(1), style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            entry.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
