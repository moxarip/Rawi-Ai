package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.model.StoryProject
import com.example.data.model.StoryScene
import com.example.data.repository.StoryRepository
import com.example.util.AudioPlayerHelper
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
    data class Loading(val message: String) : GenerationState
    data class Success(val message: String) : GenerationState
    data class Error(val errorMessage: String) : GenerationState
}

data class LiveChatMessage(
    val role: String, // "user" or "model"
    val text: String
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StoryRepository(application)
    private val geminiClient = GeminiApiClient(application)
    val audioPlayer = AudioPlayerHelper(application)

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

    fun clearActiveStory() {
        _activeStory.value = null
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
     * Create story from text prompt.
     */
    fun createStoryFromPrompt(
        prompt: String,
        style: String,
        aspectRatio: String,
        sceneCount: Int,
        useSearch: Boolean
    ) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري نسج القصة والسيناريو عبر الذكاء الاصطناعي...")
            try {
                val story = geminiClient.generateStory(
                    prompt = prompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    useSearch = useSearch,
                    sceneCount = sceneCount
                )
                _activeStory.value = story.copy(userId = currentUserId ?: "")
                _generationState.value = GenerationState.Success("تم توليد القصة بنجاح! يمكنك الآن تعديل النصوص والمشاهد.")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error creating story: ${e.message}", e)
                _generationState.value = GenerationState.Error(e.localizedMessage ?: "حدث خطأ أثناء توليد القصة")
            }
        }
    }

    /**
     * Analyze YouTube video and create similar inspired story.
     */
    fun createStoryFromYouTube(
        youtubeUrl: String,
        notes: String,
        style: String,
        aspectRatio: String
    ) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري تحليل فيديو يوتيوب وصياغة سيناريو شبيه متطور...")
            try {
                val story = geminiClient.analyzeAndRecreateYouTube(
                    youtubeUrl = youtubeUrl,
                    userNotes = notes,
                    style = style,
                    aspectRatio = aspectRatio
                )
                _activeStory.value = story.copy(userId = currentUserId ?: "")
                _generationState.value = GenerationState.Success("تم تحليل الفيديو وصناعة قصة جديدة بنجاح!")
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
    fun updateScene(index: Int, narration: String, imagePrompt: String, durationSec: Int) {
        val current = _activeStory.value ?: return
        val updatedScenes = current.scenes.toMutableList()
        val targetIndex = updatedScenes.indexOfFirst { it.sceneIndex == index }
        if (targetIndex >= 0) {
            val scene = updatedScenes[targetIndex]
            updatedScenes[targetIndex] = scene.copy(
                narration = narration,
                imagePrompt = imagePrompt,
                durationSec = durationSec
            )
            _activeStory.value = current.copy(scenes = updatedScenes)
        }
    }

    /**
     * Generate or regenerate image for a scene using gemini-3.1-flash-image-preview.
     */
    fun generateSceneImage(sceneIndex: Int, prompt: String, aspectRatio: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري توليد صورة المشهد بدقة عالية...")
            try {
                val imagePath = geminiClient.generateSceneImage(prompt, aspectRatio)
                if (imagePath != null) {
                    val current = _activeStory.value ?: return@launch
                    val updatedScenes = current.scenes.toMutableList()
                    val targetIndex = updatedScenes.indexOfFirst { it.sceneIndex == sceneIndex }
                    if (targetIndex >= 0) {
                        updatedScenes[targetIndex] = updatedScenes[targetIndex].copy(imageUrl = imagePath)
                        _activeStory.value = current.copy(scenes = updatedScenes)
                    }
                    _generationState.value = GenerationState.Success("تم توليد صورة المشهد بنجاح!")
                } else {
                    _generationState.value = GenerationState.Error("تعذر توليد الصورة، تحقق من الاتصال أو المفتاح.")
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error generating image: ${e.message}", e)
                _generationState.value = GenerationState.Error("خطأ في توليد الصورة: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Generate TTS voiceover for a scene using gemini-3.8-flash-tts.
     */
    fun generateSceneSpeech(sceneIndex: Int, text: String, voiceName: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري توليد التعليق الصوتي الدرامي...")
            try {
                val audioFile = geminiClient.generateSpeech(text, voiceName)
                if (audioFile != null) {
                    sceneAudioFiles[sceneIndex] = audioFile
                    audioPlayer.playFile(audioFile)
                    _generationState.value = GenerationState.Success("تم توليد الصوت وتشغيله بنجاح!")
                } else {
                    _generationState.value = GenerationState.Error("تعذر توليد التعليق الصوتي.")
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "TTS error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل توليد الصوت: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Generate Music track using Lyria.
     */
    fun generateMusic(prompt: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري توليد الموسيقى التصويرية عبر Lyria...")
            try {
                val musicFile = geminiClient.generateMusic(prompt)
                if (musicFile != null) {
                    bgmAudioFile = musicFile
                    audioPlayer.playFile(musicFile)
                    _generationState.value = GenerationState.Success("تم توليد الموسيقى التصويرية وتشغيلها!")
                } else {
                    _generationState.value = GenerationState.Error("تعذر توليد المقطع الموسيقي.")
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Music error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل توليد الموسيقى: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Generate Veo video animation for a scene using veo-3.1-fast-generate-preview.
     */
    fun generateVeoVideo(sceneIndex: Int, prompt: String, aspectRatio: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("جاري إرسال طلب تصيير فيديو سينمائي عبر Veo 3...")
            try {
                val status = geminiClient.generateVeoVideo(prompt, aspectRatio)
                val current = _activeStory.value ?: return@launch
                val updatedScenes = current.scenes.toMutableList()
                val targetIndex = updatedScenes.indexOfFirst { it.sceneIndex == sceneIndex }
                if (targetIndex >= 0) {
                    updatedScenes[targetIndex] = updatedScenes[targetIndex].copy(videoUrl = status)
                    _activeStory.value = current.copy(scenes = updatedScenes)
                }
                _generationState.value = GenerationState.Success("تم بدء توليد فيديو المشهد بنجاح!")
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Veo error: ${e.message}", e)
                _generationState.value = GenerationState.Error("فشل تصيير الفيديو: ${e.localizedMessage}")
            }
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
                    text = "أعتذر، حدثت مشكلة في الاتصال. يمكنك إعادة المحاولة وسأكون معك لصياغة الأفكار."
                )
            }
        }
    }

    fun dismissState() {
        _generationState.value = GenerationState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
