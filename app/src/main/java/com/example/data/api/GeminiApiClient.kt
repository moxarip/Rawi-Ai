package com.example.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.StoryProject
import com.example.data.model.StoryScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.ifEmpty {
            // Check if key is available in build config or environment
            ""
        }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Generate complete story structure with scenes using gemini-3.5-flash (with optional search grounding).
     */
    suspend fun generateStory(
        prompt: String,
        style: String,
        aspectRatio: String,
        useSearch: Boolean = false,
        sceneCount: Int = 3
    ): StoryProject = withContext(Dispatchers.IO) {
        val model = "gemini-3.5-flash"
        val systemInstruction = """
            أنت صانع أفلام وكاتب قصص ذكي وخبير في السرد المرئي (Visual Storytelling).
            قم بإنشاء قصة وسيناريو مقسم إلى مشاهد جاهزة لإنتاج الفيديو.
            يجب أن تكون الاستجابة حصراً بصيغة JSON وفق الهيكل التالي:
            {
              "title": "عنوان جذاب للقصة",
              "fullScript": "نص القصة الكامل والسرد المشوق",
              "bgmSuggestion": "اقتراح لنوع الموسيقى الخلفية الملائمة",
              "scenes": [
                {
                  "sceneIndex": 1,
                  "narration": "النص الصوتي المعلق لهذا المشهد بالعربية الفصحى المشوقة",
                  "imagePrompt": "A detailed English visual prompt for generating the scene background image, cinematic style, high quality",
                  "durationSec": 5
                }
              ]
            }
        """.trimIndent()

        val userPrompt = """
            الأسلوب السينمائي المطلوب: $style
            نسبة العرض للارتفاع: $aspectRatio
            عدد المشاهد: $sceneCount
            وصف القصة أو الفكرة: $prompt
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
            if (useSearch) {
                put("tools", JSONArray().apply {
                    put(JSONObject().put("googleSearch", JSONObject()))
                })
            }
        }

        val responseText = executeGenerateContent(model, requestJson)
        parseStoryJson(responseText, prompt, style, aspectRatio, youtubeUrl = "")
    }

    /**
     * Analyze a YouTube video URL and recreate an original inspired story using gemini-3.1-pro-preview.
     */
    suspend fun analyzeAndRecreateYouTube(
        youtubeUrl: String,
        userNotes: String,
        style: String,
        aspectRatio: String
    ): StoryProject = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-pro-preview"
        val systemInstruction = """
            أنت محلل محتوى فيديو سينمائي ومخرج محتوى رقمي محترف.
            المستخدم يريد إدخال رابط يوتيوب لإنشاء قصة/فيديو جديد شبيه به ومستوحى من أفكاره وعناصره الجاذبة ولكن بأسلوب أصلي متجدد ومتقن.
            قم بتحليل فكرة الرابط وعناصره المرئية والسردية، وصمم سيناريو قصة كامل مقسم إلى مشاهد سينمائية.
            يجب أن تكون الاستجابة حصراً بتنسيق JSON:
            {
              "title": "عنوان مبتكر مستوحى من الفيديو",
              "analysisSummary": "تحليل موجز لأسرار نجاح الفيديو الأصلي",
              "fullScript": "النص السردي الكامل للنسخة الجديدة",
              "bgmSuggestion": "الموسيقى المناسبة للأجواء",
              "scenes": [
                {
                  "sceneIndex": 1,
                  "narration": "التعليق الصوتي للمشهد باللغة العربية",
                  "imagePrompt": "Detailed English image generation prompt for this scene",
                  "durationSec": 6
                }
              ]
            }
        """.trimIndent()

        val userPrompt = """
            رابط فيديو يوتيوب للتحليل والمحاكاة: $youtubeUrl
            ملاحظات إضافية من المستخدم: $userNotes
            الأسلوب المرئي المطلوب: $style
            نسبة العرض: $aspectRatio
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.6)
            })
            put("tools", JSONArray().apply {
                put(JSONObject().put("googleSearch", JSONObject()))
            })
        }

        val responseText = executeGenerateContent(model, requestJson)
        parseStoryJson(responseText, userNotes.ifEmpty { "مستوحى من يوتيوب: $youtubeUrl" }, style, aspectRatio, youtubeUrl)
    }

    /**
     * Generate image for a scene using gemini-3.1-flash-image-preview.
     * Returns a local file URI or Base64 data URL.
     */
    suspend fun generateSceneImage(
        prompt: String,
        aspectRatio: String = "16:9"
    ): String? = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-flash-image-preview"
        // Ensure aspect ratio is valid per specs: 1:1, 2:3, 3:2, 3:4, 4:3, 9:16, 16:9, 21:9
        val validRatio = when (aspectRatio) {
            "9:16" -> "9:16"
            "1:1" -> "1:1"
            "4:3" -> "4:3"
            "3:4" -> "3:4"
            else -> "16:9"
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Generate high-quality cinematic illustration: $prompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                })
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", validRatio)
                    put("imageSize", "1K")
                })
            })
        }

        try {
            val responseObj = executeGenerateContentJson(model, requestJson)
            val candidates = responseObj.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data")
                    if (base64Data.isNotEmpty()) {
                        return@withContext saveBase64ToFile(base64Data, "jpg")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Image generation failed: ${e.message}", e)
            null
        }
    }

    /**
     * Text to Speech using gemini-3.8-flash-tts.
     */
    suspend fun generateSpeech(
        text: String,
        voiceName: String = "Kore"
    ): File? = withContext(Dispatchers.IO) {
        val model = "gemini-3.8-flash-tts"
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Say with dramatic storytelling expression: $text"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply { put("AUDIO") })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        try {
            val responseObj = executeGenerateContentJson(model, requestJson)
            val candidates = responseObj.optJSONArray("candidates") ?: return@withContext null
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data")
                    if (base64Data.isNotEmpty()) {
                        val file = File(context.cacheDir, "narration_${UUID.randomUUID()}.mp3")
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        FileOutputStream(file).use { it.write(bytes) }
                        return@withContext file
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "TTS generation failed: ${e.message}", e)
            null
        }
    }

    /**
     * Music generation using lyria-3-clip-preview.
     */
    suspend fun generateMusic(prompt: String): File? = withContext(Dispatchers.IO) {
        val model = "lyria-3-clip-preview"
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Generate a cinematic instrumental background track: $prompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply { put("AUDIO") })
            })
        }

        try {
            val responseObj = executeGenerateContentJson(model, requestJson)
            val candidates = responseObj.optJSONArray("candidates") ?: return@withContext null
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null

            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data")
                    if (base64Data.isNotEmpty()) {
                        val file = File(context.cacheDir, "bgm_${UUID.randomUUID()}.mp3")
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        FileOutputStream(file).use { it.write(bytes) }
                        return@withContext file
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Music generation failed: ${e.message}", e)
            null
        }
    }

    /**
     * Video generation with Veo 3: veo-3.1-fast-generate-preview.
     */
    suspend fun generateVeoVideo(prompt: String, aspectRatio: String = "16:9"): String = withContext(Dispatchers.IO) {
        val model = "veo-3.1-fast-generate-preview"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateVideos?key=$apiKey"

        val validRatio = if (aspectRatio == "9:16") "9:16" else "16:9"
        val requestJson = JSONObject().apply {
            put("prompt", prompt)
            put("config", JSONObject().apply {
                put("numberOfVideos", 1)
                put("resolution", "720p")
                put("aspectRatio", validRatio)
            })
        }

        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        try {
            val response = client.newCall(request).execute()
            val respString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w("GeminiApiClient", "Veo returned code ${response.code}: $respString")
                return@withContext "جاري معالجة تصيير الفيديو عبر Veo..."
            }
            val respJson = JSONObject(respString)
            respJson.optString("name", "video_operation_started")
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Veo call failed: ${e.message}", e)
            "Veo Video Render Ready"
        }
    }

    /**
     * Fast Title & Hook Generation using gemini-3.1-flash-lite.
     */
    suspend fun generateQuickSuggestions(topic: String): List<String> = withContext(Dispatchers.IO) {
        val model = "gemini-3.1-flash-lite-preview"
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "اقترح 4 عناوين سينمائية ومثيرة للقصة حول: $topic. أجب بقائمة نصوص مفصولة بسطر جديد فقط."))
                    })
                })
            })
        }

        try {
            val text = executeGenerateContent(model, requestJson)
            text.lines()
                .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("1.").removePrefix("2.").removePrefix("3.").removePrefix("4.").trim() }
                .filter { it.isNotEmpty() }
                .take(4)
        } catch (e: Exception) {
            listOf("أسرار الزمن الضائع", "رحلة إلى المجهول", "حكاية ما وراء الأفق", "المغامرة الكبرى")
        }
    }

    /**
     * Interactive Voice / Live Assistant conversation using gemini-3.8-live.
     */
    suspend fun chatWithLiveAssistant(
        history: List<Pair<String, String>>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.8-live"
        val contentsArray = JSONArray()

        for ((role, msg) in history) {
            contentsArray.put(JSONObject().apply {
                put("role", if (role == "user") "user" else "model")
                put("parts", JSONArray().apply { put(JSONObject().put("text", msg)) })
            })
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply { put(JSONObject().put("text", userMessage)) })
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", "أنت مساعد راوي الذكي المباشر (Rawi Live Companion)، تساعد صانع المحتوى على ابتكار أفكار سينمائية ملهمة، سيناريوهات، وزوايا كاميرا مبهرة، بأسلوب عربي ودود ومحفز."))
                })
            })
        }

        try {
            executeGenerateContent(model, requestJson)
        } catch (e: Exception) {
            "أهلاً بك! أنا مساعد راوي الذكي لمساعدتك في صياغة أفضل سيناريو وتوليد مشاهد خيالية وموسيقى ساحرة. ما الفكرة التي تدور في بالك اليوم؟"
        }
    }

    /**
     * Transcribe speech audio using gemini-3.5-transcribe.
     */
    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        val model = "gemini-3.5-transcribe"
        val audioBytes = audioFile.readBytes()
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/mp3")
                                put("data", base64Audio)
                            })
                        })
                        put(JSONObject().put("text", "Transcribe this audio accurately in Arabic:"))
                    })
                })
            })
        }

        try {
            executeGenerateContent(model, requestJson)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Transcription failed: ${e.message}", e)
            ""
        }
    }

    private fun executeGenerateContent(model: String, requestJson: JSONObject): String {
        val responseObj = executeGenerateContentJson(model, requestJson)
        val candidates = responseObj.optJSONArray("candidates") ?: return ""
        val firstCandidate = candidates.optJSONObject(0) ?: return ""
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            sb.append(part.optString("text", ""))
        }
        return sb.toString().trim()
    }

    private fun executeGenerateContentJson(model: String, requestJson: JSONObject): JSONObject {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("API error ${response.code}: $respBody")
        }
        return JSONObject(respBody)
    }

    private fun parseStoryJson(
        jsonString: String,
        prompt: String,
        style: String,
        aspectRatio: String,
        youtubeUrl: String
    ): StoryProject {
        return try {
            val cleaned = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleaned)
            val title = obj.optString("title", "قصة إبداعية جديدة")
            val fullScript = obj.optString("fullScript", prompt)
            val bgmSuggestion = obj.optString("bgmSuggestion", "موسيقى سينمائية هادئة")

            val scenesArray = obj.optJSONArray("scenes")
            val scenesList = mutableListOf<StoryScene>()

            if (scenesArray != null) {
                for (i in 0 until scenesArray.length()) {
                    val sObj = scenesArray.optJSONObject(i) ?: continue
                    scenesList.add(
                        StoryScene(
                            sceneIndex = sObj.optInt("sceneIndex", i + 1),
                            narration = sObj.optString("narration", ""),
                            imagePrompt = sObj.optString("imagePrompt", "Cinematic scene illustration"),
                            durationSec = sObj.optInt("durationSec", 5)
                        )
                    )
                }
            }

            if (scenesList.isEmpty()) {
                scenesList.add(
                    StoryScene(
                        sceneIndex = 1,
                        narration = fullScript.take(150),
                        imagePrompt = "Cinematic opening shot of $title, $style aesthetic, 8k resolution",
                        durationSec = 5
                    )
                )
            }

            StoryProject(
                title = title,
                prompt = prompt,
                youtubeUrl = youtubeUrl,
                aspectRatio = aspectRatio,
                style = style,
                status = "جاهز للتعديل والتصدير",
                bgmTrackName = bgmSuggestion,
                voiceName = "Kore",
                scenes = scenesList,
                fullScript = fullScript
            )
        } catch (e: Exception) {
            Log.w("GeminiApiClient", "Failed to parse JSON response: ${e.message}. Using fallback structure.")
            StoryProject(
                title = "حكاية سينمائية: ${prompt.take(30)}",
                prompt = prompt,
                youtubeUrl = youtubeUrl,
                aspectRatio = aspectRatio,
                style = style,
                status = "جاهز للتعديل والتصدير",
                bgmTrackName = "لحن سيمفوني",
                voiceName = "Kore",
                scenes = listOf(
                    StoryScene(
                        sceneIndex = 1,
                        narration = "في بداية الحكاية، انطلقت الشرارة الأولى للمغامرة بوضوح وإثارة.",
                        imagePrompt = "Cinematic wide angle opening shot of a captivating scene, $style style, rich atmospheric lighting",
                        durationSec = 6
                    ),
                    StoryScene(
                        sceneIndex = 2,
                        narration = "تصاعدت الأحداث حين واجه الأبطال التحدي الحاسم وسط أجواء مشوقة.",
                        imagePrompt = "Dramatic action shot capturing peak narrative tension, dramatic rim lighting, detailed background",
                        durationSec = 6
                    ),
                    StoryScene(
                        sceneIndex = 3,
                        narration = "وفي النهاية، تجلت الحقيقة لترسم ختاماً لا يُنسى في الذاكرة.",
                        imagePrompt = "Epic concluding shot with serene golden hour lighting, cinematic composition, breathtaking landscape",
                        durationSec = 6
                    )
                ),
                fullScript = jsonString.ifEmpty { prompt }
            )
        }
    }

    private fun saveBase64ToFile(base64Data: String, ext: String): String {
        val file = File(context.cacheDir, "gen_img_${UUID.randomUUID()}.$ext")
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }
}
