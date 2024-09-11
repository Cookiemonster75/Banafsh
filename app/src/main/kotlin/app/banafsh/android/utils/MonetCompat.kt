package app.banafsh.android.utils

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.banafsh.android.ui.theme.ColorPalette
import app.banafsh.android.ui.theme.defaultLightPalette
import com.kieronquinn.monetcompat.core.MonetCompat
import kotlinx.coroutines.launch

val LocalMonetCompat = staticCompositionLocalOf { MonetCompat.getInstance() }

context(LifecycleOwner)
inline fun MonetCompat.invokeOnReady(
    state: Lifecycle.State = Lifecycle.State.CREATED,
    crossinline block: () -> Unit
) = lifecycleScope.launch {
    repeatOnLifecycle(state) {
        awaitMonetReady()
        block()
    }
}

fun MonetCompat.setDefaultPalette(palette: ColorPalette = defaultLightPalette) {
    defaultAccentColor = palette.accent.toArgb()
    defaultBackgroundColor = palette.surface.toArgb()
    defaultPrimaryColor = palette.surfaceContainer.toArgb()
    defaultSecondaryColor = palette.primaryContainer.toArgb()
}
