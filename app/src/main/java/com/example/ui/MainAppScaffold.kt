package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LiveCompanionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StoryCreationScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondaryLight

enum class AppNavDestination(
    val title: String,
    val testTag: String
) {
    CREATE("إنشاء قصة", "nav_create"),
    HOME("الاستوديو", "nav_home"),
    EDITOR("المحرر", "nav_editor"),
    LIBRARY("المكتبة", "nav_library"),
    LIVE("مساعد راوي", "nav_live"),
    PROFILE("الحساب", "nav_profile")
}

@Composable
fun MainAppScaffold(
    viewModel: StoryViewModel,
    onSignOut: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.HOME) }

    // Enforce back handler to return to HOME when in sub-screens
    if (currentDestination != AppNavDestination.HOME) {
        BackHandler {
            currentDestination = AppNavDestination.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                AppNavDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            val icon = when (destination) {
                                AppNavDestination.CREATE -> if (isSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
                                AppNavDestination.HOME -> if (isSelected) Icons.Filled.Movie else Icons.Outlined.Movie
                                AppNavDestination.EDITOR -> if (isSelected) Icons.Filled.Movie else Icons.Outlined.Movie
                                AppNavDestination.LIBRARY -> if (isSelected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
                                AppNavDestination.LIVE -> if (isSelected) Icons.Filled.GraphicEq else Icons.Outlined.GraphicEq
                                AppNavDestination.PROFILE -> if (isSelected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
                            }
                            Icon(icon, contentDescription = destination.title)
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = StudioSecondaryLight,
                            indicatorColor = StudioPrimary,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppNavDestination.CREATE -> {
                    StoryCreationScreen(
                        viewModel = viewModel,
                        onNavigateToEditor = {
                            currentDestination = AppNavDestination.EDITOR
                        }
                    )
                }
                AppNavDestination.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToEditor = {
                            currentDestination = AppNavDestination.EDITOR
                        }
                    )
                }
                AppNavDestination.EDITOR -> {
                    EditorScreen(
                        viewModel = viewModel,
                        onBackToHome = {
                            currentDestination = AppNavDestination.HOME
                        }
                    )
                }
                AppNavDestination.LIBRARY -> {
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenStory = {
                            currentDestination = AppNavDestination.EDITOR
                        }
                    )
                }
                AppNavDestination.LIVE -> {
                    LiveCompanionScreen(
                        viewModel = viewModel
                    )
                }
                AppNavDestination.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
