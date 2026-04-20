package app.siga.shared

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TenantRepository(private val platform: PlatformDependencies) {

    private val settings: Settings = platform.createSettings()

    private val httpClient = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun isNetworkAvailable(): Boolean = platform.isNetworkAvailable()

    suspend fun resolveTenant(tenant: String): TenantResult {
        return try {
            val response = httpClient.get("https://api.sigastr.com.br/cliente/$tenant/id")

            if (!response.status.isSuccess()) {
                return TenantResult.ServerError(response.status.value)
            }

            val body = response.bodyAsText()
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val success = jsonObj["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

            if (!success) return TenantResult.NotFound

            val id = jsonObj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            if (id == -1) TenantResult.NotFound
            else TenantResult.Success(tenant, id)

        } catch (e: Exception) {
            when {
                e.message?.contains("timed out", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true -> TenantResult.Timeout
                else -> TenantResult.NetworkError(e.message ?: "Unknown error")
            }
        }
    }

    fun saveCredentials(tenant: String, id: Int) {
        settings["TENANT"] = tenant
        settings["TENANT_ID"] = id
    }

    fun getSavedCredentials(): Pair<String, Int>? {
        val tenant: String? = settings["TENANT"]
        val id: Int = settings["TENANT_ID", -1]
        return if (tenant != null && id != -1) tenant to id else null
    }

    fun clearCredentials() {
        settings.clear()
    }
}
