package app.banafsh.android.providers.innertube.requests

import app.banafsh.android.providers.common.runCatchingCancellable
import app.banafsh.android.providers.innertube.Innertube
import app.banafsh.android.providers.innertube.models.BrowseResponse
import app.banafsh.android.providers.innertube.models.NextResponse
import app.banafsh.android.providers.innertube.models.bodies.BrowseBody
import app.banafsh.android.providers.innertube.models.bodies.NextBody
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.lyrics(body: NextBody) =
    runCatchingCancellable {
        val nextResponse = client.post(NEXT) {
            setBody(body)
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
            ?.getOrNull(1)
            ?.tabRenderer
            ?.endpoint
            ?.browseEndpoint
            ?.browseId
            ?: return@runCatchingCancellable null

        val response = client.post(BROWSE) {
            setBody(BrowseBody(browseId = browseId))
            mask("contents.sectionListRenderer.contents.musicDescriptionShelfRenderer.description")
        }.body<BrowseResponse>()

        response.contents
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicDescriptionShelfRenderer
            ?.description
            ?.text
    }
