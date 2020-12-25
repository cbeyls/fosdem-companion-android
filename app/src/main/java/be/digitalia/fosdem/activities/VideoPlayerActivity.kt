package be.digitalia.fosdem.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import be.digitalia.fosdem.R
import be.digitalia.fosdem.utils.network.HttpUtils
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.extractor.ogg.OggExtractor
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.DrmFreeProgressiveMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer

class VideoPlayerActivity : AppCompatActivity(R.layout.player) {

    private val player: SimpleExoPlayer by lazy(LazyThreadSafetyMode.NONE) {
        createSimpleExoPlayer(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerView: PlayerView = findViewById(R.id.player_view)

        val uri = intent.getStringExtra(EXTRA_URI)!!
        val mediaItem: MediaItem = MediaItem.fromUri(uri)
        with(player) {
            playerView.player = this
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URI = "uri"

        private fun createRenderersFactory(context: Context): RenderersFactory {
            return RenderersFactory { eventHandler, videoRendererEventListener, audioRendererEventListener, _, _ ->
                arrayOf(
                        MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener),
                        MediaCodecVideoRenderer(context, MediaCodecSelector.DEFAULT,
                                DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS, eventHandler,
                                videoRendererEventListener, 50)
                )
            }
        }

        private fun createDataSourceFactory(): DataSource.Factory {
            return OkHttpDataSourceFactory(HttpUtils.deferringCallFactory) // Provide support for HTTP data sources only
        }

        private fun createExtractorsFactory(): ExtractorsFactory {
            return ExtractorsFactory { arrayOf(Mp4Extractor(), OggExtractor(), MatroskaExtractor()) }
        }

        private fun createMediaSourceFactory(dataSourceFactory: DataSource.Factory, extractorsFactory: ExtractorsFactory): MediaSourceFactory {
            return DrmFreeProgressiveMediaSourceFactory(dataSourceFactory, extractorsFactory)
        }

        fun createSimpleExoPlayer(context: Context): SimpleExoPlayer {
            return SimpleExoPlayer.Builder(context,
                    createRenderersFactory(context),
                    DefaultTrackSelector(context),
                    createMediaSourceFactory(createDataSourceFactory(), createExtractorsFactory()),
                    DefaultLoadControl(),
                    DefaultBandwidthMeter.getSingletonInstance(context),
                    AnalyticsCollector(Clock.DEFAULT)
            ).build()
        }
    }
}