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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
        val openFile = registerForActivityResult(OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                preferencesViewModel.storeFileUri(uri)
            }
        }
        setContent {
            NanoLedgerTheme {
                Scaffold(topBar = { Bar() }) { contentPadding ->
                    Column(modifier = Modifier.padding(contentPadding)) {
                        val fileUri by preferencesViewModel.fileUri.observeAsState()
                        Setting(
                            stringResource(R.string.file),
                            fileUri?.toString() ?: stringResource(R.string.select_file)
                        ) {
                            openFile.launch(arrayOf("*/*"))
                        }
                        val defaultCurrency by preferencesViewModel.defaultCurrency.observeAsState()
                        var newDefaultCurrency by remember { mutableStateOf(defaultCurrency ?: "") }
                        var defaultCurrencyOpen by remember { mutableStateOf(false) }
                        Setting(
                            stringResource(R.string.default_currency),
                            defaultCurrency ?: "€"
                        ) {
                            defaultCurrencyOpen = true
                        }
                        SettingDialog(
                            defaultCurrencyOpen,
                            stringResource(R.string.change_default_currency),
                            true,
                            { preferencesViewModel.storeDefaultCurrency(newDefaultCurrency) },
                            { defaultCurrencyOpen = false }
                        ) {
                            OutlinedTextField(newDefaultCurrency, { newDefaultCurrency = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Setting(text: String, subtext: String? = null, onClick: (() -> Unit)? = null) {
    var modifier = Modifier.fillMaxWidth()
    if (onClick != null) {
        modifier = modifier.clickable(onClick = onClick)
    }
    Column(modifier = modifier) {
        Text(
            text,
            modifier = Modifier.padding(
                top = 8.dp,
                start = 8.dp,
                bottom = if (subtext != null) 0.dp else 8.dp
            )
        )
        if (subtext != null) {
            Text(
                subtext,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
            )
        }
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
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun SettingDialog(
    opened: Boolean,
    title: String,
    canSave: Boolean,
    save: (() -> Unit),
    dismiss: (() -> Unit),
    content: @Composable () -> Unit
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
                TextButton(onClick = { save(); dismiss() }, enabled = canSave) {
                    Text(stringResource(R.string.save))
                }
            }
        )
    }
}
