package be.chvp.nanoledger.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.ui.add.AddActivity
import be.chvp.nanoledger.ui.preferences.PreferencesActivity
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val latestError by mainViewModel.latestError.observeAsState()
            val errorMessage = stringResource(R.string.error_reading_file)
            LaunchedEffect(latestError) {
                val error = latestError?.get()
                if (error != null) {
                    Log.e("be.chvp.nanoledger", "Exception while reading file", error)
                    Toast.makeText(
                        context,
                        errorMessage,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }

            val fileUri by mainViewModel.fileUri.observeAsState()
            LaunchedEffect(fileUri) {
                mainViewModel.refresh()
            }
            NanoLedgerTheme {
                Scaffold(
                    topBar = { MainBar() },
                    floatingActionButton = {
                        if (fileUri != null) {
                            FloatingActionButton(onClick = {
                                startActivity(
                                    Intent(this, AddActivity::class.java),
                                )
                            }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add),
                                )
                            }
                        }
                    },
                ) { contentPadding ->
                    if (fileUri != null) {
                        MainContent(contentPadding)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(contentPadding),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                stringResource(R.string.no_file_yet),
                                style = MaterialTheme.typography.headlineLarge,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier.align(Alignment.CenterHorizontally).padding(
                                        horizontal = 16.dp,
                                    ),
                            )
                            Text(
                                stringResource(R.string.go_to_settings),
                                style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                        textDecoration = TextDecoration.Underline,
                                        color = MaterialTheme.colorScheme.primary,
                                    ),
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier.align(Alignment.CenterHorizontally).padding(
                                        horizontal = 16.dp,
                                    ).clickable {
                                        startActivity(
                                            Intent(this@MainActivity, PreferencesActivity::class.java),
                                        )
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

fun transactionHeader(t: Transaction): String {
    var res = t.date
    if (t.status != null) res += " ${t.status}"
    res += " ${t.payee}"
    if (t.note != null) res += " | ${t.note}"
    return res
}

@Composable
fun MainContent(
    contentPadding: PaddingValues,
    mainViewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val transactions by mainViewModel.transactions.observeAsState()
    val isRefreshing by mainViewModel.isRefreshing.observeAsState()
    val state = rememberPullToRefreshState()
    if (state.isRefreshing && !(isRefreshing ?: false)) {
        LaunchedEffect(true) {
            state.endRefresh()
        }
    } else if (!state.isRefreshing && (isRefreshing ?: false)) {
        LaunchedEffect(true) {
            state.startRefresh()
        }
    }
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            mainViewModel.refresh()
            // Due to the way compositing works, we cancel the refresh in the
            // first if (even if the first if is moved below this one, and then
            // we have no way to undo the cancel).
            state.startRefresh()
        }
    }
    Box(modifier = Modifier.nestedScroll(state.nestedScrollConnection).padding(contentPadding)) {
        if ((transactions?.size ?: 0) > 0 || (isRefreshing ?: true)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions?.size ?: 0) {
                    Card(
                        modifier =
                            Modifier.fillMaxWidth().padding(
                                8.dp,
                                if (it == 0) 8.dp else 4.dp,
                                8.dp,
                                if (it == transactions!!.size - 1) 8.dp else 4.dp,
                            ),
                    ) {
                        val tr = transactions!![transactions!!.size - it - 1]
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(
                                transactionHeader(tr),
                                softWrap = false,
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                overflow = TextOverflow.Ellipsis,
                            )
                            for (p in tr.postings) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "  ${p.account}",
                                        softWrap = false,
                                        style =
                                            MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                            ),
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        p.amount ?: "",
                                        softWrap = false,
                                        style =
                                            MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                            ),
                                        modifier = Modifier.padding(start = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Text(
                        stringResource(R.string.no_transactions_yet),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        stringResource(R.string.create_transaction),
                        style =
                            MaterialTheme.typography.headlineLarge.copy(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier.padding(horizontal = 16.dp).clickable {
                                context.startActivity(
                                    Intent(context, AddActivity::class.java),
                                )
                            },
                    )
                }
            }
        }
        PullToRefreshContainer(state = state, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun MainBar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = {
                context.startActivity(Intent(context, PreferencesActivity::class.java))
            }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}
