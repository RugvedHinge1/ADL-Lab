package com.scholr.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scholr.app.navigation.Routes
import com.scholr.app.navigation.TopLevelDestination
import com.scholr.app.ui.components.ScholrBottomBar
import com.scholr.app.ui.screens.AuthMode
import com.scholr.app.ui.screens.AuthScreen
import com.scholr.app.ui.screens.ConferencesScreen
import com.scholr.app.ui.screens.HomeScreen
import com.scholr.app.ui.screens.InterestScreen
import com.scholr.app.ui.screens.LibraryScreen
import com.scholr.app.ui.screens.OnboardingScreen
import com.scholr.app.ui.screens.PaperDetailScreen
import com.scholr.app.ui.screens.ProfileScreen
import com.scholr.app.ui.screens.SearchScreen
import com.scholr.app.ui.screens.SplashScreen
import com.scholr.app.ui.theme.ClayBase

/**
 * App shell. One NavHost covers the whole journey; the bottom bar is an
 * overlay that only appears on the four top-level destinations, so the
 * splash, onboarding, auth and detail screens all get the full canvas.
 */
@Composable
fun ScholrApp(vm: ScholrViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevel = TopLevelDestination.fromRoute(currentRoute)
    val showBar = topLevel != null

    Scaffold(
        containerColor = ClayBase,
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    ScholrBottomBar(
                        current = topLevel,
                        onSelect = { destination ->
                            navController.navigateToTopLevel(destination)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.97f) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {

            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        val destination =
                            if (vm.isSignedIn) TopLevelDestination.Home.route else Routes.ONBOARDING
                        navController.navigate(destination) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        vm.completeOnboarding()
                        navController.navigate(Routes.SIGN_UP) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SIGN_UP) {
                AuthScreen(
                    mode = AuthMode.SignUp,
                    isLoading = vm.authLoading,
                    errorMessage = vm.authError,
                    onSubmit = { name, email, password ->
                        vm.signUp(name, email, password) {
                            navController.navigate(Routes.INTERESTS)
                        }
                    },
                    onGoogleToken = { idToken ->
                        vm.signInWithGoogle(idToken) { isNewUser ->
                            val destination =
                                if (isNewUser) Routes.INTERESTS else TopLevelDestination.Home.route
                            navController.navigate(destination) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onSwitchMode = {
                        vm.clearAuthError()
                        navController.navigate(Routes.SIGN_IN)
                    }
                )
            }

            composable(Routes.SIGN_IN) {
                AuthScreen(
                    mode = AuthMode.SignIn,
                    isLoading = vm.authLoading,
                    errorMessage = vm.authError,
                    onSubmit = { _, email, password ->
                        vm.signIn(email, password) {
                            navController.navigate(TopLevelDestination.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onGoogleToken = { idToken ->
                        vm.signInWithGoogle(idToken) { isNewUser ->
                            val destination =
                                if (isNewUser) Routes.INTERESTS else TopLevelDestination.Home.route
                            navController.navigate(destination) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onSwitchMode = {
                        vm.clearAuthError()
                        navController.navigate(Routes.SIGN_UP)
                    }
                )
            }

            composable(Routes.INTERESTS) {
                InterestScreen(
                    selected = vm.selectedInterests,
                    onToggle = vm::toggleInterest,
                    canContinue = vm.canContinueFromInterests,
                    onContinue = {
                        navController.navigate(TopLevelDestination.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(TopLevelDestination.Home.route) {
                HomeScreen(
                    vm = vm,
                    innerPadding = innerPadding,
                    onPaperClick = { navController.navigate(Routes.paperDetail(it)) },
                    onProfileClick = { navController.navigate(Routes.PROFILE) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onSignedOut = {
                        navController.navigate(Routes.SIGN_IN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(TopLevelDestination.Search.route) {
                SearchScreen(
                    vm = vm,
                    innerPadding = innerPadding,
                    onPaperClick = { navController.navigate(Routes.paperDetail(it)) }
                )
            }

            composable(TopLevelDestination.Conferences.route) {
                ConferencesScreen(innerPadding = innerPadding)
            }

            composable(TopLevelDestination.Library.route) {
                LibraryScreen(
                    vm = vm,
                    innerPadding = innerPadding,
                    onPaperClick = { navController.navigate(Routes.paperDetail(it)) }
                )
            }

            composable(
                route = Routes.PAPER_DETAIL,
                enterTransition = {
                    slideInHorizontally(tween(340)) { it / 3 } + fadeIn(tween(240))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200))
                }
            ) { entry ->
                val paperId = entry.arguments?.getString("paperId").orEmpty()
                PaperDetailScreen(
                    paperId = paperId,
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Standard bottom-bar navigation.
 *
 * Home is the anchor: switching tabs pops back to it (saving the outgoing
 * tab's scroll position) and pushes the new one, so the hardware back button
 * always walks Tab → Home → exit rather than unwinding every tap.
 *
 * Note we anchor on Home rather than the graph's start destination, because
 * the start destination is the splash screen and it has been popped off the
 * stack by the time any of this runs.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(TopLevelDestination.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
