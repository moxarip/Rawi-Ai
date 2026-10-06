package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying: Boolean = false

    fun playFile(file: File, onComplete: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioPlayerHelper.isPlaying = false
                    onComplete()
                }
                start()
                this@AudioPlayerHelper.isPlaying = true
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to play audio file: ${e.message}", e)
            isPlaying = false
            onComplete()
        }
    }

    fun playUri(uri: Uri, onComplete: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                setOnCompletionListener {
                    this@AudioPlayerHelper.isPlaying = false
                    onComplete()
                }
                start()
                this@AudioPlayerHelper.isPlaying = true
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Failed to play audio uri: ${e.message}", e)
            isPlaying = false
            onComplete()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }
}
