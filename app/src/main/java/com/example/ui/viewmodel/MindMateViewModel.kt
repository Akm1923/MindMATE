package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.CrisisSafetyGuard
import com.example.data.ai.MindMateAiService
import com.example.data.engine.DiscoverFilters
import com.example.data.engine.SampleWellnessData
import com.example.data.engine.WellnessIntelligenceEngine
import com.example.integrations.AgentActionParser
import com.example.integrations.AgentScheduledAction
import com.example.integrations.AppActionExecutor
import com.example.integrations.AppConnectorStatus
import com.example.integrations.ConnectedAppsPreferences
import com.example.integrations.DeviceCalendarEvent
import com.example.data.model.ActivityLog
import com.example.data.model.AiModelTier
import com.example.data.model.CompanionMessage
import com.example.data.model.CompanionMode
import com.example.data.model.JournalEntry
import com.example.data.model.MoodLog
import com.example.data.model.MoodType
import com.example.data.model.OrbState
import com.example.data.model.UserProfile
import com.example.data.model.WellnessRecommendation
import com.example.data.repository.MindMateRepository
import com.example.voice.LiveConnectionState
import com.example.voice.VoiceService
import com.example.voice.VoiceUiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MindMateDashboardState(
    val isLoading: Boolean = true,
    val profileInitError: String? = null,
    val profile: UserProfile? = null,
    val moodLogs: List<MoodLog> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val activityLogs: List<ActivityLog> = emptyList(),
    val messages: List<CompanionMessage> = emptyList(),
    val useSamplePreview: Boolean = false,
    val discoverFilters: DiscoverFilters = DiscoverFilters(),
    val companionMode: CompanionMode = CompanionMode.LISTEN,
    val selectedModelTier: AiModelTier? = null,
    val effectiveModelTier: AiModelTier = CompanionMode.LISTEN.defaultModelTier,
    val orbState: OrbState = OrbState.IDLE,
    val streamingReply: String = "",
    val lastUserPrompt: String = "",
    val pendingTranscript: String? = null,
    val companionError: String? = null,
    val ephemeralMessages: List<CompanionMessage> = emptyList(),
    val crisisBannerVisible: Boolean = false,
    val insightsRangeDays: Int = 7,
    val journalReflectingId: String? = null,
    val activeBreathingRec: WellnessRecommendation? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val conversationElapsedSeconds: Int = 0,
    val isSessionActive: Boolean = false,
    val connectedApps: List<AppConnectorStatus> = emptyList(),
    val todayCalendarEvents: List<DeviceCalendarEvent> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val messageActions: Map<String, List<AgentScheduledAction>> = emptyMap()
)

