package app.banafsh.android.ui.screens.localplaylist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.banafsh.android.LocalPlayerAwareWindowInsets
import app.banafsh.android.LocalPlayerServiceBinder
import app.banafsh.android.R
import app.banafsh.android.data.models.Playlist
import app.banafsh.android.data.models.Song
import app.banafsh.android.data.models.SongPlaylistMap
import app.banafsh.android.db.Database
import app.banafsh.android.db.query
import app.banafsh.android.db.transaction
import app.banafsh.android.providers.innertube.Innertube
import app.banafsh.android.providers.innertube.models.bodies.BrowseBody
import app.banafsh.android.providers.innertube.requests.playlistPage
import app.banafsh.android.ui.components.LocalMenuState
import app.banafsh.android.ui.components.themed.ConfirmationDialog
import app.banafsh.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.banafsh.android.ui.components.themed.Header
import app.banafsh.android.ui.components.themed.HeaderIconButton
import app.banafsh.android.ui.components.themed.InPlaylistMediaItemMenu
import app.banafsh.android.ui.components.themed.LayoutWithAdaptiveThumbnail
import app.banafsh.android.ui.components.themed.Menu
import app.banafsh.android.ui.components.themed.MenuEntry
import app.banafsh.android.ui.components.themed.ReorderHandle
import app.banafsh.android.ui.components.themed.SecondaryTextButton
import app.banafsh.android.ui.components.themed.TextFieldDialog
import app.banafsh.android.ui.items.SongItem
import app.banafsh.android.ui.reordering.animateItemPlacement
import app.banafsh.android.ui.reordering.draggedItem
import app.banafsh.android.ui.reordering.rememberReorderingState
import app.banafsh.android.ui.theme.Dimensions
import app.banafsh.android.ui.theme.LocalAppearance
import app.banafsh.android.ui.theme.utils.isLandscape
import app.banafsh.android.utils.PlaylistDownloadIcon
import app.banafsh.android.utils.asMediaItem
import app.banafsh.android.utils.completed
import app.banafsh.android.utils.enqueue
import app.banafsh.android.utils.forcePlayAtIndex
import app.banafsh.android.utils.forcePlayFromBeginning
import app.banafsh.android.utils.launchYouTubeMusic
import app.banafsh.android.utils.toast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalPlaylistSongs(
    playlist: Playlist,
    songs: ImmutableList<Song>,
    onDelete: () -> Unit,
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) = LayoutWithAdaptiveThumbnail(
    thumbnailContent = thumbnailContent,
    modifier = modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()

    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = songs,
        onDragEnd = { fromIndex, toIndex ->
            query {
                Database.move(playlist.id, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    var isRenaming by rememberSaveable { mutableStateOf(false) }

    if (isRenaming)
        TextFieldDialog(
            hintText = stringResource(R.string.enter_playlist_name_prompt),
            initialTextInput = playlist.name,
            onDismiss = { isRenaming = false },
            onAccept = { text ->
                query {
                    Database.update(playlist.copy(name = text))
                }
            }
        )

    var isDeleting by rememberSaveable { mutableStateOf(false) }

    if (isDeleting)
        ConfirmationDialog(
            text = stringResource(R.string.confirm_delete_playlist),
            onDismiss = { isDeleting = false },
            onConfirm = {
                query {
                    Database.delete(playlist)
                }
                onDelete()
            }
        )

    Box {
        LookaheadScope {
            LazyColumn(
                state = reorderingState.lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.surface)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Header(
                            title = playlist.name,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            SecondaryTextButton(
                                text = stringResource(R.string.enqueue),
                                enabled = songs.isNotEmpty(),
                                onClick = {
                                    binder?.player?.enqueue(songs.map { it.asMediaItem })
                                }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            PlaylistDownloadIcon(
                                songs = songs.map { it.asMediaItem }.toImmutableList()
                            )

                            HeaderIconButton(
                                icon = R.drawable.ellipsis_horizontal,
                                color = colorPalette.text,
                                onClick = {
                                    menuState.display {
                                        Menu {
                                            playlist.browseId?.let { browseId ->
                                                MenuEntry(
                                                    icon = R.drawable.sync,
                                                    text = stringResource(R.string.sync),
                                                    onClick = {
                                                        menuState.hide()
                                                        transaction {
                                                            runBlocking(Dispatchers.IO) {
                                                                Innertube.playlistPage(
                                                                    BrowseBody(browseId = browseId)
                                                                )?.completed()
                                                            }?.getOrNull()?.let { remotePlaylist ->
                                                                Database.clearPlaylist(playlist.id)

                                                                remotePlaylist.songsPage
                                                                    ?.items
                                                                    ?.map { it.asMediaItem }
                                                                    ?.onEach { Database.insert(it) }
                                                                    ?.mapIndexed { position, mediaItem ->
                                                                        SongPlaylistMap(
                                                                            songId = mediaItem.mediaId,
                                                                            playlistId = playlist.id,
                                                                            position = position
                                                                        )
                                                                    }
                                                                    ?.let(Database::insertSongPlaylistMaps)
                                                            }
                                                        }
                                                    }
                                                )

                                                songs.firstOrNull()?.id?.let { firstSongId ->
                                                    MenuEntry(
                                                        icon = R.drawable.play,
                                                        text = stringResource(R.string.watch_playlist_on_youtube),
                                                        onClick = {
                                                            menuState.hide()
                                                            binder?.player?.pause()
                                                            uriHandler.openUri(
                                                                "https://youtube.com/watch?v=$firstSongId&list=${
                                                                    playlist.browseId.drop(2)
                                                                }"
                                                            )
                                                        }
                                                    )

                                                    MenuEntry(
                                                        icon = R.drawable.musical_notes,
                                                        text = stringResource(R.string.open_in_youtube_music),
                                                        onClick = {
                                                            menuState.hide()
                                                            binder?.player?.pause()
                                                            if (
                                                                !launchYouTubeMusic(
                                                                    context = context,
                                                                    endpoint = "watch?v=$firstSongId&list=${
                                                                        playlist.browseId.drop(2)
                                                                    }"
                                                                )
                                                            ) context.toast(
                                                                context.getString(R.string.youtube_music_not_installed)
                                                            )
                                                        }
                                                    )
                                                }
                                            }

                                            MenuEntry(
                                                icon = R.drawable.pencil,
                                                text = stringResource(R.string.rename),
                                                onClick = {
                                                    menuState.hide()
                                                    isRenaming = true
                                                }
                                            )

                                            MenuEntry(
                                                icon = R.drawable.trash,
                                                text = stringResource(R.string.delete),
                                                onClick = {
                                                    menuState.hide()
                                                    isDeleting = true
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        if (!isLandscape) thumbnailContent()
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id },
                    contentType = { _, song -> song }
                ) { index, song ->
                    SongItem(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        InPlaylistMediaItemMenu(
                                            playlistId = playlist.id,
                                            positionInPlaylist = index,
                                            song = song,
                                            onDismiss = menuState::hide
                                        )
                                    }
                                },
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlayAtIndex(
                                        items = songs.map { it.asMediaItem },
                                        index = index
                                    )
                                }
                            )
                            .animateItemPlacement(reorderingState)
                            .draggedItem(
                                reorderingState = reorderingState,
                                index = index
                            )
                            .background(colorPalette.surface),
                        song = song,
                        thumbnailSize = Dimensions.thumbnails.song,
                        trailingContent = {
                            ReorderHandle(
                                reorderingState = reorderingState,
                                index = index
                            )
                        }
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            visible = !reorderingState.isDragging,
            onClick = {
                if (songs.isEmpty()) return@FloatingActionsContainerWithScrollToTop

                binder?.stopRadio()
                binder?.player?.forcePlayFromBeginning(
                    songs.shuffled().map { it.asMediaItem }
                )
            }
        )
    }
}
