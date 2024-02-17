package be.chvp.nanoledger.ui.preferences

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferencesActivity() : ComponentActivity() {
    private val preferencesViewModel: PreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openFile =
            registerForActivityResult(OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    preferencesViewModel.storeFileUri(uri)
                }
            }
        setContent {
            val statusMap =
                mapOf(
                    " " to stringResource(R.string.status_unmarked),
                    "!" to stringResource(R.string.status_pending),
                    "*" to stringResource(R.string.status_cleared),
                )
            val currencyOrderMap =
                mapOf(
                    true to stringResource(R.string.currency_order_before),
                    false to stringResource(R.string.currency_order_after),
                )
            NanoLedgerTheme {
                Scaffold(topBar = { Bar() }) { contentPadding ->
                    Column(
                        modifier =
                            Modifier
                                .padding(contentPadding)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        val fileUri by preferencesViewModel.fileUri.observeAsState()
                        Setting(
                            stringResource(R.string.file),
                            fileUri?.toString() ?: stringResource(R.string.select_file),
                        ) {
                            openFile.launch(arrayOf("*/*"))
                        }
                        HorizontalDivider()
                        val defaultCurrency by preferencesViewModel.defaultCurrency.observeAsState()
                        var newDefaultCurrency by remember { mutableStateOf(defaultCurrency ?: "") }
                        var defaultCurrencyOpen by remember { mutableStateOf(false) }
                        Setting(
                            stringResource(R.string.default_currency),
                            defaultCurrency ?: "€",
                        ) {
                            defaultCurrencyOpen = true
                        }
                        SettingDialog(
                            defaultCurrencyOpen,
                            stringResource(R.string.change_default_currency),
                            true,
                            { preferencesViewModel.storeDefaultCurrency(newDefaultCurrency) },
                            { defaultCurrencyOpen = false },
                        ) {
                            OutlinedTextField(newDefaultCurrency, { newDefaultCurrency = it })
                        }
                        HorizontalDivider()
                        val defaultStatus by preferencesViewModel.defaultStatus.observeAsState()
                        var expandedStatus by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedStatus,
                            onExpandedChange = { expandedStatus = !expandedStatus },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Setting(
                                stringResource(R.string.default_status),
                                statusMap[defaultStatus ?: " "] ?: stringResource(
                                    R.string.status_unmarked,
                                ),
                                modifier = Modifier.menuAnchor(),
                            ) { expandedStatus = true }
                            ExposedDropdownMenu(
                                expanded = expandedStatus,
                                onDismissRequest = { expandedStatus = false },
                                modifier = Modifier.exposedDropdownSize(true),
                            ) {
                                statusMap.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.value) },
                                        onClick = {
                                            preferencesViewModel.storeDefaultStatus(it.key)
                                            expandedStatus = false
                                        },
                                        contentPadding =
                                            ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        val currencyBeforeAmount by
                            preferencesViewModel.currencyBeforeAmount.observeAsState()
                        var expandedCurrency by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedCurrency,
                            onExpandedChange = { expandedCurrency = !expandedCurrency },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Setting(
                                stringResource(R.string.currency_amount_order),
                                currencyOrderMap[currencyBeforeAmount ?: true] ?: stringResource(
                                    R.string.currency_order_before,
                                ),
                                modifier = Modifier.menuAnchor(),
                            ) { expandedCurrency = true }
                            ExposedDropdownMenu(
                                expanded = expandedCurrency,
                                onDismissRequest = { expandedCurrency = false },
                                modifier = Modifier.exposedDropdownSize(true),
                            ) {
                                currencyOrderMap.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.value) },
                                        onClick = {
                                            preferencesViewModel.storeCurrencyBeforeAmount(it.key)
                                            expandedCurrency = false
                                        },
                                        contentPadding =
                                            ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Setting(
    text: String,
    subtext: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var localModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        localModifier = localModifier.clickable(onClick = onClick)
    }
    Column(modifier = localModifier) {
        Text(
            text,
            modifier =
                Modifier.padding(
                    top = 8.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 0.dp,
                ),
        )
        Text(
            subtext,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
        )
    }
}

@Composable
fun Bar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(onClick = { (context as Activity).finish() }) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Composable
fun SettingDialog(
    opened: Boolean,
    title: String,
    canSave: Boolean,
    save: (() -> Unit),
    dismiss: (() -> Unit),
    content: @Composable () -> Unit,
) {
    if (opened) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            text = content,
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    save()
                    dismiss()
                }, enabled = canSave) {
                    Text(stringResource(R.string.save))
                }
            },
        )
    }
}
