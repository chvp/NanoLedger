package be.chvp.nanoledger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val fileUri by mainViewModel.fileUri.observeAsState()
            LaunchedEffect(fileUri) {
                mainViewModel.refresh()
            }
            NanoLedgerTheme {
                Scaffold(
                    topBar = { Bar() },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            // TODO(chvp): Add AddActivity and open it here
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.add)
                            )
                        }
                    }
                ) { contentPadding ->
                    val fileContents by mainViewModel.fileContents.observeAsState()
                    val isRefreshing by mainViewModel.isRefreshing.observeAsState()
                    val state = rememberPullRefreshState(
                        isRefreshing ?: false,
                        { mainViewModel.refresh() }
                    )
                    Box(modifier = Modifier.pullRefresh(state).padding(contentPadding)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(fileContents?.size ?: 0) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(
                                        8.dp,
                                        if (it == 0) 8.dp else 4.dp,
                                        8.dp,
                                        if (it == fileContents!!.size - 1) 8.dp else 4.dp
                                    )
                                ) {
                                    Text(
                                        fileContents!![fileContents!!.size - it - 1],
                                        modifier = Modifier.padding(8.dp),
                                        softWrap = false,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                        }
                        PullRefreshIndicator(
                            isRefreshing ?: false,
                            state,
                            Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Bar() {
        val context = LocalContext.current
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = {
                IconButton(onClick = {
                    context.startActivity(
                        Intent(context, PreferencesActivity::class.java)
                    )
                }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}
