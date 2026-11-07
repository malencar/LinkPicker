package com.malencar.linkpicker

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Entry point for every http/https link. Either opens the link straight away
 * (remembered site, or only one browser available) or shows the picker sheet.
 */
class ChooserActivity : ComponentActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)

        val raw = intent?.data
        val scheme = raw?.scheme?.lowercase()
        if (raw == null || (scheme != "http" && scheme != "https")) {
            finish()
            return
        }

        val uri = if (prefs.cleanTracking) UrlUtils.clean(raw) else raw
        val host = UrlUtils.host(uri)

        // 1) A remembered browser for this site?
        if (host != null) {
            prefs.getRule(host)?.let { pkg ->
                val target = BrowserRepo.installed(this, prefs, includeHidden = true)
                    .firstOrNull { it.packageName == pkg }
                if (target != null) {
                    launch(uri, target)
                    return
                }
                prefs.removeRule(host) // browser was uninstalled
            }
        }

        // 2) Which browsers can we offer?
        val browsers = BrowserRepo.installed(this, prefs)
        when (browsers.size) {
            0 -> {
                Toast.makeText(this, R.string.no_browsers, Toast.LENGTH_LONG).show()
                finish()
                return
            }
            1 -> { // nothing to choose from
                launch(uri, browsers.first())
                return
            }
        }

        // 3) Ask.
        setContent {
            LinkPickerTheme {
                ChooserSheet(
                    uri = uri,
                    host = host,
                    browsers = browsers,
                    onPick = { browser, remember ->
                        if (remember && host != null) prefs.setRule(host, browser.packageName)
                        launch(uri, browser)
                    },
                    onCopy = { copy(uri) },
                    onShare = { share(uri) },
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun launch(uri: Uri, browser: BrowserInfo) {
        try {
            BrowserRepo.open(this, uri, browser)
            prefs.bumpUsage(browser.packageName)
            if (prefs.keepHistory) prefs.addHistory(uri.toString(), browser.label)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun copy(uri: Uri) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("url", uri.toString()))
        // Android 13+ shows its own confirmation.
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun share(uri: Uri) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, uri.toString())
        startActivity(Intent.createChooser(send, null))
        finish()
    }
}

@Composable
private fun ChooserSheet(
    uri: Uri,
    host: String?,
    browsers: List<BrowserInfo>,
    onPick: (BrowserInfo, Boolean) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    var always by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // Scrim: tap outside the sheet to cancel.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.navigationBarsPadding().padding(top = 20.dp, bottom = 8.dp)) {
                Text(
                    stringResource(R.string.chooser_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                // Show the real host prominently so look-alike domains are easy to spot.
                Text(
                    host ?: (uri.authority ?: ""),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp),
                )
                Text(
                    uri.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 8.dp),
                )

                Column(
                    Modifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    browsers.forEach { browser ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(browser, always && host != null) }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BrowserIcon(browser)
                            Spacer(Modifier.width(16.dp))
                            Text(browser.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                if (host != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { always = !always }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.chooser_always_for, host),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = always, onCheckedChange = { always = it })
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCopy) { Text(stringResource(R.string.copy_link)) }
                    TextButton(onClick = onShare) { Text(stringResource(R.string.share_link)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}
