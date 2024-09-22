package app.banafsh.android.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.Player
import app.banafsh.android.R
import app.banafsh.android.data.models.Info
import app.banafsh.android.data.models.Song
import app.banafsh.android.data.models.ui.UiMedia
import app.banafsh.android.db.Database
import app.banafsh.android.db.query
import app.banafsh.android.preferences.PlayerPreferences
import app.banafsh.android.service.PlayerService
import app.banafsh.android.ui.components.FadingRow
import app.banafsh.android.ui.components.SeekBar
import app.banafsh.android.ui.components.themed.IconButton
import app.banafsh.android.ui.screens.artistRoute
import app.banafsh.android.ui.theme.LocalAppearance
import app.banafsh.android.ui.theme.favoritesIcon
import app.banafsh.android.ui.theme.utils.px
import app.banafsh.android.ui.theme.utils.roundedShape
import app.banafsh.android.utils.bold
import app.banafsh.android.utils.forceSeekToNext
import app.banafsh.android.utils.forceSeekToPrevious
import app.banafsh.android.utils.secondary
import app.banafsh.android.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
fun Controls(
    media: UiMedia,
    binder: PlayerService.Binder,
    shouldBePlaying: Boolean,
    position: Long,
    modifier: Modifier = Modifier
) = with(PlayerPreferences) {
    val (colorPalette, typography) = LocalAppearance.current

    var artistInfo: List<Info>? by remember { mutableStateOf(null) }
    var maxHeight by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(media) {
        withContext(Dispatchers.IO) {
            artistInfo = Database
                .songArtistInfo(media.id)
                .takeIf { it.isNotEmpty() }
        }
    }

    var likedAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(media) {
        Database
            .likedAt(media.id)
            .distinctUntilChanged()
            .collect { likedAt = it }
    }

    val shouldBePlayingTransition = updateTransition(
        targetState = shouldBePlaying,
        label = "shouldBePlaying"
    )

    val playButtonRadius by shouldBePlayingTransition.animateDp(
        transitionSpec = { tween(durationMillis = 100, easing = LinearEasing) },
        label = "playPauseRoundness",
        targetValueByState = { if (it) 16.dp else 32.dp }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = media.title,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 300, delayMillis = 300)
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                label = ""
            ) { title ->
                FadingRow(modifier = Modifier.fillMaxWidth(0.75f)) {
                    BasicText(
                        text = title,
                        style = typography.l.bold,
                        maxLines = 1
                    )
                }
            }

            AnimatedContent(
                targetState = artistInfo,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 300, delayMillis = 300)
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                label = ""
            ) { state ->
                state?.let { artists ->
                    FadingRow(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .heightIn(maxHeight.px.dp)
                    ) {
                        artists.fastForEachIndexed { i, artist ->
                            if (i == artists.lastIndex && artists.size > 1) BasicText(
                                text = " & ",
                                style = typography.s.semiBold.secondary
                            )
                            BasicText(
                                text = artist.name.orEmpty(),
                                style = typography.s.semiBold.secondary,
                                modifier = Modifier.clickable { artistRoute.global(artist.id) }
                            )
                            if (i != artists.lastIndex && i + 1 != artists.lastIndex) BasicText(
                                text = ", ",
                                style = typography.s.semiBold.secondary
                            )
                        }
                    }
                } ?: FadingRow(modifier = Modifier.fillMaxWidth(0.75f)) {
                    BasicText(
                        text = media.artist,
                        style = typography.s.semiBold.secondary,
                        maxLines = 1,
                        modifier = Modifier.onGloballyPositioned { maxHeight = it.size.height }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        SeekBar(
            binder = binder,
            position = position,
            media = media,
            alwaysShowDuration = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                color = colorPalette.favoritesIcon,
                onClick = {
                    val currentMediaItem = binder.player.currentMediaItem

                    query {
                        if (
                            Database.like(
                                media.id,
                                if (likedAt == null) System.currentTimeMillis() else null
                            ) == 0
                        ) {
                            currentMediaItem
                                ?.takeIf { it.mediaId == media.id }
                                ?.let {
                                    Database.insert(currentMediaItem, Song::toggleLike)
                                }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )

            IconButton(
                icon = R.drawable.play_skip_back,
                color = colorPalette.text,
                onClick = binder.player::forceSeekToPrevious,
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(playButtonRadius.roundedShape)
                    .clickable {
                        if (shouldBePlaying) binder.player.pause()
                        else {
                            if (binder.player.playbackState == Player.STATE_IDLE) binder.player.prepare()
                            binder.player.play()
                        }
                    }
                    .background(colorPalette.primaryContainer)
                    .size(64.dp)
            ) {
                AnimatedPlayPauseButton(
                    playing = shouldBePlaying,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                icon = R.drawable.play_skip_forward,
                color = colorPalette.text,
                onClick = binder.player::forceSeekToNext,
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )

            IconButton(
                icon = R.drawable.infinite,
                enabled = trackLoopEnabled,
                onClick = { trackLoopEnabled = !trackLoopEnabled },
                modifier = Modifier
                    .weight(1f)
                    .size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
