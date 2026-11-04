package com.you.music.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.you.music.data.PlaylistImporter
import com.you.music.data.Playlists
import kotlinx.coroutines.launch

@Composable
fun ImportPlaylistDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Import from YouTube") },
        text = {
            Column {
                Text(
                    "Public ya unlisted playlist ka link paste karo (youtube.com/playlist?list=...).",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    singleLine = true,
                    enabled = !busy,
                    placeholder = { Text("Playlist link") }
                )
                if (busy) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Import ho raha hai...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                val e = error
                if (e != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        e,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && link.isNotBlank(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val result = com.you.music.data.PlaylistImporter.import(link)
                            if (result.songs.isEmpty()) {
                                error = "Playlist khali hai ya private hai."
                                busy = false
                            } else {
                                Playlists.createWith(
                                    result.name.ifBlank { "Imported playlist" },
                                    result.songs
                                )
                                onDone()
                            }
                        } catch (e: Throwable) {
                            error = "Import nahi hua. Link sahi hai? Playlist Public ya Unlisted honi chahiye."
                            busy = false
                        }
                    }
                }
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
