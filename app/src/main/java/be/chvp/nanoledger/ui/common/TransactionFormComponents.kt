package be.chvp.nanoledger.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import be.chvp.nanoledger.ui.util.Quadruple
import kotlinx.coroutines.launch

val TRANSACTION_INDEX_KEY = "transaction_index"

@Composable
fun TransactionForm(
    viewModel: TransactionFormViewModel,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val latestError by viewModel.latestError.observeAsState()
    val showMessage = stringResource(R.string.show)
    var openErrorDialog by rememberSaveable { mutableStateOf(false) }

    val errorMessage = stringResource(R.string.error_writing_file)
    var errorDialogMessage by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(latestError) {
        val error = latestError?.get()
        if (error != null) {
            Log.e("be.chvp.nanoledger", "Exception while writing file", error)
            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        actionLabel = showMessage,
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    openErrorDialog = true
                    errorDialogMessage = error.stackTraceToString()
                }
            }
        }
    }

    val latestMismatch by viewModel.latestMismatch.observeAsState()
    val mismatchMessage = stringResource(R.string.mismatch_no_write)
    LaunchedEffect(latestMismatch) {
        val error = latestMismatch?.get()
        if (error != null) {
            Toast.makeText(
                context,
                mismatchMessage,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
        if (openErrorDialog) {
            AlertDialog(
                onDismissRequest = { openErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                        val clip: ClipData = ClipData.newPlainText("simple text", errorDialogMessage)
                        clipboard.setPrimaryClip(clip)
                    }) { Text(stringResource(R.string.copy)) }
                },
                title = { Text(stringResource(R.string.error)) },
                text = { Text(errorDialogMessage) },
                dismissButton = {
                    TextButton(onClick = { openErrorDialog = false }) { Text(stringResource(R.string.dismiss)) }
                },
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                DateSelector(viewModel, Modifier.weight(0.3f).padding(start = 4.dp, end = 2.dp).fillMaxWidth())
                StatusSelector(viewModel, Modifier.weight(0.12f).padding(horizontal = 2.dp).fillMaxWidth())
                PayeeSelector(viewModel, Modifier.weight(0.58f).padding(start = 2.dp, end = 4.dp).fillMaxWidth())
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                NoteSelector(
                    viewModel,
                    Modifier.weight(1f).padding(horizontal = 4.dp).fillMaxWidth(),
                )
            }
            val postings by viewModel.postings.observeAsState()
            postings?.forEachIndexed { i, posting ->
                val isNote = posting.first == "" && posting.third == "" && posting.fourth != ""
                // do not show notes rows in the UI
                if (!isNote) {
                    val showAmountHint = posting.first == "" && posting.third == ""
                    PostingRow(i, posting, showAmountHint, viewModel)
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val date by viewModel.date.observeAsState()
    val formattedDate by viewModel.formattedDate.observeAsState()
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = formattedDate ?: "",
        readOnly = true,
        singleLine = true,
        onValueChange = {},
        label = { Text(stringResource(R.string.date)) },
        colors =
            ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        modifier =
            modifier.onFocusChanged {
                if (it.isFocused) {
                    dateDialogOpen = true
                }
            },
    )
    if (dateDialogOpen) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.getTime())
        DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                    dateDialogOpen = false
                    focusManager.clearFocus()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun StatusSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val status by viewModel.status.observeAsState()
    val options = listOf(" ", "!", "*")
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = (status ?: ""),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
            colors =
                ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(true),
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        viewModel.setStatus(it)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun PayeeSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val payee by viewModel.payee.observeAsState()
    val options by viewModel.possiblePayees.observeAsState()
    OutlinedLooseDropdown(
        options ?: emptyList(),
        payee ?: "",
        { viewModel.setPayee(it) },
        modifier,
    ) { Text(stringResource(R.string.payee)) }
}

@Composable
fun NoteSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val note by viewModel.note.observeAsState()
    val options by viewModel.possibleNotes.observeAsState()
    OutlinedLooseDropdown(
        options ?: emptyList(),
        note ?: "",
        { viewModel.setNote(it) },
        modifier,
    ) { Text(stringResource(R.string.note)) }
}

@Composable
fun PostingRow(
    index: Int,
    posting: Quadruple<String, String, String, String>,
    showAmountHint: Boolean,
    viewModel: TransactionFormViewModel,
) {
    val currencyBeforeAmount by viewModel.currencyBeforeAmount.observeAsState()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 2.dp)) {
        AccountSelector(
            index = index,
            value = posting.first,
            viewModel,
            modifier = Modifier.weight(2.2f).padding(horizontal = 2.dp),
        )
        if (currencyBeforeAmount ?: true) {
            CurrencyField(index, posting, viewModel, Modifier.weight(0.95f).padding(horizontal = 2.dp))
            AmountField(
                index,
                posting,
                showAmountHint,
                viewModel,
                Modifier.weight(1.25f).padding(horizontal = 2.dp),
            )
        } else {
            AmountField(
                index,
                posting,
                showAmountHint,
                viewModel,
                Modifier.weight(1.25f).padding(horizontal = 2.dp),
            )
            CurrencyField(index, posting, viewModel, Modifier.weight(0.95f).padding(horizontal = 2.dp))
        }
    }
}

@Composable
fun CurrencyField(
    index: Int,
    posting: Quadruple<String, String, String, String>,
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = posting.second,
        onValueChange = { viewModel.setCurrency(index, it) },
        singleLine = true,
        modifier = modifier,
        colors =
            ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
    )
}

@Composable
fun AmountField(
    index: Int,
    posting: Quadruple<String, String, String, String>,
    showAmountHint: Boolean,
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val unbalancedAmount by viewModel.unbalancedAmount.observeAsState()
    TextField(
        value = posting.third,
        onValueChange = { viewModel.setAmount(index, it) },
        singleLine = true,
        colors =
            ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        placeholder = {
            if (showAmountHint && unbalancedAmount != null) {
                Text(
                    unbalancedAmount!!,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                )
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
    )
}

@Composable
fun AccountSelector(
    index: Int,
    value: String,
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val options by viewModel.accounts.observeAsState()
    val filteredOptions = options?.filter { it.contains(value, ignoreCase = true) } ?: emptyList()
    LooseDropdown(filteredOptions, value, { viewModel.setAccount(index, it) }, modifier)
}

@Composable
fun OutlinedLooseDropdown(
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
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
            label = content,
            modifier =
                Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth().onFocusChanged {
                    if (!it.hasFocus) {
                        expanded = false
                    }
                },
            colors =
                ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
        )
        if (shouldShowDropdown(options, value)) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true),
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onValueChange(it)
                            focusManager.clearFocus()
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
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
    content: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
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
            modifier =
                Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth().onFocusChanged {
                    if (!it.hasFocus) {
                        expanded = false
                    }
                },
            label = content,
            colors =
                ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
        )
        if (shouldShowDropdown(options, value)) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true),
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onValueChange(it)
                            focusManager.clearFocus()
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

fun shouldShowDropdown(
    options: List<String>,
    currentValue: String,
): Boolean {
    return options.size > 1 || (options.size == 1 && options[0] != currentValue)
}
