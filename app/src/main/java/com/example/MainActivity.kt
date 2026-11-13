package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.MindMateAiService
import com.example.data.repository.MindMateRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.ui.components.ActivityGuidanceDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompanionScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsAndJournalScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.MindMateTheme
import com.example.ui.theme.NavBarSurface
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidMidnight
import com.example.ui.viewmodel.MindMateViewModel
import com.example.voice.MindMateVoiceService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_COMPANION = "companion"
const val ROUTE_DISCOVER = "discover"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_PROFILE = "profile"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MindMateDestinations = listOf(
    NavDestinationItem(ROUTE_HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    NavDestinationItem(ROUTE_COMPANION, "Companion", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavDestinationItem(ROUTE_DISCOVER, "Discover", Icons.Rounded.Explore, Icons.Outlined.Explore),
    NavDestinationItem(ROUTE_INSIGHTS, "Insights", Icons.Rounded.Insights, Icons.Outlined.Insights),
    NavDestinationItem(ROUTE_PROFILE, "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MindMateTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            MindMateAuthenticatedShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: "Friend",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        }
    }
}

@Composable
private fun MindMateAuthenticatedShell(
    currentUserId: String,
    displayName: String,
    email: String,
    photoUrl: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MindMateViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val aiService = MindMateAiService()
                MindMateViewModel(
                    repository = MindMateRepository(db),
                    aiService = aiService,
                    voiceService = MindMateVoiceService(app, aiService),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email,
                    initialPhotoUrl = photoUrl
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    var insightsSubTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        val msg = state.errorMessage ?: state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessages()
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidMidnight)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = VoidMidnight,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    MindMateBottomBar(
                        currentRoute = currentRoute,
                        onSelectRoute = { route ->
                            if (route == ROUTE_INSIGHTS) insightsSubTab = 0
                            navigateToTab(route)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = NavBarSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MindMateDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (dest.route == ROUTE_INSIGHTS) insightsSubTab = 0
                                    navigateToTab(dest.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = LuminousViolet,
                                    selectedTextColor = SoftLavender,
                                    indicatorColor = LuminousViolet.copy(alpha = 0.2f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (state.profileInitError != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DeepIndigoSurface,
                            border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cloud sync warning: ${state.profileInitError}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ErrorCoral,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = viewModel::retryProfileInit) {
                                    Text("Retry", color = CalmMint)
                                }
                            }
                        }
                    }

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LuminousViolet)
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            enterTransition = { fadeIn(animationSpec = tween(220)) },
                            exitTransition = { fadeOut(animationSpec = tween(180)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    state = state,
                                    onCheckInMood = viewModel::checkInMood,
                                    onAddHydration = viewModel::addHydration,
                                    onTalkNow = { navigateToTab(ROUTE_COMPANION) },
                                    onOpenJournal = {
                                        insightsSubTab = 1
                                        navigateToTab(ROUTE_INSIGHTS)
                                    },
                                    onStartActivity = { viewModel.openActivityGuide(it) },
                                    onActivityStatus = viewModel::setActivityStatus,
                                    onOpenProfile = { navigateToTab(ROUTE_PROFILE) }
                                )
                            }
                            composable(ROUTE_COMPANION) {
                                BackHandler { navigateToTab(ROUTE_HOME) }
                                CompanionScreen(
                                    state = state,
                                    voiceState = voiceState,
                                    onSelectMode = viewModel::setCompanionMode,
                                    onSelectModelTier = viewModel::setModelTier,
                                    onSendMessage = { viewModel.sendCompanionMessage(it) },
                                    onRetryLastMessage = viewModel::retryLastCompanionMessage,
                                    onToggleLiveVoice = viewModel::toggleLiveVoiceSession,
                                    onToggleDictate = viewModel::toggleDictateForTranscription,
                                    onInterruptAssistant = viewModel::interruptAssistant,
                                    onToggleMute = viewModel::toggleVoiceMute,
                                    onEndSession = viewModel::endLiveVoiceSession,
                                    onGrantVoiceConsent = viewModel::grantVoiceConsentAndConnect,
                                    onConsumePendingTranscript = viewModel::consumePendingTranscript,
                                    onDismissCrisis = viewModel::dismissCrisisBanner,
                                    onClearVoiceError = viewModel::clearVoiceError
                                )
                            }
                            composable(ROUTE_DISCOVER) {
                                BackHandler { navigateToTab(ROUTE_HOME) }
                                DiscoverScreen(
                                    state = state,
                                    onUpdateFilters = viewModel::updateDiscoverFilters,
                                    onStartActivity = { viewModel.openActivityGuide(it) },
                                    onActivityStatus = viewModel::setActivityStatus
                                )
                            }
                            composable(ROUTE_INSIGHTS) {
                                BackHandler { navigateToTab(ROUTE_HOME) }
                                InsightsAndJournalScreen(
                                    state = state,
                                    activeSubTab = insightsSubTab,
                                    onSelectSubTab = { insightsSubTab = it },
                                    onSelectRangeDays = viewModel::setInsightsRange,
                                    onToggleSamplePreview = viewModel::toggleSamplePreview,
                                    onCreateJournal = viewModel::createJournalEntry,
                                    onUpdateJournal = viewModel::updateJournalEntry,
                                    onDeleteJournal = viewModel::deleteJournalEntry,
                                    onGenerateJournalReflection = viewModel::generateJournalAiReflection
                                )
                            }
                            composable(ROUTE_PROFILE) {
                                BackHandler { navigateToTab(ROUTE_HOME) }
                                ProfileScreen(
                                    state = state,
                                    onSaveProfile = viewModel::updateProfileSettings,
                                    onClearHistory = viewModel::clearConversationHistory,
                                    onExportJson = viewModel::exportDataJson,
                                    onDeleteAccountData = {
                                        viewModel.deleteAccountData {
                                            signOut(credentialManager, scope)
                                        }
                                    },
                                    onSignOut = {
                                        signOut(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeGuide = state.activeBreathingRec
    if (activeGuide != null) {
        val isDone = state.activityLogs.any {
            it.recommendationId == activeGuide.id && it.status == "completed"
        }
        ActivityGuidanceDialog(
            recommendation = activeGuide,
            isCompleted = isDone,
            onDismiss = { viewModel.openActivityGuide(null) },
            onComplete = { viewModel.setActivityStatus(activeGuide, "completed") }
        )
    }
}

@Composable
private fun MindMateBottomBar(
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBarSurface)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Luminous top hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.75.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, GlassBorder, LuminousViolet.copy(alpha = 0.4f), GlassBorder, Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MindMateDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                val pillBg by animateColorAsState(
                    targetValue = if (selected) LuminousViolet.copy(alpha = 0.18f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "nav_pill_bg"
                )
                val pillBorder by animateColorAsState(
                    targetValue = if (selected) LuminousViolet.copy(alpha = 0.45f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "nav_pill_border"
                )
                val contentTint by animateColorAsState(
                    targetValue = if (selected) SoftLavender else TextMuted,
                    animationSpec = tween(200),
                    label = "nav_tint"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = LuminousViolet),
                            onClick = { onSelectRoute(dest.route) }
                        )
                        .padding(vertical = 4.dp)
                        .testTag("nav_${dest.route}")
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(pillBg)
                            .border(1.dp, pillBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label,
                            tint = if (selected) LuminousViolet else contentTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentTint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
