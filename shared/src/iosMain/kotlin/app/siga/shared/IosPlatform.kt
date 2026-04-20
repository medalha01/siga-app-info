package app.siga.shared

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

class IosPlatform : PlatformDependencies {

    override fun createSettings(): Settings {
        return NSUserDefaultsSettings(NSUserDefaults(suiteName = "SigaAppPrefs"))
    }

    override fun isNetworkAvailable(): Boolean {
        // On iOS, rely on Ktor error handling for network failures.
        return true
    }
}
