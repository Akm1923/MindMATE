package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.integrations.AgentActionType
import com.example.integrations.AgentScheduledAction
import com.example.integrations.AppConnectorCategory
import com.example.integrations.AppConnectorStatus
import com.example.integrations.DeviceCalendarEvent
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.eyebrow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedAppsSheet(
    connectors: List<AppConnectorStatus>,
    todayEvents: List<DeviceCalendarEvent>,
    hasCalendarPermission: Boolean,
    onToggleConnector: (String, Boolean) -> Unit,
    onCalendarPermissionUpdated: () -> Unit,
    onPlanDayWithAgent: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onCalendarPermissionUpdated()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepIndigoSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "CONNECTED APPS & DAY PLANNER",
                style = MaterialTheme.typography.labelSmall.eyebrow(),
                color = CalmMint
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect Google & Daily Apps",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose which apps MindMate can coordinate with when planning your day, creating events, setting alarms, or drafting messages.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onPlanDayWithAgent()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LuminousViolet,
                        contentColor = VoidMidnight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apps_schedule_day_button")
                ) {
                    Text("Schedule My Day", fontWeight = FontWeight.SemiBold)
                }

                if (!hasCalendarPermission) {
                    OutlinedButton(
                        onClick = {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CalmMint.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("grant_calendar_permission_button")
                    ) {
                        Text("Sync Calendar", color = CalmMint)
                    }
                }
            }

            if (hasCalendarPermission && todayEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GlassSurface,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TODAY'S CALENDAR (${todayEvents.size})",
                            style = MaterialTheme.typography.labelSmall.eyebrow(),
                            color = SoftLavender
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        todayEvents.take(3).forEach { ev ->
                            Text(
                                text = ev.formatSummary(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppConnectorCategory.entries.forEach { category ->
                    val group = connectors.filter { it.spec.category == category }
                    if (group.isNotEmpty()) {
                        item(key = "cat_${category.name}") {
                            Text(
                                text = category.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.eyebrow(),
                                color = SoftLavender,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(group, key = { it.spec.id }) { status ->
                            ConnectorToggleRow(
                                status = status,
                                hasCalendarPermission = hasCalendarPermission,
                                onToggle = { enabled ->
                                    onToggleConnector(status.spec.id, enabled)
                                    if (enabled && status.spec.requiresCalendarPermission && !hasCalendarPermission) {
                                        calendarPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectorToggleRow(
    status: AppConnectorStatus,
    hasCalendarPermission: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElevatedCardSurface,
        border = BorderStroke(
            1.dp,
            if (status.isEnabled) LuminousViolet.copy(alpha = 0.45f) else GlassBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connector_row_${status.spec.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status.spec.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val readinessLabel = when {
                        status.spec.requiresCalendarPermission && hasCalendarPermission -> "Synced"
                        status.isInstalledOrReady -> "Ready"
                        else -> "Not Installed"
                    }
                    Text(
                        text = readinessLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.isInstalledOrReady) CalmMint else TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = status.spec.capabilitySummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = status.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VoidMidnight,
                    checkedTrackColor = CalmMint,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DeepIndigoSurface,
                    uncheckedBorderColor = GlassBorder
                ),
                modifier = Modifier.testTag("connector_switch_${status.spec.id}")
            )
        }
    }
}

@Composable
fun AgentScheduledActionCard(
    action: AgentScheduledAction,
    onExecute: (AgentScheduledAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DeepIndigoSurface,
        border = BorderStroke(
            1.dp,
            if (action.isExecuted) CalmMint.copy(alpha = 0.55f) else ElectricIndigo.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_action_card_${action.type.wireName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val timeHint = when (action.type) {
                    AgentActionType.CALENDAR_EVENT, AgentActionType.SET_ALARM ->
                        String.format(Locale.getDefault(), "%02d:%02d", action.hour, action.minute)
                    AgentActionType.SET_TIMER -> "${action.durationMinutes} min timer"
                    AgentActionType.DRAFT_EMAIL -> action.recipient.ifBlank { "Email draft" }
                    AgentActionType.CREATE_TASK -> "Task / Note"
                    AgentActionType.OPEN_APP -> "Launch app"
                }
                Text(
                    text = timeHint.uppercase(),
                    style = MaterialTheme.typography.labelSmall.eyebrow(),
                    color = if (action.isExecuted) CalmMint else SoftLavender
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (action.detail.isNotBlank()) {
                    Text(
                        text = action.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onExecute(action) },
                enabled = !action.isExecuted,
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (action.isExecuted) CalmMint.copy(alpha = 0.2f) else CalmMint,
                    contentColor = if (action.isExecuted) CalmMint else VoidMidnight,
                    disabledContainerColor = CalmMint.copy(alpha = 0.18f),
                    disabledContentColor = CalmMint
                ),
                modifier = Modifier.testTag("execute_action_button_${action.type.wireName}")
            ) {
                Text(
                    text = if (action.isExecuted) "Done" else action.type.buttonLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
