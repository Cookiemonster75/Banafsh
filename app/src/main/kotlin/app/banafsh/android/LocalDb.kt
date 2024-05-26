package app.banafsh.android

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import app.banafsh.android.lib.core.data.enums.SongSortBy
import app.banafsh.android.lib.core.data.enums.SortOrder
import app.banafsh.android.models.LocalSong
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDB {
    companion object : LocalDB by LocalDBInitializer.instance.database

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(song: LocalSong): Long

    @Transaction
    @Query("SELECT * FROM LocalSong ORDER BY title ASC")
    @RewriteQueriesToDropUnusedColumns
    fun localSongsByTitleAsc(): Flow<List<LocalSong>>

    @Transaction
    @Query("SELECT * FROM LocalSong ORDER BY title DESC")
    @RewriteQueriesToDropUnusedColumns
    fun localSongsByTitleDesc(): Flow<List<LocalSong>>

    @Transaction
    @Query("SELECT * FROM LocalSong ORDER BY dateModified ASC")
    @RewriteQueriesToDropUnusedColumns
    fun localSongsByDateAsc(): Flow<List<LocalSong>>

    @Transaction
    @Query("SELECT * FROM LocalSong ORDER BY dateModified DESC")
    @RewriteQueriesToDropUnusedColumns
    fun localSongsByDateDesc(): Flow<List<LocalSong>>

    @Transaction
    @Query("SELECT * FROM LocalSong ORDER BY totalPlayTimeMs ASC")
    @RewriteQueriesToDropUnusedColumns
    fun localSongsByPlayTimeAsc(): Flow<List<LocalSong>>

    @Transaction
    @Query("SELECT * FROM LocalSong ORDER BY totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun localSongsByPlayTimeDesc(): Flow<List<LocalSong>>

    fun localSongs(sortBy: SongSortBy, sortOrder: SortOrder) = when (sortBy) {
        SongSortBy.Title -> when (sortOrder) {
            SortOrder.Ascending -> localSongsByTitleAsc()
            SortOrder.Descending -> localSongsByTitleDesc()
        }

        SongSortBy.DateAdded -> when (sortOrder) {
            SortOrder.Ascending -> localSongsByDateAsc()
            SortOrder.Descending -> localSongsByDateDesc()
        }

        SongSortBy.PlayTime -> when (sortOrder) {
            SortOrder.Ascending -> localSongsByPlayTimeAsc()
            SortOrder.Descending -> localSongsByPlayTimeDesc()
        }
    }

    @Query("SELECT * FROM LocalSong WHERE title LIKE :query OR artistsText LIKE :query")
    fun search(query: String): Flow<List<LocalSong>>

    @Delete
    fun delete(song: LocalSong)

}

@androidx.room.Database(
    entities = [LocalSong::class],
    version = 1,
    exportSchema = true
)
abstract class LocalDBInitializer protected constructor() : RoomDatabase() {
    abstract val database: LocalDB

    companion object {
        @Volatile
        lateinit var instance: LocalDBInitializer

        private fun buildDatabase() = Room
            .inMemoryDatabaseBuilder(
                context = Dependencies.application.applicationContext,
                klass = LocalDBInitializer::class.java,
            ).build()

        operator fun invoke() {
            if (!::instance.isInitialized) reload()
        }

        fun reload() = synchronized(this) {
            instance = buildDatabase()
        }
    }
}
