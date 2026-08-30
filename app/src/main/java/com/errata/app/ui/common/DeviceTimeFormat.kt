package com.errata.app.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun isDevice24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)
