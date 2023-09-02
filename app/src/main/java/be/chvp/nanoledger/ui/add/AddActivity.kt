package be.chvp.nanoledger.ui.add

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddActivity() : ComponentActivity() {
    private val addViewModel: AddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            NanoLedgerTheme {
                Scaffold(
                    topBar = { Bar() },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            addViewModel.append() {
                                scope.launch(Main) { finish() }
                            }
                        }) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = stringResource(R.string.save)
                            )
                        }
                    }
                ) { contentPadding ->
                    Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp)
                            ) {
                                DateSelector(
                                    modifier = Modifier
                                        .weight(0.3f)
                                        .padding(start = 4.dp, end = 2.dp)
                                        .fillMaxWidth()
                                )
                                StatusSelector(
                                    modifier = Modifier
                                        .weight(0.15f)
                                        .padding(horizontal = 2.dp)
                                        .fillMaxWidth()
                                )
                                PayeeSelector(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .padding(start = 2.dp, end = 4.dp)
                                        .fillMaxWidth()
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                NoteSelector(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 2.dp, end = 4.dp)
                                        .fillMaxWidth()
                                )
                            }
                            val postings by addViewModel.postings.observeAsState()
                            postings?.forEachIndexed { i, posting ->
                                PostingRow(index = i, posting = posting)
                            }
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
        title = { Text(stringResource(R.string.add_transaction)) },
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
fun DateSelector(addViewModel: AddViewModel = viewModel(), modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val date by addViewModel.date.observeAsState()
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = date?.format(DateTimeFormatter.ISO_DATE) ?: "",
        readOnly = true,
        singleLine = true,
        onValueChange = {},
        label = { Text(stringResource(R.string.date)) },
        trailingIcon = {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = stringResource(R.string.calendar)
            )
        },
        modifier = modifier.onFocusChanged {
            if (it.isFocused) { dateDialogOpen = true }
        }
    )
    if (dateDialogOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                ?.atStartOfDay()
                ?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { addViewModel.setDate(it) }
                    dateDialogOpen = false
                    focusManager.clearFocus()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun StatusSelector(addViewModel: AddViewModel = viewModel(), modifier: Modifier = Modifier) {
    val status by addViewModel.status.observeAsState()
    val options = listOf(" ", "*", "!")
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = (status ?: ""),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.status)) },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(true)
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        addViewModel.setStatus(it)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun PayeeSelector(addViewModel: AddViewModel = viewModel(), modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val payee by addViewModel.payee.observeAsState()
    val options by addViewModel.possiblePayees.observeAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = (payee ?: ""),
            onValueChange = {
                addViewModel.setPayee(it)
                expanded = true
            },
            label = { Text(stringResource(R.string.payee)) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        if (options?.isNotEmpty() ?: false) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(false)
            ) {
                options?.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            addViewModel.setPayee(it)
                            focusManager.clearFocus()
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
fun NoteSelector(addViewModel: AddViewModel = viewModel(), modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val note by addViewModel.note.observeAsState()
    val options by addViewModel.possibleNotes.observeAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = (note ?: ""),
            onValueChange = {
                addViewModel.setNote(it)
                expanded = true
            },
            label = { Text(stringResource(R.string.note)) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        if (options?.isNotEmpty() ?: false) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(false)
            ) {
                options?.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            addViewModel.setNote(it)
                            focusManager.clearFocus()
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
fun PostingRow(
    index: Int,
    posting: Triple<String, String, String>,
    addViewModel: AddViewModel = viewModel()
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        AccountSelector(
            index = index,
            value = posting.first,
            modifier = Modifier.weight(0.65f).padding(start = 4.dp, end = 2.dp)
        )
        TextField(
            value = posting.second,
            onValueChange = { addViewModel.setCurrency(index, it) },
            singleLine = true,
            modifier = Modifier.weight(0.1f).padding(horizontal = 2.dp)
        )
        TextField(
            value = posting.third,
            onValueChange = { addViewModel.setAmount(index, it) },
            singleLine = true,
            modifier = Modifier.weight(0.25f).padding(start = 2.dp, end = 4.dp)
        )
    }
}

@Composable
fun AccountSelector(
    index: Int,
    value: String,
    addViewModel: AddViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val options by addViewModel.accounts.observeAsState()
    val filteredOptions = options?.filter { it.contains(value, ignoreCase = true) } ?: emptyList()
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = value,
            onValueChange = {
                addViewModel.setAccount(index, it)
                expanded = true
            },
            singleLine = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true)
            ) {
                filteredOptions.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            addViewModel.setAccount(index, it)
                            focusManager.clearFocus()
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
