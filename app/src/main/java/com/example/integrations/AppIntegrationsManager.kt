package com.example.integrations

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class AppConnectorCategory(val label: String) {
    GOOGLE_APPS("Google Apps"),
    PRODUCTIVITY("Productivity & Notes"),
    WELLNESS_MEDIA("Media & Wellness"),
    COMMUNICATION("Messaging & Mail")
}

data class AppConnectorSpec(
    val id: String,
    val name: String,
    val category: AppConnectorCategory,
    val packageNames: List<String>,
    val capabilitySummary: String,
    val supportsSystemFallback: Boolean = false,
    val requiresCalendarPermission: Boolean = false
)

data class AppConnectorStatus(
    val spec: AppConnectorSpec,
    val isEnabled: Boolean,
    val isInstalledOrReady: Boolean
)

data class DeviceCalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
) {
    fun formatSummary(): String {
        if (allDay) return "$title (All day)"
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        return "${fmt.format(Date(startMillis))}–${fmt.format(Date(endMillis))}: $title"
    }
}

enum class AgentActionType(val wireName: String, val buttonLabel: String) {
    CALENDAR_EVENT("calendar_event", "Add to Calendar"),
    SET_ALARM("set_alarm", "Set Alarm"),
    SET_TIMER("set_timer", "Start Timer"),
    CREATE_TASK("create_task", "Save Task / Note"),
    DRAFT_EMAIL("draft_email", "Draft Email"),
    OPEN_APP("open_app", "Open App");

    companion object {
        fun fromWireName(value: String): AgentActionType =
            entries.firstOrNull { it.wireName.equals(value.trim(), ignoreCase = true) }
                ?: CREATE_TASK
    }
}

data class AgentScheduledAction(
    val id: String = UUID.randomUUID().toString(),
    val type: AgentActionType,
    val title: String,
    val detail: String = "",
    val hour: Int = 9,
    val minute: Int = 0,
    val durationMinutes: Int = 30,
    val targetAppId: String = "",
    val recipient: String = "",
    val isExecuted: Boolean = false
)

data class ActionExecutionResult(
    val success: Boolean,
    val message: String
)

object AppConnectorCatalog {
    val allConnectors: List<AppConnectorSpec> = listOf(
        AppConnectorSpec(
            id = "google_calendar",
            name = "Google Calendar",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.calendar"),
            capabilitySummary = "Read today's events & schedule wellness/focus blocks",
            supportsSystemFallback = true,
            requiresCalendarPermission = true
        ),
        AppConnectorSpec(
            id = "google_tasks",
            name = "Google Tasks",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.apps.tasks"),
            capabilitySummary = "Send daily to-dos and gentle habit checklists",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "gmail",
            name = "Gmail",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.gm"),
            capabilitySummary = "Draft thoughtful check-in or follow-up emails",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "google_keep",
            name = "Google Keep",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.keep"),
            capabilitySummary = "Save daily schedules, reflections & quick notes",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "clock_alarms",
            name = "Clock & Alarms",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.deskclock"),
            capabilitySummary = "Set wake-up alarms & breathing/focus timers",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "google_drive",
            name = "Google Drive",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.apps.docs"),
            capabilitySummary = "Open Drive documents & wellness logs"
        ),
        AppConnectorSpec(
            id = "google_fit",
            name = "Google Fit",
            category = AppConnectorCategory.GOOGLE_APPS,
            packageNames = listOf("com.google.android.apps.fitness"),
            capabilitySummary = "Launch movement & activity tracking"
        ),
        AppConnectorSpec(
            id = "todoist",
            name = "Todoist",
            category = AppConnectorCategory.PRODUCTIVITY,
            packageNames = listOf("com.todoist"),
            capabilitySummary = "Send structured tasks & daily routines",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "notion",
            name = "Notion",
            category = AppConnectorCategory.PRODUCTIVITY,
            packageNames = listOf("notion.id"),
            capabilitySummary = "Share day plans & journal summaries to Notion",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "outlook",
            name = "Microsoft Outlook",
            category = AppConnectorCategory.COMMUNICATION,
            packageNames = listOf("com.microsoft.office.outlook"),
            capabilitySummary = "Draft emails & check schedule handoff",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "spotify",
            name = "Spotify",
            category = AppConnectorCategory.WELLNESS_MEDIA,
            packageNames = listOf("com.spotify.music"),
            capabilitySummary = "Launch focus, sleep, or calm music sessions"
        ),
        AppConnectorSpec(
            id = "youtube_music",
            name = "YouTube Music",
            category = AppConnectorCategory.WELLNESS_MEDIA,
            packageNames = listOf("com.google.android.apps.youtube.music"),
            capabilitySummary = "Play ambient relaxation or focus playlists"
        ),
        AppConnectorSpec(
            id = "whatsapp",
            name = "WhatsApp",
            category = AppConnectorCategory.COMMUNICATION,
            packageNames = listOf("com.whatsapp"),
            capabilitySummary = "Send a quick supportive check-in message",
            supportsSystemFallback = true
        ),
        AppConnectorSpec(
            id = "slack",
            name = "Slack",
            category = AppConnectorCategory.COMMUNICATION,
            packageNames = listOf("com.Slack"),
            capabilitySummary = "Open workspace or share status updates"
        )
    )

