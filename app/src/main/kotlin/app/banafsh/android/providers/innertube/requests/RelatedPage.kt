package app.banafsh.android.providers.innertube.requests

import app.banafsh.android.providers.common.runCatchingCancellable
import app.banafsh.android.providers.innertube.Innertube
import app.banafsh.android.providers.innertube.models.BrowseResponse
import app.banafsh.android.providers.innertube.models.Context
import app.banafsh.android.providers.innertube.models.MusicCarouselShelfRenderer
import app.banafsh.android.providers.innertube.models.NextResponse
import app.banafsh.android.providers.innertube.models.bodies.BrowseBody
import app.banafsh.android.providers.innertube.models.bodies.NextBody
import app.banafsh.android.providers.innertube.utils.findSectionByStrapline
import app.banafsh.android.providers.innertube.utils.findSectionByTitle
import app.banafsh.android.providers.innertube.utils.from
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.relatedPage(body: NextBody) =
    runCatchingCancellable {
        val nextResponse = client.post(NEXT) {
            setBody(body.copy(context = Context.DefaultWebNoLang))
            @Suppress("ktlint:standard:max-line-length")
            mask(
                "contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)"
            )
        }.body<NextResponse>()

        val browseId = nextResponse
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs
            ?.getOrNull(2)
            ?.tabRenderer
            ?.endpoint
            ?.browseEndpoint
            ?.browseId
            ?: return@runCatchingCancellable null

        val response = client.post(BROWSE) {
            setBody(
                BrowseBody(
                    browseId = browseId,
                    context = Context.DefaultWebNoLang
                )
            )
            @Suppress("ktlint:standard:max-line-length")
            mask(
                "contents.sectionListRenderer.contents.musicCarouselShelfRenderer(header.musicCarouselShelfBasicHeaderRenderer(title,strapline),contents($MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK,$MUSIC_TWO_ROW_ITEM_RENDERER_MASK))"
            )
        }.body<BrowseResponse>()

        val sectionListRenderer = response
            .contents
            ?.sectionListRenderer

        Innertube.RelatedPage(
            songs = sectionListRenderer
                ?.findSectionByTitle("You might also like")
                ?.musicCarouselShelfRenderer
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull { Innertube.SongItem.from(it) },
            playlists = sectionListRenderer
                ?.findSectionByTitle("Recommended playlists")
                ?.musicCarouselShelfRenderer
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull { Innertube.PlaylistItem.from(it) }
                ?.sortedByDescending { it.channel?.name == "YouTube Music" },
            albums = sectionListRenderer
                ?.findSectionByStrapline("MORE FROM")
                ?.musicCarouselShelfRenderer
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull { Innertube.AlbumItem.from(it) },
            artists = sectionListRenderer
                ?.findSectionByTitle("Similar artists")
                ?.musicCarouselShelfRenderer
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull { Innertube.ArtistItem.from(it) }
        )
    }
