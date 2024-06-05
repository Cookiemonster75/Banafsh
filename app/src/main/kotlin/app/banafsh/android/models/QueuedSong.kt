package app.banafsh.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
class QueuedSong(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    @Embedded val song: Song,
    var position: Long?
)