class MindMateViewModel(
    appContext: Context,
    private val repository: MindMateRepository,
    private val aiService: MindMateAiService,
    private val voiceService: VoiceService,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    private val initialPhotoUrl: String?
) : ViewModel() {

    private val appCtx = appContext.applicationContext
    private val connectedAppsPrefs = ConnectedAppsPreferences(appCtx)

    private val _localUi = MutableStateFlow(LocalInteractionState())
    val voiceState: StateFlow<VoiceUiState> = voiceService.state

    private var timerJob: Job? = null
    private var activeChatJob: Job? = null

    private data class LocalInteractionState(
        val profileLoadFinished: Boolean = false,
        val profileInitError: String? = null,
        val useSamplePreview: Boolean = false,
        val discoverFilters: DiscoverFilters = DiscoverFilters(),
        val companionMode: CompanionMode = CompanionMode.LISTEN,
        val selectedModelTier: AiModelTier? = null,
        val orbState: OrbState = OrbState.IDLE,
        val streamingReply: String = "",
        val lastUserPrompt: String = "",
        val pendingTranscript: String? = null,
        val companionError: String? = null,
        val ephemeralMessages: List<CompanionMessage> = emptyList(),
        val crisisBannerVisible: Boolean = false,
        val insightsRangeDays: Int = 7,
        val journalReflectingId: String? = null,
        val activeBreathingRec: WellnessRecommendation? = null,
        val statusMessage: String? = null,
        val errorMessage: String? = null,
        val conversationElapsedSeconds: Int = 0,
        val isSessionActive: Boolean = false,
        val connectedApps: List<AppConnectorStatus> = emptyList(),
        val todayCalendarEvents: List<DeviceCalendarEvent> = emptyList(),
        val hasCalendarPermission: Boolean = false,
        val messageActions: Map<String, List<AgentScheduledAction>> = emptyMap()
    )

    private val profileFlow = repository.observeProfile(currentUserId)
        .catch { e -> handleStreamError("profile", e); emit(null) }

    private val moodsFlow = repository.observeMoodLogs(currentUserId)
        .catch { e -> handleStreamError("mood logs", e); emit(emptyList()) }

    private val journalsFlow = repository.observeJournalEntries(currentUserId)
        .catch { e -> handleStreamError("journal entries", e); emit(emptyList()) }

    private val activitiesFlow = repository.observeActivityLogs(currentUserId)
        .catch { e -> handleStreamError("activity logs", e); emit(emptyList()) }

    private val messagesFlow = repository.observeConversation(currentUserId)
        .catch { e -> handleStreamError("conversations", e); emit(emptyList()) }

    val uiState: StateFlow<MindMateDashboardState> = combine(
        profileFlow,
        moodsFlow,
        journalsFlow,
        activitiesFlow,
        messagesFlow,
        _localUi
    ) { values ->
        val profile = values[0] as UserProfile?
        @Suppress("UNCHECKED_CAST")
        val moods = values[1] as List<MoodLog>
        @Suppress("UNCHECKED_CAST")
        val journals = values[2] as List<JournalEntry>
        @Suppress("UNCHECKED_CAST")
        val activities = values[3] as List<ActivityLog>
        @Suppress("UNCHECKED_CAST")
        val messages = values[4] as List<CompanionMessage>
        val local = values[5] as LocalInteractionState

        val showSample = local.useSamplePreview && moods.isEmpty() && journals.isEmpty()
        val effectiveMoods = if (showSample) SampleWellnessData.sampleMoodLogs else moods
        val effectiveJournals = if (showSample) SampleWellnessData.sampleJournalEntries else journals
        val effectiveActivities = if (showSample) SampleWellnessData.sampleActivities else activities
        val mergedPersisted = if (messages.isEmpty()) {
            local.ephemeralMessages
        } else {
            val persistedSignatures = messages.takeLast(12).map { "${it.role}:${it.content}" }.toSet()
            val pendingExtras = local.ephemeralMessages.filterNot { "${it.role}:${it.content}" in persistedSignatures }
            messages + pendingExtras
        }

        val effectiveMessages = if (profile?.conversationHistoryEnabled == false) {
            local.ephemeralMessages
        } else if (mergedPersisted.isEmpty() && showSample) {
            SampleWellnessData.sampleMessages
        } else {
            mergedPersisted
        }

        val resolvedProfile = profile ?: if (local.profileLoadFinished) {
            UserProfile(
                id = currentUserId,
                userId = currentUserId,
                displayName = initialDisplayName.ifBlank { "Friend" },
                email = initialEmail,
                photoUrl = initialPhotoUrl
            )
        } else {
            null
        }

        val effectiveMessagesWithActions = effectiveMessages.map { msg ->
            if (msg.role == "companion" && msg.scheduledActions.isEmpty()) {
                val actions = local.messageActions[msg.content]
                if (!actions.isNullOrEmpty()) msg.copy(scheduledActions = actions) else msg
            } else {
                msg
            }
        }

        MindMateDashboardState(
            isLoading = !local.profileLoadFinished && profile == null,
            profileInitError = local.profileInitError,
            profile = resolvedProfile,
            moodLogs = effectiveMoods,
            journalEntries = effectiveJournals,
            activityLogs = effectiveActivities,
            messages = effectiveMessagesWithActions,
            useSamplePreview = showSample,
            discoverFilters = local.discoverFilters,
            companionMode = local.companionMode,
            selectedModelTier = local.selectedModelTier,
            effectiveModelTier = local.selectedModelTier ?: local.companionMode.defaultModelTier,
            orbState = local.orbState,
            streamingReply = local.streamingReply,
            lastUserPrompt = local.lastUserPrompt,
            pendingTranscript = local.pendingTranscript,
            companionError = local.companionError,
            ephemeralMessages = local.ephemeralMessages,
            crisisBannerVisible = local.crisisBannerVisible,
            insightsRangeDays = local.insightsRangeDays,
            journalReflectingId = local.journalReflectingId,
            activeBreathingRec = local.activeBreathingRec,
            statusMessage = local.statusMessage,
            errorMessage = local.errorMessage,
            conversationElapsedSeconds = local.conversationElapsedSeconds,
            isSessionActive = local.isSessionActive,
            connectedApps = local.connectedApps,
            todayCalendarEvents = local.todayCalendarEvents,
            hasCalendarPermission = local.hasCalendarPermission,
            messageActions = local.messageActions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MindMateDashboardState()
    )

    init {
        retryProfileInit()
        observeLiveConnectionTimer()
        refreshConnectedAppsAndCalendar()
    }

    fun refreshConnectedAppsAndCalendar() {
        val enabled = connectedAppsPrefs.getEnabledConnectorIds(currentUserId)
        val statuses = AppActionExecutor.buildStatuses(appCtx, enabled)
        val hasCalPerm = AppActionExecutor.hasCalendarReadPermission(appCtx) &&
            AppActionExecutor.hasCalendarWritePermission(appCtx)
        val events = if ("google_calendar" in enabled && AppActionExecutor.hasCalendarReadPermission(appCtx)) {
            AppActionExecutor.readTodayCalendarEvents(appCtx)
        } else {
            emptyList()
        }
        _localUi.value = _localUi.value.copy(
            connectedApps = statuses,
            hasCalendarPermission = hasCalPerm,
            todayCalendarEvents = events
        )
    }

    fun toggleConnectedApp(connectorId: String, enabled: Boolean) {
        connectedAppsPrefs.setConnectorEnabled(currentUserId, connectorId, enabled)
        refreshConnectedAppsAndCalendar()
    }

    fun executeScheduledAction(action: AgentScheduledAction) {
        val result = AppActionExecutor.executeAction(appCtx, action)
        if (result.success) {
            val updatedMap = _localUi.value.messageActions.mapValues { (_, list) ->
                list.map { item -> if (item.id == action.id) item.copy(isExecuted = true) else item }
            }
            _localUi.value = _localUi.value.copy(
                messageActions = updatedMap,
                statusMessage = result.message
            )
            refreshConnectedAppsAndCalendar()
        } else {
            _localUi.value = _localUi.value.copy(
                statusMessage = result.message
            )
        }
    }

    private fun observeLiveConnectionTimer() {
        viewModelScope.launch {
            voiceService.state
                .map { it.connectionState }
                .distinctUntilChanged()
                .collect { connState ->
                    when (connState) {
                        LiveConnectionState.CONNECTED -> {
                            _localUi.value = _localUi.value.copy(isSessionActive = true)
                            if (timerJob?.isActive != true) {
                                timerJob = viewModelScope.launch {
                                    while (true) {
                                        delay(1000L)
                                        _localUi.value = _localUi.value.copy(
                                            conversationElapsedSeconds = _localUi.value.conversationElapsedSeconds + 1
                                        )
                                    }
                                }
                            }
                        }
                        LiveConnectionState.DISCONNECTED, LiveConnectionState.ERROR -> {
                            timerJob?.cancel()
                            timerJob = null
                            _localUi.value = _localUi.value.copy(
                                isSessionActive = false,
                                conversationElapsedSeconds = 0
                            )
                        }
                        LiveConnectionState.CONNECTING, LiveConnectionState.RECONNECTING -> Unit
                    }
                }
        }
    }

    fun retryProfileInit() {
        viewModelScope.launch {
            _localUi.value = _localUi.value.copy(profileInitError = null)
            try {
                repository.ensureUserProfile(
                    userId = currentUserId,
                    displayName = initialDisplayName,
                    email = initialEmail,
                    photoUrl = initialPhotoUrl
                )
                _localUi.value = _localUi.value.copy(
                    profileLoadFinished = true,
                    profileInitError = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize profile", e)
                _localUi.value = _localUi.value.copy(
                    profileLoadFinished = true,
                    profileInitError = e.localizedMessage ?: "Failed to sync user profile.",
                    errorMessage = e.localizedMessage ?: "Failed to sync user profile."
                )
            }
        }
    }

    private fun handleStreamError(label: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $label", root)
        _localUi.value = _localUi.value.copy(
            profileLoadFinished = true
        )
    }

    fun clearVoiceError() {
        voiceService.clearError()
    }

    fun clearBannerMessages() {
        _localUi.value = _localUi.value.copy(errorMessage = null, statusMessage = null)
    }

    fun toggleSamplePreview(enabled: Boolean) {
        _localUi.value = _localUi.value.copy(useSamplePreview = enabled)
    }

    fun setInsightsRange(days: Int) {
        _localUi.value = _localUi.value.copy(insightsRangeDays = days)
    }

    fun updateDiscoverFilters(filters: DiscoverFilters) {
        _localUi.value = _localUi.value.copy(discoverFilters = filters)
    }

    fun setCompanionMode(mode: CompanionMode) {
        _localUi.value = _localUi.value.copy(companionMode = mode)
    }

    fun setModelTier(tier: AiModelTier?) {
        _localUi.value = _localUi.value.copy(selectedModelTier = tier)
    }

    fun consumePendingTranscript() {
        if (_localUi.value.pendingTranscript != null) {
            _localUi.value = _localUi.value.copy(pendingTranscript = null)
        }
    }

    fun dismissCrisisBanner() {
        _localUi.value = _localUi.value.copy(crisisBannerVisible = false)
    }

    fun openActivityGuide(rec: WellnessRecommendation?) {
        _localUi.value = _localUi.value.copy(activeBreathingRec = rec)
    }

    fun checkInMood(mood: MoodType, note: String, energyLevel: Int) {
        viewModelScope.launch {
            try {
                repository.logMood(currentUserId, mood, note, energyLevel)
                _localUi.value = _localUi.value.copy(
                    useSamplePreview = false,
                    statusMessage = "Checked in as ${mood.label}. Daily plan updated."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Could not save mood check-in."
                )
            }
        }
    }

    private suspend fun ensureProfileSyncedIfNeeded() {
        if (_localUi.value.profileInitError != null) {
            repository.ensureUserProfile(
                userId = currentUserId,
                displayName = initialDisplayName,
                email = initialEmail,
                photoUrl = initialPhotoUrl
            )
            _localUi.value = _localUi.value.copy(profileInitError = null)
        }
    }

    fun addHydration(deltaMl: Long) {
        val current = uiState.value.profile?.hydrationTodayMl ?: 0L
        val next = (current + deltaMl).coerceIn(0L, 10000L)
        viewModelScope.launch {
            try {
                ensureProfileSyncedIfNeeded()
                repository.updateHydration(currentUserId, next)
                _localUi.value = _localUi.value.copy(statusMessage = "Logged +${deltaMl}ml hydration.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Could not update hydration."
                )
            }
        }
    }

    fun setActivityStatus(rec: WellnessRecommendation, status: String) {
        viewModelScope.launch {
            try {
                repository.setActivityStatus(currentUserId, rec, status)
                val msg = when (status) {
                    "completed" -> "Marked \"${rec.title}\" completed."
                    "saved" -> "Saved \"${rec.title}\" to your wellness library."
                    "dismissed" -> "Dismissed \"${rec.title}\" from suggestions."
                    else -> null
                }
                _localUi.value = _localUi.value.copy(statusMessage = msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Could not update activity."
                )
            }
        }
    }

    fun createJournalEntry(title: String, content: String, moodTag: String) {
        viewModelScope.launch {
            try {
                repository.createJournalEntry(currentUserId, title, content, moodTag)
                _localUi.value = _localUi.value.copy(
                    useSamplePreview = false,
                    statusMessage = "Journal entry saved privately."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to save journal entry."
                )
            }
        }
    }

    fun updateJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                repository.updateJournalEntry(currentUserId, entry)
                _localUi.value = _localUi.value.copy(statusMessage = "Journal entry updated.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to update journal entry."
                )
            }
        }
    }

    fun deleteJournalEntry(entryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteJournalEntry(currentUserId, entryId)
                _localUi.value = _localUi.value.copy(statusMessage = "Journal entry deleted.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to delete journal entry."
                )
            }
        }
    }

    fun generateJournalAiReflection(entry: JournalEntry) {
        if (entry.id.startsWith("sample_")) {
            _localUi.value = _localUi.value.copy(
                statusMessage = "Create a real journal entry to generate live Gemini AI reflections."
            )
            return
        }
        viewModelScope.launch {
            _localUi.value = _localUi.value.copy(journalReflectingId = entry.id, errorMessage = null)
            try {
                val reflection = aiService.generateJournalReflection(entry.content, entry.moodTag)
                repository.saveJournalAiReflection(
                    userId = currentUserId,
                    entryId = entry.id,
                    aiSummary = reflection.summary,
                    aiPromptQuestion = reflection.reflectionQuestion
                )
                _localUi.value = _localUi.value.copy(
                    journalReflectingId = null,
                    statusMessage = "AI reflection added."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    journalReflectingId = null,
                    errorMessage = e.localizedMessage ?: "Unable to generate reflection right now."
                )
            }
        }
    }

    fun toggleLiveVoiceSession(hasMicPermission: Boolean) {
        val conn = voiceState.value.connectionState
        if (conn == LiveConnectionState.CONNECTED ||
            conn == LiveConnectionState.CONNECTING ||
            conn == LiveConnectionState.RECONNECTING
        ) {
            endLiveVoiceSession()
        } else {
            startLiveVoiceSession(hasMicPermission)
        }
    }

    fun startLiveVoiceSession(hasMicPermission: Boolean) {
        val snapshot = uiState.value
        voiceService.connectLive(
            hasMicPermission = hasMicPermission,
            mode = snapshot.companionMode,
            profile = snapshot.profile,
            recentMoods = snapshot.moodLogs,
            onTurnComplete = { userText, assistantText ->
                persistLiveVoiceTurn(
                    userText = userText,
                    assistantText = assistantText,
                    mode = uiState.value.companionMode,
                    saveHistory = uiState.value.profile?.conversationHistoryEnabled != false
                )
            }
        )
    }

    private fun persistLiveVoiceTurn(
        userText: String,
        assistantText: String,
        mode: CompanionMode,
        saveHistory: Boolean
    ) {
        val cleanUser = userText.trim()
        val cleanAssistant = assistantText.trim()
        val crisis = CrisisSafetyGuard.containsCrisisLanguage(cleanUser)
        if (crisis) {
            _localUi.value = _localUi.value.copy(crisisBannerVisible = true)
        }

        viewModelScope.launch {
            if (cleanUser.isNotEmpty()) {
                appendMessage(
                    role = "user",
                    mode = mode,
                    content = cleanUser,
                    modelUsed = MindMateAiService.MODEL_LIVE_VOICE,
                    saveHistory = saveHistory
                )
            }
            if (cleanAssistant.isNotEmpty()) {
                appendMessage(
                    role = "companion",
                    mode = mode,
                    content = cleanAssistant,
                    modelUsed = MindMateAiService.MODEL_LIVE_VOICE,
                    saveHistory = saveHistory
                )
            }
        }
    }

    fun endLiveVoiceSession() {
        activeChatJob?.cancel()
        voiceService.disconnectLive()
        _localUi.value = _localUi.value.copy(
            orbState = OrbState.IDLE,
            streamingReply = "",
            companionError = null
        )
    }

    fun toggleDictateForTranscription(hasMicPermission: Boolean) {
        voiceService.clearError()
        if (voiceState.value.isDictating) {
            voiceService.stopAndTranscribeDictation { transcript ->
                _localUi.value = _localUi.value.copy(
                    pendingTranscript = transcript,
                    statusMessage = "Transcribed audio with ${MindMateAiService.MODEL_TRANSCRIBE}."
                )
            }
        } else {
            voiceService.startDictation(hasMicPermission) { autoTranscript ->
                _localUi.value = _localUi.value.copy(
                    pendingTranscript = autoTranscript,
                    statusMessage = "Transcribed audio with ${MindMateAiService.MODEL_TRANSCRIBE}."
                )
            }
        }
    }

    fun interruptAssistant() {
        voiceService.interruptAssistant()
        if (activeChatJob?.isActive == true) {
            activeChatJob?.cancel()
            _localUi.value = _localUi.value.copy(
                orbState = OrbState.IDLE,
                streamingReply = ""
            )
        }
    }

    fun toggleVoiceMute() {
        voiceService.toggleMute()
    }

    fun grantVoiceConsentAndConnect(hasMicPermission: Boolean) {
        val current = uiState.value.profile ?: return
        viewModelScope.launch {
            try {
                ensureProfileSyncedIfNeeded()
                repository.updateProfileSettings(
                    currentUserId,
                    current.copy(voiceConsentGranted = true)
                )
                if (hasMicPermission) {
                    startLiveVoiceSession(true)
                } else {
                    _localUi.value = _localUi.value.copy(
                        statusMessage = "Voice consent saved. Grant mic access to start speaking."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to save voice consent."
                )
            }
        }
    }

    fun retryLastCompanionMessage() {
        val last = _localUi.value.lastUserPrompt
        if (last.isNotBlank()) {
            sendCompanionMessage(last, skipSavingUserTurn = true)
        }
    }

    private suspend fun appendMessage(
        role: String,
        mode: CompanionMode,
        content: String,
        modelUsed: String,
        saveHistory: Boolean
    ) {
        val localMsg = CompanionMessage(
            id = "local_${role}_${System.currentTimeMillis()}_${content.hashCode()}",
            userId = currentUserId,
            role = role,
            mode = mode.id,
            content = content,
            modelUsed = modelUsed
        )
        _localUi.value = _localUi.value.copy(
            ephemeralMessages = _localUi.value.ephemeralMessages + localMsg
        )
        if (saveHistory) {
            try {
                repository.addCompanionMessage(currentUserId, role, mode.id, content)
                _localUi.value = _localUi.value.copy(
                    ephemeralMessages = _localUi.value.ephemeralMessages.filterNot { it.id == localMsg.id }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist $role message, keeping in memory", e)
            }
        }
    }

    fun sendCompanionMessage(rawText: String, skipSavingUserTurn: Boolean = false) {
        val cleanText = rawText.trim()
        if (cleanText.isEmpty()) return

        voiceService.clearError()

        val crisisDetected = CrisisSafetyGuard.containsCrisisLanguage(cleanText)
        val snapshotState = uiState.value
        val activeMode = snapshotState.companionMode
        val activeTier = snapshotState.effectiveModelTier
        val saveHistory = snapshotState.profile?.conversationHistoryEnabled != false

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            _localUi.value = _localUi.value.copy(
                orbState = OrbState.THINKING,
                streamingReply = "",
                lastUserPrompt = cleanText,
                companionError = null,
                crisisBannerVisible = crisisDetected || _localUi.value.crisisBannerVisible,
                errorMessage = null
            )

            try {
                if (!skipSavingUserTurn) {
                    appendMessage(
                        role = "user",
                        mode = activeMode,
                        content = cleanText,
                        modelUsed = activeTier.modelId,
                        saveHistory = saveHistory
                    )
                }

                val connectedSummary = snapshotState.connectedApps
                    .filter { it.isEnabled }
                    .joinToString(", ") { "${it.spec.name} (id=${it.spec.id})" }
                val calendarSummary = snapshotState.todayCalendarEvents
                    .take(10)
                    .joinToString("; ") { it.formatSummary() }

                var finalReply = ""
                aiService.streamCompanionReply(
                    userMessage = cleanText,
                    mode = activeMode,
                    modelTier = activeTier,
                    history = snapshotState.messages,
                    profile = snapshotState.profile,
                    recentMoods = snapshotState.moodLogs,
                    connectedAppsSummary = connectedSummary,
                    todayCalendarSummary = calendarSummary
                ).collect { accumulated ->
                    finalReply = accumulated
                    val cleanStreaming = AgentActionParser.stripStreamingActionTags(accumulated)
                    _localUi.value = _localUi.value.copy(
                        orbState = OrbState.SPEAKING,
                        streamingReply = cleanStreaming
                    )
                }

                if (finalReply.isNotBlank()) {
                    val parsed = AgentActionParser.parse(finalReply)
                    val displayContent = parsed.displayText.ifBlank { finalReply }
                    if (parsed.actions.isNotEmpty()) {
                        _localUi.value = _localUi.value.copy(
                            messageActions = _localUi.value.messageActions + (displayContent to parsed.actions)
                        )
                    }
                    appendMessage(
                        role = "companion",
                        mode = activeMode,
                        content = displayContent,
                        modelUsed = activeTier.modelId,
                        saveHistory = saveHistory
                    )
                    if (snapshotState.profile?.voiceSpeechEnabled == true &&
                        !voiceState.value.isMuted &&
                        !voiceState.value.connectionState.isLiveConnected
                    ) {
                        voiceService.speakTextReply(displayContent)
                    }
                }
                _localUi.value = _localUi.value.copy(
                    orbState = OrbState.IDLE,
                    streamingReply = "",
                    companionError = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Companion turn failed", e)
                val errText = e.localizedMessage ?: "MindMate AI could not respond right now."
                _localUi.value = _localUi.value.copy(
                    orbState = OrbState.ERROR,
                    streamingReply = "",
                    companionError = errText
                )
            }
        }
    }

    fun updateProfileSettings(updated: UserProfile) {
        viewModelScope.launch {
            try {
                ensureProfileSyncedIfNeeded()
                repository.updateProfileSettings(currentUserId, updated)
                _localUi.value = _localUi.value.copy(statusMessage = "Preferences updated.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to save settings."
                )
            }
        }
    }

    fun clearConversationHistory() {
        viewModelScope.launch {
            try {
                repository.clearConversationHistory(currentUserId)
                _localUi.value = _localUi.value.copy(
                    ephemeralMessages = emptyList(),
                    companionError = null,
                    statusMessage = "Conversation history cleared."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Could not clear conversation history."
                )
            }
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAllUserData(currentUserId)
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _localUi.value = _localUi.value.copy(
                    errorMessage = e.localizedMessage ?: "Failed to delete user data."
                )
            }
        }
    }

    fun exportDataJson(): String {
        val state = uiState.value
        return repository.exportUserDataJson(
            profile = state.profile,
            moods = state.moodLogs,
            journals = state.journalEntries,
            activities = state.activityLogs,
            messages = state.messages
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        voiceService.release()
    }

    private companion object {
        const val TAG = "MindMateVM"
    }
}
