package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class StoryScene(
    val sceneIndex: Int = 1,
    val narration: String = "",
    val imagePrompt: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val durationSec: Int = 5
)

data class StoryProject(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val prompt: String = "",
    val youtubeUrl: String = "",
    val aspectRatio: String = "16:9",
    val style: String = "سينمائي كلاسيكي",
    val status: String = "جاهز",
    val bgmTrackName: String = "لحن ملحمي هادئ",
    val voiceName: String = "Kore",
    val scenes: List<StoryScene> = emptyList(),
    val fullScript: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredVoice: String = "Kore",
    val preferredRatio: String = "16:9",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
