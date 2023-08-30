package be.chvp.nanoledger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
                    Box(modifier = Modifier.padding(contentPadding)) {
                        Button(onClick = {
                            openFile.launch(arrayOf("*/*"))
                        }) {
                            Text(text = "Open file")
                        }
                    }
                }
            }
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
