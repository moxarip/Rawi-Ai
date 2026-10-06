package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.StoryScene
import com.example.ui.GenerationState
import com.example.ui.StoryViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioPrimaryLight
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSecondaryLight
import com.example.ui.theme.StudioTertiary
import java.io.File

@Composable
fun EditorScreen(
    viewModel: StoryViewModel,
    onBackToHome: () -> Unit
) {
    val activeStory by viewModel.activeStory.collectAsState()
    val genState by viewModel.generationState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    if (activeStory == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "لا توجد قصة نشطة في المحرر حالياً",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "قم بإنشاء قصة جديدة من الشاشة الرئيسية أو اختر قصة محفوظة من مكتبتك.",
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBackToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                ) {
                    Text("الذهاب لإنشاء قصة")
                }
            }
        }
        return
    }

    val story = activeStory!!
    var editableTitle by remember(story.id, story.title) { mutableStateOf(story.title) }
    var bgmPrompt by remember(story.id, story.bgmTrackName) { mutableStateOf(story.bgmTrackName) }
    var isPlayingBgm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Editor Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "محرر المشاهد وتصدير المحتوى",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "عدّل النصوص والصور واستمع للأصوات قبل التصدير النهائي",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioSecondaryLight
                    )
                }

                IconButton(
                    onClick = {
                        val textToCopy = buildString {
                            appendLine("عنوان: ${editableTitle}")
                            appendLine("الأسلوب: ${story.style} | النسبة: ${story.aspectRatio}")
                            appendLine("---")
                            story.scenes.forEach { sc ->
                                appendLine("مشهد ${sc.sceneIndex}: ${sc.narration}")
                            }
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "نسخ السيناريو", tint = Color.White)
                }
            }
        }

        // Project Overview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "عنوان القصة والمشروع:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = {
                            editableTitle = it
                            viewModel.updateActiveStory(story.copy(title = it))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoBadge(label = "الأسلوب", value = story.style)
                        InfoBadge(label = "الأبعاد", value = story.aspectRatio)
                        InfoBadge(label = "المشاهد", value = "${story.scenes.size} مشاهد")
                    }
                }
            }
        }

        // Scene Editing List
        item {
            Text(
                text = "المشاهد المستخرجة (قابلة للتعديل وإعادة التوليد):",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(story.scenes) { index, scene ->
            SceneEditorCard(
                scene = scene,
                aspectRatio = story.aspectRatio,
                voiceName = story.voiceName,
                onUpdate = { newNarration, newPrompt, duration ->
                    viewModel.updateScene(scene.sceneIndex, newNarration, newPrompt, duration)
                },
                onGenerateImage = {
                    viewModel.generateSceneImage(scene.sceneIndex, scene.imagePrompt, story.aspectRatio)
                },
                onGenerateSpeech = {
                    viewModel.generateSceneSpeech(scene.sceneIndex, scene.narration, story.voiceName)
                },
                onGenerateVeo = {
                    viewModel.generateVeoVideo(scene.sceneIndex, scene.imagePrompt, story.aspectRatio)
                }
            )
        }

        // Soundtrack & BGM Studio (Lyria)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = StudioSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الموسيقى التصويرية المرافقة (Lyria Music):",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bgmPrompt,
                        onValueChange = { bgmPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bgm_prompt_input"),
                        placeholder = { Text("مثال: موسيقى أوركسترا ملحمية بطيئة...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioSecondary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateMusic(bgmPrompt) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_music_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توليد الموسيقى (Lyria)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (viewModel.bgmAudioFile != null) {
                            OutlinedButton(
                                onClick = {
                                    if (isPlayingBgm) {
                                        viewModel.audioPlayer.stop()
                                        isPlayingBgm = false
                                    } else {
                                        viewModel.bgmAudioFile?.let { file ->
                                            viewModel.audioPlayer.playFile(file) {
                                                isPlayingBgm = false
                                            }
                                            isPlayingBgm = true
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingBgm) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = StudioSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlayingBgm) "إيقاف" else "تشغيل", color = StudioSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Global status banner if active
        item {
            when (val state = genState) {
                is GenerationState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2452)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = StudioSecondary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = state.message, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
                is GenerationState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = state.errorMessage, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                        }
                    }
                }
                is GenerationState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF143825)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF86EFAC))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = state.message, color = Color(0xFF86EFAC), fontSize = 13.sp)
                        }
                    }
                }
                is GenerationState.Idle -> {}
            }
        }

        // Cloud Save & Final Export Button
        item {
            Button(
                onClick = { viewModel.saveActiveStoryToFirestore() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_cloud_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = StudioSecondaryLight)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "حفظ القصة والمشاهد في السحابة (Firestore)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun SceneEditorCard(
    scene: StoryScene,
    aspectRatio: String,
    voiceName: String,
    onUpdate: (narration: String, prompt: String, duration: Int) -> Unit,
    onGenerateImage: () -> Unit,
    onGenerateSpeech: () -> Unit,
    onGenerateVeo: () -> Unit
) {
    var narration by remember(scene.narration) { mutableStateOf(scene.narration) }
    var imagePrompt by remember(scene.imagePrompt) { mutableStateOf(scene.imagePrompt) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Scene Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioPrimary.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "مشهد #${scene.sceneIndex}",
                        fontWeight = FontWeight.Bold,
                        color = StudioPrimaryLight,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "${scene.durationSec} ثوانٍ",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Editable Narration Text Field
            Text(
                text = "نص السرد والتعليق الصوتي للمشهد:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = narration,
                onValueChange = {
                    narration = it
                    onUpdate(it, imagePrompt, scene.durationSec)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scene_narration_${scene.sceneIndex}"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Editable Image Prompt Text Field
            Text(
                text = "وصف صورة المشهد (Image Generation Prompt):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = imagePrompt,
                onValueChange = {
                    imagePrompt = it
                    onUpdate(narration, it, scene.durationSec)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scene_prompt_${scene.sceneIndex}"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioSecondary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Image Preview (if generated)
            if (scene.imageUrl.isNotEmpty()) {
                val imageFile = File(scene.imageUrl)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StudioPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = if (imageFile.exists()) imageFile else scene.imageUrl),
                        contentDescription = "Scene visual",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("تم التوليد بنجاح", color = Color(0xFF86EFAC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Veo Video status indicator if present
            if (scene.videoUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = StudioSecondaryLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حالة الفيديو (Veo 3): ${scene.videoUrl}",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Buttons Row: Generate Image, TTS Narration, Veo Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Generate Image button
                Button(
                    onClick = onGenerateImage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gen_img_scene_${scene.sceneIndex}"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (scene.imageUrl.isEmpty()) "توليد صورة" else "إعادة توليد", fontSize = 11.sp)
                }

                // TTS Audio button
                Button(
                    onClick = onGenerateSpeech,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tts_scene_${scene.sceneIndex}"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صوت (TTS)", color = Color.Black, fontSize = 11.sp)
                }

                // Veo Video button
                OutlinedButton(
                    onClick = onGenerateVeo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("veo_scene_${scene.sceneIndex}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فيديو Veo", color = StudioAccent, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label: ", fontSize = 11.sp, color = Color(0xFF94A3B8))
            Text(text = value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
