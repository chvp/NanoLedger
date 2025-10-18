package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Amount
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
            val payee = groups[8]!!.value.trim()
            val note = groups[10]?.value?.trim()

            val postings = ArrayList<Posting>()
            while (i < lines.size && POSTING_REGEX.find(lines[i]) != null) {
                val posting = extractPosting(lines[i])
                if (posting != null) {
                    lastLine = i
                    postings.add(posting)
                }
                i += 1
            }
            result.add(Transaction(firstLine, lastLine, date, status, code, payee, note, postings))
        }
    }
    return result
}

val COMMENT_REGEX = Regex("[ \\t]*;.*$")
val POSTING_SPLIT_REGEX = Regex("[ \\t]{2,}")

fun extractPosting(line: String): Posting? {
    // the three components of a posting
    var account: String? = null
    var amount: Amount? = null
    var note: String? = null

    // check if we have a note in the posting
    val commentMatch = COMMENT_REGEX.find(line)
    if (commentMatch != null) {
        note = commentMatch.value
    }

    val stripped = line.replace(COMMENT_REGEX, "").trim()
    // if we have more content than just a note, continue parsing
    if (stripped.isNotEmpty()) {
        val components = stripped.split(POSTING_SPLIT_REGEX, limit = 2)
        account = components[0]

        // if we have more than the account, is the amount of the posting, parse it
        if (components.size > 1) {
            amount = extractAmount(components[1].trim())
        }
    }

    return Posting(account, amount, note)
}

val ASSERTION_REGEX = Regex("=.*$")
val COST_REGEX = Regex("@.*$")
val QUANTITY_AT_START_REGEX = Regex("^(-? *[0-9][0-9,.]*)(.*)")
val QUANTITY_AT_END_REGEX = Regex("(-? *[0-9][0-9,.]*)$")

fun extractAmount(string: String): Amount {
    val stripped =
        string
            .trim()
            .replace(ASSERTION_REGEX, "")
            .trim()
            .replace(COST_REGEX, "")
            .trim()

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
