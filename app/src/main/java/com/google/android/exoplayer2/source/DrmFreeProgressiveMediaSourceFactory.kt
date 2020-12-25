package com.google.android.exoplayer2.source

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy

class DrmFreeProgressiveMediaSourceFactory(private val dataSourceFactory: DataSource.Factory,
                                           private val extractorsFactory: ExtractorsFactory) : MediaSourceFactory {
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()
    var continueLoadingCheckIntervalBytes: Int = ProgressiveMediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES

    override fun setDrmSessionManager(drmSessionManager: DrmSessionManager?) = this

    override fun setDrmHttpDataSourceFactory(drmHttpDataSourceFactory: HttpDataSource.Factory?) = this

    override fun setDrmUserAgent(userAgent: String?) = this

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy?): DrmFreeProgressiveMediaSourceFactory {
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy ?: DefaultLoadErrorHandlingPolicy()
        return this
    }

    override fun getSupportedTypes() = intArrayOf(C.TYPE_OTHER)

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        return ProgressiveMediaSource(
                mediaItem,
                dataSourceFactory,
                extractorsFactory,
                DrmSessionManager.DUMMY,
                loadErrorHandlingPolicy,
                continueLoadingCheckIntervalBytes)
    }
}