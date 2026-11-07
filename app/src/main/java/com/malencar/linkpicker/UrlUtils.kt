package com.malencar.linkpicker

import android.net.Uri

object UrlUtils {
    private val trackingKeys = setOf(
        "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
        "igshid", "igsh", "mc_eid", "mc_cid", "_hsenc", "_hsmi", "mkt_tok", "ref_src",
        "ref_url", "twclid", "ttclid", "li_fat_id", "epik", "vero_id", "oly_enc_id",
        "oly_anon_id", "_ga", "_gl", "s_cid", "spm"
    )

    /** Sites whose query string is only tracking: the whole "?..." is dropped. */
    private val stripAllQuery = setOf(
        "instagram.com", "threads.net", "threads.com", "tiktok.com", "pinterest.com",
        "pin.it", "linkedin.com", "twitter.com", "x.com", "t.co", "bsky.app",
        "tumblr.com", "snapchat.com", "twitch.tv", "spotify.com", "open.spotify.com"
    )

    /** Sites where only the listed parameters are meaningful; everything else is removed. */
    private val keepOnly = mapOf(
        "youtube.com" to setOf("v", "list", "t", "start", "end", "index", "q", "search_query", "hl"),
        "youtu.be" to setOf("t", "list", "start"),
        "reddit.com" to setOf("q", "t", "sort", "context"),
        "redd.it" to emptySet()
    )

    /** Site-specific tracking parameters (blocklist) for sites with meaningful query params. */
    private val siteTrackingKeys = mapOf(
        "facebook.com" to setOf(
            "mibextid", "__cft__[0]", "__tn__", "sfnsn", "rdid", "share_url", "ref",
            "refsrc", "_rdr", "paipv", "eav", "hc_ref", "fref", "notif_id", "notif_t"
        ),
        "fb.com" to setOf("mibextid", "ref", "sfnsn"),
        "fb.watch" to setOf("mibextid"),
        "amazon.com" to setOf("tag", "ref", "ref_", "linkcode", "linkid", "pd_rd_r", "pd_rd_w", "pd_rd_wg", "pf_rd_p", "pf_rd_r", "crid", "sprefix", "qid", "sr")
    )

    private fun hostMatches(host: String, domain: String) =
        host == domain || host.endsWith(".$domain")

    private fun <T> Map<String, T>.forHost(host: String): T? =
        entries.firstOrNull { hostMatches(host, it.key) }?.value

    /** Removes well-known tracking query parameters. Returns the same Uri if nothing changed. */
    fun clean(uri: Uri): Uri {
        if (!uri.isHierarchical) return uri
        val names = uri.queryParameterNames
        if (names.isEmpty()) return uri

        val host = uri.host?.lowercase().orEmpty()
        if (stripAllQuery.any { hostMatches(host, it) }) {
            return uri.buildUpon().clearQuery().build()
        }
        val keep = keepOnly.forHost(host)
        val siteBlock = siteTrackingKeys.forHost(host)

        var changed = false
        val builder = uri.buildUpon().clearQuery()
        for (name in names) {
            val lower = name.lowercase()
            if (lower.startsWith("utm_") || lower in trackingKeys ||
                (keep != null && lower !in keep) ||
                (siteBlock != null && lower in siteBlock)
            ) {
                changed = true
                continue
            }
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        return if (changed) builder.build() else uri
    }

    private val urlRegex = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)

    /** Cleans every http(s) URL inside free text, leaving the rest untouched. */
    fun cleanText(text: String): String = urlRegex.replace(text) { m ->
        // Trailing punctuation usually belongs to the sentence, not the URL.
        val url = m.value.trimEnd('.', ',', ')', ']', '!', '?', ';', ':')
        val tail = m.value.substring(url.length)
        clean(Uri.parse(url)).toString() + tail
    }

    /** Host without a leading "www.", used as the key for "remember this site". */
    fun host(uri: Uri): String? = uri.host?.lowercase()?.removePrefix("www.")

    /** Accepts "example.com" as well as full URLs. */
    fun normalize(input: String): String {
        val t = input.trim()
        return if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
    }
}
