package be.chvp.nanoledger.ui.stats

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class StatsActivity() : ComponentActivity() {

    private val preferencesViewModel: StatsViewModel by viewModels()


    private fun extractBinary(context: Context) {
        // Path to the destination binary
        val binaryFile = File(context.filesDir, "ledger")

        // Copy the binary from assets to the app's files directory
        context.assets.open("ledger").use { input ->
            binaryFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Set executable permissions
        try {
            Runtime.getRuntime().exec("chmod 700 ${binaryFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runLedgerCommand(context: Context, command: String): String {
        val binaryFile = File(context.filesDir, "ledger")
        if (binaryFile.canExecute()) {
            println("Binary is executable")
        } else {
            println("Binary is NOT executable")
        }
        // Build the process
        return try {
            val process = Runtime.getRuntime().exec("ledger")
            val out = process.inputStream.bufferedReader().readText()
            process.waitFor()
            println(out)
           return out
        } catch (e: Exception) {
            println(e.message)
            e.message ?: "Error executing command"
        }
    }



    private fun executeTermuxCommand() {
        val command = "termux-battery-status" // Example Termux API command
        val intent = Intent("com.termux.service_execute").apply {
            putExtra("com.termux.command", command)
        }

        try {
            startActivityForResult(intent, REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Termux API not installed or accessible", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringExtra("com.termux.result")
                Toast.makeText(this, "Result: $result", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to execute Termux command", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractBinary(applicationContext)
        executeTermuxCommand()

        setContent {
            NanoLedgerTheme {
                Scaffold(topBar = { Bar() }) { contentPadding ->
                    Column(
                        modifier =
                        Modifier
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        val ledgerCommand by preferencesViewModel.ledgerCommand.observeAsState()
                        var newLedgerCommand by remember { mutableStateOf(ledgerCommand ?: "") }
                        var defaultLedgerCommandOpen by remember { mutableStateOf(false) }
                        Setting(
                            stringResource(R.string.ledger_command),
                            ledgerCommand ?: "-V balance",
                        ) {
                            defaultLedgerCommandOpen = true
                        }

                        var output = ""
                        SettingDialog(
                            defaultLedgerCommandOpen,
                            stringResource(R.string.change_ledger_command),
                            true,
                            {
                                output = runLedgerCommand(applicationContext, " " + "-f " + preferencesViewModel.fileUri
                                + " --price-db " + preferencesViewModel.priceFileUri + " " + newLedgerCommand)
                                preferencesViewModel.storeLedgerCommand(newLedgerCommand)
                            },
                            { defaultLedgerCommandOpen = false },
                        ) {
                            OutlinedTextField(newLedgerCommand, { newLedgerCommand = it })

                        }
                        Text(output)


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
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
fun Bar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(R.string.statistics)) },
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
                    Text(stringResource(R.string.change))
                }
            },
        )
    }
}
