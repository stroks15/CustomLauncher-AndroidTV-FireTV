package com.tuusuario.customlauncher.player

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tuusuario.customlauncher.R

/** Reproductor nativo a pantalla completa. Acepta URL HTTP(S) configurada por el usuario. */
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.player_view)

        val rawUrl = intent.getStringExtra(EXTRA_STREAM_URL)?.trim().orEmpty()
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) {
            Toast.makeText(this, "Configura una URL HLS/DASH HTTP(S) autorizada para este contenido", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val exo = CustomTvPlayer.createOptimizedPlayer(this)
        player = exo
        playerView.player = exo
        playerView.keepScreenOn = true
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, "No se pudo reproducir el stream: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
            }
        })
        exo.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(title).build())
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        if (::playerView.isInitialized) playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_STREAM_URL = "com.tuusuario.customlauncher.extra.STREAM_URL"
        const val EXTRA_TITLE = "com.tuusuario.customlauncher.extra.TITLE"
    }
}
