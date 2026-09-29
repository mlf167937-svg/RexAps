package com.rexaps.rextools.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rexaps.rextools.RexToolRegistry
import com.rexaps.rextools.RexToolsScreen
import com.rexaps.rextools.search.PinterestScreen
import com.rexaps.rextools.ssweb.SsWebScreen

@Composable
fun RexNavGraph(
    navController: NavHostController = rememberNavController(),
    onBack: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = "rextools_home"
    ) {
        composable("rextools_home") {
            RexToolsScreen(
                onToolClick = { route -> navController.navigate(route) },
                onBack = onBack
            )
        }

        RexToolRegistry.allTools.forEach { tool ->
            composable(tool.route) {
                when (tool.route) {
                    "pinterest" -> PinterestScreen(
                        onBack = { navController.popBackStack() }
                    )
                    "ssweb" -> SsWebScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
