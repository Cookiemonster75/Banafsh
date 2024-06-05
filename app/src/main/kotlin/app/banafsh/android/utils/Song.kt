package app.banafsh.android.utils

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import app.banafsh.android.models.Song
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import app.banafsh.android.service.isLocal

fun MediaItem.getUri(): Uri = if (isLocal) ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    mediaId.substringAfter(LOCAL_KEY_PREFIX).toLong()
) else mediaId.toUri()

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

fun createShareSongIndent(mediaItem: MediaItem): Intent = Intent().apply {
    action = Intent.ACTION_SEND
    if (mediaItem.isLocal) {
        type = "audio/*"
        putExtra(
            Intent.EXTRA_STREAM,
            mediaItem.getUri()
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "https://music.youtube.com/watch?v=${mediaItem.mediaId}"
        )
    }
}
