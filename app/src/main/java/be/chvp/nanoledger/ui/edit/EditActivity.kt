package be.chvp.nanoledger.ui.edit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.common.TRANSACTION_INDEX_KEY
import be.chvp.nanoledger.ui.common.TransactionForm
import be.chvp.nanoledger.ui.main.MainActivity
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditActivity() : ComponentActivity() {
    private val editViewModel: EditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!getIntent().hasExtra(TRANSACTION_INDEX_KEY)) {
            Log.e("be.chvp.nanoledger", "Edit started without transaction index")
            finish()
        }
        val transactionIndex = getIntent().getIntExtra(TRANSACTION_INDEX_KEY, 0)
        editViewModel.loadTransactionFromIndex(transactionIndex)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val snackbarHostState = remember { SnackbarHostState() }

            BackHandler(enabled = true) {
                finish()
                startActivity(Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            val saving by editViewModel.saving.observeAsState()
            val valid by editViewModel.valid.observeAsState()
            val enabled = !(saving ?: true) && (valid ?: false)
            NanoLedgerTheme {
                Scaffold(
                    topBar = { Bar() },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (enabled) {
                                    editViewModel.save {
                                        scope.launch(Main) {
                                            finish()
                                            startActivity(
                                                Intent(context, MainActivity::class.java).setFlags(
                                                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                            containerColor =
                                if (enabled) {
                                    FloatingActionButtonDefaults.containerColor
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                        ) {
                            if (saving ?: true) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = stringResource(R.string.save),
                                )
                            }
                        }
                    },
                ) { contentPadding ->
                    TransactionForm(editViewModel, contentPadding, snackbarHostState)
                }
            }
        }
    }
}

@Composable
fun Bar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(R.string.edit_transaction)) },
        navigationIcon = {
            IconButton(onClick = {
                (context as Activity).apply {
                    startActivity(
                        Intent(context, MainActivity::class.java).setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP,
                        ),
                    )
                    finish()
                }
            }) {
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
