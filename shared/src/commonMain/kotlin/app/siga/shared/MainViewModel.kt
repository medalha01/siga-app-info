package app.siga.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: TenantRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkSavedAccess()
    }

    private fun checkSavedAccess() {
        val credentials = repository.getSavedCredentials()
        if (credentials != null) {
            _uiState.value = UiState.WebViewReady(credentials.first, credentials.second)
        }
    }

    fun onAccessClicked(tenant: String) {
		val normalizedTenant = tenant.trim().lowercase()
        if (!repository.isNetworkAvailable()) {
            _uiState.value = UiState.Error("Sem conexão com a internet. Verifique sua rede e tente novamente.")
            return
        }

        if (normalizedTenant.isEmpty()) {
            _uiState.value = UiState.Error("Por favor, digite o nome do ambiente.")
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            val result = repository.resolveTenant(normalizedTenant)
            _uiState.value = when (result) {
                is TenantResult.Success -> {
                    repository.saveCredentials(result.tenant, result.id)
                    UiState.WebViewReady(result.tenant, result.id)
                }
                is TenantResult.NotFound ->
                    UiState.Error("Ambiente inválido ou não encontrado.")
                is TenantResult.ServerError ->
                    UiState.Error("Erro no servidor (código ${result.code}). Tente novamente mais tarde.")
                is TenantResult.Timeout ->
                    UiState.Error("A conexão demorou demais. Verifique sua internet e tente novamente.")
                is TenantResult.NetworkError ->
                    UiState.Error("Erro de conexão. Verifique sua internet.")
            }
        }
    }

    fun onLogout() {
        repository.clearCredentials()
        _uiState.value = UiState.Idle
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }

    fun observeUiState(): FlowWrapper<UiState> = FlowWrapper(uiState)
}
