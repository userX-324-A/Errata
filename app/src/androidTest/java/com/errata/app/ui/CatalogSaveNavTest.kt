package com.errata.app.ui

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogSaveNavTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveFromCatalog_popsToPending() {
        lateinit var nav: TestNavHostController
        composeRule.setContent {
            nav = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(navController = nav, startDestination = "pending") {
                composable("pending") { Text("pending") }
                composable(Routes.STARTERS) { Text("starters") }
                composable("task") { Text("task") }
            }
        }
        composeRule.runOnIdle {
            nav.navigate(Routes.STARTERS)
            nav.navigate("task")
            EditorSaveNav.popAfterSave(nav)
        }
        composeRule.runOnIdle {
            assertEquals("pending", nav.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun saveFromEdit_popsOnceToPending() {
        lateinit var nav: TestNavHostController
        composeRule.setContent {
            nav = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(navController = nav, startDestination = "pending") {
                composable("pending") { Text("pending") }
                composable(Routes.STARTERS) { Text("starters") }
                composable("task") { Text("task") }
            }
        }
        composeRule.runOnIdle {
            nav.navigate("task")
            EditorSaveNav.popAfterSave(nav)
        }
        composeRule.runOnIdle {
            assertEquals("pending", nav.currentBackStackEntry?.destination?.route)
        }
    }
}
