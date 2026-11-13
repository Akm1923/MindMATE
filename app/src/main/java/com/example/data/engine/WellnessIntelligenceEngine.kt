package com.example.data.engine

import com.example.data.model.ActivityLog
import com.example.data.model.CompanionMessage
import com.example.data.model.CompanionMode
import com.example.data.model.JournalEntry
import com.example.data.model.MoodLog
import com.example.data.model.MoodType
import com.example.data.model.UserProfile
import com.example.data.model.WellnessCategory
import com.example.data.model.WellnessRecommendation
import com.google.firebase.Timestamp
import java.util.Calendar

data class DiscoverFilters(
    val category: WellnessCategory? = null,
    val moodFilter: MoodType? = null,
    val maxMinutes: Int = 60,
    val indoorOutdoor: String = "all", // "all", "indoor", "outdoor"
    val budget: String = "all"         // "all", "free", "low"
)

object WellnessIntelligenceEngine {

    val catalog: List<WellnessRecommendation> = listOf(
        // MOVE
        WellnessRecommendation(
            id = "move_sunlight_walk",
            title = "15-Minute Mindful Walk",
            description = "A steady, unhurried walk without rushing to check notifications.",
            whySuggested = "Gentle movement lifts sluggish energy and helps clear mental fog.",
            category = WellnessCategory.MOVE,
            durationMinutes = 15,
            indoorOutdoor = "outdoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.OKAY, MoodType.LOW, MoodType.GOOD, MoodType.STRESSED),
            steps = listOf(
                "Leave your phone on silent in your pocket.",
                "Notice three sounds and three colors around you.",
                "Keep a relaxed, natural stride for 15 minutes."
            ),
            imageKey = "nature"
        ),
        WellnessRecommendation(
            id = "move_neck_shoulder_stretch",
            title = "Desk & Shoulder Unwind",
            description = "Slow neck rolls, chest openers, and seated spinal twists.",
            whySuggested = "Releases physical tension built up during focused screen time.",
            category = WellnessCategory.MOVE,
            durationMinutes = 7,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.STRESSED, MoodType.OKAY, MoodType.LOW),
            steps = listOf(
                "Drop your shoulders away from your ears and take 3 slow breaths.",
                "Gently tilt ear to shoulder for 20 seconds per side.",
                "Clasp hands behind your back and open your chest for 30 seconds."
            ),
            imageKey = "hero"
        ),
        WellnessRecommendation(
            id = "move_beginner_yoga",
            title = "Gentle Floor Yoga Flow",
            description = "Beginner cat-cow, child's pose, and gentle hamstring stretch.",
            whySuggested = "Pairs slow movement with steady breathing for grounded calm.",
            category = WellnessCategory.MOVE,
            durationMinutes = 20,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.GOOD, MoodType.OKAY, MoodType.STRESSED),
            steps = listOf(
                "Unroll a mat or towel in a quiet corner.",
                "Move through 6 rounds of slow Cat-Cow with your breath.",
                "Rest in Child's Pose for 60 seconds at the end."
            ),
            imageKey = "hero"
        ),
        WellnessRecommendation(
            id = "move_energizing_circuit",
            title = "Home Energy Mobility",
            description = "Bodyweight squats, arm swings, and brisk step-touches to spark momentum.",
            whySuggested = "Great when you feel Great or Good and want to channel bright energy.",
            category = WellnessCategory.MOVE,
            durationMinutes = 15,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.GREAT, MoodType.GOOD),
            steps = listOf(
                "Warm up with 2 minutes of easy marching in place.",
                "Do 2 rounds of 10 bodyweight squats and 10 wall push-ups.",
                "Finish with 2 minutes of deep breathing."
            ),
            imageKey = "hero"
        ),

        // EAT WELL
        WellnessRecommendation(
            id = "eat_moong_chilla",
            title = "Spiced Moong Dal Chilla",
            description = "Savory protein-rich green gram crepes with grated carrot, coriander, and mint chutney.",
            whySuggested = "Warm, comforting Indian home food that sustains steady afternoon energy.",
            category = WellnessCategory.EAT_WELL,
            durationMinutes = 20,
            indoorOutdoor = "indoor",
            budgetLevel = "low",
            moodAffinity = setOf(MoodType.GREAT, MoodType.GOOD, MoodType.OKAY),
            dietaryTags = setOf("balanced", "vegetarian", "vegan", "indian_home", "high_protein"),
            steps = listOf(
                "Blend soaked yellow/green moong dal with ginger and a pinch of cumin.",
                "Fold in finely chopped spinach, tomato, and coriander.",
                "Cook crisp on a lightly oiled pan and enjoy warm."
            ),
            imageKey = "nutrition"
        ),
        WellnessRecommendation(
            id = "eat_khichdi_bowl",
            title = "Warm Comfort Dal Khichdi",
            description = "Simple one-pot rice and yellow lentils tempered with cumin, turmeric, and seasonal veggies.",
            whySuggested = "Easy on digestion and deeply soothing on low or stressed days.",
            category = WellnessCategory.EAT_WELL,
            durationMinutes = 25,
            indoorOutdoor = "indoor",
            budgetLevel = "low",
            moodAffinity = setOf(MoodType.LOW, MoodType.STRESSED, MoodType.OKAY),
            dietaryTags = setOf("balanced", "vegetarian", "vegan", "indian_home"),
            steps = listOf(
                "Rinse rice and moong dal together and simmer with turmeric and veggies.",
                "Prepare a light jeera-hing tempering.",
                "Serve warm with plain yogurt or cucumber slices."
            ),
            imageKey = "nutrition"
        ),
        WellnessRecommendation(
            id = "eat_oats_fruit_breakfast",
            title = "Warm Cinnamon Oat & Fruit Bowl",
            description = "Rolled oats topped with banana slices, roasted seeds, and a dash of cardamom.",
            whySuggested = "Budget-friendly balanced breakfast that avoids sharp energy crashes.",
            category = WellnessCategory.EAT_WELL,
            durationMinutes = 10,
            indoorOutdoor = "indoor",
            budgetLevel = "low",
            moodAffinity = setOf(MoodType.GOOD, MoodType.OKAY, MoodType.GREAT),
            dietaryTags = setOf("balanced", "vegetarian", "vegan"),
            steps = listOf(
                "Simmer rolled oats in milk or plant milk for 5 minutes.",
                "Stir in a pinch of cinnamon or cardamom.",
                "Top with seasonal fruit and pumpkin or flax seeds."
            ),
            imageKey = "nutrition"
        ),
        WellnessRecommendation(
            id = "eat_rainbow_fruit_plate",
            title = "Crisp Fruit & Nut Snack Plate",
            description = "Sliced seasonal guava, apple, or pomegranate with a handful of soaked almonds.",
            whySuggested = "Quick hydrating snack when cooking feels like too much effort.",
            category = WellnessCategory.EAT_WELL,
            durationMinutes = 5,
            indoorOutdoor = "indoor",
            budgetLevel = "low",
            moodAffinity = setOf(MoodType.LOW, MoodType.STRESSED, MoodType.OKAY, MoodType.GOOD),
            dietaryTags = setOf("balanced", "vegetarian", "vegan", "indian_home", "high_protein"),
            steps = listOf(
                "Wash and slice 1-2 fresh fruits you enjoy.",
                "Add 6-8 almonds or walnuts on the side.",
                "Eat away from your work desk if possible."
            ),
            imageKey = "nutrition"
        ),

        // RELAX
        WellnessRecommendation(
            id = "relax_478_breathing",
            title = "4-7-8 Calm Breathing",
            description = "Inhale for 4 seconds, hold softly for 7, and exhale slowly for 8.",
            whySuggested = "Slowing your exhale signals your nervous system to settle down.",
            category = WellnessCategory.RELAX,
            durationMinutes = 5,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.STRESSED, MoodType.LOW, MoodType.OKAY),
            steps = listOf(
                "Sit comfortably with your jaw relaxed.",
                "Inhale quietly through your nose for 4 counts.",
                "Hold gently for 7 counts, then exhale slowly for 8 counts. Repeat 4 cycles."
            ),
            imageKey = "hero"
        ),
        WellnessRecommendation(
            id = "relax_54321_grounding",
            title = "5-4-3-2-1 Sensory Grounding",
            description = "Anchor your attention to the room around you using all five senses.",
            whySuggested = "Helpful when racing thoughts pull you away from the present moment.",
            category = WellnessCategory.RELAX,
            durationMinutes = 5,
            indoorOutdoor = "both",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.STRESSED, MoodType.LOW),
            steps = listOf(
                "Name 5 things you can see right now.",
                "Notice 4 textures you can feel, 3 sounds you can hear.",
                "Notice 2 scents and 1 comforting taste or slow breath."
            ),
            imageKey = "hero"
        ),
        WellnessRecommendation(
            id = "relax_body_scan",
            title = "Evening Body Scan Meditation",
            description = "Progressive attention from forehead to toes, softening tension on each exhale.",
            whySuggested = "Prepares your mind and body for deeper rest.",
            category = WellnessCategory.RELAX,
            durationMinutes = 12,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.OKAY, MoodType.STRESSED, MoodType.GOOD),
            steps = listOf(
                "Lie down or recline comfortably.",
                "Soften your brow, jaw, and shoulders on a slow exhale.",
                "Bring gentle warmth down through arms, chest, legs, and feet."
            ),
            imageKey = "hero"
        ),

        // GET OUTSIDE
        WellnessRecommendation(
            id = "outside_park_bench_pause",
            title = "Park Bench Sky Gaze",
            description = "Sit at a nearby park or balcony for 10 minutes watching trees and sky.",
            whySuggested = "Natural daylight and open horizons offer effortless mental restoration.",
            category = WellnessCategory.GET_OUTSIDE,
            durationMinutes = 12,
            indoorOutdoor = "outdoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.LOW, MoodType.STRESSED, MoodType.OKAY, MoodType.GOOD),
            steps = listOf(
                "Step outside to a park, courtyard, or breezy balcony.",
                "Leave headphones off for the first 5 minutes.",
                "Take in the breeze and distant horizon."
            ),
            imageKey = "nature"
        ),
        WellnessRecommendation(
            id = "outside_coffee_friend_walk",
            title = "Call or Walk with a Friend",
            description = "Invite a trusted friend for a casual neighborhood stroll or voice catch-up.",
            whySuggested = "Warm social connection is one of the strongest daily mood anchors.",
            category = WellnessCategory.GET_OUTSIDE,
            durationMinutes = 25,
            indoorOutdoor = "outdoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.GREAT, MoodType.GOOD, MoodType.LOW),
            steps = listOf(
                "Send a quick message to someone you enjoy talking with.",
                "Keep it low-pressure—just a 15-25 minute walk or call.",
                "Share one lighthearted story from your week."
            ),
            imageKey = "nature"
        ),

        // DAILY HABITS
        WellnessRecommendation(
            id = "habit_hydration_reset",
            title = "Glass of Water + Posture Reset",
            description = "Drink a full glass of water slowly and stand up to stretch for 60 seconds.",
            whySuggested = "Mild dehydration often hides as afternoon fatigue or irritability.",
            category = WellnessCategory.DAILY_HABITS,
            durationMinutes = 3,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.OKAY, MoodType.LOW, MoodType.STRESSED, MoodType.GOOD, MoodType.GREAT),
            steps = listOf(
                "Fill a 250ml glass of cool or room-temperature water.",
                "Stand near a window while you drink it.",
                "Log +250ml on your Home hydration ring."
            ),
            imageKey = "hero"
        ),
        WellnessRecommendation(
            id = "habit_screen_sunset",
            title = "20-Minute Screen-Free Wind-Down",
            description = "Dim bright overhead lights and switch from feeds to calm music or reading.",
            whySuggested = "Protects evening wind-down and helps you fall asleep more naturally.",
            category = WellnessCategory.DAILY_HABITS,
            durationMinutes = 20,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.STRESSED, MoodType.OKAY, MoodType.GOOD),
            steps = listOf(
                "Plug your phone in across the room 20 minutes before bed.",
                "Switch to warm lamp light.",
                "Read a few pages or jot down 1 gratitude note."
            ),
            imageKey = "hero"
        ),
        WellnessRecommendation(
            id = "habit_three_line_journal",
            title = "3-Line Evening Reflection",
            description = "Write one thing that felt heavy, one bright moment, and one gentle priority for tomorrow.",
            whySuggested = "Clears lingering mental loops before rest.",
            category = WellnessCategory.DAILY_HABITS,
            durationMinutes = 5,
            indoorOutdoor = "indoor",
            budgetLevel = "free",
            moodAffinity = setOf(MoodType.LOW, MoodType.STRESSED, MoodType.GOOD, MoodType.GREAT),
            steps = listOf(
                "Open the MindMate Journal tab.",
                "Write three honest sentences—no polish required.",
                "Optionally tap AI Reflection for a gentle question."
            ),
            imageKey = "hero"
        )
    )

    fun buildDailyPlan(
        currentMood: MoodType?,
        profile: UserProfile?,
        activityLogs: List<ActivityLog>
    ): List<WellnessRecommendation> {
        val dismissedIds = activityLogs.filter { it.status == "dismissed" }.map { it.recommendationId }.toSet()
        val activeMood = currentMood ?: MoodType.OKAY
        val dietPref = profile?.dietaryPreference ?: "balanced"
        val indoorOutdoorPref = profile?.indoorOutdoorPreference ?: "both"

        val candidates = catalog.filterNot { it.id in dismissedIds }

        val scored = candidates.sortedByDescending { rec ->
            var score = 0
            if (activeMood in rec.moodAffinity) score += 40
            if (rec.category == WellnessCategory.EAT_WELL && dietPref in rec.dietaryTags) score += 25
            if (indoorOutdoorPref == "both" || rec.indoorOutdoor == "both" || rec.indoorOutdoor == indoorOutdoorPref) {
                score += 15
            }
            if (profile?.activityPreference == "gentle" && rec.durationMinutes <= 15) score += 10
            if (profile?.activityPreference == "outdoor_first" && rec.category == WellnessCategory.GET_OUTSIDE) score += 20
            score
        }

        // Pick one best match per category for a balanced daily plan (Move, Eat Well, Relax, Get Outside, Daily Habits)
        val picked = mutableListOf<WellnessRecommendation>()
        for (cat in WellnessCategory.entries) {
            val match = scored.firstOrNull { it.category == cat }
            if (match != null) picked.add(match)
        }
        return picked
    }

    fun filterDiscover(
        filters: DiscoverFilters,
        profile: UserProfile?,
        activityLogs: List<ActivityLog>
    ): List<WellnessRecommendation> {
        val dismissedIds = activityLogs.filter { it.status == "dismissed" }.map { it.recommendationId }.toSet()
        return catalog.filter { rec ->
            if (rec.id in dismissedIds) return@filter false
            if (filters.category != null && rec.category != filters.category) return@filter false
            if (filters.moodFilter != null && filters.moodFilter !in rec.moodAffinity) return@filter false
            if (rec.durationMinutes > filters.maxMinutes) return@filter false
            if (filters.indoorOutdoor != "all" && rec.indoorOutdoor != "both" && rec.indoorOutdoor != filters.indoorOutdoor) {
                return@filter false
            }
            if (filters.budget != "all" && rec.budgetLevel != filters.budget) return@filter false
            if (rec.category == WellnessCategory.EAT_WELL && profile != null &&
                profile.dietaryPreference != "balanced" &&
                rec.dietaryTags.isNotEmpty() &&
                profile.dietaryPreference !in rec.dietaryTags
            ) {
                return@filter false
            }
            true
        }
    }

    fun generateReflectiveObservation(moods: List<MoodLog>, activities: List<ActivityLog>): String {
        if (moods.size < 3) {
            return "Log at least 3 mood check-ins over the coming days to unlock personal emotional pattern observations."
        }
        val positiveCount = moods.count { it.mood == MoodType.GREAT.id || it.mood == MoodType.GOOD.id }
        val stressedOrLow = moods.count { it.mood == MoodType.STRESSED.id || it.mood == MoodType.LOW.id }
        val completedCount = activities.count { it.status == "completed" }

        return when {
            positiveCount > moods.size / 2 ->
                "You recorded more positive mood check-ins (${positiveCount} of ${moods.size}) in your logged entries, alongside $completedCount completed wellness activities."
            stressedOrLow >= moods.size / 2 ->
                "Several recent check-ins felt heavier or tense ($stressedOrLow of ${moods.size}). Gentle breathing pauses or talking with someone you trust can help lighten the load."
            else ->
                "Your recent check-ins show a steady mix of moods across ${moods.size} reflections, with $completedCount wellness activities completed."
        }
    }
}

