package com.securefolder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.securefolder.features.auth.LockScreen
import com.securefolder.features.auth.PinSetupScreen
import com.securefolder.features.browser.SecureBrowserScreen
import com.securefolder.features.dashboard.DashboardScreen
import com.securefolder.features.notes.NotesScreen
import com.securefolder.features.notes.NoteEditorScreen
import com.securefolder.features.settings.SettingsScreen
import com.securefolder.features.vault.VaultScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val PIN_SETUP = "pin_setup"
    const val LOCK_SCREEN = "lock_screen"
    const val DASHBOARD = "dashboard"
    const val VAULT = "vault"
    const val NOTES = "notes"
    const val NOTE_EDITOR = "note_editor/{noteId}"
    const val BROWSER = "browser"
    const val SETTINGS = "settings"

    fun noteEditor(noteId: Long = -1L) = "note_editor/$noteId"
}

/**
 * Main navigation graph.
 */
@Composable
fun SecureFolderNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.PIN_SETUP) {
            PinSetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.PIN_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOCK_SCREEN) {
            LockScreen(
                onUnlocked = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOCK_SCREEN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToVault = { navController.navigate(Routes.VAULT) },
                onNavigateToNotes = { navController.navigate(Routes.NOTES) },
                onNavigateToBrowser = { navController.navigate(Routes.BROWSER) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.NOTES) {
            NotesScreen(
                onBack = { navController.popBackStack() },
                onEditNote = { noteId ->
                    navController.navigate(Routes.noteEditor(noteId))
                },
                onNewNote = {
                    navController.navigate(Routes.noteEditor(-1L))
                }
            )
        }

        composable(Routes.NOTE_EDITOR) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: -1L
            NoteEditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BROWSER) {
            SecureBrowserScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
