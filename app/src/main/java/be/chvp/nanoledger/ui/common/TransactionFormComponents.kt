package be.chvp.nanoledger.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.CostType
import be.chvp.nanoledger.data.Posting
import kotlinx.coroutines.launch

const val TRANSACTION_INDEX_KEY = "transaction_index"

@Composable
fun TransactionForm(
    viewModel: TransactionFormViewModel,
    contentPadding: PaddingValues,
    bottomOffset: Dp,
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
            Toast
                .makeText(
                    context,
                    mismatchMessage,
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    Box(modifier = Modifier
        .padding(contentPadding)
        .fillMaxSize()) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 2.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            with(LocalDensity.current) {
                FlowRow(
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.Bottom
                ) {
                    DateSelector(viewModel, Modifier
                        .weight(0.25f)
                        .width((8 * 16).sp.toDp()))
                    StatusSelector(viewModel, Modifier.width((3 * 16).sp.toDp()))
                    CodeField(viewModel, Modifier
                        .weight(0.5f)
                        .width((16 * 16).sp.toDp()))
                    PayeeSelector(viewModel, Modifier
                        .weight(0.5f)
                        .width((16 * 16).sp.toDp()))
                    NoteSelector(viewModel, Modifier
                        .weight(0.75f)
                        .width((16 * 16).sp.toDp()))
                }
                val postings by viewModel.postings.observeAsState()
                postings?.forEachIndexed { i, posting ->
                    HorizontalDivider(Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp))
                    PostingRow(i, posting, posting.isEmpty(), viewModel)
                }
            }
            Box(Modifier
                .height(bottomOffset)
                .fillMaxWidth())
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
        label = { Text(stringResource(R.string.date), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.time)
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
    if (status != null) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier,
        ) {
            OutlinedTextField(
                value = (status ?: ""),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
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
}

@Composable
fun CodeField(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val code by viewModel.code.observeAsState()
    if (code != null) {
        OutlinedTextField(
            (code ?: ""),
            { viewModel.setCode(it) },
            modifier,
            label = { Text(stringResource(R.string.code), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
    }
}

@Composable
fun PayeeSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val payee by viewModel.payee.observeAsState()
    val options by viewModel.possiblePayees.observeAsState()
    if (payee != null) {
        OutlinedLooseDropdown(
            options ?: emptyList(),
            payee ?: "",
            { viewModel.setPayee(it) },
            modifier,
        ) { Text(stringResource(R.string.payee), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun NoteSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val note by viewModel.note.observeAsState()
    val options by viewModel.possibleNotes.observeAsState()
    if (note != null) {
        OutlinedLooseDropdown(
            options ?: emptyList(),
            note ?: "",
            { viewModel.setNote(it) },
            modifier,
        ) { Text(stringResource(R.string.note), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun PostingRow(
    index: Int,
    posting: Posting,
    showAmountHint: Boolean,
    viewModel: TransactionFormViewModel,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        itemVerticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        with(LocalDensity.current) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (posting.isComment()) {
                    CommentField(posting.comment ?: "", index, viewModel, Modifier.weight(1.0f))
                } else {
                    AccountSelector(index, posting.account ?: "", viewModel, Modifier.weight(1.0f))
                }
                FieldSelector(viewModel, index, posting)
                IconButton(onClick = { viewModel.removePosting(index) }) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = stringResource(R.string.remove_posting))
                }
            }
            if (posting.amount != null) {
                CurrencyAndAmountFields(
                    viewModel,
                    posting.amount.currency,
                    posting.amount.quantity,
                    showAmountHint,
                    Modifier.weight(1.0f),
                    saveCurrency = { viewModel.setCurrency(index, it) },
                    saveAmount = { viewModel.setAmount(index, it) },
                )
                if (posting.cost != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .width((20 * 16).sp.toDp())
                            .weight(1.0f)
                    ) {
                        CostTypeSelector(posting.cost.type) { viewModel.setCostType(index, it) }
                        CurrencyAndAmountFields(
                            viewModel,
                            posting.cost.amount.currency,
                            posting.cost.amount.quantity,
                            false,
                            Modifier
                                .weight(1.0f)
                                .padding(start = 4.dp),
                            saveCurrency = { viewModel.setCostCurrency(index, it) },
                            saveAmount = { viewModel.setCostAmount(index, it) },
                        )
                    }
                }
            }
            if (posting.assertion != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width((20 * 16).sp.toDp())
                        .weight(1.0f)
                ) {
                    Text("=", modifier = Modifier.padding(horizontal = 4.dp))
                    CurrencyAndAmountFields(
                        viewModel,
                        posting.assertion.currency,
                        posting.assertion.quantity,
                        false,
                        Modifier
                            .weight(1.0f)
                            .padding(start = 4.dp),
                        saveCurrency = { viewModel.setAssertionCurrency(index, it) },
                        saveAmount = { viewModel.setAssertionAmount(index, it) },
                    )
                }
                if (posting.assertionCost != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .width((20 * 16).sp.toDp())
                            .weight(1.0f)
                    ) {
                        CostTypeSelector(posting.assertionCost.type) {
                            viewModel.setAssertionCostType(
                                index,
                                it
                            )
                        }
                        CurrencyAndAmountFields(
                            viewModel,
                            posting.assertionCost.amount.currency,
                            posting.assertionCost.amount.quantity,
                            false,
                            Modifier
                                .weight(1.0f)
                                .padding(start = 4.dp),
                            saveCurrency = { viewModel.setAssertionCostCurrency(index, it) },
                            saveAmount = { viewModel.setAssertionCostAmount(index, it) },
                        )
                    }
                }
            }
            if (!posting.isComment() && posting.comment != null) {
                CommentField(posting.comment, index, viewModel, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun FieldSelector(viewModel: TransactionFormViewModel, index: Int, posting: Posting) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = stringResource(R.string.change_fields)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (!posting.isComment()) R.string.remove_account else R.string.add_account
                        )
                    )
                },
                onClick = {
                    viewModel.toggleAccount(index, posting.account == null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.amount != null) R.string.remove_amount else R.string.add_amount
                        )
                    )
                },
                onClick = {
                    viewModel.toggleAmount(index, posting.amount == null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.cost != null) R.string.remove_cost else R.string.add_cost
                        )
                    )
                },
                onClick = {
                    viewModel.toggleCost(index, posting.cost == null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.assertion != null) R.string.remove_assertion else R.string.add_assertion
                        )
                    )
                },
                onClick = {
                    viewModel.toggleAssertion(index, posting.assertion == null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.assertionCost != null) R.string.remove_assertion_cost else R.string.add_assertion_cost
                        )
                    )
                },
                onClick = {
                    viewModel.toggleAssertionCost(index, posting.assertionCost == null)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.comment != null) R.string.remove_comment else R.string.add_comment
                        )
                    )
                },
                onClick = {
                    viewModel.toggleComment(index, posting.comment == null)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun CommentField(
    comment: String,
    index: Int,
    viewModel: TransactionFormViewModel,
    modifier: Modifier,
) {
    OutlinedTextField(
        comment,
        onValueChange = { viewModel.setComment(index, it) },
        singleLine = true,
        label = { Text(stringResource(R.string.comment), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier
    )
}

@Composable
fun CostTypeSelector(
    costType: CostType,
    save: (newCostType: CostType) -> Unit
) {
    val options = listOf(CostType.UNIT, CostType.TOTAL)
    var expanded by rememberSaveable { mutableStateOf(false) }
    with(LocalDensity.current) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width((5 * 16).sp.toDp())
        ) {
            OutlinedTextField(
                value = costType.repr,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
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
                        text = { Text(it.repr) },
                        onClick = {
                            save(it)
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
fun CurrencyAndAmountFields(
    viewModel: TransactionFormViewModel,
    currency: String,
    quantity: String,
    showAmountHint: Boolean,
    modifier: Modifier,
    saveCurrency: (newCurrencyString: String) -> Unit, saveAmount: (newAmountString: String) -> Unit
) {
    val currencyBeforeAmount by viewModel.currencyBeforeAmount.observeAsState()

    with(LocalDensity.current) {
        Row(modifier = modifier.width((15 * 16).sp.toDp()), verticalAlignment = Alignment.Bottom) {
            if (currencyBeforeAmount ?: true) {
                CurrencyField(currency, Modifier.padding(end = 4.dp)) { saveCurrency(it) }
            }

            AmountField(quantity, showAmountHint, viewModel, Modifier.weight(1f)) {
                saveAmount(it)
            }

            if (!(currencyBeforeAmount ?: true)) {
                CurrencyField(currency, Modifier.padding(start = 4.dp)) {
                    saveCurrency(it)
                }
            }
        }
    }
}

@Composable
fun CurrencyField(
    currency: String,
    modifier: Modifier = Modifier,
    save: (newCurrencyString: String) -> Unit,
) {
    with(LocalDensity.current) {
        OutlinedTextField(
            value = currency,
            onValueChange = { save(it) },
            singleLine = true,
            modifier = modifier.width((6 * 16).sp.toDp()),
            colors =
                ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        )
    }
}

@Composable
fun AmountField(
    quantity: String,
    showAmountHint: Boolean,
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
    save: (newAmountString: String) -> Unit,
) {
    val unbalancedAmount by viewModel.unbalancedAmount.observeAsState()
    OutlinedTextField(
        value = quantity,
        onValueChange = { save(it) },
        singleLine = true,
        colors =
            ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        label = {
            if (showAmountHint && (unbalancedAmount ?: "") != "") {
                Text(
                    unbalancedAmount!!,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(stringResource(R.string.amount), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
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
    OutlinedLooseDropdown(filteredOptions, value, { viewModel.setAccount(index, it) }, modifier) {
        Text(stringResource(R.string.account), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
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
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth().onFocusChanged {
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

fun shouldShowDropdown(
    options: List<String>,
    currentValue: String,
): Boolean = options.size > 1 || (options.size == 1 && options[0] != currentValue)
