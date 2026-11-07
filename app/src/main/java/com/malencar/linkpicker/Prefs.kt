package com.malencar.linkpicker

import android.content.Context

data class HistoryEntry(val url: String, val browser: String)

/** Small SharedPreferences wrapper. Everything stays on the device. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("linkpicker", Context.MODE_PRIVATE)

    /** Strip utm_*, fbclid, gclid... before handing the link to the browser. */
    var cleanTracking: Boolean
        get() = sp.getBoolean(K_CLEAN, true)
        set(v) { sp.edit().putBoolean(K_CLEAN, v).apply() }

    /** Off by default: history of visited links is sensitive. */
    var keepHistory: Boolean
        get() = sp.getBoolean(K_KEEP_HISTORY, false)
        set(v) {
            sp.edit().putBoolean(K_KEEP_HISTORY, v).apply()
            if (!v) clearHistory()
        }

    /** Browsers the user chose not to see in the picker. */
    var hiddenPackages: Set<String>
        get() = HashSet(sp.getStringSet(K_HIDDEN, emptySet()) ?: emptySet())
        set(v) { sp.edit().putStringSet(K_HIDDEN, HashSet(v)).apply() }

    // ---- "Always use X for this site" rules ----

    fun getRule(host: String): String? = sp.getString(RULE + host, null)

    fun setRule(host: String, pkg: String) {
        sp.edit().putString(RULE + host, pkg).apply()
    }

    fun removeRule(host: String) {
        sp.edit().remove(RULE + host).apply()
    }

    fun allRules(): Map<String, String> =
        sp.all.entries
            .filter { it.key.startsWith(RULE) && it.value is String }
            .associate { it.key.removePrefix(RULE) to (it.value as String) }

    // ---- Usage counter: most-used browsers float to the top ----

    fun usage(pkg: String): Int = sp.getInt(USAGE + pkg, 0)

    fun bumpUsage(pkg: String) {
        sp.edit().putInt(USAGE + pkg, usage(pkg) + 1).apply()
    }

    // ---- History ----

    fun history(): List<HistoryEntry> =
        (sp.getString(K_HISTORY, "") ?: "")
            .split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val p = line.split("\t")
                if (p.size >= 2) HistoryEntry(p[0], p[1]) else null
            }

    fun addHistory(url: String, browser: String) {
        val list = history().toMutableList()
        list.add(0, HistoryEntry(url.replace("\n", "").replace("\t", ""), browser))
        saveHistory(list.take(MAX_HISTORY))
    }

    fun clearHistory() {
        sp.edit().remove(K_HISTORY).apply()
    }

    private fun saveHistory(list: List<HistoryEntry>) {
        sp.edit().putString(K_HISTORY, list.joinToString("\n") { "${it.url}\t${it.browser}" }).apply()
    }

    private companion object {
        const val K_CLEAN = "clean_tracking"
        const val K_KEEP_HISTORY = "keep_history"
        const val K_HIDDEN = "hidden_packages"
        const val K_HISTORY = "history"
        const val RULE = "rule:"
        const val USAGE = "usage:"
        const val MAX_HISTORY = 50
    }
}
