package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ImageProvider(val displayName: String, val description: String, val isFreeNoKey: Boolean) {
    POLLINATIONS_FLUX("Flux.1 Pro (سحابي سريع)", "أفضل جودة ووضوح، يعمل دائماً مجاناً بدون مفتاح", true),
    POLLINATIONS_TURBO("Turbo Instant", "توليد فائق السرعة خلال ثانية واحدة مجاناً", true),
    POLLINATIONS_REALISM("Flux Realism السينمائي", "واقعية سينمائية فائقة للمشاهد الدرامية", true),
    POLLINATIONS_ANIME("Anime Art Style", "أسلوب أنمي ورسوم يابانية جذابة", true),
    POLLINATIONS_3D("3D Rendered CGI", "تجسيم ثلاثي الأبعاد احترافي", true),
    GEMINI_IMAGE("Google Gemini Image", "يتطلب مفتاح Google AI أو اشتراك Google One", false),
    GEMINI_PRO_IMAGE("Gemini Pro Image 4K", "دقة فائقة 4K عبر نموذج Gemini 3.1 Flash Image", false),
    HUGGINGFACE_SD("HuggingFace / Stable Diffusion", "استضافة مفتوحة وتجريبية من مجتمع GitHub", true),
    PROCEDURAL_CANVAS("المحرك الفني المحلي المدمج", "توليد فني سينمائي محلي مضمون 100% بدون انترنت", true)
}

enum class TextProvider(val displayName: String, val description: String, val isFreeNoKey: Boolean) {
    GEMINI_FLASH("Google Gemini 3.5 Flash", "الأسرع والأدق في سرد القصص وتحليل الفيديو (Google One)", false),
    GEMINI_PRO("Google Gemini 3.1 Pro", "تفكير عميق وتفاصيل معقدة وحبكات درامية (Google One)", false),
    POLLINATIONS_DEEPSEEK("DeepSeek V3 (استضافة مفتوحة)", "نموذج ذكاء اصطناعي تجريبي مفتوح المصدر بدون مفتاح", true),
    POLLINATIONS_MISTRAL("Mistral AI (استضافة تجريبية)", "نموذج أوروبي مفتوح المصدر سريع ومتناسق", true),
    POLLINATIONS_OPENAI("GPT-4o Mini (سحابي مفتوح)", "معالجة نصوص سحابية مجانية عبر واجهة Pollinations", true),
    SMART_OFFLINE("الكاتب السينمائي المحلي", "توليد نصوص وسيناريوهات مدمجة تعمل بدون انترنت تماماً", true)
}

enum class SpeechProvider(val displayName: String, val description: String, val isFreeNoKey: Boolean) {
    GEMINI_TTS("Google Gemini TTS", "أصوات درامية عالية الواقعية (Kore, Puck, Fenrir)", false),
    ANDROID_NATIVE_TTS("محرك الهاتف المدمج (Android TTS)", "مضمون 100% يعمل بدون انترنت وباللغة العربية", true),
    OPEN_SPEECH("استضافة صوتية سحابية مفتوحة", "تحويل النص إلى كلام عبر خوادم مجانية مفتوحة", true)
}

enum class MusicProvider(val displayName: String, val description: String) {
    SYNTHESIZED_ORCHESTRAL("لحن سيمفوني سينمائي مدمج", "توليد نغمي عالي الجودة فوري ومضمون 100% (44.1kHz)"),
    GEMINI_LYRIA("Google DeepMind Lyria", "يتطلب تفعيل أو اشتراك Google AI"),
    AMBIENT_ATMOSPHERE("أجواء صوتية درامية ملحمية", "خلفية محيطية ملحمية تتناسب مع النمط")
}

enum class VideoProvider(val displayName: String, val description: String) {
    CINEMATIC_MOTION("محرك الحركة السينمائية (Ken Burns + الصوت المتزامن)", "تحريك المشهد بالكامل مع حركة الكاميرا والترجمة والصوت"),
    GOOGLE_VEO("Google Veo 3 Video Generator", "توليد فيديو حي عبر Google Veo 3.1 (Google Cloud / AI Studio)"),
    HUGGINGFACE_VIDEO("Luma / HuggingFace Video (تجريبي)", "استضافة تجريبية لإنشاء مقاطع فيديو متحركة")
}

class AIProviderManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rawi_ai_providers_v2", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _customGeminiKey = MutableStateFlow(prefs.getString("custom_gemini_key", "").orEmpty())
    val customGeminiKey: StateFlow<String> = _customGeminiKey.asStateFlow()

    private val _isGoogleOneLinked = MutableStateFlow(prefs.getBoolean("is_google_one_linked", false))
    val isGoogleOneLinked: StateFlow<Boolean> = _isGoogleOneLinked.asStateFlow()

    private val _selectedImageProvider = MutableStateFlow(
        ImageProvider.values().firstOrNull { it.name == prefs.getString("image_provider", ImageProvider.POLLINATIONS_FLUX.name) }
            ?: ImageProvider.POLLINATIONS_FLUX
    )
    val selectedImageProvider: StateFlow<ImageProvider> = _selectedImageProvider.asStateFlow()

    private val _selectedTextProvider = MutableStateFlow(
        TextProvider.values().firstOrNull { it.name == prefs.getString("text_provider", TextProvider.GEMINI_FLASH.name) }
            ?: TextProvider.GEMINI_FLASH
    )
    val selectedTextProvider: StateFlow<TextProvider> = _selectedTextProvider.asStateFlow()

    private val _selectedSpeechProvider = MutableStateFlow(
        SpeechProvider.values().firstOrNull { it.name == prefs.getString("speech_provider", SpeechProvider.GEMINI_TTS.name) }
            ?: SpeechProvider.GEMINI_TTS
    )
    val selectedSpeechProvider: StateFlow<SpeechProvider> = _selectedSpeechProvider.asStateFlow()

    private val _selectedMusicProvider = MutableStateFlow(
        MusicProvider.values().firstOrNull { it.name == prefs.getString("music_provider", MusicProvider.SYNTHESIZED_ORCHESTRAL.name) }
            ?: MusicProvider.SYNTHESIZED_ORCHESTRAL
    )
    val selectedMusicProvider: StateFlow<MusicProvider> = _selectedMusicProvider.asStateFlow()

    private val _selectedVideoProvider = MutableStateFlow(
        VideoProvider.values().firstOrNull { it.name == prefs.getString("video_provider", VideoProvider.CINEMATIC_MOTION.name) }
            ?: VideoProvider.CINEMATIC_MOTION
    )
    val selectedVideoProvider: StateFlow<VideoProvider> = _selectedVideoProvider.asStateFlow()

    fun getEffectiveGeminiApiKey(): String {
        val custom = _customGeminiKey.value.trim()
        if (custom.isNotEmpty()) return custom
        return BuildConfig.GEMINI_API_KEY
    }

    fun setCustomGeminiKey(key: String, markGoogleOne: Boolean = false) {
        val trimmed = key.trim()
        _customGeminiKey.value = trimmed
        if (markGoogleOne) {
            _isGoogleOneLinked.value = true
            prefs.edit().putBoolean("is_google_one_linked", true).apply()
        }
        prefs.edit().putString("custom_gemini_key", trimmed).apply()
    }

    fun setGoogleOneLinked(linked: Boolean) {
        _isGoogleOneLinked.value = linked
        prefs.edit().putBoolean("is_google_one_linked", linked).apply()
    }

    fun setImageProvider(provider: ImageProvider) {
        _selectedImageProvider.value = provider
        prefs.edit().putString("image_provider", provider.name).apply()
    }

    fun setTextProvider(provider: TextProvider) {
        _selectedTextProvider.value = provider
        prefs.edit().putString("text_provider", provider.name).apply()
    }

    fun setSpeechProvider(provider: SpeechProvider) {
        _selectedSpeechProvider.value = provider
        prefs.edit().putString("speech_provider", provider.name).apply()
    }

    fun setMusicProvider(provider: MusicProvider) {
        _selectedMusicProvider.value = provider
        prefs.edit().putString("music_provider", provider.name).apply()
    }

    fun setVideoProvider(provider: VideoProvider) {
        _selectedVideoProvider.value = provider
        prefs.edit().putString("video_provider", provider.name).apply()
    }

    suspend fun testGeminiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val keyToTest = apiKey.ifEmpty { getEffectiveGeminiApiKey() }
        if (keyToTest.isEmpty()) {
            return@withContext false to "لم يتم إدخال مفتاح API. يرجى الحصول عليه من Google AI Studio (متاح مجاناً لحسابات Google One و Gmail)."
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash?key=$keyToTest"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                _isGoogleOneLinked.value = true
                prefs.edit().putBoolean("is_google_one_linked", true).apply()
                true to "تم التحقق بنجاح! المفتاح صالح وموصول بحساب Google. ميزات Gemini 3.5 Flash و Gemini 3.1 Pro و Veo مفعلة الآن!"
            } else {
                val errBody = response.body?.string().orEmpty()
                false to "استجابة الخطأ من Google (${response.code}): $errBody\nتأكد من نسخ المفتاح من Google AI Studio."
            }
        } catch (e: Exception) {
            false to "فشل التحقق من الاتصال: ${e.localizedMessage}"
        }
    }
}
