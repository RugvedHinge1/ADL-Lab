package com.scholr.app.data.scan

import com.scholr.app.data.model.Paper

/** What the scanner screen is currently showing in its result sheet. */
sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Loading(val label: String) : ScanUiState
    data class PaperFound(val paper: Paper) : ScanUiState
    data class CollectionFound(
        val name: String,
        val emoji: String,
        /** Ids from the QR code (all of them, even ones that failed to resolve). */
        val paperIds: List<String>,
        /** Resolved papers; some ids may be missing if the lookup failed. */
        val papers: List<Paper>
    ) : ScanUiState
    data class BookFound(val book: Book, val related: List<Paper>) : ScanUiState
    data class Failed(val message: String) : ScanUiState
}
