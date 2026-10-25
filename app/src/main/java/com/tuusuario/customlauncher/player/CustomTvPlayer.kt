package com.tuusuario.customlauncher.player

import android.content.Context
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Configuración Media3 para HLS/DASH. El retardo real depende del manifiesto, segmentos,
 * CDN, DRM y red; estos valores no garantizan cero latencia.
 */
object CustomTvPlayer {
    fun createOptimizedPlayer(context: Context): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 30_000, 1_000, 1_500)
            .build()
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(5_000)
            .setReadTimeoutMs(5_000)
            .setUserAgent("CustomLauncherTV/2.0 (Android TV)")
            .setAllowCrossProtocolRedirects(true)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }
}
