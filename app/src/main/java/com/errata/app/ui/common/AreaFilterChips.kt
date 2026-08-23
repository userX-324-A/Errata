package com.errata.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.errata.app.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaFilterChips(
    usedAreas: List<String>,
    activeArea: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (usedAreas.isEmpty()) return
    Column(modifier = modifier.padding(bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.area_filter_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = activeArea == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.area_filter_all)) },
            )
            usedAreas.forEach { area ->
                FilterChip(
                    selected = activeArea == area,
                    onClick = { onSelect(area) },
                    label = { Text(area) },
                )
            }
        }
    }
}
