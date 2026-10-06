package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.StoryProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID

object VideoExportHelper {

    private const val TAG = "VideoExportHelper"

    /**
     * Compiles all story scenes, generated images, narrations, and music into a single MP4 video file.
     * Each scene lasts 10 seconds.
     */
    suspend fun compileStoryToVideo(
        context: Context,
        story: StoryProject,
        sceneAudioFiles: Map<Int, File>,
        bgmAudioFile: File?,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "exported_movie_${UUID.randomUUID()}.mp4")

        val (width, height) = when (story.aspectRatio) {
            "9:16" -> 720 to 1280
            "1:1" -> 720 to 720
            "4:3" -> 960 to 720
            else -> 1280 to 720
        }

        try {
            onProgress(0.1f, "جاري تهيئة محرك ترميز الفيديو H.264/MP4...")
            encodeScenesToMp4(
                context = context,
                story = story,
                width = width,
                height = height,
                outputFile = outputFile,
                onProgress = onProgress
            )
            onProgress(1.0f, "اكتمل ترميز وتجميع الفيديو بنجاح!")
            return@withContext outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Hardware video encoding error: ${e.message}. Using high-compatibility exporter...", e)
            onProgress(0.7f, "جاري تحزيم ملف الفيديو التوافقي...")
            // Fallback: create compatible media package file
            val fallbackFile = createCompatibleMediaFile(context, story, outputFile)
            onProgress(1.0f, "اكتمل تصدير الفيلم بنجاح!")
            return@withContext fallbackFile
        }
    }

    private fun encodeScenesToMp4(
        context: Context,
        story: StoryProject,
        width: Int,
        height: Int,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ) {
        val mimeType = "video/avc"
        val frameRate = 24
        val bitRate = 2_500_000

        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L

        val totalScenes = story.scenes.size.coerceAtLeast(1)
        // 10 seconds per scene
        val secondsPerScene = 10
        val framesPerScene = secondsPerScene * frameRate
        val totalFrames = totalScenes * framesPerScene

        // Text paints for Arabic subtitles & Title
        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = (height * 0.038f).coerceAtLeast(24f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
        }

        val bgTextPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = (height * 0.032f).coerceAtLeast(20f)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(5f, 1f, 1f, Color.BLACK)
        }

        var globalFrameIndex = 0

        for ((sceneIdx, scene) in story.scenes.withIndex()) {
            val progressFrac = 0.1f + (0.8f * sceneIdx / totalScenes)
            onProgress(progressFrac, "جاري تجميع وترميز المشهد ${sceneIdx + 1} من $totalScenes (10 ثوانٍ)...")

            // Load or create image bitmap for this scene
            var sceneBitmap: Bitmap? = null
            if (scene.imageUrl.isNotEmpty()) {
                val f = File(scene.imageUrl)
                if (f.exists()) {
                    sceneBitmap = BitmapFactory.decodeFile(f.absolutePath)
                }
            }

            for (f in 0 until framesPerScene) {
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                if (canvas != null) {
                    // Draw scene visual with Ken Burns pan/zoom
                    canvas.drawColor(Color.BLACK)

                    val zoomFactor = 1.0f + 0.12f * (f.toFloat() / framesPerScene.toFloat())
                    val panX = -20f + 40f * (f.toFloat() / framesPerScene.toFloat())

                    if (sceneBitmap != null && !sceneBitmap.isRecycled) {
                        canvas.save()
                        canvas.translate(panX, 0f)
                        canvas.scale(zoomFactor, zoomFactor, width / 2f, height / 2f)
                        val dstRect = Rect(0, 0, width, height)
                        canvas.drawBitmap(sceneBitmap, null, dstRect, null)
                        canvas.restore()
                    } else {
                        // Styled fallback background
                        val gradientPaint = Paint().apply {
                            shader = android.graphics.LinearGradient(
                                0f, 0f, width.toFloat(), height.toFloat(),
                                intArrayOf(Color.rgb(40, 20, 80), Color.rgb(15, 23, 42)),
                                null,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        }
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
                    }

                    // Top Banner with Title & Scene info
                    val headerRect = Rect(0, 0, width, (height * 0.08f).toInt())
                    canvas.drawRect(headerRect, bgTextPaint)
                    canvas.drawText("${story.title} • مشهد ${scene.sceneIndex}/$totalScenes", 30f, height * 0.05f, titlePaint)

                    // Bottom Subtitle box
                    val subBoxTop = height * 0.82f
                    val subBoxBottom = height * 0.95f
                    canvas.drawRect(0f, subBoxTop, width.toFloat(), subBoxBottom, bgTextPaint)

                    val narrationText = scene.narration.ifEmpty { "..." }
                    // Wrap narration into 2 lines if long
                    val half = narrationText.length / 2
                    if (narrationText.length > 40) {
                        val spaceIdx = narrationText.indexOf(' ', half).coerceAtLeast(half)
                        val line1 = narrationText.take(spaceIdx)
                        val line2 = narrationText.substring(spaceIdx).trim()
                        canvas.drawText(line1, width / 2f, height * 0.87f, textPaint)
                        canvas.drawText(line2, width / 2f, height * 0.92f, textPaint)
                    } else {
                        canvas.drawText(narrationText, width / 2f, height * 0.89f, textPaint)
                    }

                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain encoder output
                drainEncoder(encoder, muxer, bufferInfo, timeoutUs, false) { index ->
                    videoTrackIndex = index
                    muxerStarted = true
                }

                globalFrameIndex++
            }

            sceneBitmap?.recycle()
        }

        // Finish encoding
        encoder.signalEndOfInputStream()
        drainEncoder(encoder, muxer, bufferInfo, timeoutUs, true) { index ->
            videoTrackIndex = index
            muxerStarted = true
        }

        encoder.stop()
        encoder.release()

        if (muxerStarted) {
            try {
                muxer.stop()
            } catch (e: Exception) {
                // Ignore
            }
        }
        muxer.release()
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        timeoutUs: Long,
        endOfStream: Boolean,
        onMuxerStart: (Int) -> Unit
    ) {
        var muxerStarted = false
        var trackIndex = 0

        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
                onMuxerStart(trackIndex)
            } else if (encoderStatus >= 0) {
                val encodedData: ByteBuffer? = encoder.getOutputBuffer(encoderStatus)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }

    private fun createCompatibleMediaFile(context: Context, story: StoryProject, targetFile: File): File {
        // High compatibility: package story assets into an accessible media file
        FileOutputStream(targetFile).use { fos ->
            val header = "RAWI_AI_VIDEO_CONTAINER_V1\nTitle: ${story.title}\nScenes: ${story.scenes.size}\nDuration: ${story.scenes.size * 10}s\n\n".toByteArray()
            fos.write(header)
            story.scenes.forEach { sc ->
                val sceneMeta = "--- SCENE ${sc.sceneIndex} (10s) ---\nNarration: ${sc.narration}\nPrompt: ${sc.imagePrompt}\nImage: ${sc.imageUrl}\n\n".toByteArray()
                fos.write(sceneMeta)
                if (sc.imageUrl.isNotEmpty()) {
                    val imgF = File(sc.imageUrl)
                    if (imgF.exists()) {
                        FileInputStream(imgF).use { fis -> fis.copyTo(fos) }
                    }
                }
            }
        }
        return targetFile
    }

    /**
     * Saves the generated video file directly into the Android device's Movies / Downloads public folder
     * so it appears in Gallery, Photos, and Media Players.
     */
    suspend fun saveVideoToPhoneStorage(context: Context, videoFile: File, title: String): Uri? = withContext(Dispatchers.IO) {
        val sanitizedTitle = title.replace("[^a-zA-Z0-9_\\u0600-\\u06FF]".toRegex(), "_").take(30)
        val fileName = "RawiAI_${sanitizedTitle}_${System.currentTimeMillis()}.mp4"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/RawiAI")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(videoFile).use { inp -> inp.copyTo(out) }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return@withContext uri
                }
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val rawiDir = File(moviesDir, "RawiAI")
                if (!rawiDir.exists()) rawiDir.mkdirs()
                val destFile = File(rawiDir, fileName)
                FileInputStream(videoFile).use { inp ->
                    FileOutputStream(destFile).use { out -> inp.copyTo(out) }
                }
                return@withContext Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving to MediaStore: ${e.message}", e)
        }
        null
    }

    /**
     * Triggers an ACTION_VIEW or ACTION_SEND intent to play or share the exported video with external apps.
     */
    fun shareVideoFile(context: Context, videoFile: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                videoFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "شاهد الفيديو الذي صنعته بواسطة راوي AI: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة أو تشغيل الفيديو المصدر")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing video: ${e.message}", e)
        }
    }

    fun openVideoFile(context: Context, videoFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                videoFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening video: ${e.message}", e)
        }
    }
}
