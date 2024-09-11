package app.banafsh.android.providers.innertube.requests

import app.banafsh.android.providers.common.runCatchingCancellable
import app.banafsh.android.providers.innertube.Innertube
import app.banafsh.android.providers.innertube.models.ContinuationResponse
import app.banafsh.android.providers.innertube.models.NextResponse
import app.banafsh.android.providers.innertube.models.bodies.ContinuationBody
import app.banafsh.android.providers.innertube.models.bodies.NextBody
import app.banafsh.android.providers.innertube.utils.from
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.nextPage(body: NextBody): Result<Innertube.NextPage>? =
    runCatchingCancellable {
        val response = client.post(NEXT) {
            setBody(body)
            @Suppress("ktlint:standard:max-line-length")
            mask(
                "contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer.content.musicQueueRenderer.content.playlistPanelRenderer(continuations,contents(automixPreviewVideoRenderer,$PLAYLIST_PANEL_VIDEO_RENDERER_MASK))"
            )
        }.body<NextResponse>()

        val tabs = response
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs

        val playlistPanelRenderer = tabs
            ?.getOrNull(0)
            ?.tabRenderer
            ?.content
            ?.musicQueueRenderer
            ?.content
            ?.playlistPanelRenderer

        if (body.playlistId == null) {
            val endpoint = playlistPanelRenderer
                ?.contents
                ?.lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint

            if (endpoint != null) return nextPage(
                body.copy(
                    playlistId = endpoint.playlistId,
                    params = endpoint.params
                )
            )
        }

        Innertube.NextPage(
            playlistId = body.playlistId,
            playlistSetVideoId = body.playlistSetVideoId,
            params = body.params,
            itemsPage = playlistPanelRenderer
                ?.toSongsPage()
        )
    }

suspend fun Innertube.nextPage(body: ContinuationBody) =
    runCatchingCancellable {
        val response = client.post(NEXT) {
            setBody(body)
            @Suppress("ktlint:standard:max-line-length")
            mask(
                "continuationContents.playlistPanelContinuation(continuations,contents.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK)"
            )
        }.body<ContinuationResponse>()

        response
            .continuationContents
            ?.playlistPanelContinuation
            ?.toSongsPage()
    }

private fun NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer?.toSongsPage() =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(
                NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content
                ::playlistPanelVideoRenderer
            )?.mapNotNull { Innertube.SongItem.from(it) },
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )
