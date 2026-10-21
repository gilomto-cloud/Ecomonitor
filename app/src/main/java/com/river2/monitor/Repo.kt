package com.river2.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject

data class UiState(
    val status: String = "Not connected",
    val connected: Boolean = false,
    val raw: Map<String, Any?> = emptyMap(),
    val summary: Summary? = null,
    val energy: Energy = Energy(),
    val history: List<Sample> = emptyList(),
    val cmdResult: String = "",
    val debug: List<String> = emptyList(),
)

/** Shared state between the background service and the UI. */
object Repo {
    val state = MutableStateFlow(UiState())
    var sender: ((JSONObject) -> Unit)? = null

    fun reset() {
        sender = null
        state.value = UiState()
    }
}
