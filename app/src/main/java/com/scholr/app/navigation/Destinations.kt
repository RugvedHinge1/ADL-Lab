package com.scholr.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Routes that sit outside the bottom-bar shell. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val INTERESTS = "interests"
    const val PROFILE = "profile"
    const val PAPER_DETAIL = "paper/{paperId}"

    fun paperDetail(paperId: String) = "paper/$paperId"
}

/**
 * The four bottom-bar destinations. Home is the anchor and is the only one
 * that turns green when active — everything else uses purple clay.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isAnchor: Boolean = false
) {
    Home("home", "Home", Icons.Rounded.Home, isAnchor = true),
    Search("search", "Search", Icons.Rounded.Search),
    Conferences("conferences", "Conferences", Icons.Rounded.CalendarMonth),
    Library("library", "Library", Icons.Rounded.LocalLibrary);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
        fun fromRoute(route: String?): TopLevelDestination? =
            entries.firstOrNull { it.route == route }
    }
}
