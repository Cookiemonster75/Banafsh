package app.banafsh.android.utils

import android.net.Uri
import android.text.format.DateUtils
import androidx.core.net.toUri
import app.banafsh.android.lib.providers.innertube.Innertube
import app.banafsh.android.lib.providers.innertube.models.bodies.ContinuationBody
import app.banafsh.android.lib.providers.innertube.requests.playlistPage
import app.banafsh.android.preferences.AppearancePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

fun String?.thumbnail(
    size: Int,
    maxSize: Int = AppearancePreferences.maxThumbnailSize
): String? {
    val actualSize = size.coerceAtMost(maxSize)
    return when {
        this?.startsWith("https://lh3.googleusercontent.com") == true ->
            "$this-w$actualSize-h$actualSize"

        this?.startsWith("https://yt3.ggpht.com") == true ->
            "$this-w$actualSize-h$actualSize-s$actualSize"

        else -> this
    }
}

fun Uri?.thumbnail(size: Int) = toString().thumbnail(size)?.toUri()

fun formatAsDuration(millis: Long) =
    DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

@Suppress("LoopWithTooManyJumpStatements")
suspend fun Result<Innertube.PlaylistOrAlbumPage>.completed(
    maxDepth: Int = Int.MAX_VALUE,
    shouldDedup: Boolean = false
) = runCatching {
    val page = getOrThrow()
    val songs = page.songsPage?.items.orEmpty().toMutableList()
    var continuation = page.songsPage?.continuation

    var depth = 0

    while (continuation != null && depth++ < maxDepth) {
        val newSongs = Innertube
            .playlistPage(
                body = ContinuationBody(continuation = continuation)
            )
            ?.getOrNull()
            ?.takeUnless { it.items.isNullOrEmpty() } ?: break

        if (shouldDedup && newSongs.items?.any { it in songs } != false) break

        newSongs.items?.let { songs += it }
        continuation = newSongs.continuation
    }

    page.copy(songsPage = Innertube.ItemsPage(items = songs, continuation = null))
}.also { it.exceptionOrNull()?.printStackTrace() }

fun <T> Flow<T>.onFirst(block: suspend (T) -> Unit): Flow<T> {
    var isFirst = true

    return onEach {
        if (!isFirst) return@onEach

        block(it)
        isFirst = false
    }
}
