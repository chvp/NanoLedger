package be.chvp.nanoledger.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.add.AddActivity
import be.chvp.nanoledger.ui.common.TRANSACTION_INDEX_KEY
import be.chvp.nanoledger.ui.edit.EditActivity
import be.chvp.nanoledger.ui.preferences.PreferencesActivity
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val searching by mainViewModel.searching.observeAsState()
            val selected by mainViewModel.selectedIndex.observeAsState()

            val latestReadError by mainViewModel.latestReadError.observeAsState()
            val readErrorMessage = stringResource(R.string.error_reading_file)
            LaunchedEffect(latestReadError) {
                val error = latestReadError?.get()
                if (error != null) {
                    Log.e("be.chvp.nanoledger", "Exception while reading file", error)
                    Toast
                        .makeText(
                            context,
                            readErrorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val latestWriteError by mainViewModel.latestWriteError.observeAsState()
            val writeErrorMessage = stringResource(R.string.error_writing_file)
            LaunchedEffect(latestWriteError) {
                val error = latestWriteError?.get()
                if (error != null) {
                    Log.e("be.chvp.nanoledger", "Exception while writing file", error)
                    Toast
                        .makeText(
                            context,
                            writeErrorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val latestMismatch by mainViewModel.latestMismatch.observeAsState()
            val mismatchMessage = stringResource(R.string.mismatch_no_delete)
            LaunchedEffect(latestMismatch) {
                val error = latestMismatch?.get()
                if (error != null) {
                    Toast
                        .makeText(
                            context,
                            mismatchMessage,
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
                    topBar = {
                        if (selected != null) {
                            SelectionBar()
                        } else if (searching ?: false) {
                            SearchBar()
                        } else {
                            MainBar()
                        }
                    },
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
                    modifier = Modifier.imePadding(),
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
                                    Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(
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

@Composable
fun MainContent(
    contentPadding: PaddingValues,
    mainViewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val transactions by mainViewModel.filteredTransactions.observeAsState()
    val query by mainViewModel.query.observeAsState()
    val isRefreshing by mainViewModel.isRefreshing.observeAsState()
    val selected by mainViewModel.selectedIndex.observeAsState()
    PullToRefreshBox(isRefreshing = (isRefreshing ?: false), onRefresh = {
        mainViewModel.refresh()
    }, contentAlignment = Alignment.TopCenter, modifier = Modifier.padding(contentPadding)) {
        if ((transactions?.size ?: 0) > 0 || (isRefreshing ?: true)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions?.size ?: 0) {
                    val index = transactions!!.size - it - 1
                    val (originalIndex, transaction) = transactions!![index]
                    TransactionCard(
                        transaction,
                        originalIndex == selected,
                        { mainViewModel.toggleSelect(originalIndex) },
                        Modifier.fillMaxWidth().padding(
                            8.dp,
                            if (it == 0) 8.dp else 4.dp,
                            8.dp,
                            if (it == transactions!!.size - 1) 8.dp else 4.dp,
                        ),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    if (query.equals("")) {
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
                    } else {
                        Text(
                            stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainBar(mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = { mainViewModel.setSearching(true) }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
            }
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

@Composable
fun SelectionBar(mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val selected by mainViewModel.selectedIndex.observeAsState()
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    mainViewModel.toggleSelect(selected!!)
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stop_selection))
            }
        },
        title = { },
        actions = {
            IconButton(onClick = {
                val intent = Intent(context, AddActivity::class.java)
                intent.putExtra(TRANSACTION_INDEX_KEY, selected!!)
                mainViewModel.toggleSelect(selected!!)
                context.startActivity(intent)
            }) {
                Icon(painterResource(R.drawable.baseline_difference_24), contentDescription = stringResource(R.string.copy_and_edit))
            }
            IconButton(onClick = {
                val intent = Intent(context, EditActivity::class.java)
                intent.putExtra(TRANSACTION_INDEX_KEY, selected!!)
                mainViewModel.toggleSelect(selected!!)
                context.startActivity(intent)
            }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = { mainViewModel.deleteSelected() }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )

    BackHandler {
        mainViewModel.toggleSelect(selected!!)
    }
}

@Composable
fun SearchBar(mainViewModel: MainViewModel = viewModel()) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val query by mainViewModel.query.observeAsState()
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    mainViewModel.setSearching(false)
                    mainViewModel.setQuery("")
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stop_searching))
            }
        },
        title = {
            TextField(
                query ?: "",
                { mainViewModel.setQuery(it) },
                singleLine = true,
                placeholder = {
                    Text(
                        stringResource(R.string.search),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Normal,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedTextColor = LocalContentColor.current,
                        unfocusedTextColor = LocalContentColor.current,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = LocalContentColor.current,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusRequester.freeFocus()
                        },
                    ),
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    BackHandler {
        mainViewModel.setSearching(false)
        mainViewModel.setQuery("")
    }
}
