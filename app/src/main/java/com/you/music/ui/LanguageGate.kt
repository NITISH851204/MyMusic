package com.you.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.you.music.data.AppLanguage
import com.you.music.data.Lang

/** Pehli baar bhasha poochta hai, uske baad asli app dikhata hai */
@Composable
fun LanguageGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    remember {
        AppLanguage.ensureInit(ctx)
        true
    }
    val lang by AppLanguage.current.collectAsState()

    if (lang == null) {
        LanguagePickerScreen(onPick = { AppLanguage.set(ctx, it) })
    } else {
        content()
    }
}

@Composable
private fun LanguagePickerScreen(onPick: (Lang) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "MyMusic",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "अपनी भाषा चुनें",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Choose your language",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onPick(Lang.HINDI) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("हिन्दी", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPick(Lang.ENGLISH) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("English", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(24.dp))
        Text(
            "इसे बाद में Settings में बदल सकते हैं\nYou can change this later in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
