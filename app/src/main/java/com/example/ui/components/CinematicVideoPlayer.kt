package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.StoryProject
import com.example.data.model.StoryScene
import com.example.ui.StoryViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSecondaryLight
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun CinematicVideoPlayerDialog(
    story: StoryProject,
    initialSceneIndex: Int = 0,
    viewModel: StoryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeSceneIdx by remember { mutableIntStateOf(initialSceneIndex.coerceIn(0, (story.scenes.size - 1).coerceAtLeast(0))) }
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    val currentScene = story.scenes.getOrNull(activeSceneIdx) ?: StoryScene()
    val sceneDurationSeconds = 10

    // Play TTS speech and BGM if available
    LaunchedEffect(activeSceneIdx, isPlaying) {
        if (isPlaying) {
            // Check if audio exists for this scene
            val audioFile = viewModel.sceneAudioFiles[currentScene.sceneIndex]
            if (audioFile != null) {
                viewModel.audioPlayer.playFile(audioFile)
            } else if (viewModel.bgmAudioFile != null) {
                viewModel.audioPlayer.playFile(viewModel.bgmAudioFile!!)
            }

            // Progress ticker
            val steps = sceneDurationSeconds * 20
            for (step in 1..steps) {
                if (!isPlaying) break
                delay(50L)
                progress = step.toFloat() / steps.toFloat()
            }

            // Auto-advance to next scene if in movie mode
            if (isPlaying && activeSceneIdx < story.scenes.size - 1) {
                delay(300L)
                activeSceneIdx++
                progress = 0f
            } else {
                isPlaying = false
            }
        } else {
            viewModel.audioPlayer.stop()
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.audioPlayer.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Cinema Aspect Ratio Container
                val ratioFloat = when (story.aspectRatio) {
                    "9:16" -> 9f / 16f
                    "1:1" -> 1f
                    "4:3" -> 4f / 3f
                    else -> 16f / 9f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 40.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratioFloat)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, StudioPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {

                            // Ken Burns Camera Animation (Pan & Zoom)
                            val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
                            val cameraScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.18f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            val cameraPanX by infiniteTransition.animateFloat(
                                initialValue = -15f,
                                targetValue = 15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "panX"
                            )

                            // Scene Image with Cinematic Motion
                            if (currentScene.imageUrl.isNotEmpty()) {
                                val imageFile = File(currentScene.imageUrl)
                                Image(
                                    painter = rememberAsyncImagePainter(model = if (imageFile.exists()) imageFile else currentScene.imageUrl),
                                    contentDescription = "Scene visual",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = cameraScale
                                            scaleY = cameraScale
                                            translationX = cameraPanX
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // High quality aesthetic fallback background
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                listOf(Color(0xFF3B185F), Color(0xFF0F0B1A))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = StudioSecondary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "مشهد سينمائي #${currentScene.sceneIndex}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Dark cinema vignette overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.4f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Title & Scene Number top badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopStart)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${story.title} • مشهد ${currentScene.sceneIndex}/${story.scenes.size}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (viewModel.sceneAudioFiles.containsKey(currentScene.sceneIndex)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioSecondary.copy(alpha = 0.25f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = StudioSecondaryLight,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "صوت درامي فعال",
                                                color = StudioSecondaryLight,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Cinematic Animated Subtitles at bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .border(1.dp, StudioPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentScene.narration.ifEmpty { "..." },
                                    color = Color(0xFFFDE047),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                // Top Header Row with Export and Close Buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            viewModel.exportVideoToDevice(context) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_export_to_phone_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصدير للهاتف", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            viewModel.audioPlayer.stop()
                            onDismiss()
                        },
                        modifier = Modifier
                            .background(Color(0xFF261D42), CircleShape)
                            .testTag("close_video_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                // Bottom Video Player Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xFF0F0B1A).copy(alpha = 0.95f))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = StudioSecondary,
                        trackColor = Color(0xFF2E2452)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المشهد ${activeSceneIdx + 1} من ${story.scenes.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Replay current scene
                            IconButton(
                                onClick = {
                                    progress = 0f
                                    isPlaying = true
                                }
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = "إعادة المشهد", tint = Color.White)
                            }

                            // Play / Pause toggle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(StudioPrimary)
                                    .clickable {
                                        isPlaying = !isPlaying
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next Scene button
                            if (activeSceneIdx < story.scenes.size - 1) {
                                IconButton(
                                    onClick = {
                                        activeSceneIdx++
                                        progress = 0f
                                        isPlaying = true
                                    }
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "المشهد التالي", tint = Color.White)
                                }
                            }
                        }

                        Text(
                            text = "${sceneDurationSeconds}s",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
