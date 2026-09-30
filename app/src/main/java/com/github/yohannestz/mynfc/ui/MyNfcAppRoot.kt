package com.github.yohannestz.mynfc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.github.yohannestz.mynfc.AppContainer
import com.github.yohannestz.mynfc.data.model.RecordType
import com.github.yohannestz.mynfc.nfc.RawSessionState
import com.github.yohannestz.mynfc.nfc.ScanMode
import com.github.yohannestz.mynfc.nfc.SessionState
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.LocalSnackbar
import com.github.yohannestz.mynfc.ui.detail.TagDetailScreen
import com.github.yohannestz.mynfc.ui.home.HomeScreen
import com.github.yohannestz.mynfc.ui.rfid.RfidDumpScreen
import com.github.yohannestz.mynfc.ui.rfid.RfidScanScreen
import com.github.yohannestz.mynfc.ui.rfid.RfidScreen
import com.github.yohannestz.mynfc.ui.saved.SavedScreen
import com.github.yohannestz.mynfc.ui.scan.ScanScreen
import com.github.yohannestz.mynfc.ui.session.NfcSessionSheet
import com.github.yohannestz.mynfc.ui.session.RawSessionSheet
import com.github.yohannestz.mynfc.ui.tools.ToolsScreen
import com.github.yohannestz.mynfc.ui.write.RecordEditorScreen
import com.github.yohannestz.mynfc.ui.write.WriteScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object HomeRoute
@Serializable data object WriteRoute
@Serializable data object SavedRoute
@Serializable data object ToolsRoute
@Serializable data object ScanRoute
@Serializable data object RfidRoute
@Serializable data object RfidScanRoute
@Serializable data class RfidDumpRoute(val id: String)
@Serializable data class DetailRoute(val id: String)
@Serializable data class EditorRoute(val type: String, val recordId: String? = null)

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, HomeRoute::class, "NFC", Icons.Rounded.Contactless),
    Tab(WriteRoute, WriteRoute::class, "Write", Icons.Rounded.EditNote),
    Tab(RfidRoute, RfidRoute::class, "RFID", Icons.Rounded.Memory),
    Tab(SavedRoute, SavedRoute::class, "Saved", Icons.Rounded.Bookmarks),
    Tab(ToolsRoute, ToolsRoute::class, "Advanced", Icons.Rounded.Handyman),
)

@Composable
fun MyNfcAppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showTabs = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }
    val controller = container.nfcController
    val session by controller.session.collectAsStateWithLifecycle()
    val rawSession by controller.rawSession.collectAsStateWithLifecycle()

    // Idle taps decode NDEF everywhere except the RFID area, where they dump raw memory.
    val inRfid = destination?.hierarchy?.any {
        it.hasRoute(RfidRoute::class) || it.hasRoute(RfidScanRoute::class) || it.hasRoute(RfidDumpRoute::class)
    } == true
    LaunchedEffect(inRfid) {
        controller.scanMode = if (inRfid) ScanMode.RAW else ScanMode.NDEF
    }

    // Any tag read while no write operation is pending opens its details.
    LaunchedEffect(controller) {
        controller.scannedTags.collect { tag ->
            navController.navigate(DetailRoute(tag.id)) {
                popUpTo<ScanRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(controller) {
        controller.scannedDumps.collect { dump ->
            navController.navigate(RfidDumpRoute(dump.id)) {
                popUpTo<RfidScanRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(controller) {
        controller.readErrors.collect { snackbar.showSnackbar(it) }
    }

    CompositionLocalProvider(LocalAppContainer provides container, LocalSnackbar provides snackbar) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showTabs,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        tabs.forEach { tab ->
                            val selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, null) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showTabs) padding.calculateBottomPadding() else 0.dp),
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 12 } },
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onRead = { navController.navigate(ScanRoute) },
                        onWrite = { navController.navigate(WriteRoute) { launchSingleTop = true } },
                        onOpenTag = { navController.navigate(DetailRoute(it)) },
                        onSeeSaved = { navController.navigate(SavedRoute) { launchSingleTop = true } },
                    )
                }
                composable<ScanRoute> { ScanScreen(onClose = { navController.popBackStack() }) }
                composable<DetailRoute> { entry ->
                    TagDetailScreen(id = entry.toRoute<DetailRoute>().id, onClose = { navController.popBackStack() })
                }
                composable<WriteRoute> {
                    WriteScreen(
                        onCreate = { type -> navController.navigate(EditorRoute(type.name)) },
                        onEdit = { record -> navController.navigate(EditorRoute(record.type.name, record.id)) },
                    )
                }
                composable<EditorRoute> { entry ->
                    val route = entry.toRoute<EditorRoute>()
                    RecordEditorScreen(
                        type = RecordType.valueOf(route.type),
                        recordId = route.recordId,
                        onClose = { navController.popBackStack() },
                    )
                }
                composable<SavedRoute> { SavedScreen(onOpen = { navController.navigate(DetailRoute(it)) }) }
                composable<ToolsRoute> { ToolsScreen() }
                composable<RfidRoute> {
                    RfidScreen(
                        onScan = { navController.navigate(RfidScanRoute) },
                        onOpen = { navController.navigate(RfidDumpRoute(it)) },
                    )
                }
                composable<RfidScanRoute> { RfidScanScreen(onClose = { navController.popBackStack() }) }
                composable<RfidDumpRoute> { entry ->
                    RfidDumpScreen(id = entry.toRoute<RfidDumpRoute>().id, onClose = { navController.popBackStack() })
                }
            }
        }

        if (session != SessionState.Idle) {
            NfcSessionSheet(
                state = session,
                onDismiss = controller::dismiss,
                onRetry = { controller.start(it) },
            )
        }
        if (rawSession != RawSessionState.Idle) {
            RawSessionSheet(
                state = rawSession,
                onDismiss = controller::dismissRaw,
                onRetry = { controller.startRaw(it) },
            )
        }
    }
}
