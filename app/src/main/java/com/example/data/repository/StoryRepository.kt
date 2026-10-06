package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.StoryProject
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeStories(userId: String): Flow<List<StoryProject>> = flow {
        val path = "users/$userId/stories"
        emitAll(
            db.collection("users").document(userId).collection("stories")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(StoryProject::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveStory(story: StoryProject): String {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("stories")
        val docRef = if (story.id.isNotEmpty()) collectionRef.document(story.id) else collectionRef.document()
        val storyId = docRef.id

        val payload = hashMapOf<String, Any>(
            "id" to storyId,
            "userId" to uid,
            "title" to story.title,
            "prompt" to story.prompt,
            "youtubeUrl" to story.youtubeUrl,
            "aspectRatio" to story.aspectRatio,
            "style" to story.style,
            "status" to story.status,
            "bgmTrackName" to story.bgmTrackName,
            "voiceName" to story.voiceName,
            "scenes" to story.scenes.map { scene ->
                mapOf(
                    "sceneIndex" to scene.sceneIndex,
                    "narration" to scene.narration,
                    "imagePrompt" to scene.imagePrompt,
                    "imageUrl" to scene.imageUrl,
                    "videoUrl" to scene.videoUrl,
                    "durationSec" to scene.durationSec
                )
            },
            "fullScript" to story.fullScript,
            "createdAt" to (story.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return storyId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStory(storyId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("stories").document(storyId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("profile").document("main")
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "preferredVoice" to profile.preferredVoice,
            "preferredRatio" to profile.preferredRatio,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