    val defaultEnabledIds: Set<String> = setOf(
        "google_calendar",
        "google_tasks",
        "gmail",
        "clock_alarms"
    )

    fun findById(id: String): AppConnectorSpec? = allConnectors.firstOrNull { it.id == id }
}

class ConnectedAppsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEnabledConnectorIds(userId: String): Set<String> {
        val key = "enabled_$userId"
        if (!prefs.contains(key)) {
            return AppConnectorCatalog.defaultEnabledIds
        }
        return prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    }

    fun setConnectorEnabled(userId: String, connectorId: String, enabled: Boolean): Set<String> {
        val current = getEnabledConnectorIds(userId).toMutableSet()
        if (enabled) current.add(connectorId) else current.remove(connectorId)
        prefs.edit().putStringSet("enabled_$userId", current).apply()
        return current
    }

    private companion object {
        const val PREFS_NAME = "mindmate_connected_apps"
    }
}

object AgentActionParser {
    private val completeBlockRegex = Regex("\\[ACTION\\](.*?)\\[/ACTION\\]", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val trailingIncompleteRegex = Regex("\\[ACTION\\].*$", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    data class ParsedAgentResponse(
        val displayText: String,
        val actions: List<AgentScheduledAction>
    )

    /**
     * Strips incomplete [ACTION]... blocks while streaming so raw JSON never flickers in chat UI.
     */
    fun stripStreamingActionTags(raw: String): String {
        return raw
            .replace(completeBlockRegex, "")
            .replace(trailingIncompleteRegex, "")
            .trim()
    }

    fun parse(raw: String): ParsedAgentResponse {
        val actions = mutableListOf<AgentScheduledAction>()
        completeBlockRegex.findAll(raw).forEach { match ->
            val jsonText = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (jsonText.isNotEmpty()) {
                parseJsonAction(jsonText)?.let { actions.add(it) }
            }
        }
        val cleanText = raw
            .replace(completeBlockRegex, "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return ParsedAgentResponse(displayText = cleanText, actions = actions)
    }

    private fun parseJsonAction(jsonText: String): AgentScheduledAction? {
        return try {
            val obj = JSONObject(jsonText)
            val title = obj.optString("title", "").trim()
            if (title.isEmpty()) return null
            val type = AgentActionType.fromWireName(obj.optString("type", "create_task"))
            AgentScheduledAction(
                type = type,
                title = title,
                detail = obj.optString("detail", "").trim(),
                hour = obj.optInt("hour", 9).coerceIn(0, 23),
                minute = obj.optInt("minute", 0).coerceIn(0, 59),
                durationMinutes = obj.optInt("durationMinutes", 30).coerceIn(1, 480),
                targetAppId = obj.optString("targetAppId", "").trim(),
                recipient = obj.optString("recipient", "").trim()
            )
        } catch (_: Exception) {
            null
        }
    }
}

object AppActionExecutor {
    private const val TAG = "AppActionExecutor"

    fun hasCalendarReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCalendarWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun isConnectorInstalledOrReady(context: Context, spec: AppConnectorSpec): Boolean {
        val pm = context.packageManager
        val hasDirectPackage = spec.packageNames.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
        if (hasDirectPackage) return true
        return spec.supportsSystemFallback
    }

    fun buildStatuses(context: Context, enabledIds: Set<String>): List<AppConnectorStatus> {
        return AppConnectorCatalog.allConnectors.map { spec ->
            AppConnectorStatus(
                spec = spec,
                isEnabled = spec.id in enabledIds,
                isInstalledOrReady = isConnectorInstalledOrReady(context, spec)
            )
        }
    }

    fun readTodayCalendarEvents(context: Context): List<DeviceCalendarEvent> {
        if (!hasCalendarReadPermission(context)) return emptyList()
        val calStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = calStart.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val events = mutableListOf<DeviceCalendarEvent>()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .let { builder ->
                android.content.ContentUris.appendId(builder, calStart.timeInMillis)
                android.content.ContentUris.appendId(builder, calEnd.timeInMillis)
                builder.build()
            }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        return try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext() && events.size < 20) {
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty().trim() else ""
                    if (title.isNotEmpty()) {
                        events.add(
                            DeviceCalendarEvent(
                                id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L,
                                title = title,
                                startMillis = if (beginIdx >= 0) cursor.getLong(beginIdx) else calStart.timeInMillis,
                                endMillis = if (endIdx >= 0) cursor.getLong(endIdx) else calStart.timeInMillis,
                                allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1
                            )
                        )
                    }
                }
            }
            events
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query device calendar events", e)
            emptyList()
        }
    }