object SampleWellnessData {
    private fun daysAgoTimestamp(daysAgo: Int, hourOfDay: Int): Timestamp {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 15)
        }
        return Timestamp(cal.time)
    }

    val sampleMoodLogs: List<MoodLog> = listOf(
        MoodLog(id = "sample_m1", mood = MoodType.GOOD.id, note = "Morning walk before starting work.", energyLevel = 4, createdAt = daysAgoTimestamp(0, 9)),
        MoodLog(id = "sample_m2", mood = MoodType.GREAT.id, note = "Cooked dinner and called family.", energyLevel = 5, createdAt = daysAgoTimestamp(1, 20)),
        MoodLog(id = "sample_m3", mood = MoodType.OKAY.id, note = "Busy afternoon, took a breathing break.", energyLevel = 3, createdAt = daysAgoTimestamp(2, 15)),
        MoodLog(id = "sample_m4", mood = MoodType.STRESSED.id, note = "Tight deadlines today.", energyLevel = 2, createdAt = daysAgoTimestamp(3, 17)),
        MoodLog(id = "sample_m5", mood = MoodType.GOOD.id, note = "Slept 7.5 hours, felt rested.", energyLevel = 4, createdAt = daysAgoTimestamp(4, 10)),
        MoodLog(id = "sample_m6", mood = MoodType.LOW.id, note = "Rainy day, energy felt quiet.", energyLevel = 2, createdAt = daysAgoTimestamp(5, 19)),
        MoodLog(id = "sample_m7", mood = MoodType.GOOD.id, note = "Park stroll with music.", energyLevel = 4, createdAt = daysAgoTimestamp(6, 18))
    )

    val sampleJournalEntries: List<JournalEntry> = listOf(
        JournalEntry(
            id = "sample_j1",
            title = "Midweek Slowdown",
            content = "Noticed my shoulders creeping up around 4 PM. Stepping outside for 10 minutes without my phone helped reset my focus.",
            moodTag = MoodType.OKAY.id,
            aiSummary = "You caught physical tension early and used a brief screen-free outdoor pause to reset.",
            aiPromptQuestion = "What subtle body cues tend to tell you it's time for a pause?",
            createdAt = daysAgoTimestamp(1, 18),
            updatedAt = daysAgoTimestamp(1, 18)
        ),
        JournalEntry(
            id = "sample_j2",
            title = "Warm Kitchen Evening",
            content = "Made hot moong dal chilla with mint chutney. Cooking slowly felt like a small act of care rather than a chore.",
            moodTag = MoodType.GREAT.id,
            aiSummary = "Slowing down while preparing a nourishing meal turned routine dinner into a grounding ritual.",
            aiPromptQuestion = "How can you bring that unhurried pace into one moment tomorrow?",
            createdAt = daysAgoTimestamp(3, 21),
            updatedAt = daysAgoTimestamp(3, 21)
        )
    )

    val sampleActivities: List<ActivityLog> = listOf(
        ActivityLog(id = "sample_a1", recommendationId = "move_sunlight_walk", title = "15-Minute Mindful Walk", category = WellnessCategory.MOVE.id, durationMinutes = 15, status = "completed", createdAt = daysAgoTimestamp(1, 9)),
        ActivityLog(id = "sample_a2", recommendationId = "relax_478_breathing", title = "4-7-8 Calm Breathing", category = WellnessCategory.RELAX.id, durationMinutes = 5, status = "completed", createdAt = daysAgoTimestamp(2, 15)),
        ActivityLog(id = "sample_a3", recommendationId = "eat_moong_chilla", title = "Spiced Moong Dal Chilla", category = WellnessCategory.EAT_WELL.id, durationMinutes = 20, status = "saved", createdAt = daysAgoTimestamp(3, 19))
    )

    val sampleMessages: List<CompanionMessage> = listOf(
        CompanionMessage(
            id = "sample_c1",
            role = "companion",
            mode = CompanionMode.LISTEN.id,
            content = "Hey there — I'm MindMate. Whether you want to talk through your day, take a breathing pause, or just chat casually, I'm here.",
            createdAt = daysAgoTimestamp(0, 8)
        )
    )
}
