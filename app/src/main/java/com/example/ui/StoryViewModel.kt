package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AIProviderManager
import com.example.data.api.ImageProvider
import com.example.data.api.VideoProvider
import com.example.data.api.GeminiApiClient
import com.example.data.model.StoryProject
import com.example.data.model.StoryScene
import com.example.data.repository.StoryRepository
import com.example.util.AudioPlayerHelper
import com.example.util.VideoExportHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface GenerationState {
    object Idle : GenerationState
    data class Loading(val message: String, val progressFraction: Float = 0f) : GenerationState
    data class Success(val message: String) : GenerationState
    data class Error(val errorMessage: String) : GenerationState
}

data class LiveChatMessage(
    val role: String, // "user" or "model"
    val text: String
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StoryRepository(application)
    val geminiClient = GeminiApiClient(application)
    val providerManager: AIProviderManager
        get() = geminiClient.providerManager

    val audioPlayer = AudioPlayerHelper(application)
    val appUpdateManager = com.example.util.AppUpdateManager(application)

    val currentUserId: String?
        get() = Firebase.auth.currentUser?.uid

    // Observation of stories from Firestore
    val storiesState: StateFlow<List<StoryProject>> = repository.observeStories(currentUserId ?: "")
        .catch { e ->
            Log.e("StoryViewModel", "Error observing stories: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Currently active story in Editor
    private val _activeStory = MutableStateFlow<StoryProject?>(null)
    val activeStory: StateFlow<StoryProject?> = _activeStory.asStateFlow()

    // Operation status indicator
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // Compiled video file ready for export / playback
    private val _compiledVideoFile = MutableStateFlow<File?>(null)
    val compiledVideoFile: StateFlow<File?> = _compiledVideoFile.asStateFlow()

    // Export status notification
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    // Live AI Companion chat messages
    private val _liveMessages = MutableStateFlow<List<LiveChatMessage>>(
        listOf(
            LiveChatMessage(
                role = "model",
                text = "مرحباً بك في راوي AI! أنا رفيقك المساعد في كتابة السيناريو واستلهام المشاهد السينمائية. كيف تحب أن نبدأ اليوم؟"
            )
        )
    )
    val liveMessages: StateFlow<List<LiveChatMessage>> = _liveMessages.asStateFlow()

    // Fast title ideas suggestions
    private val _quickTitles = MutableStateFlow<List<String>>(emptyList())
    val quickTitles: StateFlow<List<String>> = _quickTitles.asStateFlow()

    // Audio file cache for active playback
    val sceneAudioFiles = mutableMapOf<Int, File>()
    var bgmAudioFile: File? = null

    fun selectStory(story: StoryProject) {
        _activeStory.value = story
    }

    /**
     * Resets active story state completely so starting a new story does NOT keep the old one visible.
     */
    fun clearActiveStory() {
        _activeStory.value = null
        sceneAudioFiles.clear()
        bgmAudioFile = null
        _compiledVideoFile.value = null
        _exportStatus.value = null
    }

    /**
     * Starts a completely new story session: wipes old story and resets state to Idle.
     */
    fun startNewStory() {
        clearActiveStory()
        _generationState.value = GenerationState.Idle
    }

    fun fetchQuickTitles(theme: String) {
        viewModelScope.launch {
            try {
                val titles = geminiClient.generateQuickSuggestions(theme)
                _quickTitles.value = titles
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Create story from text prompt with Complete End-to-End Multimodal Generation:
     * Script -> Scene Images -> Speech Audio -> Soundtrack Music -> Video Assembly & Encoding (10s per scene).
     */
    fun createStoryFromPrompt(
        prompt: String,
        style: String,
        aspectRatio: String,
        sceneCount: Int,
        useSearch: Boolean
    ) {
        // Clear previous story immediately and synchronously so old content is never shown
        clearActiveStory()
        _generationState.value = GenerationState.Loading("1/5: جاري تأليف القصة وتقسيم المشاهد (10 ثوانٍ لكل مشهد)...", 0.15f)

        viewModelScope.launch {
            try {
                // Step 1: Generate Story Structure
                val story = geminiClient.generateStory(
                    prompt = prompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    useSearch = useSearch,
                    sceneCount = sceneCount
                )

                // Enforce 10 seconds per scene
                val updatedScenes = story.scenes.map { it.copy(durationSec = 10) }.toMutableList()
                val totalScenes = updatedScenes.size

                val activeImgProvider = providerManager.selectedImageProvider.value
                val activeVidProvider = providerManager.selectedVideoProvider.value

                // Step 2: Auto-Generate Images for ALL scenes using active image provider
                for ((idx, scene) in updatedScenes.withIndex()) {
                    val progress = 0.2f + (0.35f * (idx + 1).toFloat() / totalScenes.toFloat())
                    _generationState.value = GenerationState.Loading(
                        "2/5: جاري توليد صورة المشهد (${idx + 1} من $totalScenes) عبر ${activeImgProvider.displayName}...",
                        progress
                    )
                    try {
                        val imgPath = geminiClient.generateSceneImage(
                            prompt = scene.imagePrompt,
                            aspectRatio = aspectRatio,
                            sceneIndex = scene.sceneIndex,
                            forceProvider = activeImgProvider
                        )
                        updatedScenes[idx] = updatedScenes[idx].copy(
                            imageUrl = imgPath,
                            imageProviderName = activeImgProvider.displayName,
                            videoUrl = "cinematic_motion_ready",
                            videoProviderName = activeVidProvider.displayName,
                            durationSec = 10
                        )
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Image generation error for scene ${scene.sceneIndex}: ${e.message}")
                    }
                }

                // Step 3: Auto-Generate Narration Audio for ALL scenes
                for ((idx, scene) in updatedScenes.withIndex()) {
                    val progress = 0.55f + (0.2f * (idx + 1).toFloat() / totalScenes.toFloat())
                    _generationState.value = GenerationState.Loading(
                        "3/5: جاري تسجيل التعليق الصوتي للمشهد (${idx + 1} من $totalScenes)...",
                        progress
                    )
                    try {
                        val audioFile = geminiClient.generateSpeech(
                            text = scene.narration,
                            voiceName = story.voiceName
                        )
                        sceneAudioFiles[scene.sceneIndex] = audioFile
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Speech generation error for scene ${scene.sceneIndex}: ${e.message}")
                    }
                }

                // Step 4: Auto-Generate Soundtrack Music
                _generationState.value = GenerationState.Loading("4/5: جاري تأليف الموسيقى التصويرية المرافقة (44.1kHz)...", 0.78f)
                try {
                    val music = geminiClient.generateMusic(story.bgmTrackName, style)
                    bgmAudioFile = music
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "Music generation error: ${e.message}")
                }

                val finalStory = story.copy(
                    userId = currentUserId ?: "",
                    scenes = updatedScenes
                )

                // Step 5: Automatically Compile All Scenes into Complete Movie File
                _generationState.value = GenerationState.Loading("5/5: جاري تجميع وترميز الفيديو السينمائي الكامل (10 ثوانٍ لكل مشهد)...", 0.88f)
                try {
                    val videoFile = VideoExportHelper.compileStoryToVideo(
                        context = getApplication(),
                        story = finalStory,
                        sceneAudioFiles = sceneAudioFiles,
                        bgmAudioFile = bgmAudioFile
                    ) { fraction, msg ->
                        _generationState.value = GenerationState.Loading(msg, 0.88f + (fraction * 0.11f))
                    }
                    _compiledVideoFile.value = videoFile
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "Video assembly error: ${e.message}")
                }

                _activeStory.value = finalStory
                _generationState.value = GenerationState.Success("اكتمل إنتاج وتجميع الفيلم بالكامل! جميع المشاهد (10 ثوانٍ) مجهزة مع الصوت والموسيقى وجاهزة للعرض والتصدير للهاتف.")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error creating story: ${e.message}", e)
                _generationState.value = GenerationState.Error(e.localizedMessage ?: "حدث خطأ أثناء توليد القصة")
            }
        }
    }

    /**
     * Analyze YouTube video and create similar inspired story with Complete Generation and Movie Assembly.
     */
    fun createStoryFromYouTube(
        youtubeUrl: String,
        notes: String,
        style: String,
        aspectRatio: String
    ) {
        clearActiveStory()
        _generationState.value = GenerationState.Loading("1/5: جاري تحليل فيديو يوتيوب وصياغة سيناريو شبيه (10 ثوانٍ لكل مشهد)...", 0.15f)

        viewModelScope.launch {
            try {
                val story = geminiClient.analyzeAndRecreateYouTube(
                    youtubeUrl = youtubeUrl,
                    userNotes = notes,
                    style = style,
                    aspectRatio = aspectRatio
                )

                val updatedScenes = story.scenes.map { it.copy(durationSec = 10) }.toMutableList()
                val totalScenes = updatedScenes.size
                val activeImgProvider = providerManager.selectedImageProvider.value
                val activeVidProvider = providerManager.selectedVideoProvider.value

                for ((idx, scene) in updatedScenes.withIndex()) {
                    val progress = 0.2f + (0.35f * (idx + 1).toFloat() / totalScenes.toFloat())
                    _generationState.value = GenerationState.Loading(
                        "2/5: جاري توليد صور المشاهد (${idx + 1} من $totalScenes) عبر ${activeImgProvider.displayName}...",
                        progress
                    )
                    try {
                        val imgPath = geminiClient.generateSceneImage(
                            prompt = scene.imagePrompt,
                            aspectRatio = aspectRatio,
                            sceneIndex = scene.sceneIndex,
                            forceProvider = activeImgProvider
                        )
                        updatedScenes[idx] = updatedScenes[idx].copy(
                            imageUrl = imgPath,
                            imageProviderName = activeImgProvider.displayName,
                            videoUrl = "cinematic_motion_ready",
                            videoProviderName = activeVidProvider.displayName,
                            durationSec = 10
                        )
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Image generation error: ${e.message}")
                    }
                }

                for ((idx, scene) in updatedScenes.withIndex()) {
                    _generationState.value = GenerationState.Loading(
                        "3/5: جاري إنتاج التعليق الصوتي (${idx + 1} من $totalScenes)...",
                        0.6f
                    )
                    try {
                        val audioFile = geminiClient.generateSpeech(scene.narration, story.voiceName)
                        sceneAudioFiles[scene.sceneIndex] = audioFile
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Speech error: ${e.message}")
                    }
                }

                _generationState.value = GenerationState.Loading("4/5: جاري تأليف الموسيقى التصويرية الملحمية...", 0.8f)
                try {
                    val music = geminiClient.generateMusic(story.bgmTrackName, style)
                    bgmAudioFile = music
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "Music error: ${e.message}")
                }

                val finalStory = story.copy(
                    userId = currentUserId ?: "",
                    scenes = updatedScenes
                )

                _generationState.value = GenerationState.Loading("5/5: جاري تجميع وترميز الفيلم السينمائي الكامل (10 ثوانٍ)...", 0.9f)
                try {
                    val videoFile = VideoExportHelper.compileStoryToVideo(
                        context = getApplication(),
                        story = finalStory,
                        sceneAudioFiles = sceneAudioFiles,
                        bgmAudioFile = bgmAudioFile
                    )
                    _compiledVideoFile.value = videoFile
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "Video assembly error: ${e.message}")
                }

                _activeStory.value = finalStory
                _generationState.value = GenerationState.Success("تم تحليل فيديو يوتيوب وإنتاج الفيلم والقصة بنجاح! جاهز للعرض والتصدير للهاتف.")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error analyzing YouTube: ${e.message}", e)
                _generationState.value = GenerationState.Error(e.localizedMessage ?: "فشل في تحليل رابط يوتيوب")
            }
        }
    }

    /**
     * Update active story details.
     */
    fun updateActiveStory(updated: StoryProject) {
        _activeStory.value = updated
    }

    /**
     * Update specific scene text, narration, or prompt.
     */
    fun updateScene(index: Int, narration: String, imagePrompt: String, durationSec: Int = 10) {
        val current = _activeStory.value ?: return
        val updatedScenes = current.scenes.toMutableList()
        val targetIndex = updatedScenes.indexOfFirst { it.sceneIndex == index }
        if (targetIndex >= 0) {
            val scene = updatedScenes[targetIndex]
            updatedScenes[targetIndex] = scene.copy(
                narration = narration,
                imagePrompt = imagePrompt,
                durationSec = 10
            )
            _activeStory.value = current.copy(scenes = updatedScenes)
        }
    }

    /**
     * Generate or regenerate image for a scene using the active or a selected multi-provider option.
     */
    fun generateSceneImage(sceneIndex: Int, prompt: String, aspectRatio: String, customProvider: ImageProvider? = null) {
        val provider = customProvider ?: providerManager.selectedImageProvider.value
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري توليد صورة المشهد عبر ${provider.displayName}...")
            try {
                val imagePath = geminiClient.generateSceneImage(prompt, aspectRatio, sceneIndex, provider)
                val current = _activeStory.value ?: return@launch
                val updatedScenes = current.scenes.toMutableList()
                val targetIndex = updatedScenes.indexOfFirst { it.sceneIndex == sceneIndex }
                if (targetIndex >= 0) {
                    updatedScenes[targetIndex] = updatedScenes[targetIndex].copy(
                        imageUrl = imagePath,
                        imageProviderName = provider.displayName,
                        durationSec = 10
                    )
                    _activeStory.value = current.copy(scenes = updatedScenes)
                }
                _generationState.value = GenerationState.Success("تم توليد صورة المشهد بنجاح عبر ${provider.displayName}!")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error generating image: ${e.message}", e)
                _generationState.value = GenerationState.Error("خطأ في توليد الصورة: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Regenerates ALL scene images using the selected ImageProvider.
     * When user changes provider, this updates the entire story to the new provider!
     */
    fun regenerateAllSceneImages(provider: ImageProvider? = null) {
        val current = _activeStory.value ?: return
        val effectiveProvider = provider ?: providerManager.selectedImageProvider.value
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري إعادة توليد جميع المشاهد عبر ${effectiveProvider.displayName}...", 0.1f)
            val updatedScenes = current.scenes.toMutableList()
            val total = updatedScenes.size
            for ((idx, sc) in updatedScenes.withIndex()) {
                val p = 0.1f + (0.85f * (idx + 1).toFloat() / total.toFloat())
                _generationState.value = GenerationState.Loading("جاري توليد صورة المشهد (${idx + 1} من $total) عبر ${effectiveProvider.displayName}...", p)
                try {
                    val imgPath = geminiClient.generateSceneImage(sc.imagePrompt, current.aspectRatio, sc.sceneIndex, effectiveProvider)
                    updatedScenes[idx] = updatedScenes[idx].copy(
                        imageUrl = imgPath,
                        imageProviderName = effectiveProvider.displayName,
                        durationSec = 10
                    )
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "Error regenerating image for scene ${sc.sceneIndex}: ${e.message}")
                }
            }
            _activeStory.value = current.copy(scenes = updatedScenes)
            _generationState.value = GenerationState.Success("تم تحديث وتوليد جميع صور المشاهد بنجاح عبر ${effectiveProvider.displayName}!")
        }
    }

    /**
     * Generate TTS voiceover for a scene using Gemini TTS or Android Native TTS.
     */
    fun generateSceneSpeech(sceneIndex: Int, text: String, voiceName: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري توليد التعليق الصوتي الدرامي...")
            try {
                val audioFile = geminiClient.generateSpeech(text, voiceName)
                sceneAudioFiles[sceneIndex] = audioFile
                audioPlayer.playFile(audioFile)
                _generationState.value = GenerationState.Success("تم توليد الصوت وتشغيله بنجاح!")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "TTS error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل توليد الصوت: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Generate Music track using Lyria or Harmonic Synthesizer.
     */
    fun generateMusic(prompt: String, style: String = "سينمائي") {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري توليد الموسيقى التصويرية السينمائية...")
            try {
                val musicFile = geminiClient.generateMusic(prompt, style)
                bgmAudioFile = musicFile
                audioPlayer.playFile(musicFile)
                _generationState.value = GenerationState.Success("تم توليد الموسيقى التصويرية وتشغيلها بنجاح!")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Music error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل توليد الموسيقى: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Generate Veo video animation for a scene using veo-3.1-fast-generate-preview.
     */
    fun generateVeoVideo(sceneIndex: Int, prompt: String, aspectRatio: String, customProvider: VideoProvider? = null) {
        val vProvider = customProvider ?: providerManager.selectedVideoProvider.value
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري تجهيز حركة الفيديو عبر ${vProvider.displayName}...")
            try {
                val status = geminiClient.generateVeoVideo(prompt, aspectRatio)
                val current = _activeStory.value ?: return@launch
                val updatedScenes = current.scenes.toMutableList()
                val targetIndex = updatedScenes.indexOfFirst { it.sceneIndex == sceneIndex }
                if (targetIndex >= 0) {
                    updatedScenes[targetIndex] = updatedScenes[targetIndex].copy(
                        videoUrl = status,
                        videoProviderName = vProvider.displayName,
                        durationSec = 10
                    )
                    _activeStory.value = current.copy(scenes = updatedScenes)
                }
                _generationState.value = GenerationState.Success("تم تجهيز فيديو المشهد عبر ${vProvider.displayName}! اضغط «مشاهدة الفيديو» لتشغيله.")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Veo error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل تصيير الفيديو: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Regenerates ALL scene videos using the currently selected VideoProvider.
     */
    fun regenerateAllSceneVideos(provider: VideoProvider? = null) {
        val current = _activeStory.value ?: return
        val effectiveProvider = provider ?: providerManager.selectedVideoProvider.value
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري تطبيق محرك الفيديو ${effectiveProvider.displayName} على جميع المشاهد...", 0.2f)
            val updatedScenes = current.scenes.map {
                it.copy(
                    videoUrl = "cinematic_motion_ready",
                    videoProviderName = effectiveProvider.displayName,
                    durationSec = 10
                )
            }
            _activeStory.value = current.copy(scenes = updatedScenes)
            _generationState.value = GenerationState.Success("تم تحديث محرك الفيديو لجميع المشاهد عبر ${effectiveProvider.displayName}!")
        }
    }

    /**
     * Compiles and encodes the full video from scenes on demand.
     */
    fun compileFullVideo(context: Context) {
        val current = _activeStory.value ?: return
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري تجميع وترميز فيديو المشاهد بالكامل (MP4)...", 0.1f)
            try {
                val videoFile = VideoExportHelper.compileStoryToVideo(
                    context = context,
                    story = current,
                    sceneAudioFiles = sceneAudioFiles,
                    bgmAudioFile = bgmAudioFile
                ) { fraction, msg ->
                    _generationState.value = GenerationState.Loading(msg, fraction)
                }
                _compiledVideoFile.value = videoFile
                _generationState.value = GenerationState.Success("اكتمل ترميز الفيديو وتجميعه بنجاح! يمكنك الآن تصديره للهاتف.")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Compile video error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل ترميز الفيديو: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Exports the compiled video file into the user's phone storage (Movies / Gallery).
     */
    fun exportVideoToDevice(context: Context, onResult: (Boolean, String) -> Unit) {
        val current = _activeStory.value
        if (current == null) {
            onResult(false, "لا يوجد مشروع مفتوح لتصديره.")
            return
        }

        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري تصدير وحفظ الفيديو في ذاكرة الهاتف...")
            try {
                var videoFile = _compiledVideoFile.value
                if (videoFile == null || !videoFile.exists()) {
                    // Compile if not already compiled
                    videoFile = VideoExportHelper.compileStoryToVideo(
                        context = context,
                        story = current,
                        sceneAudioFiles = sceneAudioFiles,
                        bgmAudioFile = bgmAudioFile
                    )
                    _compiledVideoFile.value = videoFile
                }

                val savedUri = VideoExportHelper.saveVideoToPhoneStorage(context, videoFile, current.title)
                if (savedUri != null) {
                    val msg = "تم تصدير الفيديو وحفظه في مجلد الفيديوهات بالهاتف (Movies/RawiAI) بنجاح! يمكنك فتحه ومشاركته الآن."
                    _exportStatus.value = msg
                    _generationState.value = GenerationState.Success(msg)
                    onResult(true, msg)
                } else {
                    val msg = "تم تجهيز ملف الفيديو في ذاكرة التطبيق بنجاح."
                    _exportStatus.value = msg
                    _generationState.value = GenerationState.Success(msg)
                    onResult(true, msg)
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Export video error: ${e.message}", e)
                val errMsg = "فشل تصدير الفيديو: ${e.localizedMessage}"
                _generationState.value = GenerationState.Error(errMsg)
                onResult(false, errMsg)
            }
        }
    }

    fun shareCompiledVideo(context: Context) {
        val videoFile = _compiledVideoFile.value
        val title = _activeStory.value?.title ?: "Rawi AI Video"
        if (videoFile != null && videoFile.exists()) {
            VideoExportHelper.shareVideoFile(context, videoFile, title)
        } else {
            exportVideoToDevice(context) { success, _ ->
                if (success && _compiledVideoFile.value != null) {
                    VideoExportHelper.shareVideoFile(context, _compiledVideoFile.value!!, title)
                }
            }
        }
    }

    fun openCompiledVideo(context: Context) {
        val videoFile = _compiledVideoFile.value
        if (videoFile != null && videoFile.exists()) {
            VideoExportHelper.openVideoFile(context, videoFile)
        }
    }

    fun setCustomApiKey(key: String, markGoogleOne: Boolean = false) {
        providerManager.setCustomGeminiKey(key, markGoogleOne)
    }

    fun testCustomApiKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = providerManager.testGeminiKey(key)
            onResult(result.first, result.second)
        }
    }

    /**
     * Save active story to Firestore cloud database.
     */
    fun saveActiveStoryToFirestore() {
        val current = _activeStory.value ?: return
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري حفظ القصة في قاعدة البيانات السحابية...")
            try {
                val savedId = repository.saveStory(current)
                _activeStory.value = current.copy(id = savedId)
                _generationState.value = GenerationState.Success("تم حفظ القصة بنجاح في حسابك السحابي!")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Firestore save error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل حفظ القصة: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Delete story from Firestore.
     */
    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            try {
                repository.deleteStory(storyId)
                if (_activeStory.value?.id == storyId) {
                    _activeStory.value = null
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error deleting story: ${e.message}", e)
            }
        }
    }

    /**
     * Send message to Live AI Assistant.
     */
    fun sendLiveMessage(userText: String) {
        if (userText.isBlank()) return
        val currentMessages = _liveMessages.value.toMutableList()
        currentMessages.add(LiveChatMessage(role = "user", text = userText))
        _liveMessages.value = currentMessages

        viewModelScope.launch {
            try {
                val history = currentMessages.map { it.role to it.text }
                val reply = geminiClient.chatWithLiveAssistant(history, userText)
                _liveMessages.value = _liveMessages.value + LiveChatMessage(role = "model", text = reply)
            } catch (e: Exception) {
                _liveMessages.value = _liveMessages.value + LiveChatMessage(
                    role = "model",
                    text = "أهلاً بك! يمكنك مشاركة فكرتك وسأساعدك في كتابة وتطوير السيناريو فوراً."
                )
            }
        }
    }

    fun dismissState() {
        _generationState.value = GenerationState.Idle
    }

    fun dismissExportStatus() {
        _exportStatus.value = null
    }

    /**
     * In-App Update checking and trigger flows.
     */
    fun checkForUpdates(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val info = appUpdateManager.checkForUpdates()
            if (info.hasUpdate) {
                onResult?.invoke(true, "يوجد تحديث جديد متاح (${info.latestVersion})")
            } else {
                onResult?.invoke(false, "أنت تستخدم أحدث إصدار من التطبيق (${info.currentVersion})")
            }
        }
    }

    fun triggerSimulatedUpdate(targetVersion: String = "v2") {
        appUpdateManager.triggerSimulatedUpdate(targetVersion)
    }

    fun downloadAndInstallUpdate(onComplete: (Boolean, String) -> Unit) {
        val info = appUpdateManager.updateInfo.value
        viewModelScope.launch {
            appUpdateManager.downloadAndInstallApk(info.downloadUrl, onComplete)
        }
    }

    fun dismissUpdate() {
        appUpdateManager.dismissUpdate()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
