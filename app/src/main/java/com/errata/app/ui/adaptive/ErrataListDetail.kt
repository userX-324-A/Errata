package com.errata.app.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PaneActions(
    val open: (String) -> Unit,
    val back: () -> Unit,
    val popToList: () -> Unit,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ErrataListDetail(
    emptyMessage: String,
    onCompactDetailChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    list: @Composable (actions: PaneActions, selectedKey: String?) -> Unit,
    detail: @Composable (key: String, actions: PaneActions) -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>(
        // Two panes from medium so list-detail matches the nav rail (see ErrataAdaptive).
        scaffoldDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
            currentWindowAdaptiveInfo(),
        ),
    )
    val scope = rememberCoroutineScope()
    val actions = remember(navigator, scope) {
        paneActions(navigator, scope)
    }
    val selectedKey = navigator.currentDestination?.contentKey
    val listHidden =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    LaunchedEffect(listHidden) {
        onCompactDetailChanged(listHidden)
    }

    NavigableListDetailPaneScaffold(
        modifier = modifier,
        navigator = navigator,
        listPane = {
            AnimatedPane {
                list(actions, selectedKey)
            }
        },
        detailPane = {
            AnimatedPane {
                if (selectedKey == null) {
                    EmptyPane(emptyMessage)
                } else {
                    detail(selectedKey, actions)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun paneActions(
    navigator: ThreePaneScaffoldNavigator<String>,
    scope: CoroutineScope,
): PaneActions = PaneActions(
    open = { key ->
        scope.launch {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, key)
        }
    },
    back = {
        scope.launch { navigator.navigateBack() }
    },
    popToList = {
        scope.launch {
            PaneSaveNav.popToList(
                canNavigateBack = { navigator.canNavigateBack() },
                navigateBack = { navigator.navigateBack() },
            )
        }
    },
)

@Composable
private fun EmptyPane(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
