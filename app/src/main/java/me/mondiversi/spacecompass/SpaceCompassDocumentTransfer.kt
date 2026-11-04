package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable

/** Keep the transfer owner and Android file pickers alive while its export page is displayed. */
internal class SpaceCompassDocumentTransfer(
    val openExport: () -> Unit,
    val openImport: () -> Unit,
    val canExport: Boolean,
    val canImport: Boolean,
    val exportPage: (@Composable () -> Unit)?
)