    fun executeAction(context: Context, action: AgentScheduledAction): ActionExecutionResult {
        return when (action.type) {
            AgentActionType.CALENDAR_EVENT -> scheduleCalendarEvent(context, action)
            AgentActionType.SET_ALARM -> setAlarm(context, action)
            AgentActionType.SET_TIMER -> setTimer(context, action)
            AgentActionType.CREATE_TASK -> sendTaskOrNote(context, action)
            AgentActionType.DRAFT_EMAIL -> draftEmail(context, action)
            AgentActionType.OPEN_APP -> openConnectedApp(context, action)
        }
    }

    private fun scheduleCalendarEvent(
        context: Context,
        action: AgentScheduledAction
    ): ActionExecutionResult {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, action.hour)
            set(Calendar.MINUTE, action.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = startCal.timeInMillis
        val endMillis = startMillis + action.durationMinutes * 60_000L

        if (hasCalendarWritePermission(context)) {
            val primaryCalendarId = findPrimaryCalendarId(context)
            if (primaryCalendarId != null) {
                try {
                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, startMillis)
                        put(CalendarContract.Events.DTEND, endMillis)
                        put(CalendarContract.Events.TITLE, action.title)
                        put(CalendarContract.Events.DESCRIPTION, action.detail.ifBlank { "Scheduled via MindMate" })
                        put(CalendarContract.Events.CALENDAR_ID, primaryCalendarId)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    }
                    val inserted = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    if (inserted != null) {
                        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                        return ActionExecutionResult(
                            true,
                            "Scheduled \"${action.title}\" at ${fmt.format(Date(startMillis))} in your Calendar."
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Direct CalendarContract insert failed, launching Calendar intent", e)
                }
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, action.title)
            putExtra(CalendarContract.Events.DESCRIPTION, action.detail.ifBlank { "Scheduled via MindMate" })
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentSafely(
            context,
            intent,
            successMessage = "Opened Calendar to save \"${action.title}\".",
            fallbackFailure = "No Calendar app found on this device."
        )
    }

    private fun findPrimaryCalendarId(context: Context): Long? {
        if (!hasCalendarReadPermission(context)) return null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null
            )?.use { cursor ->
                var firstId: Long? = null
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    if (firstId == null) firstId = id
                    if (cursor.getInt(1) == 1) return id
                }
                firstId
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setAlarm(context: Context, action: AgentScheduledAction): ActionExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.title)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentSafely(
            context,
            intent,
            successMessage = String.format(Locale.getDefault(), "Set alarm \"%s\" for %02d:%02d.", action.title, action.hour, action.minute),
            fallbackFailure = "No Clock app available to set an alarm."
        )
    }

    private fun setTimer(context: Context, action: AgentScheduledAction): ActionExecutionResult {
        val seconds = (action.durationMinutes * 60).coerceAtLeast(30)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.title)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchIntentSafely(
            context,
            intent,
            successMessage = "Started ${action.durationMinutes}-minute timer for \"${action.title}\".",
            fallbackFailure = "No Clock app available to start a timer."
        )
    }

    private fun draftEmail(context: Context, action: AgentScheduledAction): ActionExecutionResult {
        val recipient = action.recipient.trim()
        val mailUri = if (recipient.isNotEmpty()) {
            Uri.parse("mailto:${Uri.encode(recipient)}")
        } else {
            Uri.parse("mailto:")
        }
        val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, action.title)
            putExtra(Intent.EXTRA_TEXT, action.detail)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val gmailPkg = "com.google.android.gm"
        if (isPackageInstalled(context, gmailPkg)) {
            intent.setPackage(gmailPkg)
        }
        return launchIntentSafely(
            context,
            intent,
            successMessage = "Opened email draft: \"${action.title}\".",
            fallbackFailure = "No email app found on this device."
        )
    }

    private fun sendTaskOrNote(context: Context, action: AgentScheduledAction): ActionExecutionResult {
        val content = buildString {
            append(action.title)
            if (action.detail.isNotBlank()) {
                append("\n\n")
                append(action.detail)
            }
        }
        val preferredSpec = AppConnectorCatalog.findById(action.targetAppId)
        val targetPackage = preferredSpec?.packageNames?.firstOrNull { isPackageInstalled(context, it) }
            ?: listOf(
                "com.google.android.apps.tasks",
                "com.google.android.keep",
                "com.todoist",
                "notion.id"
            ).firstOrNull { isPackageInstalled(context, it) }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, action.title)
            putExtra(Intent.EXTRA_TEXT, content)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (targetPackage != null) {
            launchIntentSafely(
                context,
                shareIntent,
                successMessage = "Sent \"${action.title}\" to ${preferredSpec?.name ?: "connected app"}.",
                fallbackFailure = "Could not open tasks or notes app."
            )
        } else {
            val chooser = Intent.createChooser(shareIntent, "Save task or note").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launchIntentSafely(
                context,
                chooser,
                successMessage = "Opened share options for \"${action.title}\".",
                fallbackFailure = "No tasks or notes app available."
            )
        }
    }

    private fun openConnectedApp(context: Context, action: AgentScheduledAction): ActionExecutionResult {
        val spec = AppConnectorCatalog.findById(action.targetAppId)
        val pkg = spec?.packageNames?.firstOrNull { isPackageInstalled(context, it) }
        if (pkg != null) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launch != null) {
                return launchIntentSafely(
                    context,
                    launch,
                    successMessage = "Opened ${spec.name}.",
                    fallbackFailure = "Unable to launch ${spec.name}."
                )
            }
        }
        return ActionExecutionResult(
            false,
            "${spec?.name ?: "Requested app"} is not installed on this device."
        )
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun launchIntentSafely(
        context: Context,
        intent: Intent,
        successMessage: String,
        fallbackFailure: String
    ): ActionExecutionResult {
        return try {
            context.startActivity(intent)
            ActionExecutionResult(true, successMessage)
        } catch (e: Exception) {
            Log.w(TAG, "Intent launch failed: ${intent.action}", e)
            ActionExecutionResult(false, fallbackFailure)
        }
    }
}
