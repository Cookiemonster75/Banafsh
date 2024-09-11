package app.banafsh.android.providers.innertube.requests

import app.banafsh.android.providers.common.runCatchingCancellable
import app.banafsh.android.providers.innertube.Innertube
import app.banafsh.android.providers.innertube.models.BrowseResponse
import app.banafsh.android.providers.innertube.models.ContinuationResponse
import app.banafsh.android.providers.innertube.models.MusicCarouselShelfRenderer
import app.banafsh.android.providers.innertube.models.MusicShelfRenderer
import app.banafsh.android.providers.innertube.models.bodies.BrowseBody
import app.banafsh.android.providers.innertube.models.bodies.ContinuationBody
import app.banafsh.android.providers.innertube.utils.from
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.playlistPage(body: BrowseBody) =
    runCatchingCancellable {
        val response = client.post(BROWSE) {
            setBody(body)
            body.context.apply()
        }.body<BrowseResponse>()

        val musicDetailHeaderRenderer = response
            .header
            ?.musicDetailHeaderRenderer

        val sectionListRendererContents = response
            .contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents

        val musicShelfRenderer = sectionListRendererContents
            ?.firstOrNull()
            ?.musicShelfRenderer

        val musicCarouselShelfRenderer = sectionListRendererContents
            ?.getOrNull(1)
            ?.musicCarouselShelfRenderer

        Innertube.PlaylistOrAlbumPage(
            title = musicDetailHeaderRenderer
                ?.title
                ?.text,
            description = musicDetailHeaderRenderer
                ?.description
                ?.text,
            thumbnail = musicDetailHeaderRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                ?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) },
            authors = musicDetailHeaderRenderer
                ?.subtitle
                ?.splitBySeparator()
                ?.getOrNull(1)
                ?.map(Innertube::Info),
            year = musicDetailHeaderRenderer
                ?.subtitle
                ?.splitBySeparator()
                ?.getOrNull(2)
                ?.firstOrNull()
                ?.text,
            url = response
                .microformat
                ?.microformatDataRenderer
                ?.urlCanonical,
            songsPage = musicShelfRenderer
                ?.toSongsPage(),
            otherVersions = musicCarouselShelfRenderer
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull { Innertube.AlbumItem.from(it) },
            otherInfo = musicDetailHeaderRenderer
                ?.secondSubtitle
                ?.text
        )
    }

suspend fun Innertube.playlistPage(body: ContinuationBody) =
    runCatchingCancellable {
        val response = client.post(BROWSE) {
            setBody(body)
            parameter("continuation", body.continuation)
            parameter("ctoken", body.continuation)
            parameter("type", "next")
            body.context.apply()
        }.body<ContinuationResponse>()

        response
            .continuationContents
            ?.musicShelfContinuation
            ?.toSongsPage()
    }

private fun MusicShelfRenderer?.toSongsPage() = Innertube.ItemsPage(
    items = this
        ?.contents
        ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
        ?.mapNotNull { Innertube.SongItem.from(it) },
    continuation = this
        ?.continuations
        ?.firstOrNull()
        ?.nextContinuationData
        ?.continuation
)
