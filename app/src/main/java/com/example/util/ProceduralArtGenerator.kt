package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.random.Random

object ProceduralArtGenerator {

    /**
     * Generates a high quality cinematic concept art bitmap locally on device.
     * Guaranteed to work 100% offline without any API keys or network connection.
     */
    suspend fun generateArtwork(
        context: Context,
        prompt: String,
        style: String = "سينمائي",
        aspectRatio: String = "16:9",
        sceneIndex: Int = 1
    ): File = withContext(Dispatchers.IO) {
        val (width, height) = when (aspectRatio) {
            "9:16" -> 720 to 1280
            "1:1" -> 1024 to 1024
            "4:3" -> 1024 to 768
            "3:4" -> 768 to 1024
            else -> 1280 to 720
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val random = Random(prompt.hashCode() + sceneIndex * 31)

        // Color palettes based on style
        val (bgTop, bgMid, bgBottom, accentColor) = when {
            style.contains("أنمي") || style.contains("خيالي") -> {
                listOf(
                    Color.rgb(40, 20, 80),
                    Color.rgb(120, 45, 140),
                    Color.rgb(230, 110, 150),
                    Color.rgb(255, 220, 130)
                )
            }
            style.contains("خيال علمي") || style.contains("سايبربانك") -> {
                listOf(
                    Color.rgb(10, 15, 35),
                    Color.rgb(20, 40, 85),
                    Color.rgb(0, 180, 216),
                    Color.rgb(255, 0, 128)
                )
            }
            style.contains("غموض") || style.contains("إثارة") -> {
                listOf(
                    Color.rgb(15, 12, 25),
                    Color.rgb(35, 25, 45),
                    Color.rgb(75, 40, 60),
                    Color.rgb(240, 170, 70)
                )
            }
            else -> {
                // Epic Cinematic Golden Hour / Teal-Orange
                listOf(
                    Color.rgb(15, 23, 42),
                    Color.rgb(30, 58, 95),
                    Color.rgb(180, 83, 9),
                    Color.rgb(251, 191, 36)
                )
            }
        }

        // 1. Sky & Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(bgTop, bgMid, bgBottom),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Atmospheric Sun / Celestial Glow
        val sunX = width * (0.3f + random.nextFloat() * 0.4f)
        val sunY = height * (0.35f + random.nextFloat() * 0.25f)
        val sunRadius = width * 0.35f

        val sunPaint = Paint().apply {
            shader = RadialGradient(
                sunX, sunY, sunRadius,
                intArrayOf(accentColor, Color.argb(120, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(sunX, sunY, sunRadius, sunPaint)

        // 3. Starlight / Dust Particles
        val particlePaint = Paint().apply {
            color = Color.WHITE
            this.style = Paint.Style.FILL
        }
        for (i in 0 until 120) {
            val px = random.nextFloat() * width
            val py = random.nextFloat() * (height * 0.7f)
            val pr = 1f + random.nextFloat() * 2.5f
            particlePaint.alpha = 50 + random.nextInt(180)
            canvas.drawCircle(px, py, pr, particlePaint)
        }

        // 4. Distant Mountain / Landscape Silhouette
        val mountainPaint = Paint().apply {
            color = Color.argb(200, 10, 10, 22)
            this.style = Paint.Style.FILL
        }
        val path = android.graphics.Path()
        val baseHorizon = height * 0.65f
        path.moveTo(0f, height.toFloat())
        path.lineTo(0f, baseHorizon)

        var curX = 0f
        val step = width / 8f
        while (curX <= width + step) {
            val peakY = baseHorizon - (40f + random.nextFloat() * 140f)
            path.lineTo(curX + step * 0.5f, peakY)
            path.lineTo(curX + step, baseHorizon + (random.nextFloat() * 30f))
            curX += step
        }
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()
        canvas.drawPath(path, mountainPaint)

        // 5. Foreground Landscape Silhouette
        val fgPaint = Paint().apply {
            color = Color.rgb(8, 6, 14)
            this.style = Paint.Style.FILL
        }
        val fgPath = android.graphics.Path()
        val fgHorizon = height * 0.78f
        fgPath.moveTo(0f, height.toFloat())
        fgPath.lineTo(0f, fgHorizon)
        fgPath.cubicTo(
            width * 0.25f, fgHorizon - 40f,
            width * 0.65f, fgHorizon + 30f,
            width.toFloat(), fgHorizon - 20f
        )
        fgPath.lineTo(width.toFloat(), height.toFloat())
        fgPath.close()
        canvas.drawPath(fgPath, fgPaint)

        // 6. Solitary Explorer / Hero Silhouette
        val heroX = width * (0.42f + random.nextFloat() * 0.16f)
        val heroY = fgHorizon - 15f
        val heroPaint = Paint().apply {
            color = Color.rgb(4, 3, 8)
            this.style = Paint.Style.FILL
            isAntiAlias = true
        }
        // Head
        canvas.drawCircle(heroX, heroY - 28f, 7f, heroPaint)
        // Body / Cloak
        val cloakPath = android.graphics.Path().apply {
            moveTo(heroX - 6f, heroY - 21f)
            lineTo(heroX + 6f, heroY - 21f)
            lineTo(heroX + 11f, heroY)
            lineTo(heroX - 11f, heroY)
            close()
        }
        canvas.drawPath(cloakPath, heroPaint)

        // 7. Cinematic Letterbox & Vignette
        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f, width * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.argb(190, 0, 0, 0)),
                floatArrayOf(0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)

        // Save to cache file
        val outputFile = File(context.cacheDir, "procedural_scene_${UUID.randomUUID()}.jpg")
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()

        outputFile
    }
}
