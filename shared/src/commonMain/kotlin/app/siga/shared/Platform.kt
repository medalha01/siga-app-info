package app.siga.shared

import com.russhwolf.settings.Settings

interface PlatformDependencies {
    fun createSettings(): Settings
    fun isNetworkAvailable(): Boolean
}
