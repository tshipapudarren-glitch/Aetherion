package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.security.configureAppCheck
import com.example.data.local.AetherDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.model.VipModuleId
import com.example.data.remote.FirestoreSyncRepository
import com.example.ui.screens.AetherionAuthScreen
import com.example.ui.screens.AetherionKnowledgeScreen
import com.example.ui.screens.AutonomousStrategyFactoryScreen
import com.example.ui.screens.BlackSwanPredictorScreen
import com.example.ui.screens.BrokerIntelligenceScreen
import com.example.ui.screens.CapitalFlowRadarScreen
import com.example.ui.screens.CommandCentreScreen
import com.example.ui.screens.CommanderVoiceScreen
import com.example.ui.screens.GlobalEventImpactScreen
import com.example.ui.screens.IntegrationsSettingsScreen
import com.example.ui.screens.LiveMarketConditionsScreen
import com.example.ui.screens.MarketTimeReconstructionScreen
import com.example.ui.screens.ModulesDirectoryScreen
import com.example.ui.screens.MultiverseSimulatorScreen
import com.example.ui.screens.NarrativeIntelligenceScreen
import com.example.ui.screens.OvermindScreen
import com.example.ui.screens.QuantumAllocatorScreen
import com.example.ui.screens.RiskDefenceMatrixScreen
import com.example.ui.screens.SovereignCouncilScreen
import com.example.ui.screens.StrategyArenaScreen
import com.example.ui.screens.StrategyDnaLabScreen
import com.example.ui.screens.TradingTerminalScreen
import com.example.ui.screens.signOutAetherion
import com.example.ui.theme.AetherionTheme
import com.example.ui.theme.ChampagneSecondary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.AetherionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_COMMAND_CENTRE = "command_centre"
const val ROUTE_MARKET_CONDITIONS = "market_conditions"
const val ROUTE_NARRATIVE_INTELLIGENCE = "narrative_intelligence"
const val ROUTE_TRADING_TERMINAL = "trading_terminal"
const val ROUTE_MODULES_DIRECTORY = "modules_directory"
const val ROUTE_OVERMIND = "overmind"
const val ROUTE_RISK_MATRIX = "risk_matrix"
const val ROUTE_KNOWLEDGE_BASE = "aetherion_knowledge"
const val ROUTE_INTEGRATIONS = "integrations"
const val ROUTE_MODULE_PATTERN = "module/{moduleNumber}"

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { authInstance ->
        trySend(authInstance.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private data class TopNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val primaryTabs = listOf(
    TopNavTab(ROUTE_COMMAND_CENTRE, "Command", Icons.Filled.Dashboard, "nav_command_centre"),
    TopNavTab(ROUTE_MARKET_CONDITIONS, "Conditions", Icons.Filled.Public, "nav_market_conditions"),
    TopNavTab(ROUTE_MODULES_DIRECTORY, "Modules", Icons.Filled.Apps, "nav_modules_directory"),
    TopNavTab(ROUTE_OVERMIND, "Overmind", Icons.Filled.Psychology, "nav_overmind"),
    TopNavTab(ROUTE_INTEGRATIONS, "Settings", Icons.Filled.Settings, "nav_integrations")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)
        setContent {
            AetherionTheme {
                val currentUser by remember { Firebase.auth.authStateFlow() }
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                val user = currentUser
                if (user == null) {
                    AetherionAuthScreen(onAuthSuccess = { /* authStateFlow advances reactively */ })
                } else {
                    key(user.uid) {
                        AetherionAppShell(
                            currentUserId = user.uid,
                            userEmail = user.email
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AetherionAppShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AetherionViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                AetherionViewModel(
                    currentUserId = currentUserId,
                    localDb = AetherDatabase.getInstance(appContext),
                    firestoreRepo = FirestoreSyncRepository(appContext),
                    settingsPrefs = SettingsPreferences(appContext)
                )
            }
        }
    )

    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openModule(module: VipModuleId) {
        when (module) {
            VipModuleId.AETHERION_OVERMIND -> navigateTopTab(ROUTE_OVERMIND)
            VipModuleId.AUTONOMOUS_RISK_DEFENCE_MATRIX -> navController.navigate(ROUTE_RISK_MATRIX) {
                launchSingleTop = true
            }
            else -> navController.navigate("module/${module.number}") {
                launchSingleTop = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    containerColor = ObsidianSurface,
                    contentColor = MetallicGoldBright,
                    modifier = Modifier
                        .fillMaxHeight()
                        .safeDrawingPadding()
                ) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "VIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    primaryTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ObsidianVoid,
                                selectedTextColor = MetallicGoldBright,
                                indicatorColor = MetallicGold,
                                unselectedIconColor = SlateMuted,
                                unselectedTextColor = SlateMuted
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            Scaffold(
                containerColor = ObsidianVoid,
                contentWindowInsets = WindowInsets.navigationBars,
                topBar = {
                    Surface(
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.aetherion_logo_crest_1794720948641),
                                        contentDescription = "AETHERION Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    )
                                    Text(
                                        text = "AETHERION · VIP",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MetallicGoldBright
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (state.constitution.emergencyKillSwitch) CrimsonContainer else ObsidianElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (state.constitution.emergencyKillSwitch) CrimsonAlert else LiveEmerald.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            navController.navigate(ROUTE_RISK_MATRIX) {
                                                launchSingleTop = true
                                            }
                                        }
                                        .testTag("header_risk_pill")
                                ) {
                                    Text(
                                        text = if (state.constitution.emergencyKillSwitch) "KILL SWITCH ON"
                                        else "RISK GUARD L${state.constitution.permissionLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.constitution.emergencyKillSwitch) CrimsonAlert else LiveEmerald,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            val banner = state.statusBanner
                            if (!banner.isNullOrBlank()) {
                                Surface(
                                    color = CrimsonContainer.copy(alpha = 0.75f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = banner,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IvoryText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.dismissBanner() },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Dismiss status banner",
                                                tint = ChampagneSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            contentColor = MetallicGoldBright
                        ) {
                            primaryTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ObsidianVoid,
                                        selectedTextColor = MetallicGoldBright,
                                        indicatorColor = MetallicGold,
                                        unselectedIconColor = SlateMuted,
                                        unselectedTextColor = SlateMuted
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_COMMAND_CENTRE
                    ) {
                        composable(ROUTE_COMMAND_CENTRE) {
                            CommandCentreScreen(
                                state = state,
                                onRefreshMarkets = { viewModel.refreshMarketsAndAnalytics() },
                                onSelectSymbol = { viewModel.selectSymbol(it) },
                                onSelectTimeframe = { viewModel.setChartTimeframe(it) },
                                onToggleAutonomousCycle = { viewModel.toggleAutonomousCycle() },
                                onToggleKillSwitch = { viewModel.toggleEmergencyKillSwitch(it) },
                                onOpenConditions = { navigateTopTab(ROUTE_MARKET_CONDITIONS) },
                                onOpenNarratives = {
                                    navController.navigate(ROUTE_NARRATIVE_INTELLIGENCE) { launchSingleTop = true }
                                },
                                onOpenTradingTerminal = {
                                    navController.navigate(ROUTE_TRADING_TERMINAL) { launchSingleTop = true }
                                },
                                onOpenKnowledge = {
                                    navController.navigate(ROUTE_KNOWLEDGE_BASE) { launchSingleTop = true }
                                },
                                onOpenModule = ::openModule
                            )
                        }

                        composable(ROUTE_MARKET_CONDITIONS) {
                            LiveMarketConditionsScreen(
                                state = state,
                                onRefreshConditions = { viewModel.refreshMarketConditions() },
                                onAnalyzeConditionsWithAi = { viewModel.analyzeMarketConditionsWithAi() },
                                onOpenIntegrations = { navigateTopTab(ROUTE_INTEGRATIONS) }
                            )
                        }

                        composable(ROUTE_NARRATIVE_INTELLIGENCE) {
                            NarrativeIntelligenceScreen(
                                state = state,
                                onSelectCluster = { viewModel.selectNarrativeCluster(it) },
                                onSelectSymbol = { viewModel.selectSymbol(it) },
                                onRefreshNarratives = { viewModel.refreshHeadlines() },
                                onAnalyzeNarrativeWithAi = { viewModel.analyzeSelectedNarrativeWithAi(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_TRADING_TERMINAL) {
                            TradingTerminalScreen(
                                state = state,
                                onSelectSymbol = { viewModel.selectSymbol(it) },
                                onSelectTimeframe = { viewModel.setChartTimeframe(it) },
                                onSelectDirectionBias = { viewModel.setSetupDirectionBias(it) },
                                onRefineSetupWithAi = { viewModel.refineActiveSetupWithAi() },
                                onExecuteSetupOrder = { mt5 -> viewModel.executeActiveSetupOrder(mt5) },
                                onExecuteTerminalTicket = { side, exp, sl, tp, mt5 ->
                                    viewModel.executeTerminalTicketOrder(side, exp, sl, tp, mt5)
                                },
                                onClosePaperOrder = { viewModel.closePaperOrder(it) },
                                onPickChartScreenshot = { uri -> viewModel.analyzeUploadedChartScreenshot(context, uri) },
                                onOpenEventEngine = { openModule(VipModuleId.GLOBAL_EVENT_IMPACT_ENGINE) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_MODULES_DIRECTORY) {
                            ModulesDirectoryScreen(
                                state = state,
                                onOpenNarratives = {
                                    navController.navigate(ROUTE_NARRATIVE_INTELLIGENCE) { launchSingleTop = true }
                                },
                                onOpenTradingTerminal = {
                                    navController.navigate(ROUTE_TRADING_TERMINAL) { launchSingleTop = true }
                                },
                                onOpenKnowledge = {
                                    navController.navigate(ROUTE_KNOWLEDGE_BASE) { launchSingleTop = true }
                                },
                                onOpenModule = ::openModule
                            )
                        }

                        composable(ROUTE_KNOWLEDGE_BASE) {
                            AetherionKnowledgeScreen(
                                state = state,
                                onSearchChanged = { viewModel.setKnowledgeSearch(it) },
                                onSelectTierFilter = { viewModel.setKnowledgeTierFilter(it) },
                                onSaveKnowledge = { tier, title, body, sym ->
                                    viewModel.saveMemoryEntry(tier, title, body, sym)
                                },
                                onDistillKnowledgeWithAi = { viewModel.distillKnowledgeWithAi(it) },
                                onQueryMemoryAi = { viewModel.queryCollectiveMemoryWithAi(it) },
                                onBoostConfidence = { viewModel.boostKnowledgeConfidence(it) },
                                onDeleteEntry = { viewModel.deleteKnowledgeEntry(it) },
                                onOpenArenaAi = { openModule(VipModuleId.STRATEGY_CHAMPIONSHIP_ARENA) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_OVERMIND) {
                            OvermindScreen(
                                state = state,
                                onToggleCycle = { viewModel.toggleAutonomousCycle() },
                                onStepCycleOnce = { viewModel.stepAutonomousCycleOnce() },
                                onBack = { navigateTopTab(ROUTE_COMMAND_CENTRE) }
                            )
                        }

                        composable(ROUTE_RISK_MATRIX) {
                            RiskDefenceMatrixScreen(
                                state = state,
                                onUpdateConstitution = { viewModel.updateRiskConstitution(it) },
                                onToggleKillSwitch = { viewModel.toggleEmergencyKillSwitch(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_INTEGRATIONS) {
                            IntegrationsSettingsScreen(
                                state = state,
                                userEmail = userEmail,
                                userId = currentUserId,
                                onUpdateSettings = { viewModel.updateProviderSettings(it) },
                                onTestProvider = { viewModel.testProviderConnection(it) },
                                onPingMt5 = { viewModel.pingMt5Bridge() },
                                onOpenRiskMatrix = {
                                    navController.navigate(ROUTE_RISK_MATRIX) {
                                        launchSingleTop = true
                                    }
                                },
                                onSignOut = {
                                    signOutAetherion(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            )
                        }

                        composable(
                            route = ROUTE_MODULE_PATTERN,
                            arguments = listOf(navArgument("moduleNumber") { type = NavType.IntType })
                        ) { entry ->
                            val moduleNumber = entry.arguments?.getInt("moduleNumber") ?: 1
                            val module = VipModuleId.entries.firstOrNull { it.number == moduleNumber }
                                ?: VipModuleId.MULTIVERSE_SIMULATOR
                            val onBack: () -> Unit = { navController.popBackStack() }

                            when (module) {
                                VipModuleId.MULTIVERSE_SIMULATOR -> MultiverseSimulatorScreen(
                                    state = state,
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onSelectRegime = { viewModel.runMultiverseScenario(it) },
                                    onBack = onBack
                                )
                                VipModuleId.AI_SOVEREIGN_COUNCIL -> SovereignCouncilScreen(
                                    state = state,
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onSelectMandate = { viewModel.selectCouncilMandate(it) },
                                    onConveneCouncil = { viewModel.conveneSovereignCouncil(it) },
                                    onBack = onBack
                                )
                                VipModuleId.STRATEGY_DNA_LAB -> StrategyDnaLabScreen(
                                    state = state,
                                    onMutateStrategy = { viewModel.mutateAndBacktestStrategy(it) },
                                    onSelectQuantTarget = { target, strat -> viewModel.selectQuantCodeTarget(target, strat) },
                                    onAuditQuantCode = { strat -> viewModel.auditQuantCodeWithAi(strat) },
                                    onBack = onBack
                                )
                                VipModuleId.GLOBAL_CAPITAL_FLOW_RADAR -> CapitalFlowRadarScreen(
                                    state = state,
                                    onRefreshMarkets = { viewModel.refreshMarketsAndAnalytics() },
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onAnalyzeFlowsAi = { viewModel.analyzeCapitalFlowsWithAi() },
                                    onBack = onBack
                                )
                                VipModuleId.BLACK_SWAN_PREDICTOR -> BlackSwanPredictorScreen(
                                    state = state,
                                    onEngageKillSwitch = { viewModel.toggleEmergencyKillSwitch(it) },
                                    onBack = onBack
                                )
                                VipModuleId.STRATEGY_CHAMPIONSHIP_ARENA -> StrategyArenaScreen(
                                    state = state,
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onPromoteStrategy = { strat, stage -> viewModel.promoteOrRetireStrategy(strat, stage) },
                                    onExecutePaper = { strat, side, exp ->
                                        viewModel.executeAuthorisedTrade(strat, side, exp, routeToMt5Bridge = false)
                                    },
                                    onRunArenaAi = { viewModel.runArenaAiMatchupWithKnowledge() },
                                    onSaveArenaLessonToKnowledge = { viewModel.saveArenaVerdictToKnowledge(it) },
                                    onOpenKnowledge = {
                                        navController.navigate(ROUTE_KNOWLEDGE_BASE) { launchSingleTop = true }
                                    },
                                    onBack = onBack
                                )
                                VipModuleId.AUTONOMOUS_BROKER_INTELLIGENCE -> BrokerIntelligenceScreen(
                                    state = state,
                                    onPingBridge = { viewModel.pingMt5Bridge() },
                                    onExecuteOrder = { strat, side, exp, mt5 ->
                                        viewModel.executeAuthorisedTrade(strat, side, exp, routeToMt5Bridge = mt5)
                                    },
                                    onClosePaperOrder = { viewModel.closePaperOrder(it) },
                                    onOpenSettings = { navigateTopTab(ROUTE_INTEGRATIONS) },
                                    onBack = onBack
                                )
                                VipModuleId.MARKET_TIME_RECONSTRUCTION -> MarketTimeReconstructionScreen(
                                    state = state,
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onSeekReplay = { viewModel.setReplayIndex(it) },
                                    onTogglePlayback = { viewModel.toggleTimeReplayPlayback() },
                                    onBack = onBack
                                )
                                VipModuleId.QUANTUM_CAPITAL_ALLOCATOR -> QuantumAllocatorScreen(
                                    state = state,
                                    onBack = onBack
                                )
                                VipModuleId.COLLECTIVE_INTELLIGENCE_MEMORY -> AetherionKnowledgeScreen(
                                    state = state,
                                    onSearchChanged = { viewModel.setKnowledgeSearch(it) },
                                    onSelectTierFilter = { viewModel.setKnowledgeTierFilter(it) },
                                    onSaveKnowledge = { tier, title, body, sym ->
                                        viewModel.saveMemoryEntry(tier, title, body, sym)
                                    },
                                    onDistillKnowledgeWithAi = { viewModel.distillKnowledgeWithAi(it) },
                                    onQueryMemoryAi = { viewModel.queryCollectiveMemoryWithAi(it) },
                                    onBoostConfidence = { viewModel.boostKnowledgeConfidence(it) },
                                    onDeleteEntry = { viewModel.deleteKnowledgeEntry(it) },
                                    onOpenArenaAi = { openModule(VipModuleId.STRATEGY_CHAMPIONSHIP_ARENA) },
                                    onBack = onBack
                                )
                                VipModuleId.COMMANDER_VOICE_INTELLIGENCE -> CommanderVoiceScreen(
                                    state = state,
                                    onStartLiveConversation = { viewModel.startGeminiLiveConversation(context) },
                                    onStopLiveConversation = { viewModel.stopGeminiLiveConversation() },
                                    onSendLiveText = { viewModel.sendGeminiLiveText(it) },
                                    onClearLiveTranscripts = { viewModel.clearGeminiLiveTranscripts() },
                                    onStartDeepgramCapture = { viewModel.startDeepgramCapture(context) },
                                    onStopDeepgramAndExecute = { speak -> viewModel.stopDeepgramAndExecute(context, speak) },
                                    onSpeakElevenLabs = { text -> viewModel.speakWithElevenLabs(context, text) },
                                    onStopElevenLabs = { viewModel.stopElevenLabsPlayback() },
                                    onFetchElevenLabsVoices = { viewModel.fetchElevenLabsVoices() },
                                    onUpdateSettings = { viewModel.updateProviderSettings(it) },
                                    onSubmitInstruction = { cmd, speak -> viewModel.submitCommanderInstruction(cmd, speak, context) },
                                    onBack = onBack
                                )
                                VipModuleId.AUTONOMOUS_RISK_DEFENCE_MATRIX -> RiskDefenceMatrixScreen(
                                    state = state,
                                    onUpdateConstitution = { viewModel.updateRiskConstitution(it) },
                                    onToggleKillSwitch = { viewModel.toggleEmergencyKillSwitch(it) },
                                    onBack = onBack
                                )
                                VipModuleId.GLOBAL_EVENT_IMPACT_ENGINE -> GlobalEventImpactScreen(
                                    state = state,
                                    onRefreshHeadlines = { viewModel.refreshHeadlines() },
                                    onAnalyzeHeadline = { viewModel.analyzeHeadlineImpact(it) },
                                    onSelectSymbolAndOpenChart = { sym ->
                                        viewModel.selectSymbol(sym)
                                        navController.navigate(ROUTE_TRADING_TERMINAL) { launchSingleTop = true }
                                    },
                                    onOpenNarratives = {
                                        navController.navigate(ROUTE_NARRATIVE_INTELLIGENCE) { launchSingleTop = true }
                                    },
                                    onOpenTradingTerminal = {
                                        navController.navigate(ROUTE_TRADING_TERMINAL) { launchSingleTop = true }
                                    },
                                    onBack = onBack
                                )
                                VipModuleId.AUTONOMOUS_STRATEGY_FACTORY -> AutonomousStrategyFactoryScreen(
                                    state = state,
                                    onSelectSymbol = { viewModel.selectSymbol(it) },
                                    onRunFactoryBatch = { viewModel.runAutonomousStrategyFactoryBatch(it) },
                                    onBack = onBack
                                )
                                VipModuleId.AETHERION_OVERMIND -> OvermindScreen(
                                    state = state,
                                    onToggleCycle = { viewModel.toggleAutonomousCycle() },
                                    onStepCycleOnce = { viewModel.stepAutonomousCycleOnce() },
                                    onBack = onBack
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
