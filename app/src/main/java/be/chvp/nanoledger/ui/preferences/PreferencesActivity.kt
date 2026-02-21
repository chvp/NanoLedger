package be.chvp.nanoledger.ui.preferences

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreferencesActivity : ComponentActivity() {
    private val preferencesViewModel: PreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openFile =
            registerForActivityResult(OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    preferencesViewModel.storeFileUri(uri)
                }
            }
        setContent {
            val statusMap =
                mapOf(
                    " " to stringResource(R.string.status_unmarked),
                    "!" to stringResource(R.string.status_pending),
                    "*" to stringResource(R.string.status_cleared),
                )
            val currencyOrderMap =
                mapOf(
                    true to stringResource(R.string.currency_order_before),
                    false to stringResource(R.string.currency_order_after),
                )
            val currencySpacingMap =
                mapOf(
                    true to stringResource(R.string.currency_amount_spacing_on),
                    false to stringResource(R.string.currency_amount_spacing_off),
                )
            val separatorMap =
                mapOf(
                    "." to stringResource(R.string.separator_point),
                    "," to stringResource(R.string.separator_comma),
                )
            NanoLedgerTheme {
                Scaffold(topBar = { Bar() }, modifier = Modifier.imePadding()) { contentPadding ->
                    Column(
                        modifier =
                            Modifier
                                .padding(contentPadding)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        val fileUri by preferencesViewModel.fileUri.observeAsState()
                        Setting(
                            stringResource(R.string.file),
                            fileUri?.toString() ?: stringResource(R.string.select_file),
                        ) {
                            openFile.launch(arrayOf("*/*"))
                        }
                        HorizontalDivider()
                        val transactionDefaultElements by preferencesViewModel.transactionDefaultElements.observeAsState(emptyList())
                        var transactionDefaultElementsOpen by remember { mutableStateOf(false) }
                        val status by preferencesViewModel.transactionStatusPresentByDefault.observeAsState(true)
                        val code by preferencesViewModel.transactionCodePresentByDefault.observeAsState(true)
                        val payee by preferencesViewModel.transactionPayeePresentByDefault.observeAsState(true)
                        val note by preferencesViewModel.transactionNotePresentByDefault.observeAsState(true)
                        val currencies by preferencesViewModel.transactionCurrenciesPresentByDefault.observeAsState(true)
                        ExposedDropdownMenuBox(
                            expanded = transactionDefaultElementsOpen,
                            onExpandedChange = { transactionDefaultElementsOpen = !transactionDefaultElementsOpen },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Setting(
                                stringResource(R.string.default_transaction_fields),
                                transactionDefaultElements.map { stringResource(it) }.joinToString(", "),
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            ) { transactionDefaultElementsOpen = true }
                            ExposedDropdownMenu(
                                expanded = transactionDefaultElementsOpen,
                                onDismissRequest = { transactionDefaultElementsOpen = false },
                                modifier = Modifier.exposedDropdownSize(true)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (status) R.string.remove_status else R.string.add_status)) },
                                    onClick = {
                                        preferencesViewModel.storeTransactionStatusPresentByDefault(!status)
                                        transactionDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (code) R.string.remove_code else R.string.add_code)) },
                                    onClick = {
                                        preferencesViewModel.storeTransactionCodePresentByDefault(!code)
                                        transactionDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (payee) R.string.remove_payee else R.string.add_payee)) },
                                    onClick = {
                                        preferencesViewModel.storeTransactionPayeePresentByDefault(!payee)
                                        transactionDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (note) R.string.remove_note else R.string.add_note)) },
                                    onClick = {
                                        preferencesViewModel.storeTransactionNotePresentByDefault(!note)
                                        transactionDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (currencies) R.string.remove_currency else R.string.add_currency)) },
                                    onClick = {
                                        preferencesViewModel.storeTransactionCurrenciesPresentByDefault(!currencies)
                                        transactionDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                        HorizontalDivider()
                        val postingDefaultElements by preferencesViewModel.postingDefaultElements.observeAsState(emptyList())
                        var postingDefaultElementsOpen by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = postingDefaultElementsOpen,
                            onExpandedChange = { postingDefaultElementsOpen = !postingDefaultElementsOpen },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Setting(
                                stringResource(R.string.default_posting_fields),
                                postingDefaultElements.map { stringResource(it) }.joinToString(", ")
                            ) { postingDefaultElementsOpen = true }
                            ExposedDropdownMenu(
                                expanded = postingDefaultElementsOpen,
                                onDismissRequest = { postingDefaultElementsOpen = false },
                                modifier = Modifier.exposedDropdownSize(true)
                            ) {
                                val amount by preferencesViewModel.postingAmountPresentByDefault.observeAsState(true)
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (amount) R.string.remove_amount else R.string.add_amount)) },
                                    onClick = {
                                        preferencesViewModel.storePostingAmountPresentByDefault(!amount)
                                        postingDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                val cost by preferencesViewModel.postingCostPresentByDefault.observeAsState(true)
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (cost) R.string.remove_cost else R.string.add_cost)) },
                                    onClick = {
                                        preferencesViewModel.storePostingCostPresentByDefault(!cost)
                                        postingDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                val assertion by preferencesViewModel.postingAssertionPresentByDefault.observeAsState(true)
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (assertion) R.string.remove_assertion else R.string.add_assertion)) },
                                    onClick = {
                                        preferencesViewModel.storePostingAssertionPresentByDefault(!assertion)
                                        postingDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                val assertionCost by preferencesViewModel.postingAssertionCostPresentByDefault.observeAsState(true)
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (assertionCost) R.string.remove_assertion_cost else R.string.add_assertion_cost)) },
                                    onClick = {
                                        preferencesViewModel.storePostingAssertionCostPresentByDefault(!assertionCost)
                                        postingDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                val comment by preferencesViewModel.postingCommentPresentByDefault.observeAsState(true)
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (comment) R.string.remove_comment else R.string.add_comment)) },
                                    onClick = {
                                        preferencesViewModel.storePostingCommentPresentByDefault(!comment)
                                        postingDefaultElementsOpen = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                        HorizontalDivider()
                        val defaultCurrency by preferencesViewModel.defaultCurrency.observeAsState()
                        var newDefaultCurrency by remember { mutableStateOf(defaultCurrency ?: "") }
                        var defaultCurrencyOpen by remember { mutableStateOf(false) }
                        Setting(
                            stringResource(R.string.default_currency),
                            defaultCurrency ?: "€",
                        ) {
                            defaultCurrencyOpen = true
                        }
                        SettingDialog(
                            defaultCurrencyOpen,
                            stringResource(R.string.change_default_currency),
                            true,
                            { preferencesViewModel.storeDefaultCurrency(newDefaultCurrency) },
                            { defaultCurrencyOpen = false },
                        ) {
                            OutlinedTextField(newDefaultCurrency, { newDefaultCurrency = it })
                        }
                        HorizontalDivider()
                        val postingWidth by preferencesViewModel.postingWidth.observeAsState()
                        var newPostingWidth by remember { mutableStateOf("${postingWidth ?: 72}") }
                        var postingWidthOpen by remember { mutableStateOf(false) }
                        Setting(stringResource(R.string.posting_width), "${postingWidth ?: 72}") {
                            postingWidthOpen = true
                        }
                        SettingDialog(postingWidthOpen, stringResource(R.string.change_posting_width), true, {
                            preferencesViewModel.storePostingWidth(Integer.parseInt(newPostingWidth))
                        }, { postingWidthOpen = false }) {
                            OutlinedTextField(
                                newPostingWidth,
                                {
                                    if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                        newPostingWidth = it
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                        HorizontalDivider()
                        val defaultStatus by preferencesViewModel.defaultStatus.observeAsState()
                        var expandedStatus by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedStatus,
                            onExpandedChange = { expandedStatus = !expandedStatus },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Setting(
                                stringResource(R.string.default_status),
                                statusMap[defaultStatus ?: " "] ?: stringResource(
                                    R.string.status_unmarked,
                                ),
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            ) { expandedStatus = true }
                            ExposedDropdownMenu(
                                expanded = expandedStatus,
                                onDismissRequest = { expandedStatus = false },
                                modifier = Modifier.exposedDropdownSize(true),
                            ) {
                                statusMap.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.value) },
                                        onClick = {
                                            preferencesViewModel.storeDefaultStatus(it.key)
                                            expandedStatus = false
                                        },
                                        contentPadding =
                                            ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        val decimalSeparator by preferencesViewModel.decimalSeparator.observeAsState()
                        var expandedSeparator by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedSeparator,
                            onExpandedChange = { expandedSeparator = !expandedSeparator },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Setting(
                                stringResource(R.string.decimal_separator),
                                separatorMap[decimalSeparator ?: "."] ?: stringResource(
                                    R.string.separator_point,
                                ),
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            ) { expandedSeparator = true }
                            ExposedDropdownMenu(
                                expanded = expandedSeparator,
                                onDismissRequest = { expandedSeparator = false },
                                modifier = Modifier.exposedDropdownSize(true),
                            ) {
                                separatorMap.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.value) },
                                        onClick = {
                                            preferencesViewModel.storeDecimalSeparator(it.key)
                                            expandedSeparator = false
                                        },
                                        contentPadding =
                                            ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        val currencyBeforeAmount by
                            preferencesViewModel.currencyBeforeAmount.observeAsState()
                        var expandedCurrency by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedCurrency,
                            onExpandedChange = { expandedCurrency = !expandedCurrency },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Setting(
                                stringResource(R.string.currency_amount_order),
                                currencyOrderMap[currencyBeforeAmount ?: true] ?: stringResource(
                                    R.string.currency_order_before,
                                ),
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            ) { expandedCurrency = true }
                            ExposedDropdownMenu(
                                expanded = expandedCurrency,
                                onDismissRequest = { expandedCurrency = false },
                                modifier = Modifier.exposedDropdownSize(true),
                            ) {
                                currencyOrderMap.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.value) },
                                        onClick = {
                                            preferencesViewModel.storeCurrencyBeforeAmount(it.key)
                                            expandedCurrency = false
                                        },
                                        contentPadding =
                                            ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        val currencyAmountSpacing by preferencesViewModel.spacingBetweenCurrencyAndAmount.observeAsState()
                        var expandedCurrencySpacing by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedCurrencySpacing,
                            onExpandedChange = { expandedCurrencySpacing = !expandedCurrencySpacing },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Setting(
                                stringResource(R.string.currency_amount_spacing),
                                currencySpacingMap[currencyAmountSpacing ?: true] ?: stringResource(
                                    R.string.currency_amount_spacing_on,
                                ),
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            ) { expandedCurrencySpacing = true }
                            ExposedDropdownMenu(
                                expanded = expandedCurrencySpacing,
                                onDismissRequest = { expandedCurrencySpacing = false },
                                modifier = Modifier.exposedDropdownSize(true),
                            ) {
                                currencySpacingMap.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.value) },
                                        onClick = {
                                            preferencesViewModel.storeCurrencyAmountSpacing(it.key)
                                            expandedCurrencySpacing = false
                                        },
                                        contentPadding =
                                            ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
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
        title = { Text(stringResource(R.string.settings)) },
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
                    Text(stringResource(R.string.save))
                }
            },
        )
    }
}
