package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ActivityLog
import com.example.data.model.CompanionMessage
import com.example.data.model.JournalEntry
import com.example.data.model.MoodLog
import com.example.data.model.MoodType
import com.example.data.model.UserProfile
import com.example.data.model.WellnessRecommendation
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class MindMateRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    private fun <T> unwrapFirestoreFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        userId: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        val activeUid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
        if (exceptionToLog != null && (activeUid == null || activeUid == userId)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    unwrapFirestoreFlowError<UserProfile?>(error, OperationType.GET, path, userId)
                }
        )
    }

    fun observeMoodLogs(userId: String): Flow<List<MoodLog>> = flow {
        val path = "users/$userId/mood_logs"
        emitAll(
            userDoc(userId).collection("mood_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(120)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MoodLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapFirestoreFlowError<List<MoodLog>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeJournalEntries(userId: String): Flow<List<JournalEntry>> = flow {
        val path = "users/$userId/journal_entries"
        emitAll(
            userDoc(userId).collection("journal_entries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JournalEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapFirestoreFlowError<List<JournalEntry>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeActivityLogs(userId: String): Flow<List<ActivityLog>> = flow {
        val path = "users/$userId/activity_logs"
        emitAll(
            userDoc(userId).collection("activity_logs")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(150)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActivityLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapFirestoreFlowError<List<ActivityLog>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeConversation(userId: String): Flow<List<CompanionMessage>> = flow {
        val path = "users/$userId/conversations"
        emitAll(
            userDoc(userId).collection("conversations")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CompanionMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    unwrapFirestoreFlowError<List<CompanionMessage>>(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): UserProfile {
        val docRef = userDoc(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) return existing
            }
            val safeName = displayName.trim().ifBlank { "Friend" }.take(60)
            val safeEmail = email.trim().take(120)
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "displayName" to safeName,
                "email" to safeEmail,
                "dietaryPreference" to "balanced",
                "activityPreference" to "moderate",
                "indoorOutdoorPreference" to "both",
                "wellnessGoal" to "More calm pauses and steady hydration",
                "hydrationGoalMl" to 2000L,
                "hydrationTodayMl" to 500L,
                "notificationsEnabled" to true,
                "voiceSpeechEnabled" to true,
                "reducedMotionEnabled" to false,
                "voiceConsentGranted" to false,
                "memoryConsentGranted" to false,
                "conversationHistoryEnabled" to true,
                "onboardingCompleted" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                payload["photoUrl"] = photoUrl.take(500)
            }
            docRef.set(payload).await()
            return docRef.get().await().toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(id = userId, userId = userId, displayName = safeName, email = safeEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateHydration(userId: String, newHydrationMl: Long) {
        val docRef = userDoc(userId)
        val clamped = newHydrationMl.coerceIn(0L, 10000L)
        try {
            docRef.update(
                mapOf(
                    "hydrationTodayMl" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfileSettings(userId: String, profile: UserProfile) {
        val docRef = userDoc(userId)
        val safeName = profile.displayName.trim().ifBlank { "Friend" }.take(60)
        val safeGoal = profile.wellnessGoal.trim().take(160)
        val updates = mapOf(
            "displayName" to safeName,
            "dietaryPreference" to profile.dietaryPreference,
            "activityPreference" to profile.activityPreference,
            "indoorOutdoorPreference" to profile.indoorOutdoorPreference,
            "wellnessGoal" to safeGoal,
            "hydrationGoalMl" to profile.hydrationGoalMl.coerceIn(500L, 5000L),
            "hydrationTodayMl" to profile.hydrationTodayMl.coerceIn(0L, 10000L),
            "notificationsEnabled" to profile.notificationsEnabled,
            "voiceSpeechEnabled" to profile.voiceSpeechEnabled,
            "reducedMotionEnabled" to profile.reducedMotionEnabled,
            "voiceConsentGranted" to profile.voiceConsentGranted,
            "memoryConsentGranted" to profile.memoryConsentGranted,
            "conversationHistoryEnabled" to profile.conversationHistoryEnabled,
            "onboardingCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun logMood(
        userId: String,
        mood: MoodType,
        note: String,
        energyLevel: Int
    ): String {
        val docRef = userDoc(userId).collection("mood_logs").document()
        val payload = mapOf(
            "userId" to userId,
            "mood" to mood.id,
            "note" to note.trim().take(500),
            "energyLevel" to energyLevel.coerceIn(1, 5).toLong(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun createJournalEntry(
        userId: String,
        title: String,
        content: String,
        moodTag: String
    ): String {
        val docRef = userDoc(userId).collection("journal_entries").document()
        val cleanContent = content.trim().take(4000)
        require(cleanContent.isNotEmpty()) { "Journal content cannot be empty." }
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().take(120),
            "content" to cleanContent,
            "moodTag" to moodTag,
            "aiSummary" to "",
            "aiPromptQuestion" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateJournalEntry(userId: String, entry: JournalEntry) {
        require(entry.id.isNotBlank()) { "Journal entry ID is required for update." }
        val docRef = userDoc(userId).collection("journal_entries").document(entry.id)
        val cleanContent = entry.content.trim().take(4000)
        require(cleanContent.isNotEmpty()) { "Journal content cannot be empty." }
        try {
            docRef.update(
                mapOf(
                    "title" to entry.title.trim().take(120),
                    "content" to cleanContent,
                    "moodTag" to entry.moodTag,
                    "aiSummary" to entry.aiSummary.take(1000),
                    "aiPromptQuestion" to entry.aiPromptQuestion.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveJournalAiReflection(
        userId: String,
        entryId: String,
        aiSummary: String,
        aiPromptQuestion: String
    ) {
        require(entryId.isNotBlank()) { "Journal entry ID is required." }
        val docRef = userDoc(userId).collection("journal_entries").document(entryId)
        try {
            docRef.update(
                mapOf(
                    "aiSummary" to aiSummary.trim().take(1000),
                    "aiPromptQuestion" to aiPromptQuestion.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteJournalEntry(userId: String, entryId: String) {
        val docRef = userDoc(userId).collection("journal_entries").document(entryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun setActivityStatus(
        userId: String,
        recommendation: WellnessRecommendation,
        status: String
    ): String {
        val colRef = userDoc(userId).collection("activity_logs")
        val docRef = colRef.document(recommendation.id)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "recommendationId" to recommendation.id.take(80),
                        "title" to recommendation.title.take(120),
                        "category" to recommendation.category.id,
                        "durationMinutes" to recommendation.durationMinutes.coerceIn(1, 240).toLong(),
                        "status" to status,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun addCompanionMessage(
        userId: String,
        role: String,
        mode: String,
        content: String
    ): String {
        val docRef = userDoc(userId).collection("conversations").document()
        val cleanContent = content.trim().take(4000)
        require(cleanContent.isNotEmpty()) { "Message cannot be empty." }
        val payload = mapOf(
            "userId" to userId,
            "role" to role,
            "mode" to mode,
            "content" to cleanContent,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun clearConversationHistory(userId: String) {
        val colPath = "users/$userId/conversations"
        try {
            val docs = userDoc(userId).collection("conversations").get().await()
            val batch = db.batch()
            docs.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colPath)
            throw e
        }
    }

    suspend fun deleteAllUserData(userId: String) {
        val subcollections = listOf("mood_logs", "journal_entries", "activity_logs", "conversations")
        try {
            for (sub in subcollections) {
                val snapshot = userDoc(userId).collection(sub).get().await()
                if (!snapshot.isEmpty) {
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            userDoc(userId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            throw e
        }
    }

    fun exportUserDataJson(
        profile: UserProfile?,
        moods: List<MoodLog>,
        journals: List<JournalEntry>,
        activities: List<ActivityLog>,
        messages: List<CompanionMessage>
    ): String {
        return JSONObject().apply {
            put("exportedAt", Timestamp.now().toDate().toString())
            put("app", "MindMate")
            put("profile", JSONObject().apply {
                put("displayName", profile?.displayName ?: "")
                put("email", profile?.email ?: "")
                put("wellnessGoal", profile?.wellnessGoal ?: "")
                put("dietaryPreference", profile?.dietaryPreference ?: "")
                put("activityPreference", profile?.activityPreference ?: "")
                put("hydrationGoalMl", profile?.hydrationGoalMl ?: 2000L)
            })
            put("moodLogs", JSONArray(moods.map { m ->
                JSONObject().apply {
                    put("mood", m.mood)
                    put("energyLevel", m.energyLevel)
                    put("note", m.note)
                    put("createdAt", m.createdAt?.toDate()?.toString() ?: "")
                }
            }))
            put("journalEntries", JSONArray(journals.map { j ->
                JSONObject().apply {
                    put("title", j.title)
                    put("content", j.content)
                    put("moodTag", j.moodTag)
                    put("aiSummary", j.aiSummary)
                    put("createdAt", j.createdAt?.toDate()?.toString() ?: "")
                }
            }))
            put("activityLogs", JSONArray(activities.map { a ->
                JSONObject().apply {
                    put("title", a.title)
                    put("category", a.category)
                    put("durationMinutes", a.durationMinutes)
                    put("status", a.status)
                }
            }))
            put("conversationCount", messages.size)
        }.toString(2)
    }
}
