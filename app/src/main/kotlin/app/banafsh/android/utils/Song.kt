package app.banafsh.android.utils

import android.content.ContentUris
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import app.banafsh.android.models.Song
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import app.banafsh.android.service.isLocal

fun Song.getUri() = if (isLocal)
    ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        id.substringAfter(LOCAL_KEY_PREFIX).toLong()
    )
else id.toUri()

fun MediaItem.toSong(): Song {
    val extras = mediaMetadata.extras?.songBundle
    return Song(
        id = mediaId,
        mediaMetadata.title?.toString().orEmpty(),
        artistsText = mediaMetadata.artist?.toString(),
        durationText = extras?.durationText,
        thumbnailUrl = mediaMetadata.artworkUri?.toString(),
        explicit = extras?.explicit == true
    )
}

val Song.asMediaItem: MediaItem
    @OptIn(UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "explicit" to explicit
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(getUri())
        .setCustomCacheKey(id)
        .build()
