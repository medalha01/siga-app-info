package app.siga.shared

sealed class TenantResult {
    data class Success(val tenant: String, val id: Int) : TenantResult()
    data object NotFound : TenantResult()
    data class ServerError(val code: Int) : TenantResult()
    data object Timeout : TenantResult()
    data class NetworkError(val cause: String) : TenantResult()
}
