package com.errata.app.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TaskAreaLabel(
    area: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val label = area?.trim().orEmpty()
    if (label.isEmpty()) return
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier.padding(bottom = 2.dp),
    )
}
