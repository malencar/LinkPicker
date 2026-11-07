package com.malencar.linkpicker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Share target: "Share > LinkPicker" cleans tracking parameters from the shared text
 * and lets you copy the clean text or share it onward. Always cleans, regardless of
 * the "remove tracking parameters" option, because the user asked for it explicitly.
 */
class ShareCleanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val original = intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        if (intent?.action != Intent.ACTION_SEND || original.isNullOrBlank()) {
            finish()
            return
        }
        val cleaned = UrlUtils.cleanText(original)

        setContent {
            LinkPickerTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { finish() },
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
                                stringResource(
                                    if (cleaned != original) R.string.share_cleaned_title
                                    else R.string.share_nothing_to_clean
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Text(
                                cleaned,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { copy(cleaned) }) { Text(stringResource(R.string.copy_link)) }
                                TextButton(onClick = { share(cleaned) }) { Text(stringResource(R.string.share_link)) }
                                TextButton(onClick = { finish() }) { Text(stringResource(R.string.cancel)) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copy(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("url", text))
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        // Don't offer ourselves again in the share sheet.
        val chooser = Intent.createChooser(send, null).putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(this, ShareCleanActivity::class.java))
        )
        startActivity(chooser)
        finish()
    }
}
