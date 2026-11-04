package com.you.music.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THEME_CACHE = "theme_cache"
private const val SEED_KEY = "seed"

/** Pichli baar ka album-art rang (0 = koi nahi). App shuru hote hi sahi rang dikhane ke liye. */
fun cachedSeed(context: Context): Int =
    context.getSharedPreferences(THEME_CACHE, Context.MODE_PRIVATE).getInt(SEED_KEY, 0)

@Composable
fun AppTheme(
    settings: ThemeSettings,
    artworkUrl: String?,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        else -> true
    }
    val amoled = settings.mode == ThemeMode.AMOLED

    var artSeed by remember {
        mutableStateOf(
            if (settings.artColors) cachedSeed(ctx).takeIf { it != 0 } else null
        )
    }

    LaunchedEffect(artworkUrl, settings.artColors) {
        if (!settings.artColors) {
            artSeed = null
        } else {
            if (artSeed == null) {
                cachedSeed(ctx).takeIf { it != 0 }?.let { artSeed = it }
            }
            if (artworkUrl != null) {
                extractSeed(ctx, artworkUrl)?.let {
                    artSeed = it
                    ctx.getSharedPreferences(THEME_CACHE, Context.MODE_PRIVATE)
                        .edit().putInt(SEED_KEY, it).apply()
                }
            }
        }
    }

    val seed = artSeed ?: Presets[settings.preset].color
    val scheme = animated(schemeFromSeed(seed, dark, amoled))

    // Status bar aur navigation bar ko app ke background jaisa rakho
    val bg = scheme.background.toArgb()
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = bg
            window.navigationBarColor = bg
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}

private suspend fun extractSeed(ctx: Context, url: String): Int? =
    withContext(Dispatchers.Default) {
        try {
            val request = ImageRequest.Builder(ctx)
                .data(url)
                .allowHardware(false)
                .size(128)
                .build()
            val result = ctx.imageLoader.execute(request)
            val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
            bitmap?.let {
                val p = Palette.from(it).generate()
                p.getVibrantColor(p.getMutedColor(p.getDominantColor(0))).takeIf { c -> c != 0 }
            }
        } catch (e: Throwable) {
            null
        }
    }

fun schemeFromSeed(seed: Int, dark: Boolean, amoled: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    val h = hsl[0]
    val s = if (hsl[1] < 0.1f) 0f else hsl[1].coerceIn(0.35f, 0.85f)

    fun c(sat: Float, l: Float) = Color(ColorUtils.HSLToColor(floatArrayOf(h, sat, l)))

    return if (dark) {
        val bg = if (amoled) Color.Black else c(0.12f, 0.08f)
        darkColorScheme(
            primary = c(s, 0.72f),
            onPrimary = c(s, 0.15f),
            primaryContainer = c(s, 0.28f),
            onPrimaryContainer = c(s, 0.90f),
            secondary = c(s * 0.5f, 0.70f),
            background = bg,
            surface = bg,
            onBackground = c(0.10f, 0.92f),
            onSurface = c(0.10f, 0.92f),
            surfaceVariant = if (amoled) c(0.10f, 0.14f) else c(0.12f, 0.17f),
            onSurfaceVariant = c(0.10f, 0.75f),
            outline = c(0.08f, 0.50f),
            outlineVariant = c(0.10f, 0.28f),
            surfaceContainer = if (amoled) c(0.10f, 0.10f) else c(0.12f, 0.12f),
            surfaceContainerHigh = if (amoled) c(0.10f, 0.14f) else c(0.12f, 0.16f)
        )
    } else {
        val bg = c(0.15f, 0.98f)
        lightColorScheme(
            primary = c(s, 0.40f),
            onPrimary = Color.White,
            primaryContainer = c(s, 0.88f),
            onPrimaryContainer = c(s, 0.12f),
            secondary = c(s * 0.5f, 0.40f),
            background = bg,
            surface = bg,
            onBackground = c(0.10f, 0.10f),
            onSurface = c(0.10f, 0.10f),
            surfaceVariant = c(0.18f, 0.90f),
            onSurfaceVariant = c(0.10f, 0.30f),
            outline = c(0.08f, 0.50f),
            outlineVariant = c(0.12f, 0.80f),
            surfaceContainer = c(0.15f, 0.94f),
            surfaceContainerHigh = c(0.15f, 0.92f)
        )
    }
}

@Composable
private fun anim(color: Color): Color =
    animateColorAsState(color, tween(500), label = "themeColor").value

@Composable
private fun animated(s: ColorScheme): ColorScheme = s.copy(
    primary = anim(s.primary),
    onPrimary = anim(s.onPrimary),
    primaryContainer = anim(s.primaryContainer),
    onPrimaryContainer = anim(s.onPrimaryContainer),
    secondary = anim(s.secondary),
    background = anim(s.background),
    surface = anim(s.surface),
    onBackground = anim(s.onBackground),
    onSurface = anim(s.onSurface),
    surfaceVariant = anim(s.surfaceVariant),
    onSurfaceVariant = anim(s.onSurfaceVariant),
    outline = anim(s.outline),
    outlineVariant = anim(s.outlineVariant),
    surfaceContainer = anim(s.surfaceContainer),
    surfaceContainerHigh = anim(s.surfaceContainerHigh)
)
