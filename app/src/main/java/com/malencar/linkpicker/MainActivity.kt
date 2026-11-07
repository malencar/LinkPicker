package com.malencar.linkpicker

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs

    // Compose state refreshed every time the screen comes back to the foreground.
    private var isDefault by mutableStateOf(false)
    private var refreshTick by mutableStateOf(0)

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)

        setContent {
            LinkPickerTheme {
                MainScreen(
                    prefs = prefs,
                    isDefault = isDefault,
                    refreshTick = refreshTick,
                    onSetDefault = ::requestDefaultBrowser,
                    onOpenDefaultSettings = ::openDefaultAppsSettings,
                    onOpenUrl = ::openInPicker,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        isDefault = BrowserRepo.isDefaultBrowser(this)
        refreshTick++
    }

    /** Android 10+: system dialog "Set LinkPicker as default browser?". Otherwise: settings page. */
    private fun requestDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !rm.isRoleHeld(RoleManager.ROLE_BROWSER)
            ) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                return
            }
        }
        openDefaultAppsSettings()
    }

    private fun openDefaultAppsSettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** Sends a URL through the real picker flow (same code path as a tapped link). */
    private fun openInPicker(url: String) {
        val uri = Uri.parse(UrlUtils.normalize(url))
        startActivity(Intent(Intent.ACTION_VIEW, uri, this, ChooserActivity::class.java))
    }
}
