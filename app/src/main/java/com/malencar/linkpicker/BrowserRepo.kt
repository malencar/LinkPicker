package com.malencar.linkpicker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build

data class BrowserInfo(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: Drawable,
)

object BrowserRepo {

    private fun probeIntent() =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)

    @Suppress("DEPRECATION")
    private fun query(pm: PackageManager): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(
                probeIntent(),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            pm.queryIntentActivities(probeIntent(), PackageManager.MATCH_ALL)
        }

    /**
     * Browsers installed on the device, excluding LinkPicker itself.
     * A generic https://www.example.com probe only matches apps that accept ANY web link,
     * i.e. real browsers (apps tied to one domain don't match).
     * Sorted by how often you use them, then alphabetically.
     */
    fun installed(context: Context, prefs: Prefs, includeHidden: Boolean = false): List<BrowserInfo> {
        val pm = context.packageManager
        val hidden = if (includeHidden) emptySet() else prefs.hiddenPackages

        return query(pm)
            .filter { it.activityInfo != null && it.activityInfo.exported }
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName !in hidden }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                BrowserInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = pkg,
                    component = ComponentName(pkg, ri.activityInfo.name),
                    icon = ri.loadIcon(pm),
                )
            }
            .sortedWith(
                compareByDescending<BrowserInfo> { prefs.usage(it.packageName) }
                    .thenBy { it.label.lowercase() }
            )
    }

    private val whatsappHosts = setOf(
        "wa.me", "api.whatsapp.com", "chat.whatsapp.com", "web.whatsapp.com", "whatsapp.com",
    )
    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    /**
     * For WhatsApp links: every installed WhatsApp flavour (normal and Business) that can open
     * [uri]. Empty for any other link.
     */
    fun whatsappApps(context: Context, uri: Uri): List<BrowserInfo> {
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return emptyList()
        if (host !in whatsappHosts) return emptyList()
        val pm = context.packageManager
        return whatsappPackages.mapNotNull { pkg ->
            val probe = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
            @Suppress("DEPRECATION")
            val matches: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(probe, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                pm.queryIntentActivities(probe, 0)
            }
            val ri = matches.firstOrNull { it.activityInfo?.exported == true }
                ?: return@mapNotNull null
            BrowserInfo(
                label = ri.loadLabel(pm).toString(),
                packageName = pkg,
                component = ComponentName(pkg, ri.activityInfo.name),
                icon = ri.loadIcon(pm),
            )
        }
    }

    /** Opens [uri] directly in the chosen browser (bypassing the system default). */
    fun open(context: Context, uri: Uri, browser: BrowserInfo) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setComponent(browser.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** True when LinkPicker currently handles web links by default. */
    fun isDefaultBrowser(context: Context): Boolean {
        val ri = context.packageManager.resolveActivity(probeIntent(), PackageManager.MATCH_DEFAULT_ONLY)
        return ri?.activityInfo?.packageName == context.packageName
    }
}
