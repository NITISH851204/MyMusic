package com.you.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.you.music.data.Accounts
import com.you.music.data.Prefs
import com.you.music.ui.theme.Presets
import com.you.music.ui.theme.ThemeManager
import com.you.music.ui.theme.ThemeMode
import com.you.music.ui.theme.ThemeSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: ThemeSettings,
    manager: ThemeManager,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var sponsorBlock by remember { mutableStateOf(Prefs.sponsorBlock(ctx)) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

                Spacer(Modifier.size(16.dp))
        AccountSection()
        Spacer(Modifier.size(24.dp))
        ExtraSettings()

        Spacer(Modifier.size(24.dp))
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.values().forEach { m ->
                FilterChip(
                    selected = settings.mode == m,
                    onClick = { scope.launch { manager.setMode(m) } },
                    label = { Text(m.label) }
                )
            }
        }

        Spacer(Modifier.size(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Album art colors", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Gaane ke cover se app ke colors badlein",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.artColors,
                onCheckedChange = { scope.launch { manager.setArtColors(it) } }
            )
        }

        Spacer(Modifier.size(24.dp))
        Text("Accent preset", style = MaterialTheme.typography.titleMedium)
        Text(
            "Album art colors band ho, ya koi gaana na chal raha ho, tab ye rang use hota hai",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Presets.forEachIndexed { index, preset ->
                val selected = settings.preset == index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(preset.color))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { scope.launch { manager.setPreset(index) } }
                    )
                    Text(
                        preset.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SponsorBlock", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sponsor, intro/outro aur music se alag hisse apne aap skip karo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = sponsorBlock,
                onCheckedChange = {
                    sponsorBlock = it
                    Prefs.setSponsorBlock(ctx, it)
                }
            )
        }
    }
}

@Composable
private fun AccountSection() {
    val user by Accounts.current.collectAsState()
    var dialog by remember { mutableStateOf<String?>(null) }

    Text("Account", style = MaterialTheme.typography.titleMedium)
    Text(
        if (user == null) "Guest" else "@$user",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
    Spacer(Modifier.size(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (user == null) {
            Button(onClick = { dialog = "login" }) { Text("Login") }
            OutlinedButton(onClick = { dialog = "register" }) { Text("Create account") }
        } else {
            OutlinedButton(onClick = { Accounts.logout() }) { Text("Logout") }
        }
    }
    Text(
        "Har account ke liked, playlists, recent aur offline list alag hote hain. Ye sirf is phone me save hota hai. Password bhool gaye to recover nahi hota.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )

    val d = dialog
    if (d != null) {
        AuthDialog(
            register = d == "register",
            onDismiss = { dialog = null },
            onDone = { dialog = null }
        )
    }
}

@Composable
private fun AuthDialog(register: Boolean, onDismiss: () -> Unit, onDone: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (register) "Create account" else "Login") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    label = { Text("Username") }
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (register) {
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = password2,
                        onValueChange = { password2 = it },
                        singleLine = true,
                        label = { Text("Password dobara") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                val e = error
                if (e != null) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        e,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (register && password != password2) {
                    error = "Dono password same nahi hain"
                } else {
                    val result = if (register) {
                        Accounts.register(username, password)
                    } else {
                        Accounts.login(username, password)
                    }
                    if (result == null) onDone() else error = result
                }
            }) { Text(if (register) "Create" else "Login") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
