@file:OptIn(UnstableApi::class)

package app.banafsh.android.utils

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import app.banafsh.android.lib.providers.innertube.Innertube
import app.banafsh.android.lib.providers.piped.models.Playlist
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import app.banafsh.android.service.isLocal

fun MediaItem.getUri(): Uri = if (isLocal)
    ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        mediaId.substringAfter(LOCAL_KEY_PREFIX).toLong()
    )
else mediaId.toUri()

val Innertube.SongItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name.orEmpty() })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    SongBundleAccessor.bundle {
                        albumId = album?.endpoint?.browseId
                        durationText = this@asMediaItem.durationText
                        artistNames = authors
                            ?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name }
                        explicit = this@asMediaItem.explicit
                    }
                )
                .build()
        )
        .build()
val Innertube.VideoItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name.orEmpty() })
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "artistNames" to if (isOfficialMusicVideo) authors?.filter { it.endpoint != null }
                            ?.mapNotNull { it.name } else null,
                        "artistIds" to if (isOfficialMusicVideo) authors?.mapNotNull { it.endpoint?.browseId }
                        else null
                    )
                )
                .build()
        )
        .build()
val Playlist.Video.asMediaItem: MediaItem?
    get() {
        val key = id ?: return null

        return MediaItem.Builder()
            .setMediaId(key)
            .setUri(key)
            .setCustomCacheKey(key)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(uploaderName)
                    .setArtworkUri(Uri.parse(thumbnailUrl.toString()))
                    .setExtras(
                        bundleOf(
                            "durationText" to duration.toComponents { minutes, seconds, _ ->
                                "$minutes:${seconds.toString().padStart(2, '0')}"
                            },
                            "artistNames" to listOf(uploaderName),
                            "artistIds" to uploaderId?.let { listOf(it) }
                        )
                    )
                    .build()
            )
            .build()
    }
