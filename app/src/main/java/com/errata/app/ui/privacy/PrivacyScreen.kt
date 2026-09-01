package com.errata.app.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.errata.app.R
import com.errata.app.ui.theme.ErrataScreenInsets
import com.errata.app.ui.theme.ErrataTopInsets
import com.errata.app.ui.theme.errataContentWidth
import com.errata.app.ui.theme.errataTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = errataTopAppBarColors(),
                windowInsets = ErrataTopInsets,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ErrataScreenInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .errataContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Body(stringResource(R.string.privacy_intro))
            Heading(stringResource(R.string.privacy_heading_stored))
            Body(stringResource(R.string.privacy_stored))
            Heading(stringResource(R.string.privacy_heading_not))
            Body(stringResource(R.string.privacy_not))
            Heading(stringResource(R.string.privacy_heading_permissions))
            BulletList(stringArrayResource(R.array.privacy_permission_items).toList())
            Heading(stringResource(R.string.privacy_heading_copies))
            Body(stringResource(R.string.privacy_copies))
            Heading(stringResource(R.string.privacy_heading_system))
            Body(stringResource(R.string.privacy_system))
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Text(
                text = "· $item",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
