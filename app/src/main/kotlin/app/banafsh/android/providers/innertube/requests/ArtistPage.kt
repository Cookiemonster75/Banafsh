package app.banafsh.android.providers.innertube.requests

import app.banafsh.android.providers.common.runCatchingCancellable
import app.banafsh.android.providers.innertube.Innertube
import app.banafsh.android.providers.innertube.models.BrowseResponse
import app.banafsh.android.providers.innertube.models.Context
import app.banafsh.android.providers.innertube.models.MusicCarouselShelfRenderer
import app.banafsh.android.providers.innertube.models.MusicShelfRenderer
import app.banafsh.android.providers.innertube.models.bodies.BrowseBody
import app.banafsh.android.providers.innertube.utils.findSectionByTitle
import app.banafsh.android.providers.innertube.utils.from
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext

suspend fun Innertube.artistPage(body: BrowseBody) =
    runCatchingCancellable {
        val ctx = currentCoroutineContext()
        val response = client.post(BROWSE) {
            setBody(body)
            mask("contents,header")
        }.body<BrowseResponse>()

        val responseNoLang by lazy {
            CoroutineScope(ctx).async(start = CoroutineStart.LAZY) {
                client.post(BROWSE) {
                    setBody(body.copy(context = Context.DefaultWebNoLang))
                    mask("contents,header")
                }.body<BrowseResponse>()
            }
        }

        suspend fun findSectionByTitle(text: String) = response
            .contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.get(0)
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.findSectionByTitle(text) ?: responseNoLang.await()
            .contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.get(0)
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.findSectionByTitle(text)

        val songsSection = findSectionByTitle("Songs")?.musicShelfRenderer
        val albumsSection = findSectionByTitle("Albums")?.musicCarouselShelfRenderer
        val singlesSection = findSectionByTitle("Singles")?.musicCarouselShelfRenderer

        Innertube.ArtistPage(
            name = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.title
                ?.text,
            description = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.description
                ?.text,
            thumbnail = (
                response
                    .header
                    ?.musicImmersiveHeaderRenderer
                    ?.foregroundThumbnail
                    ?: response
                        .header
                        ?.musicImmersiveHeaderRenderer
                        ?.thumbnail
                )
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                ?.getOrNull(0),
            shuffleEndpoint = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.playButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.watchEndpoint,
            radioEndpoint = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.startRadioButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.watchEndpoint,
            songs = songsSection
                ?.contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull { Innertube.SongItem.from(it) },
            songsEndpoint = songsSection
                ?.bottomEndpoint
                ?.browseEndpoint,
            albums = albumsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull { Innertube.AlbumItem.from(it) },
            albumsEndpoint = albumsSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            singles = singlesSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull { Innertube.AlbumItem.from(it) },
            singlesEndpoint = singlesSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            subscribersCountText = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.subscriptionButton
                ?.subscribeButtonRenderer
                ?.subscriberCountText
                ?.text
        )
    }
