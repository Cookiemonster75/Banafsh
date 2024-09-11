package app.banafsh.android.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import app.banafsh.android.Dependencies
import app.banafsh.android.data.enums.SongSortBy
import app.banafsh.android.data.enums.SortOrder
import app.banafsh.android.data.models.Song
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Dao
interface TempDatabase {
    companion object : TempDatabase by TempDatabaseInitializer.instance.database

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY title ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY title DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleDesc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY dateModified ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDateAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY dateModified DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByDateDesc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY totalPlayTimeMs ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM Song WHERE id LIKE '$LOCAL_KEY_PREFIX%' ORDER BY totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeDesc(): Flow<List<Song>>

    fun songs(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<Song>> {
        val tempSongs = when (sortBy) {
            SongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsByPlayTimeAsc()
                SortOrder.Descending -> songsByPlayTimeDesc()
            }

            SongSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> songsByTitleAsc()
                SortOrder.Descending -> songsByTitleDesc()
            }

            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsByDateAsc()
                SortOrder.Descending -> songsByDateDesc()
            }
        }

        return tempSongs
            .distinctUntilChanged()
            .map { tempSongsList ->
                val songs = mutableListOf<Song>()
                query {
                    songs.addAll(Database.songs(tempSongsList.map { it.id }))
                }
                val songIds = songs.map { it.id }
                songs + tempSongsList.filter { it.id !in songIds }
            }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(song: Song): Long

    @Delete
    fun delete(song: Song)
}

@androidx.room.Database(
    entities = [
        Song::class
    ],
    version = 1
)
abstract class TempDatabaseInitializer protected constructor() : RoomDatabase() {
    abstract val database: TempDatabase

    companion object {
        @Volatile
        lateinit var instance: TempDatabaseInitializer

        private fun buildDatabase() = Room
            .inMemoryDatabaseBuilder(
                context = Dependencies.application.applicationContext,
                klass = TempDatabaseInitializer::class.java
            )
            .build()

        operator fun invoke() {
            if (!Companion::instance.isInitialized) reload()
        }

        fun reload() = synchronized(this) {
            instance = buildDatabase()
        }
    }
}

fun queryTemp(block: () -> Unit) =
    TempDatabaseInitializer.instance.queryExecutor.execute(block)

fun transactionTemp(block: () -> Unit) = with(TempDatabaseInitializer.instance) {
    transactionExecutor.execute {
        runInTransaction(block)
    }
}
