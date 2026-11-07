package com.malencar.linkpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    prefs: Prefs,
    isDefault: Boolean,
    refreshTick: Int,
    onSetDefault: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val ctx = LocalContext.current

    // Reloaded whenever the activity resumes (refreshTick changes).
    val allBrowsers = remember(refreshTick) { BrowserRepo.installed(ctx, prefs, includeHidden = true) }
    val hidden = remember(refreshTick) { mutableStateListOf<String>().apply { addAll(prefs.hiddenPackages) } }
    val rules = remember(refreshTick) {
        mutableStateListOf<Pair<String, String>>().apply {
            addAll(prefs.allRules().toList().sortedBy { it.first })
        }
    }
    val history = remember(refreshTick) { mutableStateListOf<HistoryEntry>().apply { addAll(prefs.history()) } }

    var clean by remember(refreshTick) { mutableStateOf(prefs.cleanTracking) }
    var keepHistory by remember(refreshTick) { mutableStateOf(prefs.keepHistory) }
    var testUrl by remember { mutableStateOf("https://example.com") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ---- Default browser status ----
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDefault) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(if (isDefault) R.string.default_on_title else R.string.default_off_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(if (isDefault) R.string.default_on_desc else R.string.default_off_desc),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!isDefault) {
                            Button(onClick = onSetDefault, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.set_default))
                            }
                        }
                        OutlinedButton(onClick = onOpenDefaultSettings, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.open_default_settings))
                        }
                    }
                }
            }

            // ---- Options ----
            item { SectionTitle(stringResource(R.string.section_options)) }
            item {
                SwitchRow(
                    title = stringResource(R.string.opt_clean),
                    desc = stringResource(R.string.opt_clean_desc),
                    checked = clean,
                ) {
                    clean = it
                    prefs.cleanTracking = it
                }
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.opt_history),
                    desc = stringResource(R.string.opt_history_desc),
                    checked = keepHistory,
                ) {
                    keepHistory = it
                    prefs.keepHistory = it
                    if (!it) history.clear()
                }
            }

            // ---- Try it ----
            item { SectionTitle(stringResource(R.string.section_test)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = testUrl,
                        onValueChange = { testUrl = it },
                        label = { Text(stringResource(R.string.test_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { if (testUrl.isNotBlank()) onOpenUrl(testUrl) }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (testUrl.isNotBlank()) onOpenUrl(testUrl) }) {
                        Text(stringResource(R.string.test_button))
                    }
                }
            }

            // ---- Browsers shown in the picker ----
            item { SectionTitle(stringResource(R.string.section_browsers)) }
            items(allBrowsers, key = { it.packageName }) { browser ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { toggleHidden(prefs, hidden, browser.packageName) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrowserIcon(browser, size = 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(browser.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = browser.packageName !in hidden,
                        onCheckedChange = { toggleHidden(prefs, hidden, browser.packageName) },
                    )
                }
            }

            // ---- Remembered sites ----
            item { SectionTitle(stringResource(R.string.section_rules)) }
            if (rules.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.rules_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(rules, key = { it.first }) { (host, pkg) ->
                    val label = allBrowsers.firstOrNull { it.packageName == pkg }?.label ?: pkg
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(host, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = {
                            prefs.removeRule(host)
                            rules.removeAll { it.first == host }
                        }) { Text(stringResource(R.string.remove)) }
                    }
                }
            }

            // ---- History (only if enabled) ----
            if (keepHistory) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.section_history)) }
                        if (history.isNotEmpty()) {
                            TextButton(onClick = {
                                prefs.clearHistory()
                                history.clear()
                            }) { Text(stringResource(R.string.clear)) }
                        }
                    }
                }
                if (history.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(history) { entry ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenUrl(entry.url) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(entry.url, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(R.string.history_opened_with, entry.browser),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun toggleHidden(prefs: Prefs, hidden: MutableList<String>, pkg: String) {
    if (pkg in hidden) hidden.remove(pkg) else hidden.add(pkg)
    prefs.hiddenPackages = hidden.toSet()
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
