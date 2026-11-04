package com.you.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.you.music.data.Playlist

@Composable
fun PlaylistList(
    playlists: List<Playlist>,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        item {
            Button(onClick = onNew, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New playlist")
            }
        }
        if (playlists.isEmpty()) {
            item {
                Text(
                    "Abhi koi playlist nahi. Upar New playlist dabao, ya kisi gaane ke 3-dot menu se Add to playlist karo.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(playlists, key = { it.id }) { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(p.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Art(p.songs.firstOrNull()?.thumb, Modifier.size(52.dp), 10.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        p.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "${p.songs.size} gaane",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistHeader(
    name: String,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Button(onClick = onPlayAll) { Text("Play all") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist")
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Playlist ka naam") }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (playlists.isEmpty()) {
                    Text("Abhi koi playlist nahi. Neeche New playlist dabao.")
                }
                playlists.forEach { p ->
                    Text(
                        p.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p.id) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onNew) { Text("New playlist") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
