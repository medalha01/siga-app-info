package app.siga.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class AndroidPlatform(private val context: Context) : PlatformDependencies {

    override fun createSettings(): Settings {
        val prefs = context.getSharedPreferences("SigaAppPrefs", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(prefs)
    }

    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
