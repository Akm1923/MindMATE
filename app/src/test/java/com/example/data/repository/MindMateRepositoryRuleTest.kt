package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.MoodType
import com.example.data.model.WellnessCategory
import com.example.data.model.WellnessRecommendation
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MindMateRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileAndLogMood_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = MindMateRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile(uid, "Alex", ALICE_EMAIL, null)
        }
        assertEquals(uid, profile.userId)
        assertEquals("Alex", profile.displayName)

        val moodId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logMood(uid, MoodType.GOOD, "Morning sun felt refreshing", 4)
        }
        assertTrue(moodId.isNotBlank())

        val moods = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMoodLogs(uid).first { list -> list.any { it.id == moodId } }
        }
        assertTrue(moods.any { it.id == moodId && it.mood == "good" })
    }

    @Test
    fun journalCrudAndActivityStatus_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = MindMateRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile(uid, "Alex", ALICE_EMAIL, null)
        }

        val entryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createJournalEntry(
                userId = uid,
                title = "Evening reset",
                content = "Stepped away from screens for 15 minutes and stretched.",
                moodTag = "okay"
            )
        }
        assertTrue(entryId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveJournalAiReflection(
                userId = uid,
                entryId = entryId,
                aiSummary = "You noticed screen fatigue and created space to stretch.",
                aiPromptQuestion = "How did your body feel right after stepping away?"
            )
        }

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeJournalEntries(uid).first { list ->
                list.any { it.id == entryId && it.aiSummary.isNotBlank() }
            }
        }
        assertTrue(entries.any { it.id == entryId })

        val rec = WellnessRecommendation(
            id = "rec_${UUID.randomUUID().toString().take(8)}",
            title = "4-7-8 Calm Breathing",
            description = "Slow rhythmic breathing",
            whySuggested = "Helps settle a busy afternoon",
            category = WellnessCategory.RELAX,
            durationMinutes = 5,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.STRESSED, MoodType.OKAY)
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setActivityStatus(uid, rec, "completed")
        }
        val activities = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeActivityLogs(uid).first { list -> list.any { it.recommendationId == rec.id } }
        }
        assertEquals("completed", activities.first { it.recommendationId == rec.id }.status)
    }

    @Test
    fun observeMoodLogs_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = MindMateRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(aliceUid, "Alice", ALICE_EMAIL, null)
            repo.logMood(aliceUid, MoodType.GREAT, "Private note", 5)
        }

        signInTestUser(BOB_EMAIL)
        val exception = assertThrowsSuspend<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMoodLogs(aliceUid).first()
            }
        }
        val firestoreEx = exception.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeMoodLogs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MindMateRepository(firestore)
        val exception = assertThrowsSuspend<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMoodLogs("some_user_123").first()
            }
        }
        val firestoreEx = exception.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_mindmate@test.com"
        const val BOB_EMAIL = "bob_mindmate@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
