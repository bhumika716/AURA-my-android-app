package com.aura.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.aura.app.ui.components.AuraBottomBar
import com.aura.app.ui.components.avatar.StageUpCelebration
import com.aura.app.ui.screens.avatar.AvatarScreen
import com.aura.app.ui.screens.dashboard.DashboardScreen
import com.aura.app.ui.screens.journal.JournalScreen
import com.aura.app.ui.screens.mirror.MirrorScreen
import com.aura.app.ui.screens.missions.MissionsScreen
import com.aura.app.ui.screens.onboarding.OnboardingScreen
import com.aura.app.ui.screens.sociallab.ChatScreen
import com.aura.app.ui.screens.sociallab.SocialLabScreen
import com.aura.app.viewmodel.UserViewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object SocialLab : Screen("social_lab")
    object SocialLabChat : Screen("social_lab/{scenarioId}") {
        fun createRoute(scenarioId: String) = "social_lab/$scenarioId"
    }
    object Missions : Screen("missions")
    object Journal : Screen("journal")
    object Mirror : Screen("mirror")
    object Avatar : Screen("avatar")
}

@Composable
fun AuraNavGraph(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val userProfile by userViewModel.userProfile.collectAsState()
    val isProfileLoaded by userViewModel.isProfileLoaded.collectAsState()
    
    // Check if we are still loading the profile to avoid redirecting to Onboarding incorrectly
    if (!isProfileLoaded) {
    return
    }
    
    val hasOnboarded = userProfile?.hasOnboarded ?: false

    // Stage-up celebration
    var showStageUp by remember { mutableStateOf(false) }
    var newStage by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        userViewModel.stageUpEvent.collect { stage ->
            newStage = stage
            showStageUp = true
        }
    }

    val startDestination = if (hasOnboarded) Screen.Dashboard.route else Screen.Onboarding.route

    // Determine if bottom bar should be visible
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.SocialLab.route,
        Screen.Missions.route,
        Screen.Journal.route,
        Screen.Avatar.route,
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                AuraBottomBar(
                    currentRoute = currentRoute ?: "",
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 } },
            exitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -it / 4 } },
            popEnterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 } },
            popExitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it / 4 } },
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = { name, ageGroup, empathy, confidence, comms ->
                        userViewModel.createProfile(name, ageGroup, empathy, confidence, comms)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    userProfile = userProfile,
                    onNavigateToSocialLab = { navController.navigate(Screen.SocialLab.route) },
                    onNavigateToMissions = { navController.navigate(Screen.Missions.route) },
                    onNavigateToAvatar = { navController.navigate(Screen.Avatar.route) },
                    onNavigateToMirror = { navController.navigate(Screen.Mirror.route) },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                )
            }

            composable(Screen.SocialLab.route) {
                SocialLabScreen(
                    ageGroup = userProfile?.ageGroup ?: "adults",
                    onSelectScenario = { scenarioId ->
                        navController.navigate(Screen.SocialLabChat.createRoute(scenarioId))
                    }
                )
            }

            composable(
                route = Screen.SocialLabChat.route,
                arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
                ChatScreen(
                    scenarioId = scenarioId,
                    userViewModel = userViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Missions.route) {
                MissionsScreen(userViewModel = userViewModel)
            }

            composable(Screen.Journal.route) {
                JournalScreen()
            }

            composable(Screen.Mirror.route) {
                MirrorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Avatar.route) {
                AvatarScreen(userProfile = userProfile)
            }
        }
    }

    // Stage-up celebration overlay
    if (showStageUp) {
        StageUpCelebration(
            stage = newStage,
            ageGroup = userProfile?.ageGroup ?: "adults",
            onDismiss = { showStageUp = false }
        )
    }
}
