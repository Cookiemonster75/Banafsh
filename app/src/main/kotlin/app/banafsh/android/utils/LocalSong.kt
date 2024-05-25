package app.banafsh.android.utils

import app.banafsh.android.models.LocalSong
import app.banafsh.android.models.Song

fun LocalSong.toSong() = Song(
    id = id,
    title = title,
    artistsText = artistsText,
    durationText = durationText,
    thumbnailUrl = thumbnailUrl
)
