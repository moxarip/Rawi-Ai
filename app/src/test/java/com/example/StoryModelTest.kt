package com.example

import com.example.data.model.StoryProject
import com.example.data.model.StoryScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryModelTest {

    @Test
    fun storyProject_defaultValues_areValid() {
        val project = StoryProject(
            id = "test_1",
            userId = "user_123",
            title = "قصة المغامر",
            prompt = "مغامرة في الفضاء",
            scenes = listOf(
                StoryScene(sceneIndex = 1, narration = "في البداية", imagePrompt = "Prompt 1")
            )
        )

        assertEquals("test_1", project.id)
        assertEquals("user_123", project.userId)
        assertEquals("قصة المغامر", project.title)
        assertEquals(1, project.scenes.size)
        assertEquals("16:9", project.aspectRatio)
    }

    @Test
    fun storyScene_updatesCorrectly() {
        val scene = StoryScene(sceneIndex = 1, narration = "نص قديم", imagePrompt = "قديم")
        val updated = scene.copy(narration = "نص جديد بعد التعديل", imagePrompt = "وصف جديد")

        assertEquals("نص جديد بعد التعديل", updated.narration)
        assertEquals("وصف جديد", updated.imagePrompt)
    }
}
