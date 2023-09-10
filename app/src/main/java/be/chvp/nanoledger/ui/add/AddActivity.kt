package be.chvp.nanoledger.ui.add

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
            val context = LocalContext.current
            val latestError by addViewModel.latestError.observeAsState()
            val errorMessage = stringResource(R.string.error_writing_file)
            LaunchedEffect(latestError) {
                val error = latestError?.get()
                if (error != null) {
                    Log.e("be.chvp.nanoledger", "Exception while writing file", error)
                    Toast.makeText(
                        context,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            val scope = rememberCoroutineScope()
            val saving by addViewModel.saving.observeAsState()
            val valid by addViewModel.valid.observeAsState()
            val enabled = !(saving ?: true) && (valid ?: false)
            NanoLedgerTheme {
                Scaffold(
                    topBar = { Bar() },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (enabled) {
                                    addViewModel.append() {
                                        scope.launch(Main) { finish() }
                                    }
                                }
                            },
                            containerColor = if (enabled) {
                                FloatingActionButtonDefaults.containerColor
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = stringResource(R.string.save)
                            )
                        }
                    }
                ) { contentPadding ->
                    Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                DateSelector(
                                    modifier = Modifier
                                        .weight(0.3f)
                                        .padding(start = 4.dp, end = 2.dp)
                                        .fillMaxWidth()
                                )
                                StatusSelector(
                                    modifier = Modifier
                                        .weight(0.12f)
                                        .padding(horizontal = 2.dp)
                                        .fillMaxWidth()
                                )
                                PayeeSelector(
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .padding(start = 2.dp, end = 4.dp)
                                        .fillMaxWidth()
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                NoteSelector(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .fillMaxWidth()
                                )
                            }
                            val postings by addViewModel.postings.observeAsState()
                            var encounteredEmptyAmount = false
                            postings?.forEachIndexed { i, posting ->
                                val firstEmpty =
                                    encounteredEmptyAmount == false && posting.third == ""
                                encounteredEmptyAmount = encounteredEmptyAmount || firstEmpty
                                PostingRow(
                                    index = i,
                                    posting = posting,
                                    firstEmptyAmount = firstEmpty
                                )
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
fun DateSelector(modifier: Modifier = Modifier, addViewModel: AddViewModel = viewModel()) {
    val focusManager = LocalFocusManager.current
    val date by addViewModel.date.observeAsState()
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = date?.format(DateTimeFormatter.ISO_DATE) ?: "",
        readOnly = true,
        singleLine = true,
        onValueChange = {},
        label = { Text(stringResource(R.string.date)) },
        colors = ExposedDropdownMenuDefaults.textFieldColors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
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
fun StatusSelector(modifier: Modifier = Modifier, addViewModel: AddViewModel = viewModel()) {
    val status by addViewModel.status.observeAsState()
    val options = listOf(" ", "!", "*")
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
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
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
fun PayeeSelector(modifier: Modifier = Modifier, addViewModel: AddViewModel = viewModel()) {
    val payee by addViewModel.payee.observeAsState()
    val options by addViewModel.possiblePayees.observeAsState()
    OutlinedLooseDropdown(
        options ?: emptyList(),
        payee ?: "",
        { addViewModel.setPayee(it) },
        modifier
    ) { Text(stringResource(R.string.payee)) }
}

@Composable
fun NoteSelector(modifier: Modifier = Modifier, addViewModel: AddViewModel = viewModel()) {
    val note by addViewModel.note.observeAsState()
    val options by addViewModel.possibleNotes.observeAsState()
    OutlinedLooseDropdown(
        options ?: emptyList(),
        note ?: "",
        { addViewModel.setNote(it) },
        modifier
    ) { Text(stringResource(R.string.note)) }
}

@Composable
fun PostingRow(
    index: Int,
    posting: Triple<String, String, String>,
    firstEmptyAmount: Boolean,
    addViewModel: AddViewModel = viewModel()
) {
    val currencyBeforeAmount by addViewModel.currencyBeforeAmount.observeAsState()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        AccountSelector(
            index = index,
            value = posting.first,
            modifier = Modifier.weight(0.57f).padding(start = 4.dp, end = 2.dp)
        )
        if (currencyBeforeAmount ?: true) {
            CurrencyField(index, posting, Modifier.weight(0.18f).padding(horizontal = 2.dp))
            AmountField(
                index,
                posting,
                firstEmptyAmount,
                Modifier.weight(0.25f).padding(start = 2.dp, end = 4.dp)
            )
        } else {
            AmountField(
                index,
                posting,
                firstEmptyAmount,
                Modifier.weight(0.25f).padding(horizontal = 2.dp)
            )
            CurrencyField(index, posting, Modifier.weight(0.18f).padding(start = 2.dp, end = 4.dp))
        }
    }
}

@Composable
fun CurrencyField(
    index: Int,
    posting: Triple<String, String, String>,
    modifier: Modifier = Modifier,
    addViewModel: AddViewModel = viewModel()
) {
    TextField(
        value = posting.second,
        onValueChange = { addViewModel.setCurrency(index, it) },
        singleLine = true,
        modifier = modifier,
        colors = ExposedDropdownMenuDefaults.textFieldColors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
    )
}

@Composable
fun AmountField(
    index: Int,
    posting: Triple<String, String, String>,
    firstEmptyAmount: Boolean,
    modifier: Modifier = Modifier,
    addViewModel: AddViewModel = viewModel()
) {
    val unbalancedAmount by addViewModel.unbalancedAmount.observeAsState()
    TextField(
        value = posting.third,
        onValueChange = { addViewModel.setAmount(index, it) },
        singleLine = true,
        colors = ExposedDropdownMenuDefaults.textFieldColors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        placeholder = {
            if (firstEmptyAmount && unbalancedAmount != null) {
                Text(
                    unbalancedAmount!!,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
    )
}

@Composable
fun AccountSelector(
    index: Int,
    value: String,
    modifier: Modifier = Modifier,
    addViewModel: AddViewModel = viewModel()
) {
    val options by addViewModel.accounts.observeAsState()
    val filteredOptions = options?.filter { it.contains(value, ignoreCase = true) } ?: emptyList()
    LooseDropdown(filteredOptions, value, { addViewModel.setAccount(index, it) }, modifier)
}

@Composable
fun OutlinedLooseDropdown(
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (it.length > value.length) {
                    expanded = true
                }
                onValueChange(it)
            },
            singleLine = true,
            label = label,
            modifier = Modifier.menuAnchor().fillMaxWidth().onFocusChanged {
                if (!it.hasFocus) { expanded = false }
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        if (shouldShowDropdown(options, value)) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true)
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onValueChange(it)
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
fun LooseDropdown(
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = value,
            onValueChange = {
                if (it.length > value.length) {
                    expanded = true
                }
                onValueChange(it)
            },
            singleLine = true,
            modifier = Modifier.menuAnchor().fillMaxWidth().onFocusChanged {
                if (!it.hasFocus) { expanded = false }
            },
            label = label,
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        if (shouldShowDropdown(options, value)) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true)
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onValueChange(it)
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

fun shouldShowDropdown(options: List<String>, currentValue: String): Boolean {
    return options.size > 1 || (options.size == 1 && options[0] != currentValue)
}
