package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.api.ImageProvider
import com.example.data.api.VideoProvider
import com.example.data.model.StoryScene
import com.example.ui.GenerationState
import com.example.ui.StoryViewModel
import com.example.ui.components.CinematicVideoPlayerDialog
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: StoryViewModel,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val activeStory by viewModel.activeStory.collectAsState()
    val genState by viewModel.generationState.collectAsState()
    val compiledVideoFile by viewModel.compiledVideoFile.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val providerManager = viewModel.providerManager
    val selectedImageProvider by providerManager.selectedImageProvider.collectAsState()
    val selectedVideoProvider by providerManager.selectedVideoProvider.collectAsState()

    var showProviderSelector by remember { mutableStateOf(false) }

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
    var showMoviePlayer by remember { mutableStateOf(false) }
    var moviePlayerInitialScene by remember { mutableIntStateOf(0) }

    if (showMoviePlayer) {
        CinematicVideoPlayerDialog(
            story = story,
            initialSceneIndex = moviePlayerInitialScene,
            viewModel = viewModel,
            onDismiss = { showMoviePlayer = false }
        )
    }

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
                        text = "محرر المشاهد والإنتاج السينمائي",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "كل مشهد مدته 10 ثوانٍ • يمكنك التعديل وإعادة التوليد والتصدير للهاتف",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioSecondaryLight
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            viewModel.startNewStory()
                            onBackToHome()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("editor_new_story_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioSecondaryLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قصة جديدة", color = Color.White, fontSize = 11.5.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { showProviderSelector = !showProviderSelector }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "تغيير مزود الذكاء الاصطناعي",
                            tint = if (showProviderSelector) StudioSecondary else Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            val textToCopy = buildString {
                                appendLine("عنوان: ${editableTitle}")
                                appendLine("الأسلوب: ${story.style} | النسبة: ${story.aspectRatio}")
                                appendLine("---")
                                story.scenes.forEach { sc ->
                                    appendLine("مشهد ${sc.sceneIndex} (10s): ${sc.narration}")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "تم نسخ السيناريو كاملاً", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "نسخ السيناريو", tint = Color.White)
                    }
                }
            }
        }

        // DYNAMIC PROVIDERS SELECTOR BAR IN THE EDITOR
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1735)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, StudioPrimary.copy(alpha = 0.5f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioSecondaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "موقع الصور الفعال: ${selectedImageProvider.displayName.take(18)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = if (showProviderSelector) "إخفاء الخيارات ▲" else "تغيير المزود ▼",
                            fontSize = 11.sp,
                            color = StudioSecondaryLight,
                            modifier = Modifier.clickable { showProviderSelector = !showProviderSelector }
                        )
                    }

                    AnimatedVisibility(visible = showProviderSelector) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "اختر موقع / محرك توليد الصور لهذا المحرر:",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ImageProvider.values().forEach { provider ->
                                    val isSelected = selectedImageProvider == provider
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { providerManager.setImageProvider(provider) },
                                        label = { Text(provider.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StudioPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurfaceVariant,
                                            labelColor = Color(0xFF94A3B8)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "اختر محرك الفيديو:",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VideoProvider.values().forEach { vProvider ->
                                    val isSel = selectedVideoProvider == vProvider
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { providerManager.setVideoProvider(vProvider) },
                                        label = { Text(vProvider.displayName.take(18), fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StudioSecondary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = DarkSurfaceVariant,
                                            labelColor = Color(0xFF94A3B8)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.regenerateAllSceneImages() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تطبيق وتوليد كل الصور بموقع (${selectedImageProvider.displayName.take(10)})", fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.regenerateAllSceneVideos() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp), tint = StudioSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تطبيق محرك الفيديو (${selectedVideoProvider.displayName.take(10)})", fontSize = 10.sp, color = StudioSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // PRIMARY ACTION BUTTONS: Play Movie & Export Video to Phone
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Play Full Movie button
                Button(
                    onClick = {
                        moviePlayerInitialScene = 0
                        showMoviePlayer = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_full_movie_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "عرض الفيلم السينمائي الكامل (Ken Burns + الصوت المدمج)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }

                // EXPORT TO PHONE STORAGE BUTTON
                Button(
                    onClick = {
                        viewModel.exportVideoToDevice(context) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_video_to_phone_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D9488),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير وتنزيل الفيديو إلى الهاتف (حفظ MP4 في الاستوديو)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }

                // If compiled video exists, show Share and Open buttons
                if (compiledVideoFile != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openCompiledVideo(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = StudioSecondaryLight)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح في مشغل الهاتف", fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareCompiledVideo(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = StudioSecondaryLight)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة الفيديو", fontSize = 12.sp, color = Color.White)
                        }
                    }
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
                        InfoBadge(label = "المدة", value = "${story.scenes.size * 10} ثانية")
                        InfoBadge(label = "المشاهد", value = "${story.scenes.size} مشاهد")
                    }
                }
            }
        }

        // Scene Editing List
        item {
            Text(
                text = "المشاهد السينمائية (10 ثوانٍ لكل مشهد - قابلة للتعديل وإعادة التوليد):",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(story.scenes) { index, scene ->
            SceneEditorCard(
                scene = scene,
                aspectRatio = story.aspectRatio,
                voiceName = story.voiceName,
                viewModel = viewModel,
                activeImageProvider = selectedImageProvider,
                activeVideoProvider = selectedVideoProvider,
                onUpdate = { newNarration, newPrompt, duration ->
                    viewModel.updateScene(scene.sceneIndex, newNarration, newPrompt, duration)
                },
                onGenerateImage = { customProv ->
                    viewModel.generateSceneImage(scene.sceneIndex, scene.imagePrompt, story.aspectRatio, customProv)
                },
                onGenerateSpeech = {
                    viewModel.generateSceneSpeech(scene.sceneIndex, scene.narration, story.voiceName)
                },
                onGenerateVeo = { customProv ->
                    viewModel.generateVeoVideo(scene.sceneIndex, scene.imagePrompt, story.aspectRatio, customProv)
                },
                onPlayVideo = {
                    moviePlayerInitialScene = index
                    showMoviePlayer = true
                }
            )
        }

        // Soundtrack & BGM Studio (Lyria / Synthesizer)
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
                            text = "الموسيقى التصويرية المرافقة (Lyria & Audio Studio):",
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
                            onClick = { viewModel.generateMusic(bgmPrompt, story.style) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_music_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توليد الموسيقى التصويرية", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = StudioSecondary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = state.message, color = Color.White, fontSize = 13.sp)
                            }
                            if (state.progressFraction > 0f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { state.progressFraction },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = StudioSecondary,
                                    trackColor = Color(0xFF3B2F5C)
                                )
                            }
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

        // Cloud Save Button
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneEditorCard(
    scene: StoryScene,
    aspectRatio: String,
    voiceName: String,
    viewModel: StoryViewModel,
    activeImageProvider: ImageProvider,
    activeVideoProvider: VideoProvider,
    onUpdate: (narration: String, prompt: String, duration: Int) -> Unit,
    onGenerateImage: (ImageProvider?) -> Unit,
    onGenerateSpeech: () -> Unit,
    onGenerateVeo: (VideoProvider?) -> Unit,
    onPlayVideo: () -> Unit
) {
    var narration by remember(scene.narration) { mutableStateOf(scene.narration) }
    var imagePrompt by remember(scene.imagePrompt) { mutableStateOf(scene.imagePrompt) }
    var isPlayingNarration by remember { mutableStateOf(false) }
    var showSceneProviderPicker by remember { mutableStateOf(false) }

    val hasAudio = viewModel.sceneAudioFiles.containsKey(scene.sceneIndex)

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    if (hasAudio) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF14532D))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("الصوت جاهز", color = Color(0xFF86EFAC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 10 SECONDS BADGE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF271F42))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "10 ثوانٍ",
                        color = StudioSecondaryLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Provider Indicators for this scene
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentImgProv = scene.imageProviderName.ifEmpty { activeImageProvider.displayName }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "موقع الصورة: ${currentImgProv.take(20)}",
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val currentVidProv = scene.videoProviderName.ifEmpty { activeVideoProvider.displayName }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF241C3E))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "محرك الفيديو: ${currentVidProv.take(16)}",
                        color = StudioSecondaryLight,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Scene Provider Picker Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSceneProviderPicker = !showSceneProviderPicker }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تغيير موقع إنشاء الصورة لهذا المشهد:",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (showSceneProviderPicker) "إخفاء ▲" else "اختيار موقع آخر ▼",
                    fontSize = 11.sp,
                    color = StudioSecondaryLight,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = showSceneProviderPicker) {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ImageProvider.values().forEach { ip ->
                            val isSelected = (scene.imageProviderName.isNotEmpty() && scene.imageProviderName == ip.displayName) ||
                                    (scene.imageProviderName.isEmpty() && activeImageProvider == ip)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGenerateImage(ip) },
                                label = { Text(ip.displayName.take(16), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Editable Narration Text Field
            Text(
                text = "نص السرد والتعليق الصوتي للمشهد (10 ثوانٍ):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = narration,
                onValueChange = {
                    narration = it
                    onUpdate(it, imagePrompt, 10)
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
                    onUpdate(narration, it, 10)
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
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "صورة جاهزة • ${scene.imageProviderName.ifEmpty { activeImageProvider.displayName }}",
                            color = Color(0xFF86EFAC),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Play Scene Video button (if image or video motion is ready)
            Button(
                onClick = onPlayVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("play_scene_video_${scene.sceneIndex}"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StudioSecondaryLight)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مشاهدة فيديو المشهد (10 ثوانٍ + صوت)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Buttons Row: Generate Image, TTS Narration, Veo Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Generate Image button
                Button(
                    onClick = { onGenerateImage(null) },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("gen_img_scene_${scene.sceneIndex}"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (scene.imageUrl.isEmpty()) "توليد (${activeImageProvider.displayName.take(7)})" else "إعادة (${activeImageProvider.displayName.take(7)})",
                        fontSize = 10.5.sp
                    )
                }

                // TTS Audio button
                Button(
                    onClick = onGenerateSpeech,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("tts_scene_${scene.sceneIndex}"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("صوت (TTS)", color = Color.Black, fontSize = 10.5.sp)
                }

                // Listen Audio directly if present
                if (hasAudio) {
                    OutlinedButton(
                        onClick = {
                            val aFile = viewModel.sceneAudioFiles[scene.sceneIndex]
                            if (aFile != null) {
                                if (isPlayingNarration) {
                                    viewModel.audioPlayer.stop()
                                    isPlayingNarration = false
                                } else {
                                    viewModel.audioPlayer.playFile(aFile) {
                                        isPlayingNarration = false
                                    }
                                    isPlayingNarration = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingNarration) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = StudioSecondaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Video button
                OutlinedButton(
                    onClick = { onGenerateVeo(null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("veo_scene_${scene.sceneIndex}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = StudioAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("فيديو (${activeVideoProvider.displayName.take(6)})", color = StudioAccent, fontSize = 10.5.sp)
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
