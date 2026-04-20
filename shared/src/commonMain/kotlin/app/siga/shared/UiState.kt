package app.siga.shared

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class WebViewReady(val tenant: String, val id: Int) : UiState() {
        val url: String get() = "https://app.sigastr.com.br/app/www/#/login/$tenant/$id"
    }
    data class Error(val message: String) : UiState()
}
