package app.banafsh.android.lib.core.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.google.material_color_utilities.hct.Hct
import com.google.material_color_utilities.scheme.SchemeTonalSpot

@Immutable
data class ColorPalette(
    val surface: Color,
    val surfaceContainer: Color,
    val primaryContainer: Color,
    val accent: Color,
    val onAccent: Color,
    val red: Color = Color(0xffbf4040),
    val blue: Color = Color(0xff4472cf),
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val isDefault: Boolean,
    val isDark: Boolean
) {
    object Saver : androidx.compose.runtime.saveable.Saver<ColorPalette, List<Any>> {
        override fun restore(value: List<Any>) = ColorPalette(
            surface = Color(value[0] as Int),
            surfaceContainer = Color(value[1] as Int),
            primaryContainer = Color(value[2] as Int),
            accent = Color(value[3] as Int),
            onAccent = Color(value[4] as Int),
            red = Color(value[5] as Int),
            blue = Color(value[6] as Int),
            text = Color(value[7] as Int),
            textSecondary = Color(value[8] as Int),
            textDisabled = Color(value[9] as Int),
            isDefault = value[10] as Boolean,
            isDark = value[11] as Boolean
        )

        override fun SaverScope.save(value: ColorPalette) = listOf(
            value.surface.toArgb(),
            value.surfaceContainer.toArgb(),
            value.primaryContainer.toArgb(),
            value.accent.toArgb(),
            value.onAccent.toArgb(),
            value.red.toArgb(),
            value.blue.toArgb(),
            value.text.toArgb(),
            value.textSecondary.toArgb(),
            value.textDisabled.toArgb(),
            value.isDefault,
            value.isDark
        )
    }
}

private val defaultAccentColor = Color(0xff3e44ce)

val defaultLightPalette = lightColorPalette(defaultAccentColor)
val defaultDarkPalette = darkColorPalette(defaultAccentColor)

fun lightColorPalette(accent: Color): ColorPalette {
    val (hue, saturation) = accent.hsl

    return ColorPalette(
        surface = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.925f
        ),
        surfaceContainer = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.90f
        ),
        primaryContainer = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.85f
        ),
        text = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.02f),
            lightness = 0.12f
        ),
        textSecondary = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.40f
        ),
        textDisabled = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.2f),
            lightness = 0.65f
        ),
        accent = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.5f),
            lightness = 0.5f
        ),
        onAccent = Color.White,
        isDefault = false,
        isDark = false
    )
}

fun darkColorPalette(accent: Color): ColorPalette {
    val (hue, saturation) = accent.hsl

    return ColorPalette(
        surface = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.10f
        ),
        surfaceContainer = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.15f
        ),
        primaryContainer = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.2f
        ),
        text = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.02f),
            lightness = 0.88f
        ),
        textSecondary = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.65f
        ),
        textDisabled = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.2f),
            lightness = 0.40f
        ),
        accent = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.5f),
            lightness = 0.5f
        ),
        onAccent = Color.White,
        isDefault = false,
        isDark = true
    )
}

fun dynamicAccentColorOf(
    bitmap: Bitmap,
    isDark: Boolean
): Color? {
    val palette = Palette
        .from(bitmap)
        .maximumColorCount(8)
        .addFilter(if (isDark) ({ _, hsl -> hsl[0] !in 50f..100f }) else null)
        .generate()

    val hsl = if (isDark) {
        palette.dominantSwatch ?: Palette
            .from(bitmap)
            .maximumColorCount(8)
            .generate()
            .dominantSwatch
    } else {
        palette.dominantSwatch
    }?.hsl ?: return null

    val arr = if (hsl[1] < 0.08)
        palette.swatches
            .map(Palette.Swatch::getHsl)
            .sortedByDescending(FloatArray::component2)
            .find { it[1] != 0f }
            ?: hsl
    else hsl

    return arr.hsl.color
}

fun ColorPalette.pureBlack() = if (isDark) {
    copy(
        surface = Color.Black,
        surfaceContainer = Color.Black,
        primaryContainer = Color.Black
    )
} else this

fun ColorPalette.amoled() = if (isDark) {
    val (hue, saturation) = accent.hsl

    copy(
        surface = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f),
            lightness = 0.10f
        ),
        surfaceContainer = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f),
            lightness = 0.15f
        ),
        primaryContainer = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.2f
        ),
        accent = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f),
            lightness = 0.5f
        )
    )
} else this

fun colorPaletteOf(
    source: ColorSource,
    darkness: Darkness,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
): ColorPalette {
    val accentColor = when (source) {
        ColorSource.Default -> defaultAccentColor
        else -> sampleBitmap?.let { dynamicAccentColorOf(it, isDark) }
            ?: materialAccentColor ?: defaultAccentColor
    }

    val colorPalette = if (source == ColorSource.MaterialYou) {
        SchemeTonalSpot(
            /* sourceColorHct = */ Hct.fromInt(accentColor.toArgb()),
            /* isDark = */ isDark,
            /* contrastLevel = */ 0.0
        ).toColorPalette(isDark)
    } else {
        if (isDark) darkColorPalette(accentColor) else lightColorPalette(accentColor)
    }

    return when (darkness) {
        Darkness.Normal -> colorPalette
        Darkness.AMOLED -> colorPalette.amoled()
        Darkness.PureBlack -> colorPalette.pureBlack()
    }
}

inline val ColorPalette.isPureBlack get() = surface == Color.Black
inline val ColorPalette.collapsedPlayerProgressBar
    get() = if (isPureBlack) surface else primaryContainer
inline val ColorPalette.favoritesIcon get() = if (isDefault) red else accent
inline val ColorPalette.shimmer get() = if (isDefault) Color(0xff838383) else accent
inline val ColorPalette.primaryButton get() = if (isPureBlack) Color(0xff272727) else primaryContainer

@Suppress("UnusedReceiverParameter")
inline val ColorPalette.overlay get() = Color.Black.copy(alpha = 0.75f)

inline val ColorPalette.onOverlay get() = text

inline val ColorPalette.onOverlayShimmer get() = shimmer

fun SchemeTonalSpot.toColorPalette(isDark: Boolean) = ColorPalette(
    surface = Color(surface),
    surfaceContainer = Color(surfaceContainerLow),
    primaryContainer = Color(primaryContainer),
    accent = Color(primary),
    onAccent = Color(onPrimary),
    red = Color(error),
    text = Color(onPrimaryContainer),
    textSecondary = Color(onSecondaryContainer),
    textDisabled = Color(outline).copy(alpha = 0.38f),
    isDefault = false,
    isDark = isDark
)
