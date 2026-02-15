package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.Cost
import be.chvp.nanoledger.data.CostType
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction

const val DATE_PART = "((\\d{4}[-/.])?\\d{1,2}[-/.]\\d{1,2}(=(\\d{4}[-/.])?\\d{1,2}[-/.]\\d{1,2})?)"
val HEADER_REGEX = Regex("^$DATE_PART[ \t]*([*!])?([ \t]*\\(([^)]*)\\)[ \t]*)?([^|]*)(\\|(.*))?$")
val POSTING_REGEX = Regex("^[ \t]+\\S.*$")

fun extractTransactions(lines: List<String>): List<Transaction> {
    val result = ArrayList<Transaction>()
    var i = 0
    while (i < lines.size) {
        val match = HEADER_REGEX.find(lines[i])
        i += 1
        if (match != null) {
            val firstLine = i - 1
            var lastLine = i - 1
            val groups = match.groups
            val date = groups[1]!!.value
            val status = groups[5]?.value
            val code = groups[7]?.value?.trim()
            var payee: String? = groups[8]!!.value.trim()
            var note = groups[10]?.value?.trim()

            if (note == null) {
                note = payee
                payee = null
            }

            val postings = ArrayList<Posting>()
            while (i < lines.size && POSTING_REGEX.find(lines[i]) != null) {
                val posting = extractPosting(lines[i])
                lastLine = i
                postings.add(posting)
                i += 1
            }
            result.add(Transaction(firstLine, lastLine, date, status, code, payee, note, postings))
        }
    }
    return result
}

val COMMENT_REGEX = Regex("[ \\t]*;.*$")
val POSTING_SPLIT_REGEX = Regex("[ \\t]{2,}")

fun extractPosting(line: String): Posting {
    var account: String? = null
    var amount: Amount? = null
    var cost: Cost? = null
    var assertion: Amount? = null
    var assertionCost: Cost? = null
    var comment: String? = null

    val commentMatch = COMMENT_REGEX.find(line)
    if (commentMatch != null) {
        comment = commentMatch.value.trim().trimStart(';').trim()
    }

    val stripped = line.replace(COMMENT_REGEX, "").trim()
    if (stripped.isNotEmpty()) {
        val components = stripped.split(POSTING_SPLIT_REGEX, limit = 2)
        account = components[0]

        if (components.size > 1) {
            val fullAmountString = components[1].trim()
            if (fullAmountString.contains('=')) {
                val amountComponents = fullAmountString.split('=', limit = 2)
                val baseRes = extractAmountAndCost(amountComponents[0].trim())
                amount = baseRes.first
                cost = baseRes.second
                val assertionRes = extractAmountAndCost(amountComponents[1].trim())
                assertion = assertionRes.first
                assertionCost = assertionRes.second
            } else {
                val res = extractAmountAndCost(fullAmountString)
                amount = res.first
                cost = res.second
            }
        }
    }

    return Posting(account, amount, cost, assertion, assertionCost, comment)
}

val QUANTITY_AT_START_REGEX = Regex("^(-? *[0-9][0-9,.]*)(.*)")
val QUANTITY_AT_END_REGEX = Regex("(-? *[0-9][0-9,.]*)$")

val COST_SPLIT_REGEX = Regex("@@?")

fun extractAmountAndCost(string: String): Pair<Amount?, Cost?> {
    if (string.isEmpty()) {
        return Pair(null, null)
    }
    if (string.contains(COST_SPLIT_REGEX)) {
        val costType = if (string.contains("@@")) {
            CostType.TOTAL
        } else {
            CostType.UNIT
        }

        val (amountString, costString) = string.split(COST_SPLIT_REGEX, limit = 2)
        return Pair(extractAmount(amountString.trim()), Cost(extractAmount(costString.trim()), costType))
    } else {
        return Pair(extractAmount(string), null)
    }
}

fun extractAmount(string: String): Amount {
    val stripped = string.trim()

    if (stripped.isEmpty()) {
        return Amount("", "", string)
    }

    val matchForStart = QUANTITY_AT_START_REGEX.find(stripped)
    if (matchForStart != null) {
        val groups = matchForStart.groups
        val quantity = groups[1]!!.value.trim()
        val currency = groups[2]!!.value.trim()
        return Amount(quantity, currency, string)
    }
    val matchForEnd = QUANTITY_AT_END_REGEX.find(stripped)
    if (matchForEnd != null) {
        val quantity = matchForEnd.value.trim()
        val currency = stripped.replace(QUANTITY_AT_END_REGEX, "").trim()
        return Amount(quantity, currency, string)
    }

    return Amount("", "", string)
}
