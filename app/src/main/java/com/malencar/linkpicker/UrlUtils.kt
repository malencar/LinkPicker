package com.malencar.linkpicker

import android.net.Uri

object UrlUtils {
    private val trackingKeys = setOf(
        "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
        "igshid", "mc_eid", "mc_cid", "_hsenc", "_hsmi", "mkt_tok", "ref_src"
    )

    /** Removes well-known tracking query parameters. Returns the same Uri if nothing changed. */
    fun clean(uri: Uri): Uri {
        if (!uri.isHierarchical) return uri
        val names = uri.queryParameterNames
        if (names.isEmpty()) return uri

        var changed = false
        val builder = uri.buildUpon().clearQuery()
        for (name in names) {
            val lower = name.lowercase()
            if (lower.startsWith("utm_") || lower in trackingKeys) {
                changed = true
                continue
            }
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        return if (changed) builder.build() else uri
    }

    /** Host without a leading "www.", used as the key for "remember this site". */
    fun host(uri: Uri): String? = uri.host?.lowercase()?.removePrefix("www.")

    /** Accepts "example.com" as well as full URLs. */
    fun normalize(input: String): String {
        val t = input.trim()
        return if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
    }
}
