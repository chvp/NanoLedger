package be.chvp.nanoledger.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import be.chvp.nanoledger.data.Transaction

fun transactionHeader(t: Transaction): String {
    var res = t.date
    if (t.status != null) res += " ${t.status}"
    if (t.code != null) res += " (${t.code})"
    res += " ${t.payee}"
    if (t.note != null) res += " | ${t.note}"
    return res
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            if (selected) {
                CardDefaults.outlinedCardColors()
            } else {
                CardDefaults.cardColors()
            },
        elevation =
            if (selected) {
                CardDefaults.outlinedCardElevation()
            } else {
                CardDefaults.cardElevation()
            },
        border =
            if (selected) {
                CardDefaults.outlinedCardBorder(true)
            } else {
                null
            },
        modifier = modifier,
    ) {
        Box(modifier = Modifier.clickable { onClick() }) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    transactionHeader(transaction),
                    softWrap = false,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    overflow = TextOverflow.Ellipsis,
                )
                for (p in transaction.postings) {
                    if (p.isComment()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "  ; ${p.comment!!}",
                                softWrap = false,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "  ${p.account}",
                                softWrap = false,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                p.fullAmountDisplayString(),
                                softWrap = false,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
