package app.banafsh.android.utils

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import app.banafsh.android.models.LocalSong
import app.banafsh.android.models.Song
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import app.banafsh.android.service.isLocal

fun LocalSong.toSong() = Song(
    id = id,
    title = title,
    artistsText = artistsText,
    durationText = durationText,
    thumbnailUrl = thumbnailUrl
)

fun MediaItem.getUri(): Uri {
    return if (isLocal) ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        mediaId.substringAfter(LOCAL_KEY_PREFIX).toLong()
    ) else mediaId.toUri()
}

fun createShareLocalSongIndent(mediaItem: MediaItem): Intent {
    return Intent().apply {
        action = Intent.ACTION_SEND
        type = "audio/*"
        putExtra(
            Intent.EXTRA_STREAM,
            mediaItem.getUri()
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
