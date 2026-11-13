package com.example.data.model

import com.example.integrations.AgentScheduledAction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MoodType(
    val id: String,
    val label: String,
    val score: Int,
    val subtitle: String,
    val followUpPrompt: String
) {
    GREAT("great", "Great", 5, "Energized & uplifted", "What's contributing most to your bright energy today?"),
    GOOD("good", "Good", 4, "Steady & balanced", "What small moment felt good so far today?"),
    OKAY("okay", "Okay", 3, "Neutral & cruising", "Would you like a gentle boost or just a quiet pause?"),
    LOW("low", "Low", 2, "Quiet or heavy", "Be gentle with yourself. Want to share what's weighing on you?"),
    STRESSED("stressed", "Stressed", 1, "Tense or overwhelmed", "Let's take one slow breath. What feels most urgent right now?");

    companion object {
        fun fromId(id: String): MoodType = entries.firstOrNull { it.id == id } ?: OKAY
    }
}

enum class AiModelTier(
    val modelId: String,
    val label: String,
    val roleLabel: String,
    val description: String,
    val roleInstruction: String
) {
    HYDROGEN(
        modelId = "gemini-4-hydrogen",
        label = "Hydrogen",
        roleLabel = "General Tasks",
        description = "Balanced everyday wellness conversation",
        roleInstruction = "ROLE (Hydrogen • General Companion): Provide warm, balanced, emotionally considerate responses suited for everyday check-ins, active listening, and gentle grounding."
    ),
    CARBON(
        modelId = "gemini-4-carbon",
        label = "Carbon",
        roleLabel = "Deep Reasoning",
        description = "Thoughtful reflection & emotional pattern analysis",
        roleInstruction = "ROLE (Carbon • Deep Reasoning Guide): Reason carefully and reflectively to help the user unpack layered feelings, conflicting priorities, or recurring habits without ever diagnosing."
    ),
    ARGON(
        modelId = "gemini-4-argon",
        label = "Argon",
        roleLabel = "Complex Interactive",
        description = "Multi-step coping plans & interactive routines",
        roleInstruction = "ROLE (Argon • Interactive Wellness Architect): Guide complex interactive wellness workflows—co-creating structured, realistic daily plans, step-by-step coping sequences, and adaptable routines."
    ),
    FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        label = "Flash Lite",
        roleLabel = "Fast Tasks",
        description = "Quick, concise responses & micro-resets",
        roleInstruction = "ROLE (Flash Lite • Fast Companion): Respond rapidly with crisp, warm, concise replies (1-2 short paragraphs max) ideal for quick check-ins and casual banter."
    );

    companion object {
        fun fromModelId(modelId: String): AiModelTier =
            entries.firstOrNull { it.modelId == modelId } ?: HYDROGEN
    }
}

enum class CompanionMode(
    val id: String,
    val label: String,
    val tagline: String,
    val defaultModelTier: AiModelTier,
    val systemPrompt: String
) {
    LISTEN(
        id = "listen",
        label = "Listen",
        tagline = "Thoughtful listening & gentle questions",
        defaultModelTier = AiModelTier.HYDROGEN,
        systemPrompt = "You are MindMate in Listen Mode. Listen warmly, reflect back what you hear with empathy, and ask one thoughtful, non-intrusive follow-up question. Never rush to give unsolicited wellness checklists."
    ),
    EXPLORE(
        id = "explore",
        label = "Explore",
        tagline = "Deep reflection on feelings & patterns",
        defaultModelTier = AiModelTier.CARBON,
        systemPrompt = "You are MindMate in Explore Mode. Help the user gently unpack their thoughts, values, and emotions with curiosity and zero judgment. Never diagnose or use clinical labels."
    ),
    SUPPORT(
        id = "support",
        label = "Support",
        tagline = "Interactive coping plans & daily structure",
        defaultModelTier = AiModelTier.ARGON,
        systemPrompt = "You are MindMate in Support Mode. Offer 1-2 grounded, realistic, everyday wellness ideas (like a short walk, hydration, breaking a task down, or talking to a trusted friend). Keep it warm and practical, never prescriptive or medical."
    ),
    RELAX(
        id = "relax",
        label = "Relax",
        tagline = "Guided breathing, grounding & calm",
        defaultModelTier = AiModelTier.HYDROGEN,
        systemPrompt = "You are MindMate in Relax Mode. Speak in a soothing, unhurried cadence. Guide simple 5-4-3-2-1 grounding or slow breathing exercises step by step."
    ),
    CASUAL(
        id = "casual",
        label = "Casual",
        tagline = "Quick friendly chat & everyday life",
        defaultModelTier = AiModelTier.FLASH_LITE,
        systemPrompt = "You are MindMate in Casual Mode. Have a warm, natural, friendly conversation about hobbies, daily routines, music, books, food, or goals. Do not force wellness advice into casual chat."
    );

    companion object {
        fun fromId(id: String): CompanionMode = entries.firstOrNull { it.id == id } ?: LISTEN
    }
}

enum class OrbState(val label: String) {
    IDLE("Ready to listen"),
    LISTENING("Listening to you..."),
    SPEAKING("MindMate is speaking"),
    THINKING("Thinking..."),
    ERROR("Connection error")
}

enum class WellnessCategory(val id: String, val label: String) {
    MOVE("move", "Move"),
    EAT_WELL("eat_well", "Eat Well"),
    RELAX("relax", "Relax"),
    GET_OUTSIDE("get_outside", "Get Outside"),
    DAILY_HABITS("daily_habits", "Daily Habits");

    companion object {
        fun fromId(id: String): WellnessCategory = entries.firstOrNull { it.id == id } ?: MOVE
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Friend",
    val email: String = "",
    val photoUrl: String? = null,
    val dietaryPreference: String = "balanced",
    val activityPreference: String = "moderate",
    val indoorOutdoorPreference: String = "both",
    val wellnessGoal: String = "More calm pauses and steady hydration",
    val hydrationGoalMl: Long = 2000L,
    val hydrationTodayMl: Long = 0L,
    val notificationsEnabled: Boolean = true,
    val voiceSpeechEnabled: Boolean = true,
    val reducedMotionEnabled: Boolean = false,
    val voiceConsentGranted: Boolean = false,
    val memoryConsentGranted: Boolean = false,
    val conversationHistoryEnabled: Boolean = true,
    val onboardingCompleted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MoodLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mood: String = MoodType.OKAY.id,
    val note: String = "",
    val energyLevel: Long = 3L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JournalEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val moodTag: String = "",
    val aiSummary: String = "",
    val aiPromptQuestion: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recommendationId: String = "",
    val title: String = "",
    val category: String = WellnessCategory.MOVE.id,
    val durationMinutes: Long = 10L,
    val status: String = "saved",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CompanionMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "companion",
    val mode: String = CompanionMode.LISTEN.id,
    val content: String = "",
    val modelUsed: String = "",
    val scheduledActions: List<AgentScheduledAction> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class WellnessRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val whySuggested: String,
    val category: WellnessCategory,
    val durationMinutes: Int,
    val indoorOutdoor: String, // "indoor", "outdoor", "both"
    val budgetLevel: String,   // "free", "low", "moderate"
    val moodAffinity: Set<MoodType>,
    val dietaryTags: Set<String> = emptySet(),
    val steps: List<String> = emptyList(),
    val imageKey: String = "hero"
)
