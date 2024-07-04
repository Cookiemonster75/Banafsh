package app.banafsh.android.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.banafsh.android.Database
import app.banafsh.android.R
import app.banafsh.android.TempDatabase
import app.banafsh.android.lib.core.ui.LocalAppearance
import app.banafsh.android.lib.core.ui.utils.isAtLeastAndroid13
import app.banafsh.android.lib.core.ui.utils.isCompositionLaunched
import app.banafsh.android.models.Song
import app.banafsh.android.preferences.OrderPreferences
import app.banafsh.android.service.LOCAL_KEY_PREFIX
import app.banafsh.android.transaction
import app.banafsh.android.transactionTemp
import app.banafsh.android.ui.components.themed.SecondaryTextButton
import app.banafsh.android.ui.screens.Route
import app.banafsh.android.utils.AudioMediaCursor
import app.banafsh.android.utils.hasPermission
import app.banafsh.android.utils.hasPermissions
import app.banafsh.android.utils.medium
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

private val readPermission = if (isAtLeastAndroid13) Manifest.permission.READ_MEDIA_AUDIO
else Manifest.permission.READ_EXTERNAL_STORAGE

private val permissions = arrayOf(readPermission, Manifest.permission.WRITE_EXTERNAL_STORAGE)

@Route
@Composable
fun HomeLocalSongs(onSearchClick: () -> Unit) = with(OrderPreferences) {
    val context = LocalContext.current
    val (_, typography) = LocalAppearance.current

    var hasPermission by remember(isCompositionLaunched()) {
        mutableStateOf(context.applicationContext.hasPermissions(permissions))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { hasPermission = it.values.all { it } }
    )

    LaunchedEffect(hasPermission) {
        if (hasPermission) context.musicFilesAsFlow().collect()
    }

    if (hasPermission)
        HomeSongs(
            onSearchClick = onSearchClick,
            songProvider = {
                TempDatabase.songs(
                    sortBy = localSongSortBy,
                    sortOrder = localSongSortOrder
                ).map { songs ->
                    songs.filter { it.durationText != "0:00" }
                }
            },
            sortBy = localSongSortBy,
            setSortBy = { localSongSortBy = it },
            sortOrder = localSongSortOrder,
            setSortOrder = { localSongSortOrder = it },
            title = stringResource(R.string.songs)
        )
    else {
        LaunchedEffect(Unit) { launcher.launch(permissions) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = stringResource(R.string.media_permission_declined),
                modifier = Modifier.fillMaxWidth(0.75f),
                style = typography.m.medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryTextButton(
                text = stringResource(R.string.open_settings),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            )
        }
    }
}

private val mediaScope = CoroutineScope(Dispatchers.IO + CoroutineName("MediaStore worker"))
fun Context.musicFilesAsFlow(): StateFlow<List<Song>> = flow {
    var version: String? = null

    while (currentCoroutineContext().isActive) {
        val newVersion = MediaStore.getVersion(applicationContext)

        if (version != newVersion) {
            version = newVersion

            AudioMediaCursor.query(contentResolver) {
                buildList {
                    while (next()) {
                        if (!isMusic || duration == 0) continue
                        add(
                            Song(
                                id = "$LOCAL_KEY_PREFIX$id",
                                title = title,
                                artistsText = artist,
                                durationText = duration.milliseconds.toComponents { minutes, seconds, _ ->
                                    "$minutes:${seconds.toString().padStart(2, '0')}"
                                },
                                dateModified = dateModified,
                                thumbnailUrl = albumUri.toString(),
                                path = path
                            )
                        )
                    }
                }
            }?.let { emit(it) }
        }
        delay(5.seconds)
    }
}.distinctUntilChanged()
    .onEach { songs ->
        transactionTemp { songs.forEach(TempDatabase::insert) }
        transaction { songs.forEach(Database::insert) }
    }
    .stateIn(mediaScope, SharingStarted.Eagerly, listOf())
